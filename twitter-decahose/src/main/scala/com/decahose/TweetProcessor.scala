/**
  * (C) Copyright IBM Corp. 2015, 2016
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */
package com.decahose

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SaveMode
import org.apache.spark.streaming._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object TweetProcessor {
  // Extract file names
  val regExp = "\\b(hdfs:|file:)\\S+".r
  val logger = LoggerFactory.getLogger(this.getClass)

  def loadHistoricalDataAndStartStreaming(): Unit = {
    loadHistoricalData()
    startTweetsStreaming()
  }

  def startTweetsStreaming(): Unit = {
    logger.info(s"""Starting Streaming at: ${ApplicationContext.Config.sparkConf.getString("decahose.twitterStreamingDataPath")}""") // scalastyle:ignore
    logger.info(s"""Partition number: ${ApplicationContext.Config.sparkConf.getInt("partitionNumber")}""") // scalastyle:ignore

    val ssc = createContext()
    logger.info("Starting Streaming")
    ssc.start()
    ssc.awaitTermination()
  }

  def createContext(): StreamingContext = {

    logger.info("Creating streaming new context")
    // Create the context with a 1 second batch size
    val ssc = new StreamingContext(ApplicationContext.sparkContext, Seconds(ApplicationContext.Config.sparkConf.getInt("decahose.streamingBatchTime")))

    // Filtering file's path in order to avoid _copying files
    val tweetsStreaming = ssc.fileStream[LongWritable, Text, TextInputFormat](ApplicationContext.Config.sparkConf.getString("decahose.twitterStreamingDataPath"),
      (p: Path) => {
        if (p.getName().toLowerCase().endsWith(ApplicationContext.Config.sparkConf.getString("decahose.fileExtension"))) true // scalastyle:ignore
        else if ((p.getName().toLowerCase().endsWith(ApplicationContext.Config.sparkConf.getString("decahose.fileExtensionAuxiliar")))) true // scalastyle:ignore
        else false
      }, true).map(_._2.toString)

    tweetsStreaming.foreachRDD { (rdd: RDD[String], time: Time) =>
      logger.info(s"========= $time =========")
      if (!rdd.partitions.isEmpty) {
        logger.info("Processing File(s):")
        regExp.findAllMatchIn(rdd.toDebugString).foreach((name) => logger.info(name.toString))
        loadJSONExtractInfoWriteToDatabase(rdd)
      }
    }

    return ssc
  }

  def loadHistoricalData(): Unit = {
    if (ApplicationContext.Config.sparkConf.getBoolean("decahose.loadHistoricalData")) {
      logger.info(s"""Loading historical data from: ${ApplicationContext.Config.sparkConf.getString("decahose.twitterHistoricalDataPath")}""") // scalastyle:ignore
      val jsonRDDs = ApplicationContext.sparkContext.textFile(ApplicationContext.Config.sparkConf.getString("decahose.twitterHistoricalDataPath"), ApplicationContext.Config.sparkConf.getInt("partitionNumber")) // scalastyle:ignore
      if (!jsonRDDs.partitions.isEmpty) {
        loadJSONExtractInfoWriteToDatabase(jsonRDDs)
      }
      else {
        logger.warn("No historical files to be loaded")
      }
    }
    else {
      logger.warn(s"No historical data to be loaded.")
    }
  }

  def loadJSONExtractInfoWriteToDatabase(rdd: RDD[String]): Unit = {
    try {
      var rddToBeProcessed = rdd
      /* Twitter files that are provided by Bluemix Search API need to be parsed
       * and mapped in order to get one tweet per line
       */
      if (TweetField.jsonPrefix != null) {
        rddToBeProcessed = rdd.flatMap(file => (Json.parse(file) \ TweetField.jsonPrefix).as[List[JsObject]]).map(tweet => Json.stringify(tweet)) // scalastyle:ignore
      }
      ApplicationContext.sqlContext.read.json(rddToBeProcessed)
        .filter(s"${TweetField.verb} = 'post' OR ${TweetField.verb} = 'share'")
        .selectExpr(s"${TweetField.tweet_id} as tweet_id",
          s"${TweetField.tweet_created_at} AS created_at",
          s"${TweetField.language} AS language",
          s"getLocation(${TweetField.tweet_text}) AS tweet_location",
          s"getProfession(${TweetField.user_description}) AS tweet_professions",
          s"getSentiment(${TweetField.tweet_text}) AS tweet_sentiment",
          s"${TweetField.tweet_text} AS tweet_text",
          s"${TweetField.user_followers_count} AS user_followers_count",
          s"${TweetField.user_handle} AS user_handle",
          s"${TweetField.user_id} AS user_id",
          s"${TweetField.user_profileImgURL} AS user_image_url",
          s"${TweetField.user_name} user_name",
          s"convertCreatedAtFormat(${TweetField.tweet_created_at}, '${ApplicationContext.Config.sparkConf.getString("decahose.timestampFormat")}') AS created_at_timestamp", // scalastyle:ignore
          s"stringTokenizerArray(${TweetField.tweet_text}) as tweet_text_array_tokens",
          s"convertCreatedAtFormat(${TweetField.tweet_created_at}, '${ApplicationContext.Config.sparkConf.getString("decahose.timestampFormatDay")}') AS created_at_timestamp_day") // scalastyle:ignore
        // .filter(s"created_at IS NOT NULL AND tweet_text IS NOT NULL")
        .write.mode(SaveMode.Append)
        .format("org.elasticsearch.spark.sql")
        .options(Map("pushdown" -> "true", "es.nodes" -> ApplicationContext.Config.esConf.getString("bindIP"), "es.port" -> ApplicationContext.Config.esConf.getString("bindPort"))) // scalastyle:ignore
        .save(s"""${ApplicationContext.Config.esConf.getString("decahoseIndexName")}/${ApplicationContext.Config.esConf.getString("esType")}""")

      if (ApplicationContext.Config.sparkConf.getBoolean("decahose.deleteProcessedFiles")) {
        // Delete file just if it was processed
        logger.info("Deleting File(s):")
        regExp.findAllMatchIn(rdd.toDebugString).foreach((name) => deleteFile(name.toString))
      }
    }
    catch {
      case e: Exception => {
        logger.error("Processing Tweets", e)
        logger.error("####### File(s) not processed ########")
      }
    }
  }

  def deleteFile(fileName: String): Unit = {
    val filePath = new Path(fileName)
    if (ApplicationContext.hadoopFS.isDirectory(filePath)) {
      ApplicationContext.hadoopFS.listStatus(filePath).foreach((status) => {
        logger.info(status.getPath().toString)
        ApplicationContext.hadoopFS.delete(status.getPath(), true)
      })
    }
    else {
      logger.info(fileName)
      ApplicationContext.hadoopFS.delete(filePath, true)
    }
  }


}
