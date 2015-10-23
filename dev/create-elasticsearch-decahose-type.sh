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
  echo " ==========  Deleting ES Redrock Decahose Index ============"
  curl -XDELETE 'http://localhost:9200/redrock_decahose/'
  echo ""
fi

# \u0027 = unicode for '
# asciifolding - converts 'ā, ă, etc' to 'a'

echo " ==========  Creating ES Redrock Decahose Index ============"
curl -XPUT 'http://localhost:9200/redrock_decahose/' -d '
{
"settings": {
    "analysis": {
      "analyzer": {
        "tweet_analyzer_decahose":{
          "type" : "custom",
          "char_filter": "html_strip",
          "tokenizer" : "whitespace",
          "filter" : [ "lowercase"],
          "analyzer" : "tweet_analyzer"
        }
      }
    }
  }
}
'
echo ""

echo " ==========  Creating ES Decahose Type ============"
curl -XPUT 'http://localhost:9200/redrock_decahose/_mapping/processed_tweets' -d '
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
        "index": "not_analyzed"
      },
      "user_image_url": {
        "type": "string",
        "index": "no"
      },
      "user_followers_count": {
        "type": "long",
        "index": "not_analyzed"
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
        "index": "not_analyzed"
      },
      "tweet_sentiment": {
        "type": "integer",
        "index": "not_analyzed"
      },
      "tweet_location": {
        "type": "string",
        "index": "not_analyzed"
      },
      "tweet_professions": {
        "type": "nested",
          "properties" :
          {
            "_1": {
              "type" : "string",
              "index": "not_analyzed"
            },
            "_2":{
              "type" : "string",
              "index": "not_analyzed"
            }
          }
      },
      "created_at_timestamp" : {
        "type": "date",
        "format": "MM/dd HH",
        "index": "not_analyzed"
      },
      "tweet_text_array_tokens" : {
        "type": "string",
        "analyzer": "tweet_analyzer_decahose"
      },
      "created_at_timestamp_day" : {
        "type": "date",
        "format": "MM/dd",
        "index": "not_analyzed"
      }
    }
  }
}
'

echo ""

