package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}
import org.apache.spark.sql.functions._
import play.api.libs.json._

object ExecuteSearchRequest 
{
	def runSearchAnalysis(includeTerms: String, excludeTerms: String, top: Int): String = 
	{	
		println("Processing search:")
		println("Include: " + includeTerms)
		println("Exclude: " + excludeTerms)

		val filteredTweets = selectTweetsAndInformation(includeTerms.toLowerCase(),excludeTerms.toLowerCase())
		filteredTweets.cache()
		//extracTopWordDistance(includeTerms)
		//println(Json.prettyPrint(buildJSONResponse(top)))
		Json.stringify(buildJSONResponse(top, filteredTweets))
		//s"{include: ${includeTerms}, exclude: ${excludeTerms} }"
	}

	def buildJSONResponse(top: Int, filteredTweets: DataFrame):JsValue =
	{	
		val numberOfTweets = filteredTweets.count()
		val totalUsers = getTotalUsers(filteredTweets)
		val professions = formatProfession(filteredTweets)
		val location = formatLocation(filteredTweets)
		val sentiment = formatSentiment(filteredTweets)
		val topTweets = formatTopTweets(top, filteredTweets)

		val json: JsValue = Json.obj(
  			"status" -> 0,
			"totaltweets" -> PrepareTweets.totalTweets,
			"totalfilteredtweets" -> numberOfTweets,
			"totalusers" -> (if (totalUsers == -1) JsNull else totalUsers),
			"profession" -> (if (professions._2) JsNull else professions._1),
			"location" ->  Json.obj("fields" -> Json.arr("Date", "Country", "Count"), "location" -> (if (location._2) JsNull else location._1)),
			"sentiment" -> Json.obj("fields" -> Json.arr("Date", "Positive", "Negative", "Neutral"), "sentiment" -> (if (sentiment._2) JsNull else sentiment._1)),
			"toptweets" -> Json.obj("tweets" -> (if (topTweets._2) JsNull else topTweets._1)),
			"cluster" -> JsNull,
			"distance" -> JsNull
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
						reduceByKey(_ + _).map(prof => (prof._1._1, Json.obj("name" -> prof._1._2, "size" -> prof._2))).
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

	def extracTopWordDistance(includeTerms: String): JsObject = 
	{
		var distance_cluster:JsObject = Json.obj()
		import SparkContVal.sqlContext.implicits._
		val vector = w2v(includeTerms)
		/*dot product of the included terms*/
		val vectorDot:Double = vector.select($"C1"*$"C1"+$"C2"*$"C2"+$"C3"*$"C3"+$"C4"*$"C4"+$"C5"*$"C5"+$"C6"*$"C6"+$"C7"*$"C7"+$"C8"*$"C8"+$"C9"*$"C9"+$"C10"*$"C10"+
									  $"C11"*$"C11"+$"C12"*$"C12"+$"C13"*$"C13"+$"C14"*$"C14"+$"C15"*$"C15"+$"C16"*$"C16"+$"C17"*$"C17"+$"C18"*$"C18"+$"C19"*$"C19"+$"C20"*$"C20"+
									  $"C21"*$"C21"+$"C22"*$"C22"+$"C23"*$"C23"+$"C24"*$"C24"+$"C25"*$"C25"+$"C26"*$"C26"+$"C27"*$"C27"+$"C28"*$"C28"+$"C29"*$"C29"+$"C30"*$"C30"+
									  $"C31"*$"C31"+$"C32"*$"C32"+$"C33"*$"C33"+$"C34"*$"C34"+$"C35"*$"C35"+$"C36"*$"C36"+$"C37"*$"C37"+$"C38"*$"C38"+$"C39"*$"C39"+$"C40"*$"C40"+
									  $"C41"*$"C41"+$"C42"*$"C42"+$"C43"*$"C43"+$"C44"*$"C44"+$"C45"*$"C45"+$"C46"*$"C46"+$"C47"*$"C47"+$"C48"*$"C48"+$"C49"*$"C49"+$"C50"*$"C50"+
									  $"C51"*$"C51"+$"C52"*$"C52"+$"C53"*$"C53"+$"C54"*$"C54"+$"C55"*$"C55"+$"C56"*$"C56"+$"C57"*$"C57"+$"C58"*$"C58"+$"C59"*$"C59"+$"C60"*$"C60"+
									  $"C61"*$"C61"+$"C62"*$"C62"+$"C63"*$"C63"+$"C64"*$"C64"+$"C65"*$"C65"+$"C66"*$"C66"+$"C67"*$"C67"+$"C68"*$"C68"+$"C69"*$"C69"+$"C70"*$"C70"+
									  $"C71"*$"C71"+$"C72"*$"C72"+$"C73"*$"C73"+$"C74"*$"C74"+$"C75"*$"C75"+$"C76"*$"C76"+$"C77"*$"C77"+$"C78"*$"C78"+$"C79"*$"C79"+$"C80"*$"C80"+
									  $"C81"*$"C81"+$"C82"*$"C82"+$"C83"*$"C83"+$"C84"*$"C84"+$"C85"*$"C85"+$"C86"*$"C86"+$"C87"*$"C87"+$"C88"*$"C88"+$"C89"*$"C89"+$"C90"*$"C90"+
									  $"C91"*$"C91"+$"C92"*$"C92"+$"C93"*$"C93"+$"C94"*$"C94"+$"C95"*$"C95"+$"C96"*$"C96"+$"C97"*$"C97"+$"C98"*$"C98"+$"C99"*$"C99"+$"C100"*$"C100" as "DOT").collect()(0).getDouble(0)
		println(vectorDot)
		if(vectorDot > 0.0000001)
		{
			val vectorCol = vector.collect()(0)
			val distance = LoadWords2Vec.words2vec.map(line => getDistances(vectorCol, vectorDot, line)).sortByKey(false,2).take(20)
			//println(distance.getNumPartitions())
			//distance.foreach(println)
			var mapDistance = Array[JsValue]()
			var mapCluster = Array[JsValue]()
			for (words <- distance)
			{
				mapDistance = mapDistance :+ Json.arr(words._2._1, words._1, words._2._3)
				mapCluster = mapCluster :+ Json.arr(words._2._1, words._1, words._2._2.toInt, words._2._3)
			}
			distance_cluster = Json.obj("distance" -> mapDistance, "cluster" -> mapCluster)
		}

		return distance_cluster
	}

	def getDistances(includeVec: org.apache.spark.sql.Row, includeDot: Double, wordVector: org.apache.spark.sql.Row):(Double, (String, String, String)) = 
	{	
		val wordDot = wordVector.getString(101).toDouble
		val sqrWordInclude:Double = Math.sqrt(wordDot * includeDot)
		var dotIncludeWord:Double = 0.0
		var i = 0
		for (i <- 1 to 100)
		{
			dotIncludeWord = dotIncludeWord + (wordVector.getString(i).toDouble)*(includeVec.getDouble(i-1))
		}

		//distance, (word, cluster, freq)
		return (dotIncludeWord/(sqrWordInclude), (wordVector.getString(0), wordVector.getString(102), wordVector.getString(103)))
	}
	
	def w2v(includeTerms: String): DataFrame = 
	{
		import SparkContVal.sqlContext.implicits._
		val preparedIncludeTerms = "'" + includeTerms.replaceAll(",", "','") + "'"
		LoadWords2Vec.words2vec.filter(s"C0 IN ($preparedIncludeTerms)").agg(sum($"C1") as "C1",sum($"C2") as "C2",sum($"C3") as "C3",sum($"C4") as "C4",sum($"C5") as "C5",sum($"C6") as "C6",sum($"C7") as "C7",sum($"C8") as "C8",sum($"C9") as "C9",sum($"C10") as "C10",
																	 sum($"C11") as "C11",sum($"C12") as "C12",sum($"C13") as "C13",sum($"C14") as "C14",sum($"C15") as "C15",sum($"C16") as "C16",sum($"C17") as "C17",sum($"C18") as "C18",sum($"C19") as "C19",sum($"C20") as "C20",
																	 sum($"C21") as "C21",sum($"C22") as "C22",sum($"C23") as "C23",sum($"C24") as "C24",sum($"C25") as "C25",sum($"C26") as "C26",sum($"C27") as "C27",sum($"C28") as "C28",sum($"C29") as "C29",sum($"C30") as "C30",
																	 sum($"C31") as "C31",sum($"C32") as "C32",sum($"C33") as "C33",sum($"C34") as "C34",sum($"C35") as "C35",sum($"C36") as "C36",sum($"C37") as "C37",sum($"C38") as "C38",sum($"C39") as "C39",sum($"C40") as "C40",
																	 sum($"C41") as "C41",sum($"C42") as "C42",sum($"C43") as "C43",sum($"C44") as "C44",sum($"C45") as "C45",sum($"C46") as "C46",sum($"C47") as "C47",sum($"C48") as "C48",sum($"C49") as "C49",sum($"C50") as "C50",
																	 sum($"C51") as "C51",sum($"C52") as "C52",sum($"C53") as "C53",sum($"C54") as "C54",sum($"C55") as "C55",sum($"C56") as "C56",sum($"C57") as "C57",sum($"C58") as "C58",sum($"C59") as "C59",sum($"C60") as "C60",
																	 sum($"C61") as "C61",sum($"C62") as "C62",sum($"C63") as "C63",sum($"C64") as "C64",sum($"C65") as "C65",sum($"C66") as "C66",sum($"C67") as "C67",sum($"C68") as "C68",sum($"C69") as "C69",sum($"C70") as "C70",
																	 sum($"C71") as "C71",sum($"C72") as "C72",sum($"C73") as "C73",sum($"C74") as "C74",sum($"C75") as "C75",sum($"C76") as "C76",sum($"C77") as "C77",sum($"C78") as "C78",sum($"C79") as "C79",sum($"C80") as "C80",
																	 sum($"C81") as "C81",sum($"C82") as "C82",sum($"C83") as "C83",sum($"C84") as "C84",sum($"C85") as "C85",sum($"C86") as "C86",sum($"C87") as "C87",sum($"C88") as "C88",sum($"C89") as "C89",sum($"C90") as "C90",
																	 sum($"C91") as "C91",sum($"C92") as "C92",sum($"C93") as "C93",sum($"C94") as "C94",sum($"C95") as "C95",sum($"C96") as "C96",sum($"C97") as "C97",sum($"C98") as "C98",sum($"C99") as "C99",sum($"C100") as "C100")
	}

	/*def time[R](block: => R, blockName: String): R = {
    	val t0 = System.nanoTime()
    	val result = block    // call-by-name
    	val t1 = System.nanoTime()
    	println(blockName + ": " + (t1 - t0) + "ns")
    	result
	}*/

}