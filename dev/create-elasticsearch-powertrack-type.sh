#!/bin/bash
#
# (C) Copyright IBM Corp. 2015, 2015
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if [ -z "$REDROCK_HOME" ]; then echo "REDROCK_HOME is NOT set"; else echo "REDROCK_HOME defined as '$REDROCK_HOME'"; fi

if [ "$1" = "--delete" ]
then
  echo " ==========  Deleting ES Redrock Powertrack Index ============"
  curl -XDELETE 'http://localhost:9200/redrock_powertrack/'
  echo ""
fi

# \u0027 = unicode for '
# asciifolding - converts 'ā, ă, etc' to 'a'

echo " ==========  Creating ES Redrock Powertrack Index ============"
curl -XPUT 'http://localhost:9200/redrock_powertrack/' -d '
{
"settings": {
    "analysis": {
      "filter": {
       "tweet_stop_words":{
           "type" : "stop",
           "ignore_case" : "true",
           "stopwords" : ["a","able","about","above","abst","accordance","according","accordingly","across","act","actually","added","adj","affected","affecting","affects","after","afterwards","again","against","ah","all","almost","alone","along","already","also","although","always","am","among","amongst","an","and","announce","another","any","anybody","anyhow","anymore","anyone","anything","anyway","anyways","anywhere","apparently","approximately","are","aren","arent","arise","around","as","aside","ask","asking","at","auth","available","away","awfully","b","back","be","became","because","become","becomes","becoming","been","before","beforehand","begin","beginning","beginnings","begins","behind","being","believe","below","beside","besides","between","beyond","biol","both","brief","briefly","but","by","c","ca","came","can","cannot","can\u0027t","cause","causes","certain","certainly","co","com","come","comes","contain","containing","contains","could","couldnt","d","date","did","didn\u0027t","different","do","does","doesn\u0027t","doing","done","don\u0027t","down","downwards","due","during","e","each","ed","edu","effect","eg","eight","eighty","either","else","elsewhere","end","ending","enough","especially","et","et-al","etc","even","ever","every","everybody","everyone","everything","everywhere","ex","except","f","far","few","ff","fifth","first","five","fix","followed","following","follows","for","former","formerly","forth","found","four","from","further","furthermore","g","gave","get","gets","getting","give","given","gives","giving","go","goes","gone","got","gotten","h","had","happens","hardly","has","hasn\u0027t","have","haven\u0027t","having","he","hed","hence","her","here","hereafter","hereby","herein","heres","hereupon","hers","herself","hes","hi","hid","him","himself","his","hither","home","how","howbeit","however","hundred","i","id","ie","if","i\u0027ll","im","immediate","immediately","importance","important","in","inc","indeed","index","information","instead","into","invention","inward","is","isn\u0027t","it","itd","it\u0027ll","its","itself","i\u0027ve","j","just","k","keep    keeps","kept","kg","km","know","known","knows","l","largely","last","lately","later","latter","latterly","least","less","lest","let","lets","like","liked","likely","line","little","\u0027ll","look","looking","looks","ltd","m","made","mainly","make","makes","many","may","maybe","me","mean","means","meantime","meanwhile","merely","mg","might","million","miss","ml","more","moreover","most","mostly","mr","mrs","much","mug","must","my","myself","n","na","name","namely","nay","nd","near","nearly","necessarily","necessary","need","needs","neither","never","nevertheless","new","next","nine","ninety","no","nobody","non","none","nonetheless","noone","nor","normally","nos","not","noted","nothing","now","nowhere","o","obtain","obtained","obviously","of","off","often","oh","ok","okay","old","omitted","on","once","one","ones","only","onto","or","ord","other","others","otherwise","ought","our","ours","ourselves","out","outside","over","overall","owing","own","p","page","pages","part","particular","particularly","past","per","perhaps","placed","please","plus","poorly","possible","possibly","potentially","pp","predominantly","present","previously","primarily","probably","promptly","proud","provides","put","q","que","quickly","quite","qv","r","ran","rather","rd","re","readily","really","recent","recently","ref","refs","regarding","regardless","regards","related","relatively","research","respectively","resulted","resulting","results","right","run","s","said","same","saw","say","saying","says","sec","section","see","seeing","seem","seemed","seeming","seems","seen","self","selves","sent","seven","several","shall","she","shed","she\u0027ll","shes","should","shouldn\u0027t","show","showed","shown","showns","shows","significant","significantly","similar","similarly","since","six","slightly","so","some","somebody","somehow","someone","somethan","something","sometime","sometimes","somewhat","somewhere","soon","sorry","specifically","specified","specify","specifying","still","stop","strongly","sub","substantially","successfully","such","sufficiently","suggest","sup","sure    t","take","taken","taking","tell","tends","th","than","thank","thanks","thanx","that","that\u0027ll","thats","that\u0027ve","the","their","theirs","them","themselves","then","thence","there","thereafter","thereby","thered","therefore","therein","there\u0027ll","thereof","therere","theres","thereto","thereupon","there\u0027ve","these","they","theyd","they\u0027ll","theyre","they\u0027ve","think","this","those","thou","though","thoughh","thousand","throug","through","throughout","thru","thus","til","tip","to","together","too","took","toward","towards","tried","tries","truly","try","trying","ts","twice","two","u","un","under","unfortunately","unless","unlike","unlikely","until","unto","up","upon","ups","us","use","used","useful","usefully","usefulness","uses","using","usually","v","value","various","\u0027ve","very","via","viz","vol","vols","vs","w","want","wants","was","wasnt","way","we","wed","welcome","we\u0027ll","went","were","werent","we\u0027ve","what","whatever","what\u0027ll","whats","when","whence","whenever","where","whereafter","whereas","whereby","wherein","wheres","whereupon","wherever","whether","which","while","whim","whither","who","whod","whoever","whole","who\u0027ll","whom","whomever","whos","whose","why","widely","willing","wish","with","within","without","wont","words","world","would","wouldnt","www","x","y","yes","yet","you","youd","you\u0027ll","your","youre","yours","yourself","yourselves","you\u0027ve","z","zero", "\u0027s", "\u0027ll", "\u0027t", "!", ",", "?", ":", ";", "...", ".", "&", "$"]
       }
      },
      "analyzer": {
        "tweet_analyzer_powertrack":{
          "type" : "custom",
          "char_filter": "html_strip",
          "tokenizer" : "whitespace",
          "filter" : [ "lowercase", "tweet_stop_words"],
          "analyzer" : "tweet_analyzer"
        }
      }
    }
  }
}
'
echo ""

echo " ==========  Creating ES Powertrack Type ============"
curl -XPUT 'http://localhost:9200/redrock_powertrack/_mapping/processed_tweets' -d '
{
  "processed_tweets": {
    "_id": {
      "path": "tweet_id"
    },
    "properties": {
      "tweet_id": {
        "type": "string",
        "index": "not_analyzed"
      },
      "tweet_text": {
        "type": "string",
        "index": "no"
      },
      "created_at": {
        "type": "date",
        "format": "dateOptionalTime",
        "index": "not_analyzed"
      },
      "language": {
        "type": "string",
        "index": "no"
      },
      "user_image_url": {
        "type": "string",
        "index": "no"
      },
      "user_followers_count": {
        "type": "long",
        "index": "no"
      },
      "user_name": {
        "type": "string",
        "index": "no"
      },
      "user_handle": {
        "type": "string",
        "index": "no"
      },
      "user_id": {
        "type": "string",
        "fields": {
            "hash": {
              "type": "murmur3"
            }
        }
      },
      "tweet_text_array_tokens" : {
        "type": "string",
        "analyzer": "tweet_analyzer_powertrack"
      }
    }
  }
}
'
echo ""

