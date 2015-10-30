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

import org.slf4j.LoggerFactory

import scala.io.Source._
import scala.util.matching.Regex

object ProfessionInfo
{
	val logger = LoggerFactory.getLogger(this.getClass)
	val professions = loadProfessionsTable()

	def loadProfessionsTable():Array[(Regex,String)] =
	{
		val professionsPath = LoadConf.globalConf.getString("homePath") + "/twitter-decahose/src/main/resources/Profession/professions.csv"

		val professionsList = fromFile(professionsPath)("ISO-8859-1").getLines.drop(1).filter(line => line.trim().length > 0).
												map(line => mapProfession(line)).toArray

 		logger.info(s"Professions Loaded ==> ${professionsList.size}")
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
