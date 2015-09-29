package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}
import scala.io.Source._
import scala.util.matching.Regex
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

object AnalysisFunction
{
	//Location Data
	val states:Array[String] = Array("AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS",
								"KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND",
								"OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY")
  	val cities_global = LoadLocationData.cities
  	val countries_global =  LoadLocationData.countries
  	val cities_keys_sorted = LoadLocationData.cities_keys

  	//Profession data
  	val professions_global = ProfessionInfo.professions

	def registerAnalysisFunctions() = 
	{
		SparkContVal.sqlContext.udf.register("validTweet", (text: scala.collection.mutable.WrappedArray[String], includeTerms: String, excludeTerms: String) => validateTweetText(text, includeTerms, excludeTerms))
		SparkContVal.sqlContext.udf.register("getSentiment", (text: String) => extractSentimentFromText(text))
		SparkContVal.sqlContext.udf.register("getTimeStamp", (text: String) => getTimeStamp(text))
		SparkContVal.sqlContext.udf.register("getLocation", (text: String) => extractLocation(text))
		SparkContVal.sqlContext.udf.register("getProfession", (description: String) => extractProfession(description))	
		SparkContVal.sqlContext.udf.register("stringTokenizer", (text: String) => stringTokenizer(text))
		SparkContVal.sqlContext.udf.register("convertCreatedAtFormat", (created_at: String) => convertCreatedAtFormat(created_at))
	}
	
	//Check if the text contain all the include terms and do not contain any of the exclude terms
	def validateTweetText(text: scala.collection.mutable.WrappedArray[String], includeTerms: String, excludeTerms: String): Boolean =
	{
		if (excludeTerms != "")
		{ 
			val exclude:Array[String] = excludeTerms.split(",")
			if(exclude.exists(exc => text.contains(exc)))
			{
				return false
			}
		}

		val include:Array[String] = includeTerms.split(",")
		if(include.exists(inc => !text.contains(inc)))
		{
			return false
		}

		return true
	}

	def extractSentimentFromText(text: String): Int = 
	{
		val textToken = text.toLowerCase().split(" ")

		val positive = SentimentInfo.positiveWords.intersect(textToken).length
		val negative = SentimentInfo.negativeWords.intersect(textToken).length

		//positive
		if (positive > negative)
		{
			return 1
		}
		// negative
		else if (positive < negative)
		{
			return -1
		}
		//neutral
		else
		{
			return 0
		}
	}

	def extractProfession(description: String): Map[String,String] =
	{
		if (description != null && description.trim() != "")
		{
			return professions_global.filter(profession => {profession._1.findFirstIn(description) != None}).
								map(profession => (profession._1.findFirstIn(description).get.toLowerCase(), profession._2)).toMap 
		}
		return Map[String,String]()
	}

	def stringTokenizer(text: String): String = 
	{
		return Twokenize.tokenize(text.toLowerCase().trim()).mkString(" ")
	}

	def extractLocation(text: String): String = 
	{	
		val tokens = text.toLowerCase().trim()

		val location_country = countries_global.keys.find(x => tokens.contains(x))
		if(location_country == None)
		{
			val location_city = cities_keys_sorted.find(x => tokens.contains(x))
			if(location_city == None)
			{
				val location_states = states.find(x => text.contains(x))
				if(location_states == None)
				{
					return ""
				}
				else
				{
					return "United States"	
				}
			}
			else
			{
				return cities_global(location_city.get)._1
			}
		}
		else
		{
			return countries_global(location_country.get)
		}
	}

	def getTimeStamp(text: String):String = 
	{
		if (text != null && text.trim() != "")
		{
			return text.substring(4,13)
		}
		return null
	}

	def convertCreatedAtFormat(created_at: String): Long =
	{
		val sdf:SimpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
		val date = sdf.parse(created_at)
		return date.getTime()
		
		//Fri May 01 07:12:43 +0000 2015
	}
}