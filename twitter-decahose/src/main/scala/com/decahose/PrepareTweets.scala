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
package com.decahose

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

import org.apache.hadoop.fs.{FileSystem, Path, PathFilter}


object PrepareTweets
{
    //Extract file names
    val regExp = "\\b(hdfs:|file:)\\S+".r
    def loadHistoricalDataAndStartStreaming() =
    {
        loadHistoricalData()
        startTweetsStreaming()
    }

    def startTweetsStreaming() =
    {
        println(s"""Starting Streaming at: ${LoadConf.sparkConf.getString("decahose.twitterStreamingDataPath")}""")
        println(s"""Partition number: ${LoadConf.sparkConf.getInt("partitionNumber")}""")

        val ssc = createContext()
        println("Starting Streaming")
        ssc.start()
        ssc.awaitTermination()
    }

    def createContext(): StreamingContext = {
        
        println("Creating streaming new context")
        // Create the context with a 1 second batch size
        val ssc = new StreamingContext(ApplicationContext.sparkContext, Seconds(LoadConf.sparkConf.getInt("decahose.streamingBatchTime")))

        val tweetsStreaming = ssc.textFileStream(LoadConf.sparkConf.getString("decahose.twitterStreamingDataPath"))
         
        tweetsStreaming.foreachRDD{ (rdd: RDD[String], time: Time) =>
            println(s"========= $time =========")
            if(!rdd.partitions.isEmpty)
            {
                println("Processing File(s):")
                regExp.findAllMatchIn(rdd.toDebugString).foreach(println)
                loadJSONExtractInfoWriteToDatabase(rdd)
                println("Deleting File(s):")
                regExp.findAllMatchIn(rdd.toDebugString).foreach((name) => deleteFile(name.toString))
            }
        }

        return ssc
    }

    def loadHistoricalData() =
    {
        if (LoadConf.sparkConf.getBoolean("decahose.loadHistoricalData"))
        {
            println(s"""Loading historical data from: ${LoadConf.sparkConf.getString("decahose.twitterHistoricalDataPath")}""")
            val jsonRDDs = ApplicationContext.sparkContext.textFile(LoadConf.sparkConf.getString("decahose.twitterHistoricalDataPath"),LoadConf.sparkConf.getInt("partitionNumber"))
            if(!jsonRDDs.partitions.isEmpty)
            {
                loadJSONExtractInfoWriteToDatabase(jsonRDDs)
                println("Deleting File(s):")
                regExp.findAllMatchIn(jsonRDDs.toDebugString).foreach((name) => deleteFile(name.toString))
            }
            else
            {
                println("No historical files to be loaded")
            }
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
            ApplicationContext.sqlContext.read.json(rdd)
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
                        s"convertCreatedAtFormat(${TweetField.tweet_created_at}) AS created_at_timestamp",
                        s"stringTokenizerArray(${TweetField.tweet_text}) as tweet_text_array_tokens")
                        //.filter(s"created_at IS NOT NULL AND tweet_text IS NOT NULL")
                        .write.mode(SaveMode.Append)
                        .format("org.elasticsearch.spark.sql")
                        .options(Map("pushdown" -> "true", "es.nodes" -> LoadConf.esConf.getString("bindIP"), "es.port" -> LoadConf.esConf.getString("bindPort")))
                        .save(s"""${LoadConf.esConf.getString("indexName")}/${LoadConf.esConf.getString("decahoseType")}""")
        }
        catch {
          case e: Exception => 
          {
            Utils.printException(e, "Processing Tweets")
          }
        }
    }

    def deleteFile(fileName: String) =
    {
        val filePath = new Path(fileName)
        if (ApplicationContext.hadoopFS.isDirectory(filePath))
        {
            ApplicationContext.hadoopFS.listStatus(filePath).foreach((status) => {
                                                        println(status.getPath())
                                                        ApplicationContext.hadoopFS.delete(status.getPath(), true)
                                                    })
        }
        else
        {
            println(fileName)
            ApplicationContext.hadoopFS.delete(filePath, true)
        }
    }


}
