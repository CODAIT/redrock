package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}
import scala.util.matching.Regex

object PrepareTweets
{
    var totalTweets:Long = 0

    def registerPreparedTweetsTempTable(): DataFrame =
    {
        println(s"Loading data: ${Config.dataPath}")
        println(s"Partition number: ${Config.numberOfPartitions}")
        val jsonAsRDD = SparkContVal.sc.textFile(Config.dataPath,Config.numberOfPartitions)
        val tweets = SparkContVal.sqlContext.read.json(jsonAsRDD).selectExpr("text",
                                        "created_at",
                                        "getTimeStamp(created_at) AS timestamp",
                                        "lang",
                                        "user.profile_image_url",
                                        "user.followers_count",
                                        "user.name",
                                        "user.screen_name",
                                        "user.id",
                                        "getSentiment(text) AS sentiment",
                                        "getLocation(text) AS location",
                                        "getProfession(user.description) AS profession",
                                        "stringTokenizer(text) as tokens").filter("created_at IS NOT NULL AND text IS NOT NULL").repartition(Config.numberOfPartitions).cache()

        tweets.printSchema()
        totalTweets = tweets.count()
        println(s"Total loaded tweets ==> ${totalTweets}")
        return tweets
    }
}
