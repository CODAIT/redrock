package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.sql.functions._
import play.api.libs.json._
import java.io._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future,future, Await}

object ExecuteSearchRequest 
{
	def runSearchAnalysis(includeTerms: String, excludeTerms: String, top: Int): String = 
	{	
		println("Processing search:")
		println("Include: " + includeTerms)
		println("Exclude: " + excludeTerms)
		
		Json.stringify(executeAsynchronous(top,includeTerms.toLowerCase(),excludeTerms.toLowerCase()))
	}
	
	def executeAsynchronous(top: Int, includeTerms: String, excludeTerms: String): JsValue =
	{
		try 
		{
			val cluster_distance: Future[JsValue] = future { extracTopWordDistance(includeTerms,excludeTerms) }
			val spark_dataAnalisys: Future[JsValue] = future { extractSparkAnalysis(top,includeTerms,excludeTerms) }
		
			val tasks: Seq[Future[JsValue]] = Seq(spark_dataAnalisys, cluster_distance)
			val aggregated: Future[Seq[JsValue]] = Future.sequence(tasks)
		

			val jsonResults: Seq[JsValue] = Await.result(aggregated, 500.seconds)
			return (jsonResults(0).as[JsObject] ++ jsonResults(1).as[JsObject])
		}
		catch {
		  case e: Exception => println(e); return emptyJSONResponse()
		}
	}

	def extractSparkAnalysis(top: Int, includeTerms: String, excludeTerms: String):JsValue =
	{	
		//Filtering tweets
		val (filteredTweets,exception) = filterTweets(includeTerms, excludeTerms)
		if (exception)
		{	
			return emptyJSONResponse()
		}
		filteredTweets.cache()

		val json = executeSparkAsynchronous(filteredTweets, top)

		filteredTweets.unpersist()

		return json
	}

	def executeSparkAsynchronous(filteredTweets: DataFrame, top: Int): JsValue =
	{
		try {
			val totalUsers: Future[JsObject] = future { getTotalUsers(filteredTweets) }
			val numberOfTweets: Future[JsObject] = future { getTotalFilteredTweets(filteredTweets) }
			val professions: Future[JsObject] = future { formatProfession(filteredTweets) }
			val location: Future[JsObject] = future { formatLocation(filteredTweets) }
			val sentiment: Future[JsObject] = future { formatSentiment(filteredTweets) }
			val topTweets: Future[JsObject] = future { formatTopTweets(top, filteredTweets) }

			val tasks: Seq[Future[JsObject]] = Seq(numberOfTweets, totalUsers, professions, location, sentiment, topTweets)
			val aggregated: Future[Seq[JsObject]] = Future.sequence(tasks)

			val initialJson: JsObject = Json.obj("status" -> 0, "totaltweets" -> PrepareTweets.totalTweets) 
			
			val jsonResults: Seq[JsObject] = Await.result(aggregated, 500.seconds)
			return (initialJson ++ jsonResults(0) ++ jsonResults(1) ++ jsonResults(2) ++ jsonResults(3) ++ jsonResults(4) ++ jsonResults(5))
			
			/*val totalUsers = getTotalUsers(filteredTweets) 
			val numberOfTweets = getTotalFilteredTweets(filteredTweets) 
			val professions = formatProfession(filteredTweets) 
			val location = formatLocation(filteredTweets) 
			val sentiment = formatSentiment(filteredTweets) 
			val topTweets = formatTopTweets(top, filteredTweets)
			val initialJson: JsObject = Json.obj("status" -> 0, "totaltweets" -> PrepareTweets.totalTweets) 
			return (initialJson ++ totalUsers ++ numberOfTweets ++ professions ++ location ++ sentiment ++ topTweets)*/
		}
		catch {
		  case e: Exception => println(e); return emptyJSONResponse()
		}
	}

	def emptyJSONResponse(): JsValue = 
	{
		Json.obj("status" -> JsNull, "totaltweets" -> JsNull, 
		  		"totalfilteredtweets" -> JsNull, "totalusers" -> JsNull,
				"profession" -> JsNull, "location" -> JsNull, 
				"sentiment" -> JsNull, "toptweets" -> JsNull,
				"cluster" -> JsNull, "distance" -> JsNull)
	}

	def getTotalFilteredTweets(filteredTweets: DataFrame): JsObject = 
	{	
		try { 
		  return Json.obj( "totalfilteredtweets" -> filteredTweets.count())
		} catch {
		  case e: Exception => println(e); 
		}

		return Json.obj( "totalfilteredtweets" -> JsNull)
	}

	def getTotalUsers(filteredTweets: DataFrame): JsObject =
	{
		try { 
		  
		  val total = filteredTweets.dropDuplicates(Array(ColNames.id)).count()
		  return Json.obj("totalusers" -> total)
		
		} catch {
		  case e: Exception => println(e); 
		}

		return Json.obj("totalusers" -> JsNull)
	}

	def formatTopTweets(top: Int, filteredTweets: DataFrame): JsObject =
	{
		try { 
			//0 - created_at, 1 - text, 2 - id
			//3 - name, 4 - handle, 5 - followers, 6 - profileURL
			val topTweetsByLang = extractTopTweets(top, filteredTweets)
			return Json.obj("toptweets" -> Json.obj("tweets" -> topTweetsByLang))
		}
		catch {
		  case e: Exception => println("" + e.getStackTrace.mkString("\n")); 
		}

		return Json.obj("toptweets" -> Json.obj("tweets" -> JsNull))
	}

