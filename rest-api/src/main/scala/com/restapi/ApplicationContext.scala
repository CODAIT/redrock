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

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.hive._

object ApplicationContext
{
  private val sparkConf = new SparkConf()
  //sparkConf.setMaster(masterNode)
  sparkConf.setAppName(LoadConf.globalConf.getString("appName") + " - REST API")
  sparkConf.set("spark.scheduler.mode", "FAIR")

  //Spark master resources
  sparkConf.set("spark.executor.memory",s"""${LoadConf.globalConf.getString("spark.restapi.executorMemory")}""")
  sparkConf.set("spark.ui.port",s"""${LoadConf.globalConf.getString("spark.restapi.sparkUIPort")}""")
  sparkConf.set("spark.cores.max",s"""${LoadConf.globalConf.getInt("spark.restapi.totalCores")}""")

  val sparkContext = new SparkContext(sparkConf)
  val sqlContext = new HiveContext(sparkContext)

  /* config sqlContext */
  sqlContext.setConf("spark.sql.shuffle.partitions", s"""${LoadConf.globalConf.getInt("spark.partitionNumber")}""")
  sqlContext.setConf("spark.sql.codegen", "true")

}
