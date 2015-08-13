package com.redRock

import scala.io.Source._

object SentimentInfo
{
	val positivePath = "./src/main/resources/Sentiment/positive.txt"
	val negativePath = "./src/main/resources/Sentiment/negative.txt"

	val positiveWords = fromFile(positivePath)("ISO-8859-1").getLines.map(line => line.trim().toLowerCase()).toArray
	println(s"Sentiment loaded ==> Positive ==> ${positiveWords.size}")
	val negativeWords = fromFile(negativePath)("ISO-8859-1").getLines.map(line => line.trim().toLowerCase()).toArray
	println(s"Sentiment loaded ==> Negative ==> ${negativeWords.size}")
}