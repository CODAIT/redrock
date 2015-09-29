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
	val redRockHomePath = "/Users/barbaragomes/Projects/RedRock-Server/redrock/"
	
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
	// ES spark configuration
	val elasticsearchConfig = Map("pushdown" -> "true", "es.nodes" -> elasticsearchIP, "es.port" -> elasticsearchPort)
	//database name
	val esIndex = "redrock"
	//table name
	val esTable = "processed_tweets"
}