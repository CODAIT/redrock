package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame, SaveMode}
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.dstream.DStream
import scala.util.matching.Regex
import scala.concurrent.{Future,future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.spark.rdd.RDD
import java.io._
//import com.datastax.spark.connector._
import org.apache.spark.sql.Row
import org.elasticsearch.spark._ 
import org.elasticsearch.spark.sql._


object PrepareTweets
{
    def loadHistoricalDataAndStartStreaming() =
    {
        loadHistoricalData()
        startTweetsStreaming()
    }

    def startTweetsStreaming() =
    {
        println(s"Starting Streaming at: ${Config.twitterStreamingDataPath}")
        println(s"Partition number: ${Config.numberOfPartitions}")

        val ssc = StreamingContext.getOrCreate(Config.checkPointDirForStreaming,
        () => {
            createContext()
        })

        /* Must be in a Future because we need to start the REST API after */
        Future
        {
            ssc.start()
            ssc.awaitTermination()
        }
    }

    def createContext(): StreamingContext = {
        
        println("Creating streaming new context")
        // Create the context with a 1 second batch size
        val ssc = new StreamingContext(SparkContVal.sc, Seconds(Config.streamingBatchTime))
        ssc.checkpoint(Config.checkPointDirForStreaming)

        //Extract file names
        val regExp = "\\b(hdfs:|file:)[\\w|:|/|-]+".r

        val tweetsStreaming = ssc.textFileStream(Config.twitterStreamingDataPath)

        tweetsStreaming.foreachRDD{ (rdd: RDD[String], time: Time) =>
            println(s"========= $time =========")
            if(!rdd.partitions.isEmpty)
            {
                println("Processing File(s):")
                regExp.findAllMatchIn(rdd.toDebugString).foreach(println)
                loadJSONExtractInfoWriteToDatabase(rdd)
            }
        }

        return ssc
    }

    def loadHistoricalData() =
    {
        if (Config.loadHistoricalData)
        {
            println(s"Loading historical data from: ${Config.twitterHistoricalDataPath}")
            loadJSONExtractInfoWriteToDatabase(SparkContVal.sc.textFile(Config.twitterHistoricalDataPath,Config.numberOfPartitions))
        }
        else
        {
            println(s"No historical data to be loaded.")
        }
    }

    def loadJSONExtractInfoWriteToDatabase(rdd: RDD[String]) = 
    {
        try
        {
            SparkContVal.sqlContext.read.json(rdd)
                        .selectExpr("id as tweet_id",
                        //"convertCreatedAtFormat(created_at) AS created_at",
                        "created_at AS created_at",
                        "lang AS language",
                        "getLocation(text) AS tweet_location",
                        "getProfession(user.description) AS tweet_professions",
                        "getSentiment(text) AS tweet_sentiment",
                        "text AS tweet_text",
                        "stringTokenizer(text) as tweet_text_tokens",
                        "user.followers_count AS user_followers_count",
                        "user.screen_name AS user_handle",
                        "user.id AS user_id",
                        "user.profile_image_url AS user_image_url",
                        "user.name user_name")
                        .filter("created_at IS NOT NULL AND tweet_text IS NOT NULL")
                        .write.mode(SaveMode.Append)
                        .format("org.elasticsearch.spark.sql")
                        .options(Config.elasticsearchConfig)
                        .save(s"${Config.esIndex}/${Config.esTable}")
                        /*.format("org.apache.spark.sql.cassandra")
                        .options(Map("keyspace" -> "tweets", "table" -> "processed_tweets"))
                        .save()*/
        }
        catch {
          case e: Exception => 
          {
            printException(e, "Processing Tweets")
          }
        }
    }

    def printException(thr: Throwable, module: String) =
    {
        println("Exception on: " + module)
        val sw = new StringWriter
        thr.printStackTrace(new PrintWriter(sw))
        println(sw.toString)
    }

}
