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
import sbt._
import sbt.Keys._

object BuildSettings {

  val ParentProject = "redrock-parent"
  val RestAPIName = "redrock-restapi"
  val DecahoseName = "redrock-decahose"
  val PowertrackName = "redrock-powertrack"
  val WebSocketsName = "redrock-websockets"

  val Version = "3.0"
  val ScalaVersion = "2.10.4"
  val ScalaVersion11 = "2.11.6"

  lazy val rootbuildSettings = Defaults.coreDefaultSettings ++ Seq (
    name          := ParentProject,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.ibm.spark.redrock",
    description   := "RedRock External Project",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-Xlint")
  )

  lazy val restAPIbuildSettings = Defaults.coreDefaultSettings ++ Seq (
    name          := RestAPIName,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.ibm.spark.redrock",
    description   := "RedRock REST API Application",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-Xlint")
  )

  lazy val decahosebuildSettings = Defaults.coreDefaultSettings ++ Seq (
    name          := DecahoseName,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.ibm.spark.redrock",
    description   := "RedRock Decahose Spark Streaming Application",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-Xlint")
  )

  lazy val powertrackbuildSettings = Defaults.coreDefaultSettings ++ Seq (
    name          := PowertrackName,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.ibm.spark.redrock",
    description   := "RedRock Powertrack Spark Streaming Application",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-Xlint")
  )

  lazy val webSocketsbuildSettings = Defaults.coreDefaultSettings ++ Seq (
    name          := WebSocketsName,
    version       := Version,
    scalaVersion  := ScalaVersion11,
    organization  := "com.ibm.spark.redrock",
    description   := "RedRock WebSockets Server",
    scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8", "-Xlint")
  )

}

object Resolvers {
  
  val typesafe = "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
  val sonatype = "Sonatype Release" at "https://oss.sonatype.org/content/repositories/releases"
  val mvnrepository = "MVN Repo" at "http://mvnrepository.com/artifact"

  val allResolvers = Seq(typesafe, sonatype, mvnrepository)

}

object Dependency {
  object Version {
    val Spark        = "1.5.1"
    val akkaV = "2.3.9"
    val sprayV = "1.3.3"
    val ElasticsearchVersion = "1.7.2"
    val HttpClientVersion = "4.2.2"
    val Slf4jVersion = "1.7.12"
    val Log4jVersion = "1.2.17"
    val ElasticasearchSparkVersion = "2.1.1"
  }

  // Spark dependencies
  /* Do not remove "provided" - We do not need to include spark dependency 
  on the jar because the jar is gonna be executed by spark-submit*/
  val sparkCore      = "org.apache.spark"  %% "spark-core"      % Version.Spark  % "provided"
  val sparkStreaming = "org.apache.spark"  %% "spark-streaming" % Version.Spark  % "provided"
  val sparkSQL       = "org.apache.spark"  %% "spark-sql"       % Version.Spark  % "provided"
  val sparkRepl      = "org.apache.spark"  %% "spark-repl"      % Version.Spark  % "provided"
  val sparkHive      = "org.apache.spark"  %% "spark-hive"      % Version.Spark  % "provided"
  val sparkMlLib     = "org.apache.spark"  %% "spark-mllib"     % Version.Spark  % "provided"

  val sprayCan       = "io.spray"          %%  "spray-can"      % Version.sprayV
  val sprayRouting   = "io.spray"          %%  "spray-routing"  % Version.sprayV
  val akkaActor      = "com.typesafe.akka" %%  "akka-actor"     % Version.akkaV 
  val specs2Core     = "org.specs2"        %%  "specs2-core"    % "2.3.7" % "test"

  //Config library
  val configLib      = "com.typesafe" % "config" % "1.2.1"

  // Json library
  val playJson       = "com.typesafe.play" %% "play-json"       % "2.3.0"

  //csv library
  val readCSV       = "com.databricks"    %% "spark-csv"  % "1.1.0"

