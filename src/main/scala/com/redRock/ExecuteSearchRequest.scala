package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.sql.functions._
import play.api.libs.json._
import java.io._

object ExecuteSearchRequest 
{
	def runSearchAnalysis(includeTerms: String, excludeTerms: String, top: Int): String = 
	{	
		println("Processing search:")
		println("Include: " + includeTerms)
		println("Exclude: " + excludeTerms)

		val filteredTweets = selectTweetsAndInformation(includeTerms.toLowerCase(),excludeTerms.toLowerCase())
		filteredTweets.cache()
		Json.stringify(buildJSONResponse(top, filteredTweets,includeTerms,excludeTerms))
	}

	def buildJSONResponse(top: Int, filteredTweets: DataFrame, includeTerms: String, excludeTerms: String):JsValue =
	{	
		val numberOfTweets = filteredTweets.count()
		val totalUsers = getTotalUsers(filteredTweets)
		val professions = formatProfession(filteredTweets)
		val location = formatLocation(filteredTweets)
		val sentiment = formatSentiment(filteredTweets)
		val topTweets = formatTopTweets(top, filteredTweets)
		val clusterDistance = extracTopWordDistance(includeTerms, excludeTerms)

		val json: JsValue = Json.obj(
  			"status" -> 0,
			"totaltweets" -> PrepareTweets.totalTweets,
			"totalfilteredtweets" -> numberOfTweets,
			"totalusers" -> (if (totalUsers == -1) JsNull else totalUsers),
			"profession" -> (if (professions._2) JsNull else professions._1),
			"location" ->  Json.obj("fields" -> Json.arr("Date", "Country", "Count"), "location" -> (if (location._2) JsNull else location._1)),
			"sentiment" -> Json.obj("fields" -> Json.arr("Date", "Positive", "Negative", "Neutral"), "sentiment" -> (if (sentiment._2) JsNull else sentiment._1)),
			"toptweets" -> Json.obj("tweets" -> (if (topTweets._2) JsNull else topTweets._1)),
			"cluster-distance" -> clusterDistance
		)

		filteredTweets.unpersist()

		return json
	}

	def getTotalUsers(filteredTweets: DataFrame): Long =
	{
		try { 
		  filteredTweets.dropDuplicates(Array(ColNames.id)).count()
		} catch {
		  case e: Exception => println(e); return  -1
		}
	}

	def formatTopTweets(top: Int, filteredTweets: DataFrame): (Array[JsObject],Boolean) =
	{
		var mapTopTweets = Array[JsObject]()
		try { 
			//0 - created_at, 1 - text, 2 - id
			//3 - name, 4 - handle, 5 - followers, 6 - profileURL
			val topTweetsByLang = extractTopTweets(top, filteredTweets)
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

			return (mapTopTweets,false)
		}
		catch {
		  case e: Exception => println(e); return (mapTopTweets,true)
		}
	}

	def formatProfession(filteredTweets: DataFrame): (JsObject,Boolean) =
	{
		try
		{
			val resultProfessionMap = extractProfession(filteredTweets)
			(Json.obj("profession" -> resultProfessionMap),false)
		}
		catch {
		  case e: Exception => println(e); return (Json.obj(), true)
		}
	}

	def formatLocation(filteredTweets: DataFrame): (Array[JsArray],Boolean) =
	{
		try { 
			// timestamp, Country, count
			val resultLocationDF = extractLocation(filteredTweets)
			return (resultLocationDF,false)
		}
		catch {
		  case e: Exception => println(e); return (Array[JsArray](),true)
		}
	}

	def formatSentiment(filteredTweets: DataFrame): (Array[JsArray],Boolean) = 
	{
		try { 
			//(1,-1,0)
			// timestamp, sentiment, count
			val resultSentimentDF = extractSentiment(filteredTweets)
			return (resultSentimentDF,false)
		} catch {
		  case e: Exception => println(e); return (Array[JsArray](),true)
		}
	}

	def extractTopTweets(top: Int, filteredTweets: DataFrame): Array[org.apache.spark.sql.Row] = 
	{
		//import SparkContVal.sqlContext.implicits._
		filteredTweets.select(ColNames.created_at, ColNames.text, ColNames.id, 
							ColNames.name, ColNames.handle, ColNames.followers, 
							ColNames.profileImgURL, ColNames.lang).
						filter(s"${ColNames.lang} = '${Config.language}'").
						orderBy(desc("followers_count")).limit(top).collect()
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