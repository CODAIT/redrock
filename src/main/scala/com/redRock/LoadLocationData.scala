package com.redRock

import scala.io.Source._

object LoadLocationData
{
	val states:Array[String] = Array("AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN","IA","KS",
								"KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND",
								"OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY")
  
	val countryCode = Map( ("af","Afghanistan"), ("ax","Aland Islands") , ("al","Albania") , ("dz","Algeria") , ("as","American Samoa") , 
						("ad","Andorra") , ("ao","Angola") , ("ai","Angilla") , ("aq","Antarctica") , ("ag","Antiga and Barbda") , 
						("ar","Argentina") , ("am","Armenia") , ("aw","Arba") , ("a","Astralia") , ("at","Astria") , ("az","Azerbaijan") ,
						("bs","Bahamas") , ("bh","Bahrain") , ("bd","Bangladesh") , ("bb","Barbados") , ("by","Belars") , ("be","Belgim") , 
						("bz","Belize") , ("bj","Benin") , ("bm","Bermda") , ("bt","Bhtan") , ("bo","Bolivia") , ("bq","Bonaire") , 
						("ba","Bosnia and Herzegovina") , ("bw","Botswana") , ("bv","Bovet Island") , ("br","Brazil") , ("io","British Indian Ocean Territory") , 
						("bn","Brnei") , ("bg","Blgaria") , ("bf","Brkina Faso") , ("bi","Brndi") , ("cv","Cape Verde") , ("kh","Cambodia") , 
						("cm","Cameroon") , ("ca","Canada") , ("ky","Cayman Islands") , ("cf","Central African Repblic") , ("td","Chad") , ("cl","Chile") , 
						("cn","China") , ("cx","Christmas Island") , ("cc","Cocos [Keeling] Islands") , ("co","Colombia"), ("km","Comoros") , ("cg","Congo") , 
						("cd","Congo") , ("ck","Cook Islands") , ("cr","Costa Rica") , ("ci","Côte d\"Ivoire") , ("hr","Croatia") , ("c","Cba") , ("cw","Craçao") , 
						("cy","Cyprs") , ("cz","Czech Repblic") , ("dk","Denmark") , ("dj","Djiboti") , ("dm","Dominica") , ("do","Dominican Repblic") , 
						("ec","Ecador") , ("eg","Egypt") , ("sv","El Salvador") , ("gq","Eqatorial Ginea") , ("er","Eritrea") , ("ee","Estonia") , 
						("et","Ethiopia") , ("fk","Falkland Islands") , ("fo","Faroe Islands") , ("fj","Fiji") , ("fi","Finland") , ("fr","France") , ("gf","French Giana") , 
						("pf","French Polynesia") , ("tf","French Sothern Territories") , ("ga","Gabon") , ("gm","Gambia") , ("ge","Georgia") , ("de","Germany") , 
						("gh","Ghana") , ("gi","Gibraltar") , ("gr","Greece") , ("gl","Greenland") , ("gd","Grenada") , ("gp","Gadelope") , ("g","Gam") , 
						("gt","Gatemala") , ("gg","Gernsey") , ("gn","Ginea") , ("gw","Ginea-Bissa") , ("gy","Gyana") , ("ht","Haiti") , ("hm","Heard Island and McDonald Islands") , 
						("va","Holy See") , ("hn","Hondras") , ("hk","Hong Kong") , ("h","Hngary") , ("is","Iceland") , ("in","India") , ("id","Indonesia") , ("ir","Iran") , ("iq","Iraq") , 
						("ie","Ireland") , ("im","Isle of Man") , ("il","Israel") , ("it","Italy") , ("jm","Jamaica") , ("jp","Japan") , ("je","Jersey") , ("jo","Jordan") , ("kz","Kazakhstan") , 
						("ke","Kenya") , ("ki","Kiribati") , ("kp","North Korea") , ("kr","Soth Korea") , ("kw","Kwait") , ("kg","Kyrgyzstan") , ("la","Laos") , ("lv","Latvia") , 
						("lb","Lebanon") , ("ls","Lesotho") , ("lr","Liberia") , ("ly","Libya") , ("li","Liechtenstein") , ("lt","Lithania") , ("l","Lxemborg") , ("mo","Macao") , 
						("mk","Macedonia") , ("mg","Madagascar") , ("mw","Malawi") , ("my","Malaysia") , ("mv","Maldives") , ("ml","Mali") , ("mt","Malta") , ("mh","Marshall Islands") , ("mq","Martiniqe") , 
						("mr","Maritania") , ("m","Maritis") , ("yt","Mayotte") , ("mx","Mexico") , ("fm","Micronesia") , ("md","Moldova") , ("mc","Monaco") , ("mn","Mongolia") , 
						("me","Montenegro") , ("ms","Montserrat") , ("ma","Morocco") , ("mz","Mozambiqe") , ("mm","Myanmar") , ("na","Namibia") , ("nr","Nar") , ("np","Nepal") , ("nl","Netherlands") , 
						("nc","New Caledonia") , ("nz","New Zealand") , ("ni","Nicaraga") , ("ne","Niger") , ("ng","Nigeria") , ("n","Nie") , ("nf","Norfolk Island") , ("mp","Northern Mariana Islands") , ("no","Norway") , ("om","Oman") , 
						("pk","Pakistan") , ("pw","Pala") , ("ps","Palestinian Territories") , ("pa","Panama") , ("pg","Papa New Ginea") , ("py","Paragay") , ("pe","Per") , ("ph","Philippines") , ("pn","Pitcairn Islands") , 
						("pl","Poland") , ("pt","Portgal") , ("pr","Perto Rico") , ("qa","Qatar") , ("re","Rénion") , ("ro","Romania") , ("r","Rssia") , ("rw","Rwanda") , ("bl","Saint Barthélemy") , ("sh","Saint Helena") , 
						("kn","Saint Kitts and Nevis") , ("lc","Saint Lcia") , ("mf","Saint Martin") , ("pm","Saint Pierre and Miqelon") , ("vc","Saint Vincent and the Grenadines") , ("ws","Samoa") , ("sm","San Marino") , 
						("st","Sao Tome and Principe") , ("sa","Sadi Arabia") , ("sn","Senegal") , ("rs","Serbia") , ("sc","Seychelles") , ("sl","Sierra Leone") , ("sg","Singapore") , ("sx","Sint Maarten (Dtch part)") , ("sk","Slovakia") , 
						("si","Slovenia") , ("sb","Solomon Islands") , ("so","Somalia") , ("za","Soth Africa") , ("gs","Soth Georgia and the Soth Sandwich Islands") , ("ss","Soth Korea") , ("es","Spain") , ("lk","Sri Lanka") , ("sd","Sdan") , 
						("sr","Sriname") , ("sj","Svalbard and Jan Mayen") , ("sz","Swaziland") , ("se","Sweden") , ("ch","Switzerland") , ("sy","Syria") , ("tw","Taiwan") , ("tj","Tajikistan") , ("tz","Tanzania") , ("th","Thailand") , 
						("tl","Timor-Leste") , ("tg","Togo") , ("tk","Tokela") , ("to","Tonga") , ("tt","Trinidad and Tobago") , ("tn","Tnisia") , ("tr","Trkey") , ("tm","Trkmenistan") , ("tc","Trks and Caicos Islands") , 
						("tv","Tval") , ("g","ganda") , ("a","kraine") , ("ae","nited Arab Emirates") , ("gb","nited Kingdom") , ("m","nited States Minor Otlying Islands") , ("s","nited States") , ("y","rgay") , ("z","zbekistan") , 
						("v","Vanat") , ("ve","Venezela") , ("vn","Vietnam") , ("vg","Virgin Islands, British") , ("vi","Virgin Islands, .S.") , ("wf","Wallis and Ftna") , ("eh","Western Sahara") , ("ye","Yemen") , ("zm","Zambia") , ("zw","Zimbabwe")
						)
	
