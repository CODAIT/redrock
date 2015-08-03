package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SQLContext, DataFrame}

object SparkContVal 
{
	val conf = new SparkConf()
    //conf.setMaster(masterNode)
    conf.setAppName(Config.appName)

    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    // Change to a more reasonable default number of partitions (from 200)
	sqlContext.setConf("spark.sql.shuffle.partitions", s"${Config.numberOfPartitions}")
}