	def formatProfession(filteredTweets: DataFrame): JsObject =
	{
		try
		{
			val resultProfessionMap = extractProfession(filteredTweets)
			return Json.obj("profession" -> Json.obj("profession" -> resultProfessionMap))
		}
		catch {
		  case e: Exception => println(e); 
		}

		return Json.obj("profession" -> JsNull)
	}

	def formatLocation(filteredTweets: DataFrame): JsObject =
	{
		try { 
			// timestamp, Country, count
			val resultLocationDF = extractLocation(filteredTweets)
			return Json.obj("location" ->  Json.obj("fields" -> Json.arr("Date", "Country", "Count"), "location" -> resultLocationDF))
		}
		catch {
		  case e: Exception => println(e);
		}

		return Json.obj("location" ->  Json.obj("fields" -> Json.arr("Date", "Country", "Count"), "location" -> JsNull))
	}

	def formatSentiment(filteredTweets: DataFrame): JsObject = 
	{
		try { 
			//(1,-1,0)
			// timestamp, sentiment, count
			val resultSentimentDF = extractSentiment(filteredTweets)
			return Json.obj("sentiment" -> Json.obj("fields" -> Json.arr("Date", "Positive", "Negative", "Neutral"), "sentiment" -> resultSentimentDF))
		} catch {
		  case e: Exception => println(e);
		}

		return Json.obj("sentiment" -> Json.obj("fields" -> Json.arr("Date", "Positive", "Negative", "Neutral"), "sentiment" -> JsNull))
	}

	def extractTopTweets(top: Int, filteredTweets: DataFrame): Array[JsObject] = 
	{
		filteredTweets.select(ColNames.created_at, ColNames.text, ColNames.id, 
							ColNames.name, ColNames.handle, ColNames.followers, 
							ColNames.profileImgURL, ColNames.lang).
						filter(s"${ColNames.lang} = '${Config.language}'").
						orderBy(desc("followers_count")).limit(top).
						map(tweet => Json.obj("created_at" -> tweet.getString(0), "text" -> tweet.getString(1),
											"user" -> Json.obj("name" -> tweet.getString(3),"screen_name" -> tweet.getString(4),
																"followers_count" -> tweet.getLong(5),"id" -> tweet.getLong(2),
																"profile_image_url" -> tweet.getString(6)))).collect()
	}

	def extractLocation(filteredTweets: DataFrame): Array[JsArray] =
	{
		filteredTweets.filter(s"${ColNames.location} != ''").groupBy(ColNames.timestamp, ColNames.location).count().
															orderBy("timestamp").
															map(locaTime => Json.arr(locaTime.getString(0),locaTime.getString(1),locaTime.getLong(2).toInt)).collect()
	}

	def extractSentiment(filteredTweets: DataFrame): Array[JsArray]=
	{
		filteredTweets.groupBy(ColNames.timestamp, ColNames.sentiment).count().
								map(sentiment => (sentiment.getString(0), (sentiment.getInt(1), sentiment.getLong(2).toInt))).
								groupByKey().
								sortByKey().
								map(sentTime => Json.arr(sentTime._1, sentTime._2.find{case (sent:Int,count:Int) => (sent,count) == (1,count)}.getOrElse((0,0))._2, 
																	sentTime._2.find{case (sent:Int,count:Int) => (sent,count) == (-1,count)}.getOrElse((0,0))._2,
																	sentTime._2.find{case (sent:Int,count:Int) => (sent,count) == (0,count)}.getOrElse((0,0))._2)).
								collect()
	}

	def extractProfession(filteredTweets: DataFrame): Array[JsObject]=
	{
		
		filteredTweets.flatMap(row => row.getSeq[org.apache.spark.sql.Row](11)).map(prof => ((prof.getString(0), prof.getString(1)), 1)).
						reduceByKey(_ + _).map(prof => (prof._1._1, Json.obj("name" -> prof._1._2, "value" -> prof._2))).
						groupByKey().map(prof => Json.obj("name" -> prof._1, "children" -> prof._2)).collect()
	}

	def filterTweets(includeTerms: String, excludeTerms: String): (DataFrame, Boolean) =
	{
		try { 
		 	return (selectTweetsAndInformation(includeTerms, excludeTerms), false)
		} catch {
		  case e: Exception => println(e); return (null, true)
		}
	}

	def selectTweetsAndInformation(includeTerms: String, excludeTerms: String): DataFrame = 
	{
		val query = s"""
						SELECT * 
						FROM realTweets 
						WHERE validTweet(tokens, \"$includeTerms\", \"$excludeTerms\")
					"""
					
		SparkContVal.sqlContext.sql(query)
	}

	def extracTopWordDistance(includeTerms: String, excludeTerms: String): JsValue =
	{
		try{
			 
			val cmd = Array[String](Config.pythonVersion,Config.pythonScriptPath,includeTerms,excludeTerms)
			
			// create runtime to execute external command
			val rt: Runtime = Runtime.getRuntime()
			val pr: Process = rt.exec(cmd)
	 
			// retrieve output from python script
			val stdOutput: BufferedReader = new BufferedReader(new InputStreamReader(pr.getInputStream()))
			val stdError: BufferedReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()))
			val error = stdError.readLine()
			if(error != null)
			{
				println(error)
				var error_ite = stdError.readLine()
				while(error_ite != null)
				{
					println(error_ite)
					error_ite = stdError.readLine()
				}
			}
			else
			{
				// If no errors occurs, Json will be printed in the first line of th program 
				return Json.parse(stdOutput.readLine())
			}

		}catch {
			  case e: Exception => println(e);
		}
		return Json.obj("cluster" -> JsNull, "distance" -> JsNull)
	}
}