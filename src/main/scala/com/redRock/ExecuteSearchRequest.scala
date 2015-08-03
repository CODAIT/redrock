package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.sql.functions._
import play.api.libs.json._

object ExecuteSearchRequest 
{
	//Loads empty Data Frame
	var filteredTweets: DataFrame = SparkContVal.sqlContext.read.parquet()

	def runSearchAnalysis(includeTerms: String, excludeTerms: String, top: Int): String = 
	{	
		filteredTweets = selectTweetsAndInformation(includeTerms,excludeTerms)
		
		//println(Json.prettyPrint(buildJSONResponse(top)))
		Json.stringify(buildJSONResponse(top))
		//s"{include: ${includeTerms}, exclude: ${excludeTerms} }"
	}

	def buildJSONResponse(top: Int):JsValue =
	{
		val numberOfTweets = filteredTweets.count()

		val json: JsValue = Json.obj(
  			"status" -> 0,
			"totaltweets" -> PrepareTweets.totalTweets,
			"totalfilteredtweets" -> numberOfTweets,
			"totalusers" -> getTotalUsers(),
			"profession" -> formatProfession(),
			"location" ->  Json.obj("fields" -> Json.arr("Date", "Country", "Count"), "location" -> formatLocation()),
			"sentiment" -> Json.obj("fields" -> Json.arr("Date", "Positive", "Negative", "Neutral"), "sentiment" -> formatSentiment()),
			"toptweets" -> formatTopTweets(top)
		)

		//filteredTweets.unpersist()

		return json
	}

	def getTotalUsers(): Long =
	{
		filteredTweets.dropDuplicates(Array(ColNames.id)).count()
	}

	def formatTopTweets(top: Int): Array[JsObject] =
	{
		//0 - created_at, 1 - text, 2 - id
		//3 - name, 4 - handle, 5 - followers, 6 - profileURL
		val topTweetsByLang = extractTopTweets(top)
		var mapTopTweets = Array[JsObject]()

		for (tweet <- topTweetsByLang)
		{
			mapTopTweets = mapTopTweets :+ Json.obj(
				"created_at"	-> 	tweet.getString(0),
				"text" 			-> 	tweet.getString(1),
				"user" 			-> 	Json.obj(
										"name" -> tweet.getString(3),
										"screen_name" -> tweet.getString(4),
										"followers_count" -> tweet.getLong(5),
										"id" -> tweet.getLong(2),
										"profile_image_url" -> tweet.getString(6)
									)
			)
		}

		return mapTopTweets
		
		//println(topTweetsByLang.count())
	}

	def formatProfession(): JsObject =
	{
		val resultProfessionMap = extractProfession()
		/*var mapProfession = Map[String,Int]()
		
		// ._1 profession, ._2 count
		for (profession <- resultProfessionDF)
		{
			val name = profession.getString(0)
			val count = profession.getLong(1).toInt
			
			mapProfession = mapProfession + (name -> count)
		}*/

		//mapProfession.foreach(println)
		Json.obj("Profession" -> resultProfessionMap)
	}

	def formatLocation(): Array[JsValue] =
	{
		// timestamp, Country, count
		val resultLocationDF = extractLocation()
		var mapLocation = Array[JsValue]()
		
		// ._1 timestamp, ._2 country, ._3 count
		for (location <- resultLocationDF)
		{
			val country = location.getString(1)
			val timestamp = location.getString(0)
			val count = location.getLong(2).toInt
			
			mapLocation = mapLocation :+ Json.arr(timestamp,country,count)
		}
		return mapLocation
		//mapLocation.foreach(println)
	}

	def formatSentiment(): Array[JsValue] = 
	{
		// timestamp, sentiment, count
		val resultSentimentDF = extractSentiment()
		var mapSentiment = Map[String, (String,Int,Int,Int)]()
		
		// ._1 positive, ._2 negative, ._3 neutral
		for (sentiment <- resultSentimentDF)
		{
			val sent = sentiment.getInt(1)
			val timestamp = sentiment.getString(0)
			val count = sentiment.getLong(2).toInt
			if (!mapSentiment.contains(timestamp))
			{
				mapSentiment = mapSentiment + (timestamp -> (timestamp,0,0,0))
			}

			var sentTuple = mapSentiment(timestamp)
			if (sent == 1)
			{
				sentTuple = sentTuple.copy(_2 = count)
			}
			else if (sent == -1)
			{
				sentTuple = sentTuple.copy(_3 = count)
			}
			else
			{
				sentTuple = sentTuple.copy(_4 = count)
			}

			mapSentiment = mapSentiment + (timestamp -> sentTuple)
		}

		var jsonSent = Array[JsValue]()

		for (sent <- mapSentiment.values)
		{
			jsonSent = jsonSent :+ Json.arr(sent._1, sent._2, sent._3, sent._4)
		}
		
		return jsonSent

		//mapSentiment.foreach(println)
	}

	def extractTopTweets(top: Int): Array[org.apache.spark.sql.Row] = 
	{
		import SparkContVal.sqlContext.implicits._
		filteredTweets.select(ColNames.created_at, ColNames.text, ColNames.id, 
							ColNames.name, ColNames.handle, ColNames.followers, 
							ColNames.profileImgURL, ColNames.lang).
						filter(s"${ColNames.lang} = '${Config.language}'").
						orderBy($"followers_count".desc).limit(top).collect()
	}

	def extractLocation(): Array[org.apache.spark.sql.Row] =
	{
		filteredTweets.filter(s"${ColNames.location} != ''").groupBy(ColNames.timestamp, ColNames.location).count().collect()
	}

	def extractSentiment(): Array[org.apache.spark.sql.Row]=
	{
		filteredTweets.groupBy(ColNames.timestamp, ColNames.sentiment).count().collect()
	}

	/*def extractProfession(): Array[org.apache.spark.sql.Row] = 
	{
		filteredTweets.filter(s"${ColNames.professionGroup} IS NOT NULL").groupBy(ColNames.professionGroup).count().collect()
	}*/

	def extractProfession(): Map[String, Long]={
		filteredTweets.select(s"${ColNames.profession}").filter(s"${ColNames.profession} != ''").
					flatMap(prof => prof.getString(0).split(",")).
					map(prof => (prof, 1)).countByKey().toMap	
	}

	def selectTweetsAndInformation(includeTerms: String, excludeTerms: String): DataFrame = 
	{
		val query = s"""
						SELECT * 
						FROM realTweets 
						WHERE validTweet(text, \"$includeTerms\", \"$excludeTerms\")
					"""
					
		SparkContVal.sqlContext.sql(query).cache()
	}
	
}