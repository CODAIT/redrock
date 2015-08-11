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

		val preparedTweets = getTweetsAndNecessaryFields()/*.dropDuplicates(Array(ColNames.text, ColNames.created_at, 
											ColNames.timestamp, ColNames.retweets,
											ColNames.favorites, ColNames.description,
											ColNames.profileImgURL, ColNames.followers,
											ColNames.name, ColNames.handle,
											ColNames.id, ColNames.sentiment,
											ColNames.location, ColNames.lang))*/
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
					//stringTokenizer(text) as tokens
					/* Be careful with the profession extraction. As much as professions it finds a match for each tweet,
	as much as duplication you will have. That means if it matches 2 professions for the same tweets,
	the left join is gonna to return 2 lines with all the same data but the profession fields
	*/
					//professionsDF.Profession
					//professionsDF.Keyword
					//LEFT JOIN professionsDF ON hasProfessionAssociated(user.description, professionsDF.CaseSensitive, professionsDF.Keyword)
		SparkContVal.sqlContext.sql(query)
	}	
}