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
		val tweets = SparkContVal.sqlContext.read.json(Config.dataPath).selectExpr("text",
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
						"stringTokenizer(text) as tokens").orderBy("created_at").repartition(Config.numberOfPartitions).cache()

		tweets.printSchema()
		totalTweets = tweets.count()
		println(s"Total loaded tweets ==> ${totalTweets}")
		return tweets
	}
}