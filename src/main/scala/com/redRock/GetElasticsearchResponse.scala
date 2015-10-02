package com.redRock

class GetElasticsearchResponse(val topTweets: Int, val includeTerms:Array[String], val excludeTerms:Array[String])
{
	def getTopTweetsResponse(): String = 
	{
		//TODO: Call Location Request and return response
		val jsonRequest = GetJSONRequest.getLocationJSONRequest(includeTerms, excludeTerms)
		return ""
	}

	def getLocationResponse(): String =
	{
		//TODO: Call Location Request and return response
		val jsonRequest = GetJSONRequest.getLocationJSONRequest(includeTerms, excludeTerms)
		return ""

	}

	def getSentimentResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getSentimentJSONRequest(includeTerms, excludeTerms)
		return ""
	}

	def getProfessionResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getProfessionJSONRequest(includeTerms, excludeTerms)
		return ""
	}

	def getTotalTweetsESResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getTotalTweetsJSONRequest(includeTerms, excludeTerms)
		return ""
	}

	def getTotalFilteredTweetsAndTotalUserResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getTotalFilteredTweetsAndTotalUserJSONRequest(includeTerms, excludeTerms)
		return ""
	}
}