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

import java.io._
import java.util.zip._
import scala.io.Source._


object LoadLocationData
{
	val states:Array[String] = Array("AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS",
								"KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND",
								"OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY")

	val countryCode = Map(("af","Afghanistan"),("ax","Aland Islands"),("al","Albania"),("dz","Algeria"),("as","American Samoa"),("ad","Andorra"),("ao","Angola"),
				("ai","Anguilla"),("aq","Antarctica"),("ag","Antigua and Barbuda"),("ar","Argentina"),("am","Armenia"),("aw","Aruba"),("au","Australia"),
				("at","Austria"),("az","Azerbaijan"),("bs","Bahamas"),("bh","Bahrain"),("bd","Bangladesh"),("bb","Barbados"),("by","Belarus"),("be","Belgium"),
				("bz","Belize"),("bj","Benin"),("bm","Bermuda"),("bt","Bhutan"),("bo","Bolivia"),("bq","Bonaire"),("ba","Bosnia and Herzegovina"),("bw","Botswana"),
				("bv","Bouvet Island"),("br","Brazil"),("io","British Indian Ocean Territory"),("bn","Brunei"),("bg","Bulgaria"),("bf","Burkina Faso"),("bi","Burundi"),
				("cv","Cape Verde"),("kh","Cambodia"),("cm","Cameroon"),("ca","Canada"),("ky","Cayman Islands"),("cf","Central African Republic"),("td","Chad"),("cl","Chile"),
				("cn","China"),("cx","Christmas Island"),("cc","Cocos [Keeling] Islands"),("co","Colombia"),("km","Comoros"),("cg","Congo"),("cd","Congo"),("ck","Cook Islands"),
				("cr","Costa Rica"),("ci","Côte d'Ivoire"),("hr","Croatia"),("cu","Cuba"),("cw","Curaçao"),("cy","Cyprus"),("cz","Czech Republic"),("dk","Denmark"),("dj","Djibouti"),
				("dm","Dominica"),("do","Dominican Republic"),("ec","Ecuador"),("eg","Egypt"),("sv","El Salvador"),("gq","Equatorial Guinea"),("er","Eritrea"),("ee","Estonia"),("et","Ethiopia"),
				("fk","Falkland Islands"),("fo","Faroe Islands"),("fj","Fiji"),("fi","Finland"),("fr","France"),("gf","French Guiana"),("pf","French Polynesia"),("tf","French Southern Territories"),
				("ga","Gabon"),("gm","Gambia"),("ge","Georgia"),("de","Germany"),("gh","Ghana"),("gi","Gibraltar"),("gr","Greece"),("gl","Greenland"),("gd","Grenada"),("gp","Guadeloupe"),("gu","Guam"),
				("gt","Guatemala"),("gg","Guernsey"),("gn","Guinea"),("gw","Guinea-Bissau"),("gy","Guyana"),("ht","Haiti"),("hm","Heard Island and McDonald Islands"),("va","Holy See"),("hn","Honduras"),
				("hk","Hong Kong"),("hu","Hungary"),("is","Iceland"),("in","India"),("id","Indonesia"),("ir","Iran"),("iq","Iraq"),("ie","Ireland"),("im","Isle of Man"),("il","Israel"),("it","Italy"),("jm","Jamaica"),
				("jp","Japan"),("je","Jersey"),("jo","Jordan"),("kz","Kazakhstan"),("ke","Kenya"),("ki","Kiribati"),("kp","North Korea"),("kr","South Korea"),("kw","Kuwait"),("kg","Kyrgyzstan"),("la","Laos"),("lv","Latvia"),("lb","Lebanon"),
				("ls","Lesotho"),("lr","Liberia"),("ly","Libya"),("li","Liechtenstein"),("lt","Lithuania"),("lu","Luxembourg"),("mo","Macao"),("mk","Macedonia"),("mg","Madagascar"),("mw","Malawi"),("my","Malaysia"),("mv","Maldives"),
				("ml","Mali"),("mt","Malta"),("mh","Marshall Islands"),("mq","Martinique"),("mr","Mauritania"),("mu","Mauritius"),("yt","Mayotte"),("mx","Mexico"),("fm","Micronesia"),("md","Moldova"),("mc","Monaco"),("mn","Mongolia"),
				("me","Montenegro"),("ms","Montserrat"),("ma","Morocco"),("mz","Mozambique"),("mm","Myanmar"),("na","Namibia"),("nr","Naur"),("np","Nepal"),("nl","Netherlands"),("nc","New Caledonia"),("nz","New Zealand"),("ni","Nicaragua"),
				("ne","Niger"),("ng","Nigeria"),("nu","Niue"),("nf","Norfolk Island"),("mp","Northern Mariana Islands"),("no","Norway"),("om","Oman"),("pk","Pakistan"),("pw","Palau"),("ps","Palestinian Territories"),("pa","Panama"),("pg","Papua New Guinea"),
				("py","Paraguay"),("pe","Peru"),("ph","Philippines"),("pn","Pitcairn Islands"),("pl","Poland"),("pt","Portugal"),("pr","Puerto Rico"),("qa","Qatar"),("re","Réunion"),("ro","Romania"),("ru","Russia"),("rw","Rwanda"),("bl","Saint Barthélemy"),
				("sh","Saint Helena"),("kn","Saint Kitts and Nevis"),("lc","Saint Lucia"),("mf","Saint Martin"),("pm","Saint Pierre and Miquelon"),("vc","Saint Vincent and the Grenadines"),("ws","Samoa"),("sm","San Marino"),("st","Sao Tome and Principe"),
				("sa","Saudi Arabia"),("sn","Senegal"),("rs","Serbia"),("sc","Seychelles"),("sl","Sierra Leone"),("sg","Singapore"),("sx","Sint Maarten (Dutch part)"),("sk","Slovakia"),("si","Slovenia"),("sb","Solomon Islands"),("so","Somalia"),("za","South Africa"),
				("gs","South Georgia and the South Sandwich Islands"),("ss","South Korea"),("es","Spain"),("lk","Sri Lanka"),("sd","Sudan"),("sr","Suriname"),("sj","Svalbard and Jan Mayen"),("sz","Swaziland"),("se","Sweden"),("ch","Switzerland"),("sy","Syria"),("tw","Taiwan"),
				("tj","Tajikistan"),("tz","Tanzania"),("th","Thailand"),("tl","Timor-Leste"),("tg","Togo"),("tk","Tokelau"),("to","Tonga"),("tt","Trinidad and Tobago"),("tn","Tunisia"),("tr","Turkey"),("tm","Turkmenistan"),("tc","Turks and Caicos Islands"),("tv","Tuvalu"),("ug","Uganda"),
				("ua","Ukraine"),("ae","United Arab Emirates"),("gb","United Kingdom"),("um","United States Minor Outlying Islands"),("us","United States"),("uy","Uruguay"),("uz","Uzbekistan"),("vu","Vanuatu"),("ve","Venezuela"),("vn","Vietnam"),("vg","Virgin Islands, British"),("vi","Virgin Islands, U.S."),
				("wf","Wallis and Futuna"),("eh","Western Sahara"),("ye","Yemen"),("zm","Zambia"),("zw","Zimbabwe"))

