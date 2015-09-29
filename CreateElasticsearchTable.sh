#!/bin/bash

echo " ==========  Creating/Updating ES Schema ============"
curl -XPUT 'http://localhost:9200/redrock/' -d '
{
      "settings" : {
        "number_of_shards" : 1,
        "number_of_replicas" : 0
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
                  "type": "string"
               }
            }
         }
      }
}
'

echo ""

