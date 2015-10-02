package com.redRock

class GetElasticsearchResponse(val topTweets: Int, includeTerms:Array[String], excludeTerms:Array[String])
{
	val includeTermsES = includeTerms.mkString(" ")
	val excludeTermsES = excludeTerms.mkString(" ")

	def getTopTweetsResponse(): String = 
	{
		//TODO: Call Location Request and return response
		val jsonRequest = GetJSONRequest.getTopTweetsJSONRequest(includeTermsES, excludeTermsES, topTweets)
		return ""
	}

	def getLocationResponse(): String =
	{
		//TODO: Call Location Request and return response
		val jsonRequest = GetJSONRequest.getLocationJSONRequest(includeTermsES, excludeTermsES)
		return ""

	}

	def getSentimentResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getSentimentJSONRequest(includeTermsES, excludeTermsES)
		return ""
	}

	def getProfessionResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getProfessionJSONRequest(includeTermsES, excludeTermsES)
		return ""
	}

	def getTotalTweetsESResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getTotalTweetsJSONRequest()
		return ""
	}

	def getTotalFilteredTweetsAndTotalUserResponse(): String =
	{
		//TODO: Call Sentiment Request and return response
		val jsonRequest = GetJSONRequest.getTotalFilteredTweetsAndTotalUserJSONRequest(includeTermsES, excludeTermsES)
		return ""
	}
}