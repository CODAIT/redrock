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

  val Name = "redRock"
  val Version = "3.0"
  val ScalaVersion = "2.10.4"

  lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq (
    name          := Name,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.ibm.spark.redrock",
    description   := "Red Rock Data Frame",
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
    val Spark        = "1.5.0"
    val ScalaTest    = "2.2.4"
    val ScalaCheck   = "1.12.2"
    val akkaV = "2.3.9"
    val sprayV = "1.3.3"
    val ElasticsearchVersion = "1.7.2"
    val HttpClientVersion = "4.2.2"
    val Slf4jVersion = "1.7.12"
    val Log4jVersion = "1.2.17"
  }

  // Spark dependencies
  // Do not remove "provided" - We do not need to includ spark dependency on the jar because
  // the jar is gonna be executed by spark-submit
  val sparkCore      = "org.apache.spark"  %% "spark-core"      % Version.Spark  % "provided"
  val sparkStreaming = "org.apache.spark"  %% "spark-streaming" % Version.Spark  % "provided"
  val sparkSQL       = "org.apache.spark"  %% "spark-sql"       % Version.Spark  % "provided"
  val sparkRepl      = "org.apache.spark"  %% "spark-repl"      % Version.Spark  % "provided"
  val sparkHive      = "org.apache.spark"  %% "spark-hive"      % Version.Spark  % "provided"

  val cassandraConnector = "com.datastax.spark" %% "spark-cassandra-connector" % "1.4.0-M2"
  val elasticSearchConnector = "org.elasticsearch" % "elasticsearch-spark_2.10" % "2.1.1" % "provided"

  val sprayCan       = "io.spray"          %%  "spray-can"      % Version.sprayV
  val sprayRouting   = "io.spray"          %%  "spray-routing"  % Version.sprayV
  //val sprayTestKit   = "io.spray"          %%  "spray-testkit"  % Version.sprayV  % "test"
  val akkaActor      = "com.typesafe.akka" %%  "akka-actor"     % Version.akkaV   % "provided"
  //val akkaTestKit    = "com.typesafe.akka" %%  "akka-testkit"   % Version.akkaV   % "test"
  val specs2Core     = "org.specs2"        %%  "specs2-core"    % "2.3.7" % "test"

  // For testing.
  val scalaTest      = "org.scalatest"     %% "scalatest"       % Version.ScalaTest  % "test"
  val scalaCheck     = "org.scalacheck"    %% "scalacheck"      % Version.ScalaCheck % "test"

  // Json library
  val playJson       = "com.typesafe.play" %% "play-json"       % "2.3.0"

  //csv library
  val readCSV       = "com.databricks"    %% "spark-csv"  % "1.1.0"

  //Elasticsearch dependencies
  val elasticSearch  = "org.elasticsearch" % "elasticsearch" % Version.ElasticsearchVersion
  val slf4j          = "org.slf4j" % "slf4j-api" % Version.Slf4jVersion % "provided"
  val log4j          = "log4j" % "log4j" % Version.Log4jVersion % "provided"
  val log4Slf4j      = "org.slf4j" % "log4j-over-slf4j" % Version.Slf4jVersion % "provided"

  //HTTP client
  val httpClient     = "org.apache.httpcomponents" % "httpclient" % Version.HttpClientVersion
}

object Dependencies {
  import Dependency._

  val redRockDependecies =
    Seq(sparkCore,sparkSQL,sparkRepl,
      sparkHive, elasticSearchConnector,
      scalaTest, scalaCheck, playJson,
      sprayCan, sprayRouting,
      akkaActor,specs2Core,
      readCSV, elasticSearch,
      slf4j, log4j, log4Slf4j)
}

object RedRockBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._

  lazy val redRock = Project(
    id = "redRock-Test",
    base = file("."),
    settings = buildSettings ++ Seq(
      maxErrors := 5,
      // Suppress warnings about Scala patch differences in dependencies.
      // This is slightly risky, so consider not doing this for production
      // software, see what the warnings are using the sbt 'evicted' command,
      // then "ask your doctor if this setting is right for you..."
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      triggeredMessage := Watched.clearWhenTriggered,
      // runScriptSetting,
      resolvers := allResolvers,
      libraryDependencies ++= Dependencies.redRockDependecies,
      unmanagedResourceDirectories in Compile += baseDirectory.value / "conf",
      mainClass := Some("com.redRock.Boot"),
      // Must run the examples and tests in separate JVMs to avoid mysterious
      // scala.reflect.internal.MissingRequirementError errors. (TODO)
      fork := true,
      //This is important for some programs to read input from stdin
      connectInput in run := true,
      // Must run Spark tests sequentially because they compete for port 4040!
      // TODO. There is now a Spark property to disable the web console. If we
      // use it, then we can remove the following setting:
      parallelExecution in Test := false))
}