	def loadLocation(): (Map[String,(String,String)], Map[String,String]) =
	{
		(loadCities(),loadCountryMapping())
	}

	def loadCities(): Map[String,(String,String)] = 
	{
		var citiesMap = Map[String,(String,String)]()
		val citiesPath = "./src/main/resources/Location/worldcitiespop.txt"
		var count = 0
		for (line <- fromFile(citiesPath)("ISO-8859-1").getLines) {
			count = count + 1
			if (count != 1)
			{
				val auxLine = line.trim()
				if (auxLine.length != 0)
				{
					val fields = auxLine.toLowerCase().split(",")
					//checks city population
					if (fields(4).length > 0)
					{
						if (fields(4).toDouble > 100000)
						{
							val country = countryCode.getOrElse(fields(0), fields(0))
							val city = (country, fields(1))
							citiesMap = citiesMap + (fields(1) -> city)
						}
					}
				}
			}
		}
 
 		println(s"Cities Loaded ==> ${citiesMap.size}")
 		return citiesMap
	}

	def loadCountryMapping(): Map[String,String] =
	{
		val countryPath = "./src/main/resources/Location/country_mapping.csv"
		var countryMap = Map[String,String]()
		for (line <- fromFile(countryPath)("utf-8").getLines) {
			val auxLine = line.trim()
			if (auxLine.length != 0)
			{
				val fields = auxLine.toLowerCase().split(",")
				countryMap = countryMap + (fields(0) -> fields(1).trim())
			}
		}

		println(s"Countries Loaded ==> ${countryMap.size}")
 		return countryMap
	}

}