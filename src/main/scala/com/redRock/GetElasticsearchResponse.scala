package com.redRock

import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import java.io._

class GetElasticsearchResponse(val topTweets: Int, includeTerms:Array[String], excludeTerms:Array[String])
{
	val includeTermsES = includeTerms.mkString(" ")
	val excludeTermsES = excludeTerms.mkString(" ")

	def getTopTweetsResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getTopTweetsJSONRequest(includeTermsES, excludeTermsES, topTweets)
		return performSearch(jsonRequest)
	}

	def getLocationResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getLocationJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(jsonRequest)
	}

	def getSentimentResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getSentimentJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(jsonRequest)
	}

	def getProfessionResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getProfessionJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(jsonRequest)
	}

	def getTotalTweetsESResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getTotalTweetsJSONRequest()
		return performSearch(jsonRequest)
	}

	def getTotalFilteredTweetsAndTotalUserResponse(): String =
	{
		val jsonRequest = GetJSONRequest.getTotalFilteredTweetsAndTotalUserJSONRequest(includeTermsES, excludeTermsES)
		return performSearch(jsonRequest)
	}

	def performSearch(jsonQueryRequest:String): String = {
		try
		{
			val settings = ImmutableSettings.settingsBuilder()
			      .put("cluster.name", "elasticsearch")
			      .build();

	    val client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(Config.elasticsearchIP, Config.elasticsearchApiPort.toInt));

	    val response = client
	      .prepareSearch("redrock")
	      .setTypes("processed_tweets")
	      .setQuery(jsonQueryRequest)
	      .setSize(60)
	      .setExplain(true)
	      .execute()
	      .actionGet()

			val jsonResponse = response.toString

	    client.close()

			return jsonResponse
		} catch {
			case e: Exception => {
				printException(e, "Retrieve ElasticSearch Response")
				return ""
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
