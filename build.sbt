
//changing shell prompt name
shellPrompt := { state =>
  "sbt (%s)> ".format(Project.extract(state).currentProject.id)
}

// Simulating spark Shell inside the program
initialCommands += """
  import org.apache.spark.SparkContext
  import org.apache.spark.SparkContext._
  import org.apache.spark.sql.SQLContext
  val sc = new SparkContext("local[*]", "Spark Console")
  val sqlContext = new SQLContext(sc)
  import sqlContext.implicits._
  """

cleanupCommands += """
  println("Closing the SparkContext:")
  sc.stop()
  """

assemblyJarName in assembly := "redRock.jar"

Revolver.settings
