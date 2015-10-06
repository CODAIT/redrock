/**
 * (C) Copyright IBM Corp. 2015, 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.redRock

import scala.io.Source._

object SentimentInfo
{
	val positivePath = Config.redRockHomePath + "src/main/resources/Sentiment/positive.txt"
	val negativePath = Config.redRockHomePath + "src/main/resources/Sentiment/negative.txt"

	val positiveWords = fromFile(positivePath)("ISO-8859-1").getLines.map(line => line.trim().toLowerCase()).toArray
	println(s"Sentiment loaded ==> Positive ==> ${positiveWords.size}")
	val negativeWords = fromFile(negativePath)("ISO-8859-1").getLines.map(line => line.trim().toLowerCase()).toArray
	println(s"Sentiment loaded ==> Negative ==> ${negativeWords.size}")
}