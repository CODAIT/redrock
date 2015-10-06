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

object Config
{
	/* Spark names */
	val appName = "RedRock Streaming ES"
	val appVersion = "2.0"

	/* REST API */
	val restName = "RedRock"
	val port = 16666

	/* Search configurations */
	val defaultTopTweets = 100
	val tweetsLanguage = "en"
	val topWordsToVec = 20

	/* Process Config */
	//Format to be used to group by timestamp for Sentiment and Location
	val timestampFormat = "MM/dd HH"

	/* Spark partitions number */
	val numberOfPartitions = 5

	/* RedRock home path */
	val redRockHomePath = sys.env("REDROCK_HOME")

	/* Twitter historical data */
	// Set to false if historical data was already processed to ES
	val loadHistoricalData = true
	// Historical data path
	val twitterHistoricalDataPath = "hdfs://localhost:9000/user/hadoop/decahose_historical"

	/* Spark streaming configuration */
	//Hadoop directory where the streaming is going to read from
	val twitterStreamingDataPath = "hdfs://localhost:9000/user/hadoop/decahose_streaming"
	//Streaming batch time in seconds
	val streamingBatchTime = 60
	//Hadoop check point directory for streaming data.
	val checkPointDirForStreaming = "hdfs://localhost:9000/user/hadoop/spark_streaming_checkpoint"

	/* Cluster and Distance configuration*/
	val pythonScriptPath = redRockHomePath + "Python/main.py"
	val pythonVersion= "python2.7"

	/* ElasticSearch configuration*/
	// ES bind IP (localhost)
	val elasticsearchIP = "127.0.0.1"
	// ES bind port
	val elasticsearchPort = "9200"
	// ES API bind port
	val elasticsearchApiPort = "9300"
	// ES spark configuration
	val elasticsearchConfig = Map("pushdown" -> "true", "es.nodes" -> elasticsearchIP, "es.port" -> elasticsearchPort)
	//database name
	val esIndex = "redrock"
	//table name
	val esTable = "processed_tweets"
}
