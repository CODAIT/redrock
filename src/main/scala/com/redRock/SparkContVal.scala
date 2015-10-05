package com.redRock

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.hive._
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.dstream.DStream
import org.elasticsearch.spark._ 
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.apache.hadoop.conf._
import org.apache.hadoop.fs._
import java.net.URI


object SparkContVal 
{
	val conf = new SparkConf()
    //conf.setMaster(masterNode)
    conf.setAppName(Config.appName)
    conf.set("spark.scheduler.mode", "FAIR")
    //conf.set("spark.cassandra.connection.host", "127.0.0.1")
    //Do not allow infer schema. Schema must be defined at ES before start the app
   	conf.set("es.index.auto.create", "true")

    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    /* config sqlContext */
    sqlContext.setConf("spark.sql.shuffle.partitions", s"${Config.numberOfPartitions}")
    sqlContext.setConf("spark.sql.codegen", "true")

    /* delete HDFS processed files */
    val hadoopFS :FileSystem = FileSystem.get(sc.hadoopConfiguration)
}