  //Elasticsearch dependencies
  val elasticSearchConnector = "org.elasticsearch" %% "elasticsearch-spark" % Version.ElasticasearchSparkVersion % "provided"
  val elasticSearch  = "org.elasticsearch" % "elasticsearch" % Version.ElasticsearchVersion
  val slf4j          = "org.slf4j" % "slf4j-api" % Version.Slf4jVersion % "provided"
  val log4j          = "log4j" % "log4j" % Version.Log4jVersion % "provided"
  val log4Slf4j      = "org.slf4j" % "log4j-over-slf4j" % Version.Slf4jVersion % "provided"

  //HTTP client
  val httpClient     = "org.apache.httpcomponents" % "httpclient" % Version.HttpClientVersion

  //Web Sockets dependencies
  val akkaStream     = "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC2"
  val akkaHttpCore   = "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-RC2" 
  val scalcHttp      = "org.scalaj" %% "scalaj-http" % "1.1.5"
}

object Dependencies {
  import Dependency._

  val decahoseAndPowertrackDependencies = Seq(sparkCore,sparkSQL,sparkRepl,
                                              sparkHive, elasticSearchConnector, readCSV, configLib)

  val restAPIDependecies = Seq(playJson, sprayCan, sprayRouting, akkaActor, 
                               specs2Core, elasticSearch, httpClient, slf4j, log4j, log4Slf4j, configLib,
                               sparkMlLib, sparkCore, sparkSQL)
  val webSocketsDependencies = Seq(akkaStream, akkaHttpCore, playJson, scalcHttp)
}

object RedRockBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val parent = Project(
    id = "redrock-parent",
    base = file("."),
    aggregate = Seq(restapi, decahose, powertrack),
    settings = rootbuildSettings ++ Seq(
      aggregate in update := false
    )
  )

  lazy val restapi = Project(
    id = "redrock-restapi",
    base = file("./rest-api"),
    settings = restAPIbuildSettings ++ Seq(
      maxErrors := 5,
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      triggeredMessage := Watched.clearWhenTriggered,
      resolvers := allResolvers,
      libraryDependencies ++= Dependencies.restAPIDependecies,
      unmanagedResourceDirectories in Compile += file(".") / "conf",
      mainClass := Some("com.restapi.Application"),
      fork := true,
      connectInput in run := true
    ))

  lazy val decahose = Project(
    id = "redrock-decahose",
    base = file("./twitter-decahose"),
    settings = decahosebuildSettings ++ Seq(
      maxErrors := 5,
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      triggeredMessage := Watched.clearWhenTriggered,
      resolvers := allResolvers,
      libraryDependencies ++= Dependencies.decahoseAndPowertrackDependencies,
      unmanagedResourceDirectories in Compile += file(".") / "conf",
      mainClass := Some("com.decahose.Application"),
      fork := true,
      connectInput in run := true
    ))

  lazy val powertrack = Project(
    id = "redrock-powertrack",
    base = file("./twitter-powertrack"),
    settings = powertrackbuildSettings ++ Seq(
      maxErrors := 5,
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      triggeredMessage := Watched.clearWhenTriggered,
      resolvers := allResolvers,
      libraryDependencies ++= Dependencies.decahoseAndPowertrackDependencies,
      unmanagedResourceDirectories in Compile += file(".") / "conf",
      mainClass := Some("com.powertrack.Application"),
      fork := true,
      connectInput in run := true
    ))

  lazy val websockets = Project(
    id = "redrock-websockets",
    base = file("./websockets"),
    settings = webSocketsbuildSettings ++ Seq(
      maxErrors := 5,
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      triggeredMessage := Watched.clearWhenTriggered,
      resolvers := allResolvers,
      libraryDependencies ++= Dependencies.webSocketsDependencies,
      unmanagedResourceDirectories in Compile += file(".") / "conf",
      mainClass := Some("com.websokets.Server"),
      fork := true,
      connectInput in run := true
    ))
}
