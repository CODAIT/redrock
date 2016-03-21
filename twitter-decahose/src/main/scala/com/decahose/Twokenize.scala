/**
 * (C) Copyright IBM Corp. 2015, 2016
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
/*
 TweetMotif is licensed under the Apache License 2.0: 
 http://www.apache.org/licenses/LICENSE-2.0.html
 Copyright Brendan O'Connor, Michel Krieger, and David Ahn, 2009-2010.
*/

/*

 Scala port of Brendar O' Connor's twokenize.py

 This is not a direct port, as some changes were made in the aim of
 simplicity.  In the Python version, the @tokenize@ method returned a
 Tokenization object which wrapped a Python List with some extra
 methods.

 The @tokenize@ method given here receives a String and returns the
 tokenized Array[String] of the input text Twokenize.tokenize("foobar
 baz.") => ['foobar', 'baz', '.']

 The main method reads from stdin like it's python counterpart and
 calls the above method on each line

  > scalac twokenize.scala
  > echo "@foo #bar \$1.00 isn't 8:42 p.m. Mr. baz." | scala Twokenize
  @foo $1.0 #bar baz .

 - David Snyder (dsnyder@cs.utexas.edu)
   April 2011

 Modifications to more functional style, fix a few bugs, and making
 output more like twokenize.py. Added abbrevations. Tweaked some
 regex's to produce better tokens.

 - Jason Baldridge (jasonbaldridge@gmail.com)
   June 2011
*/

package com.decahose

import scala.util.matching.Regex

object Twokenize {

  val Contractions = """(?i)(\w+)(n't|'ve|'ll|'d|'re|'s|'m)$""".r
  val Whitespace = """\s+""".r

  val punctChars = """['“\".?!,:;]"""       // scalastyle:ignore
  val punctSeq   = punctChars + """ + """   // scalastyle:ignore
  val entity     = """&(amp|lt|gt|quot);""" // scalastyle:ignore

  //  URLs

  // David: I give the Larry David eye to this whole URL regex
  // (http://www.youtube.com/watch?v=2SmoBvg-etU) There are
  // potentially better options, see:
  //   http://daringfireball.net/2010/07/improved_regex_for_matching_urls
  //   http://mathiasbynens.be/demo/url-regex

  val urlStart1  = """(https?://|www\.)""" // scalastyle:ignore
  val commonTLDs = """(com|co\.uk|org|net|info|ca|ly)""" // scalastyle:ignore
  val urlStart2  = """[A-Za-z0-9\.-]+?\.""" + commonTLDs + """(?=[/ \W])""" // scalastyle:ignore
  val urlBody    = """[^ \t\r\n<>]*?""" // scalastyle:ignore
  val urlExtraCrapBeforeEnd = "("+punctChars+"|"+entity+")+?" // scalastyle:ignore
  val urlEnd     = """(\.\.+|[<>]|\s|$)""" // scalastyle:ignore
  val url        = """\b("""+urlStart1+"|"+urlStart2+")"+urlBody+"(?=("+urlExtraCrapBeforeEnd+")?"+urlEnd+")" // scalastyle:ignore

  // Numeric
  val timeLike   = """\d+:\d+"""  // scalastyle:ignore
  val numNum     = """\d+\.\d+""" // scalastyle:ignore
  val numberWithCommas = """(\d+,)+?\d{3}""" + """(?=([^,]|$))"""

  // Note the magic 'smart quotes' (http://en.wikipedia.org/wiki/Smart_quotes)
  val edgePunctChars = """'\"“”‘’<>«»{}\(\)\[\]""" // scalastyle:ignore
  val edgePunct    = "[" + edgePunctChars + "]" // scalastyle:ignore
  val notEdgePunct = "[a-zA-Z0-9]" // scalastyle:ignore
  val EdgePunctLeft  = new Regex("""(\s|^)("""+edgePunct+"+)("+notEdgePunct+")") // scalastyle:ignore
  val EdgePunctRight = new Regex("("+notEdgePunct+")("+edgePunct+"""+)(\s|$)""") // scalastyle:ignore

  // Abbreviations
  val boundaryNotDot = """($|\s|[“\"?!,:;]|""" + entity + ")"  // scalastyle:ignore
  val aa1  = """([A-Za-z]\.){2,}(?=""" + boundaryNotDot + ")" // scalastyle:ignore
  val aa2  = """[^A-Za-z]([A-Za-z]\.){1,}[A-Za-z](?=""" + boundaryNotDot + ")" // scalastyle:ignore
  val standardAbbreviations = """\b([Mm]r|[Mm]rs|[Mm]s|[Dd]r|[Ss]r|[Jj]r|[Rr]ep|[Ss]en|[Ss]t)\.""" // scalastyle:ignore
  val arbitraryAbbrev = "(" + aa1 +"|"+ aa2 + "|" + standardAbbreviations + ")" // scalastyle:ignore

