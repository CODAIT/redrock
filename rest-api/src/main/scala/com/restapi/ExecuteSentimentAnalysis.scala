/**
 * (C) Copyright IBM Corp. 2015, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.restapi

import org.slf4j.LoggerFactory
import play.api.libs.json._
import java.io._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future,future, Await}

//spark context
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
// machine learning features
import org.apache.spark.ml.feature.RegexTokenizer
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.ml.feature.CountVectorizer
// mllib LDA algorithm
import org.apache.spark.mllib.clustering.LDA 
import org.apache.spark.mllib.clustering.OnlineLDAOptimizer
// linear algebra
import org.apache.spark.mllib.linalg.Vector
// spark sql
import org.apache.spark.sql._

object ExecuteSentimentAnalysis 
{
	val conf = new SparkConf()
    conf.setAppName(LoadConf.globalConf.getString("appName") + " - REST API")
    conf.set("spark.scheduler.mode", "FAIR")

    //Spark resources configuration
    conf.set("spark.executor.memory",s"""${LoadConf.globalConf.getString("spark.restapi.executorMemory")}""")
    conf.set("spark.ui.port",s"""${LoadConf.globalConf.getString("spark.restapi.sparkUIPort")}""")
    conf.set("spark.cores.max",s"""${LoadConf.globalConf.getInt("spark.restapi.totalCores")}""")
    
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
	/* config sqlContext */
    sqlContext.setConf("spark.sql.shuffle.partitions", s"""${LoadConf.globalConf.getInt("spark.partitionNumber")}""")
    
    // ============== ML ======================
    // Tune this parameters if needed
	val numTopics: Int = LoadConf.restConf.getInt("sentimentAnalysis.numTopics")
	val termsPerTopic: Int = LoadConf.restConf.getInt("sentimentAnalysis.termsPerTopic") 
	// large ( >= 10) -> more accurate but slower
	// low   ( >= 1 ) -> faster but less accurate
	val miniBatchIncreaseAcc: Int = 4*1                   
	val maxIterations: Int = 100 // iterations 
	val vocabSize: Int = 10000  //

	val logger = LoggerFactory.getLogger(this.getClass)

	def runSentimentAnalysis(includeTerms: String, excludeTerms: String, top: Int, startDatetime: String, endDatetime: String, sentiment: Int): Future[String]= 
	{	
		Future
		{
			logger.info("Processing sentiment Analysis:")
			logger.info("Include: " + includeTerms)
			logger.info("Exclude: " + excludeTerms)
			logger.info(s"Date Range: $startDatetime -> $endDatetime")
			logger.info("Sentiment: " + sentiment)

			runTopicModel(includeTerms, excludeTerms, top, startDatetime, endDatetime, sentiment)	
		}
	}

	def runTopicModel(includeTerms: String, excludeTerms: String, top: Int, startDatetime: String, endDatetime: String, sentiment: Int): String =
	{	
		try { 
			/* Based on source code in: https://gist.github.com/feynmanliang/3b6555758a27adcb527d
	   			Modified by: Jorge Castanon, October 2015 
	   		*/
			import sqlContext.implicits._ 

			// Load the raw text docs, assign docIDs, and convert to DataFrame
			val rawTextRDD = sc.makeRDD(getTweetsText(includeTerms, excludeTerms, top, startDatetime, endDatetime, sentiment))
			//Return empty response if no data was found
			if (rawTextRDD.isEmpty())
			{	
				return Json.stringify(Json.obj("topics" -> Json.arr())) 
			}
			val docDF = rawTextRDD.zipWithIndex.toDF("text", "docId")
			// tweets splited into words
			// need to keep #'s and @'s and emoticons
			val tokens = new RegexTokenizer().
			  setGaps(false).
			  setMinTokenLength(2).  
			  setPattern("([@#a-zA-Z:();=8>]\\S+)"). 
			  // use this instead (no emotcons): 
			  // setPattern("([@#a-zA-Z]\\S+)"). 
			  setInputCol("text").                
			  setOutputCol("words").
			  transform(docDF)

			val filteredTokens = new StopWordsRemover().
			  //setStopWords(stopwords). // can get stopwords in English from StopWordsRemover.scala
			  setCaseSensitive(false).
			  setInputCol("words").
			  setOutputCol("filtered").
			  transform(tokens)

			// Limit to top vocabSize most common words and convert to word count vector features
			val cvModel = new CountVectorizer().
			  setInputCol("filtered").
			  setOutputCol("features").
			  setVocabSize(vocabSize).
			  fit(filteredTokens)

			val countVectors = cvModel.transform(filteredTokens).select("docId", "features").
	  			map { case Row(docId: Long, countVector: Vector) => (docId, countVector) }.cache()
	  		/**
			 * Configure and run LDA
			 */
			val mbf = {
			  // add (1.0 / actualCorpusSize) to MiniBatchFraction be more robust on tiny datasets.
			  val corpusSize = countVectors.count()
			  2.0 / maxIterations + 1.0 / corpusSize
			}
			val mbfval = math.min(1.0, miniBatchIncreaseAcc*mbf)

			val lda = new LDA().
			  setOptimizer(new OnlineLDAOptimizer().setMiniBatchFraction(mbfval)).
			  setK(numTopics).
			  setMaxIterations(maxIterations).
			  setDocConcentration(-1). // use default symmetric document-topic prior
			  setTopicConcentration(-1) // use default symmetric topic-word prior

			val startTime = System.nanoTime()
			val ldaModel = lda.run(countVectors)
			val elapsed = (System.nanoTime() - startTime) / 1e9

			// Print the topics, showing the top-weighted terms for each topic.
			val topicIndices = ldaModel.describeTopics(maxTermsPerTopic = termsPerTopic)
			val vocabArray = cvModel.vocabulary
			val topics = topicIndices.map { case (terms, termWeights) =>
			  terms.map(vocabArray(_)).zip(termWeights)
			}
			logger.info(s"Training time (sec)\t$elapsed")

			val response = topics.zipWithIndex.map { case (topic, i) =>
			  topic.map({ case (term, weight) => Json.arr(term, i, weight)}) 
			}flatten

			countVectors.unpersist()

			return Json.stringify(Json.obj("topics" -> response))
			// Print Time in seconds
		} catch {
		  	case e: Exception => {
					logger.error("Topics Training model",e)
			} 
		}
		
		return Json.stringify(Json.obj("topics" -> JsNull))
	}

	def getTweetsText(includeTerms: String, excludeTerms: String, top: Int, startDatetime: String, endDatetime: String, sentiment: Int): List[String] =
	{
		val elasticsearchRequests = new GetElasticsearchResponse(top, includeTerms.toLowerCase().trim().split(","), excludeTerms.toLowerCase().trim().split(","), startDatetime, endDatetime, LoadConf.esConf.getString("decahoseIndexName"))
		val tweets = (Json.parse(elasticsearchRequests.getSentimentWordAnalysis(sentiment)) \ "hits" \ "hits").as[List[JsObject]]
		
		tweets.map(tweet => (tweet \ "fields" \ "tweet_text")(0).as[String])
	}
}
