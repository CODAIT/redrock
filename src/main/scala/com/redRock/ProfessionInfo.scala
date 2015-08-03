package com.redRock

//import org.apache.spark.SparkContext
//import org.apache.spark.SparkContext._
//import org.apache.spark.SparkConf
//import org.apache.spark.sql.{SQLContext, DataFrame}
import scala.io.Source._

object ProfessionInfo
{
	def loadProfessionsTable():Array[(String,String,String,String)] =
	{
		val professionsPath = "./src/main/resources/Profession/professions.csv"

		/*val professionsDF = SparkContVal.sqlContext.read.format("com.databricks.spark.csv").
							option("header", "true").load(professionsPath)
	    professionsDF.printSchema()
	    professionsDF.registerTempTable("professionsDF")*/

	    var professionsList = Array[(String,String,String,String)]()
	    //skip header = first line
		var header = true
		for (line <- fromFile(professionsPath)("ISO-8859-1").getLines) {
			if (!header)
			{
				val auxLine = line.trim()
				if (auxLine.length != 0)
				{
					val fields = auxLine.split(",")
					if (fields.size == 4)
					{
						professionsList = professionsList :+ (fields(0), fields(1), fields(2), fields(3))
					}
					else if (fields.size == 2)
					{
						professionsList = professionsList :+ (fields(0), fields(1), "", "")
					}
				}
			}
			else{
				header = false
			}
		}
 
 		println(s"Professions Loaded ==> ${professionsList.size}")
 		return professionsList
	}
}