  val separators  = "(--+|―)" // scalastyle:ignore
  val decorations = """[♫]+""" // scalastyle:ignore
  val thingsThatSplitWords = """[^\s\.,]""" // scalastyle:ignore
  val embeddedApostrophe = thingsThatSplitWords+"""+'""" + thingsThatSplitWords + """+""" // scalastyle:ignore

  //  Emoticons
  val normalEyes = "(?iu)[:=]"
  val wink = "[;]"
  val noseArea = "(|o|O|-)" // rather tight precision, \S might be reasonable...
  val happyMouths = """[D\)\]]"""
  val sadMouths = """[\(\[]"""
  val tongue = "[pP]"
  val otherMouths = """[doO/\\]""" // remove forward slash if http://'s aren't cleaned

  val emoticon = "("+normalEyes+"|"+wink+")" + noseArea + "("+tongue+"|"+otherMouths+"|"+sadMouths+"|"+happyMouths+")" // scalastyle:ignore
                     
  // We will be tokenizing using these regexps as delimiters
  val Protected = new Regex(
    "(" + Array(
      emoticon,
      url,
      entity,
      timeLike,
      numNum,
      numberWithCommas,
      punctSeq,
      arbitraryAbbrev,
      separators,
      decorations,
      embeddedApostrophe
      ).mkString("|") + ")" )

  // The main work of tokenizing a tweet.
  def simpleTokenize (text: String): List[String] = {

    // Do the no-brainers first
    val splitPunctText = splitEdgePunct(text)
    val textLength = splitPunctText.length

    // Find the matches for subsequences that should be protected,
    // e.g. URLs, 1.0, U.N.K.L.E., 12:53
    val matches = Protected.findAllIn(splitPunctText).matchData.toList

    // The spans of the "bads" should not be split.
    val badSpans = matches map (mat => Tuple2(mat.start, mat.end))

    // Create a list of indices to create the "goods", which can be
    // split. We are taking "bad" spans like
    //     List((2,5), (8,10))
    // to create
    //     List(0, 2, 5, 8, 10, 12)
    // where, e.g., "12" here would be the textLength
    val indices = (0 :: badSpans.foldRight(List[Int]())((x,y) => x._1 :: x._2 :: y)) ::: List(textLength) // scalastyle:ignore
    
    // Group the indices and map them to their respective portion of the string
    val goods = indices.grouped(2) map { x => splitPunctText.slice(x(0), x(1)) } toList

    // The 'good' strings are safe to be further tokenized by whitespace
    val splitGoods = goods map { str => str.trim.split(" ").toList }

    // Storing as List[List[String]] to make zip easier later on
    val bads = badSpans map { case(start, end) => List(splitPunctText.slice(start, end)) }

    //  Reinterpolate the 'good' and 'bad' Lists, ensuring that
    //  additonal tokens from last good item get included
    val zippedStr =
      (if (splitGoods.length == bads.length) {
        splitGoods.zip(bads) map { pair => pair._1 ++ pair._2 }
      } else {
        (splitGoods.zip(bads) map { pair => pair._1 ++ pair._2 }) ::: List(splitGoods.last)
      }
     ).flatten

    // Split based on special patterns (like contractions) and check all tokens are non empty
    zippedStr.map(splitToken(_)).flatten.filter(_.length > 0)
  }

  // 'foo' => ' foo '
  def splitEdgePunct (input: String): String = {
    val splitLeft = EdgePunctLeft.replaceAllIn(input, "$1$2 $3")
    EdgePunctRight.replaceAllIn(splitLeft, "$1 $2$3")
  }

  // "foo   bar" => "foo bar"
  def squeezeWhitespace (input: String): String = Whitespace.replaceAllIn(input, " ").trim

  // Final pass tokenization based on special patterns
  def splitToken (token: String): List[String] = {
    token match {
      case Contractions(stem, contr) => List(stem.trim, contr.trim)
      case token => List(token.trim)
    }
  }

  // Apply method allows it to be used as Twokenize(line) in Scala.
  def apply (text: String): List[String] = simpleTokenize(squeezeWhitespace(text))

  // Named for Java coders who would wonder what the heck the 'apply' method is for.
  def tokenize (text: String): List[String] = apply(text)

}