import sbt._
import sbt.Keys._

object BuildSettings {

  val Name = "redRockTest"
  val Version = "3.0"
  val ScalaVersion = "2.10.4"

  lazy val buildSettings = Defaults.coreDefaultSettings ++ Seq (
    name          := Name,
    version       := Version,
    scalaVersion  := ScalaVersion,
    organization  := "com.ibm.barbara",
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
    val Spark        = "1.4.1"
    val ScalaTest    = "2.2.4"
    val ScalaCheck   = "1.12.2"
    val akkaV = "2.3.9"
    val sprayV = "1.3.3"
  }

  // Spark dependencies 
  // Do not remove "provided" - We do not need to includ spark dependency on the jar because
  // the jar is gonna be executed by spark-submit
  val sparkCore      = "org.apache.spark"  %% "spark-core"      % Version.Spark  % "provided"
  val sparkStreaming = "org.apache.spark"  %% "spark-streaming" % Version.Spark  % "provided"
  val sparkSQL       = "org.apache.spark"  %% "spark-sql"       % Version.Spark  % "provided"
  val sparkRepl      = "org.apache.spark"  %% "spark-repl"      % Version.Spark  % "provided"
  
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
}

object Dependencies {
  import Dependency._

  val redRockDependecies =
    Seq(sparkCore,sparkSQL,sparkRepl,
      scalaTest, scalaCheck, playJson, 
      sprayCan, sprayRouting,
      akkaActor,specs2Core,
      readCSV)
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



