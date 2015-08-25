package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}
import scala.util.matching.Regex

object PrepareTweets
{
	var totalTweets:Long = 0

	def registerPreparedTweetsTempTable() = 
	{
		val tweets = SparkContVal.sqlContext.read.json(Config.dataPath)
		tweets.registerTempTable("tweets")

		val preparedTweets = getTweetsAndNecessaryFields()

		preparedTweets.registerTempTable("realTweets")
		SparkContVal.sqlContext.cacheTable("realTweets")
		preparedTweets.printSchema()
		//preparedTweets.show()
		totalTweets = preparedTweets.count()
		println(s"Total loaded tweets ==> ${totalTweets}")
		//preparedTweets.foreach(println)
	}

	def getTweetsAndNecessaryFields(): DataFrame = 
	{
		val language = Config.language
		val query = s"""SELECT text,
						created_at,
						getTimeStamp(created_at) AS timestamp,
						lang,
						user.profile_image_url,
						user.followers_count,
						user.name,
						user.screen_name,
						user.id,
						getSentiment(text) AS sentiment,
						getLocation(tweets.text) AS location,
						getProfession(tweets.user.description) AS profession,
						stringTokenizer(text) as tokens

						FROM tweets
						ORDER BY created_at
					"""
		SparkContVal.sqlContext.sql(query)
	}	
}