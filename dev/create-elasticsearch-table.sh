#!/bin/bash

if [ "$1" = "--delete" ]
then
  echo " ==========  Deleting ES Schema ============"
  curl -XDELETE 'http://localhost:9200/redrock/'
  echo ""
fi

echo " ==========  Creating ES Schema ============"
curl -XPUT 'http://localhost:9200/redrock/' -d '
{
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "analysis": {
      "filter": {
        "tweet_filter": {
          "type": "standard",
          "type_table": [
            "# => ALPHA",
            "@ => ALPHA"
          ]
        }
      },
      "analyzer": {
        "tweet_analyzer": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": [
            "lowercase",
            "tweet_filter"
          ]
        }
      }
    }
  },
  "mappings": {
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
          "index": "not_analyzed"
        },
        "created_at": {
          "type": "date",
          "format": "EEE MMM dd HH:mm:ss Z yyyy"
        },
        "language": {
          "type": "string",
          "index": "not_analyzed"
        },
        "user_image_url": {
          "type": "string",
          "index": "not_analyzed"
        },
        "user_followers_count": {
          "type": "long",
          "index": "not_analyzed"
        },
        "user_name": {
          "type": "string",
          "index": "not_analyzed"
        },
        "user_handle": {
          "type": "string",
          "index": "not_analyzed"
        },
        "user_id": {
          "type": "long",
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
        "tweet_text_tokens": {
          "type": "string",
          "analyzer": "tweet_analyzer"
        },
        "created_at_timestamp" : {
          "type": "date",
          "format": "MM/dd HH",
          "index": "not_analyzed"
        }
      }
    }
  }
}
'

echo ""

