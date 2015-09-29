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
          "type": "word_delimiter",
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
          "type": "long"
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
          "type": "string"
        },
        "user_image_url": {
          "type": "string",
          "index": "not_analyzed"
        },
        "user_followers_count": {
          "type": "long"
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
          "type": "long"
        },
        "tweet_sentiment": {
          "type": "long"
        },
        "tweet_location": {
          "type": "string"
        },
        "tweet_professions": {
          "type": "object"
        },
        "tweet_text_tokens": {
          "type": "string",
          "analyzer": "tweet_analyzer"
        }
      }
    }
  }
}
'

echo ""

