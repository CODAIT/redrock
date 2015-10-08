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

object Config
{
	val appName = "RedRock REST API"
	val appVersion = "2.0"

	/* REST API */
	val restName = "redrock-rest"
	val restActor = "redrock-actor"
	val port = 16666

	/* Search configurations */
	val defaultTopTweets = 100
	val tweetsLanguage = "en"
	val topWordsToVec = 20

	/* RedRock home path */
	val redRockHomePath = sys.env("REDROCK_HOME")

	/* Cluster and Distance configuration*/
	val pythonScriptPath = redRockHomePath + "/rest-api/python/main.py"
	val pythonVersion= "python2.7"

	/* ElasticSearch configuration*/
	// ES bind IP (localhost)
	val elasticsearchIP = "127.0.0.1"
	// ES bind port
	val elasticsearchPort = "9200"
	// ES API bind port
	val elasticsearchApiPort = "9300"
	//database name
	val esIndex = "redrock"
	//table name
	val esTable = "processed_tweets"
}
