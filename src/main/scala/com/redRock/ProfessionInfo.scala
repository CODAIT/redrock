package com.redRock

import scala.io.Source._
import scala.util.matching.Regex

object ProfessionInfo
{
	val professions = loadProfessionsTable()

	def loadProfessionsTable():Array[(Regex,String)] =
	{
		val professionsPath = "./src/main/resources/Profession/professions.csv"

		val professionsList = fromFile(professionsPath)("ISO-8859-1").getLines.drop(1).filter(line => line.trim().length > 0).
												map(line => mapProfession(line)).toArray

 		println(s"Professions Loaded ==> ${professionsList.size}")
 		return professionsList
	}

	//(RegexSubgroup, SuperGroup)
	def mapProfession(line: String): (Regex,String) =
	{
		val fields = line.trim().split(",")
		if (fields.size == 4)
		{
			val regStr = "\\b" + fields(0).trim() + "\\b"
			if(fields(3) == "1")
			{
				return (regStr.r, fields(1))
			}
			return (("(?i)"+regStr).r, fields(1))
		}
		return (("(?i)\\b"+fields(0).trim()+"\\b").r, fields(1))
	}
}