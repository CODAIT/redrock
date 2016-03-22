/**
  * (C) Copyright IBM Corp. 2015, 2016
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */
package com.powertrack

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.hadoop.fs._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.hive._


object ApplicationContext {

  object Config {
    // Global Application configuration
    val appConf: Config = ConfigFactory.load("redrock-app").getConfig("redrock")
    // Spark specific configuration
    val sparkConf: Config = appConf.getConfig("spark")
    // Elastic Search configuration
    val esConf: Config = appConf.getConfig("elasticsearch")
  }

  val sparkConf = new SparkConf()
  sparkConf.setAppName(Config.appConf.getString("appName") + " - Powertrack")
  sparkConf.set("spark.scheduler.mode", "FAIR")
  sparkConf.set("es.index.auto.create", "false")

  // Spark master resources
  sparkConf.set("spark.executor.memory", s"""${Config.sparkConf.getString("powertrack.executorMemory")}""") // scalastyle:ignore
  sparkConf.set("spark.ui.port", s"""${Config.sparkConf.getString("powertrack.sparkUIPort")}""")
  sparkConf.set("spark.cores.max", s"""${Config.sparkConf.getInt("powertrack.totalCores")}""")

  val sparkContext = new SparkContext(sparkConf)
  val sqlContext = new HiveContext(sparkContext)

  /* config sqlContext */
  sqlContext.setConf("spark.sql.shuffle.partitions", s"""${Config.sparkConf.getInt("partitionNumber")}""") // scalastyle:ignore
  sqlContext.setConf("spark.sql.codegen", "true")

  /* delete HDFS processed files */
  val hadoopFS: FileSystem = FileSystem.get(sparkContext.hadoopConfiguration)
}
