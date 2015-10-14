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
package com.restapi

import play.api.libs.json._
import java.io._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future,future, Await}

object ExecuteSentimentAnalysis 
{
	def runSentimentAnalysis(includeTerms: String, excludeTerms: String, top: Int, startDatetime: String, endDatetime: String, sentiment: Int): Future[String]= 
	{	

		Future
		{
			println("Processing sentiment Analysis:")
			println("Include: " + includeTerms)
			println("Exclude: " + excludeTerms)
			println(s"Date Range: $startDatetime -> $endDatetime")
			println("Sentiment: " + sentiment)

			"Sentiment Analysis message received"
		}
			
		//executeAsynchronous(top,includeTerms.toLowerCase(),excludeTerms.toLowerCase(), startDate, endDate) map { js =>
			//Json.stringify(js)
		//}	
	}
}