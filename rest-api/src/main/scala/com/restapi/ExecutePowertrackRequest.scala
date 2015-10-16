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

import java.text.SimpleDateFormat
import java.util.{Calendar, TimeZone}
import org.apache.commons.lang.time.DateUtils
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future,future, Await}

/**
 * Created by barbaragomes on 10/16/15.
 */
object ExecutePowertrackRequest {

  def runPowertrackAnalysis(batchTime: Int, topTweets: Int, topWords: Int): Future[String] =
  {
    Future {

      val (startDate, endDate) = getStartAndEndDateAccordingBatchTime(batchTime)
      println("Powertrack request")
      println(s"UTC start date: $startDate")
      println(s"UTC end date: $endDate")

      val elasticsearchResponse = new GetElasticsearchResponse(topTweets, startDateTime = startDate, endDateTime = endDate, esType = LoadConf.esConf.getString("powertrackType"))
      val response = elasticsearchResponse.getPowertrackTweetsAndWordCount(topWords)

      //TODO: manipulate response when the JSON request is ready

      s""" Powertrack request
       start date: $startDate
       end date: $endDate
       top tweets: $topTweets
       top words: $topWords
     """
    }
  }

  def getStartAndEndDateAccordingBatchTime(batchTime: Int): (String, String) = {
    //end date should be the current date
    val endDate = Calendar.getInstance().getTime()
    val startDate = DateUtils.addMinutes(endDate, -batchTime)

    //Powertrack datetime timezine: UTC
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val sdf: SimpleDateFormat = new SimpleDateFormat(LoadConf.globalConf.getString("spark.powertrack.tweetTimestampFormat"))
    (sdf.format(startDate), sdf.format(endDate))
  }
}
