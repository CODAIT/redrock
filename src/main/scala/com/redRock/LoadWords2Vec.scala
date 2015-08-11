package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}

object LoadWords2Vec
{
	import SparkContVal.sqlContext.implicits._
	val words2vecPath = "./src/main/resources/Distance/w2v_Freq_cluster.csv"
	val words2vec = SparkContVal.sqlContext.read.format("com.databricks.spark.csv").
							option("inferSchema", "true").load(words2vecPath)
}