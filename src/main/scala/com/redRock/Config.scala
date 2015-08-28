package com.redRock

object Config
{
	val appName = "RedRock Data Frame"
	val appVersion = "1.0"
	val restName = "RedRock"
	val defaultTop = 100
	val language = "en"
	val topWordsToVec = 20
	//Change it when running on cluster
	val numberOfPartitions = 8
	//Relative Paths
	val homePath = "/Users/barbaragomes/Projects/sparkinsights-server-scala/"
	val port = 16666
	val dataPath = "/Users/barbaragomes/Projects/RedRockInsights-Demo/Data/decahose_BG20150501001246-0700_EN20150501002746-0700-1-400k"
	//"hdfs://bdavm155.svl.ibm.com:8020/spark-summit-demo/data/decahose/decahose_BG20150823230338-0700_EN20150823231838-0700"
	//"hdfs://bdavm155.svl.ibm.com:8020/spark-summit-demo/data/decahose/decahose_BG20150823230338-0700_EN20150823231838-0700.gz"
	//"/Users/barbaragomes/Projects/RedRockInsights-Demo/Data/decahose_BG20150501001246-0700_EN20150501002746-0700-1-400k"

	//Cluster and Distance execution
	val pythonScriptPath = homePath + "Python/main.py"
	val pythonVersion= "python2.7"


}