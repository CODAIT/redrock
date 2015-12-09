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

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.slf4j.LoggerFactory
import play.api.libs.json._

import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams

import scala.io.Source._

/**
 * Created by barbaragomes on 10/30/15.
 */
object ValidateTweetCompliance {

  val logger = LoggerFactory.getLogger(this.getClass)

  def getNonCompliantTweets(jsonMessageRequest:String):List[String] =
  {
    val response = performBluemixCheck(jsonMessageRequest)
    try{
      val jsonResponse = Json.parse(response).as[JsObject]
      if (jsonResponse.keys.contains("nonCompliant"))
      {
        return (jsonResponse \ "nonCompliant").as[List[String]]
      }
      else
      {
        return List[String]()
      }
    }
    catch{
      case e: Exception => {
        logger.error("Could not validate tweets list: Error on transforming response",e)
        return List[String]()
      }
    }
  }

  def performBluemixCheck(jsonMessageRequest:String): String = {
    try
    {
      val startTime = System.nanoTime()
      val httpClient = new DefaultHttpClient()
      val params = httpClient.getParams();
      HttpConnectionParams.setConnectionTimeout(params, 2000);
      HttpConnectionParams.setSoTimeout(params, 2000);

      val request = new HttpPost(LoadConf.restConf.getString("bluemixProduction.requestURLforMessagesCheck"))

      val JSONentity:StringEntity = new StringEntity(jsonMessageRequest, "UTF-8")
      JSONentity.setContentType("application/json")
      request.setEntity(JSONentity)

      val httpResponse = httpClient.execute(request)
      val entity = httpResponse.getEntity()
      var jsonResponse = "{}"
      if (entity != null) {
        val inputStream = entity.getContent()
        jsonResponse = fromInputStream(inputStream).getLines.mkString
        inputStream.close
      }
      httpClient.getConnectionManager().shutdown()
      val elapsed = (System.nanoTime() - startTime) / 1e9
      logger.info(s"Response for bluemix check tweets (sec): $elapsed")

      return jsonResponse
    }catch{
      case e: Exception => {
        logger.error("Could not validate tweets list",e)
        return "{}"
      }
    }
  }
}
