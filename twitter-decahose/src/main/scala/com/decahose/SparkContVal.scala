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
package com.decahose

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
    conf.setAppName(LoadConf.globalConf.getString("appName") + " - Decahose")
    conf.set("spark.scheduler.mode", "FAIR")
    //conf.set("spark.cassandra.connection.host", "127.0.0.1")
    //Do not allow infer schema. Schema must be defined at ES before start the app
   	conf.set("es.index.auto.create", "true")

    val sc = new SparkContext(conf)
    val sqlContext = new HiveContext(sc)

    /* config sqlContext */
    sqlContext.setConf("spark.sql.shuffle.partitions", s"""${LoadConf.sparkConf.getInt("partitionNumber")}""")
    sqlContext.setConf("spark.sql.codegen", "true")

    /* delete HDFS processed files */
    val hadoopFS :FileSystem = FileSystem.get(sc.hadoopConfiguration)
}