	val cities = loadCities()
	val countries = loadCountryMapping()
	val cities_keys = cities.keys.toArray.sortBy(key => -key.length)

	def loadCities(): Map[String,(String,Double)] =
	{
		val citiesPath = LoadConf.globalConf.getString("homePath") + "/twitter-decahose/src/main/resources/Location/worldcitiespop.txt.gz"
		val citiesStream = new GZIPInputStream(new FileInputStream(citiesPath))
		val citiesMap = fromInputStream(citiesStream)("ISO-8859-1").getLines.drop(1).filter(line => filterCityLine(line)).
											map(line => mapCity(line)).toArray.sortBy(city => city._2._2).toMap

 		println(s"Cities Loaded ==> ${citiesMap.size}")
 		return citiesMap
	}

	def mapCity(line: String): (String, (String,Double)) =
	{
		val fields = line.trim().toLowerCase().split(",")
		val country = countryCode.getOrElse(fields(0), fields(0))
		(fields(1), (country, fields(4).toDouble))
	}

	def filterCityLine(line: String): Boolean =
	{
		val auxLine = line.trim()
		if (auxLine.length != 0)
		{
			val fields = auxLine.toLowerCase().split(",")
			if (fields(4).length > 0 && fields(4).toDouble > 100000)
			{
				return true
			}
		}
		return false
	}

	def loadCountryMapping(): Map[String,String] =
	{
		val countryPath = LoadConf.globalConf.getString("homePath") + "/twitter-decahose/src/main/resources/Location/country_mapping.csv"
		val countryMap = fromFile(countryPath)("utf-8").getLines.map(line => proccessCountryLine(line)).
														filter(country => country != ("" -> "")).toMap

		println(s"Countries Loaded ==> ${countryMap.size}")
 		return countryMap
	}

	def proccessCountryLine(line: String): (String,String) =
	{
		val auxLine = line.trim()
		if (auxLine.length != 0)
		{
			val fields = auxLine.split(",")
			return (fields(0).trim().toLowerCase() -> fields(1).trim())
		}
		return ("" -> "")
	}

}
