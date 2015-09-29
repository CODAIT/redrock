#!/bin/bash

echo ...Creating Cassandra Keyspaces Column Families and Tables...
#cqlsh -e "DROP KEYSPACE IF EXISTS tweets;"
cqlsh  -e "CREATE KEYSPACE TWEETS WITH REPLICATION = { 'class': 'SimpleStrategy',  'replication_factor':1};"
#cqlsh -e "USE pipeline; DROP TABLE IF EXISTS ratings;"
cqlsh  -e "USE TWEETS; CREATE TABLE PROCESSED_TWEETS (
             TWEET_ID bigint PRIMARY KEY,
             TWEET_TEXT text,
             CREATED_AT timestamp,
             LANGUAGE text,
             USER_IMAGE_URL text,
             USER_FOLLOWERS_COUNT bigint,
             USER_NAME text,
             USER_HANDLE text,
             USER_ID bigint,
             TWEET_SENTIMENT int,
             TWEET_LOCATION text,
             TWEET_PROFESSIONS Map<text,text>,
             TWEET_TEXT_TOKENS text
);"


