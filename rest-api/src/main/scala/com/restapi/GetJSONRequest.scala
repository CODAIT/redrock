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
package com.restapi

object GetJSONRequest
{
  def getLocationJSONRequest(includeTerms:String, excludeTerms:String, startDatetime: String, endDatetime: String): String =
  {
    s"""
    {
       "query": {
       "filtered":{
         "filter":{
           "bool":{
              "must":[
                {"range" : {
                    "created_at" : {
                      "from" : "$startDatetime",
                      "to" : "$endDatetime"
                    }
                }},
                {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "and"}}
              ],
              "must_not": [
                { "terms" : {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}},
                { "term" : { "tweet_location" : ""}}
              ]

            }
         }
        }
      },
      "aggs": {
        "tweet_cnt": {
          "terms": {
            "field": "${LoadConf.restConf.getString("groupByESField")}",
            "order" : { "_term" : "asc" },
            "size": 10000
          },
          "aggs": {
            "tweet_locat": {
              "terms": {
                "field": "tweet_location",
                "size": 10000
              }
            }
          }
        }
      }
    }
    """
  }

  /* Don't need to sort the field tweet_sentiment beucase the transformation is using a map
  since the result not always returns all fields */
  def getSentimentJSONRequest(includeTerms:String, excludeTerms:String, startDatetime: String, endDatetime: String): String =
  {
    s"""
    {
      "query": {
        "filtered":{
         "filter":{
           "bool":{
              "must":[
                {"range" : {
                    "created_at" : {
                      "from" : "$startDatetime",
                      "to" : "$endDatetime"
                    }
                }},
                {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "and"}}
              ],
              "must_not":
                { "terms" : {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}}
            }
         }
        }
      },
      "aggs": {
        "tweet_cnt": {
          "terms": {
            "field": "${LoadConf.restConf.getString("groupByESField")}",
            "order" : { "_term" : "asc" },
            "size": 10000
          },
          "aggs": {
            "tweet_sent": {
              "terms": {
                "field": "tweet_sentiment",
                "size": 10000
              }
            }
          }
        }
      }
    }
    """
  }

  def getProfessionJSONRequest(includeTerms:String, excludeTerms:String, startDatetime: String, endDatetime: String): String =
  {
    s"""
    {
      "query": {
        "filtered":{
         "filter":{
           "bool":{
              "must":[
                {"range" : {
                    "created_at" : {
                      "from" : "$startDatetime",
                      "to" : "$endDatetime"
                    }
                }},
                {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "and"}}
              ],
              "must_not":
                { "terms" : {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}}
            }
         }
        }
      },
      "aggs": {
        "tweet_professions": {
          "nested": {
            "path": "tweet_professions"
          },
          "aggs": {
            "professions": {
              "terms": {
                "field": "tweet_professions._1",
                "size": 10000
              },
              "aggs": {
                "keywords": {
                  "terms": {
                    "field": "tweet_professions._2",
                    "size": 10000
                  }
                }
              }
            }
          }
        }
      }
    }
    """
  }

  def getTotalTweetsJSONRequest(startDatetime: String, endDatetime: String): String =
  {
    s"""
    {
        "query" : {
            "filtered" : {
                "filter" : {
                    "range" : {
                        "created_at": {
                            "gte" : "$startDatetime",
                            "lte"  : "$endDatetime"
                        }
                    }
                }
            }
        }
    }
    """
  }

  def getTotalFilteredTweetsAndTotalUserJSONRequest(includeTerms:String, excludeTerms:String, startDatetime: String, endDatetime: String): String =
  {
    s"""
    {
      "query": {
        "filtered":{
         "filter":{
           "bool":{
              "must":[
                {"range" : {
                    "created_at" : {
                      "from" : "$startDatetime",
                      "to" : "$endDatetime"
                    }
                }},
                {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "and"}}
              ],
              "must_not":
                { "terms" : {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}}
            }
         }
        }
      },
      "aggs": {
        "distinct_users_by_id": {
          "cardinality": {
            "field": "user_id.hash"
          }
        }
      }
    }
    """
  }

  def getTopTweetsJSONRequest(includeTerms:String, excludeTerms:String, top: Int, startDatetime: String, endDatetime: String): String =
  {
    s"""
    {
      "query": {
        "filtered":{
         "filter":{
           "bool":{
              "must":[
                {"range" : {
                    "created_at" : {
                      "from" : "$startDatetime",
                      "to" : "$endDatetime"
                    }
                }},
                {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "and"}},
                {"term": {"language": "en"}}
              ],
              "must_not":
                {"terms": {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}}
            }
         }
        }
      },
      "size" : $top,
      "sort": [
        {
          "user_followers_count": {
            "order": "desc"
          }
        }
      ]
    }
    """
  }

  def getTweetsTextBySentimentAndDate(includeTerms:String, excludeTerms:String, startDatetime: String, endDatetime: String, sentiment: Int) =
  {
    s"""{
        "query": {
            "filtered": {
                "filter": {
                    "bool": {
                        "must": [
                            {
                                "range": {
                                    "created_at": {
                                        "from": "$startDatetime",
                                        "to": "$endDatetime"
                                    }
                                }
                            },
                            {
                                "term": {
                                    "tweet_sentiment": "$sentiment"
                                }
                            },
                            {
                                "term": {
                                    "language": "en"
                                }
                            },
                            {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "and"}}
                        ],
                        "must_not":
       										{"terms": {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}}
                    }
                }
            }
        },
        "fields": [
            "tweet_text"
        ],
        "size": 5000
    }"""
  }

  def getPowertrackWordCountAndTweets(includeTerms:String, excludeTerms:String, statDate:String, endDate:String, topTweets:Int, topWords:Int): String =
  {
    /* Created by: Nakul Jindal
    * Modified by: Barbara Gomes
    * include terms for powertrack will use OR condition because of SparkSummit and SparkSummitEU
    */
    s"""{
        "size": $topTweets,
        "query": {
            "filtered": {
                "filter": {
                    "bool": {
                        "must": [
                            {
                                "range": {
                                    "created_at": {
                                        "from": "$statDate",
                                        "to": "$endDate"
                                    }
                                }
                            },
                            {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "or"}}
                        ],
                        "must_not": {"terms": { "tweet_text_array_tokens": [$excludeTerms],"execution": "or"}}
                    }
                }
            }
        },
        "aggs": {
            "top_words": {
                "terms": {
                    "field": "tweet_text_array_tokens",
                    "size": $topWords
                }
            }
        },
        "sort": [
          {
            "created_at": {
              "order": "desc"
            }
          }
        ]
    }"""
  }

  /* used just for powertrack, if decahose will use it, the execution for includeterms must be "and" */
  def getTotalRetweets(includeTerms:String, excludeTerms:String, startDatetime: String, endDatetime: String): String =
  {
    s"""
    {
      "query": {
        "filtered":{
         "filter":{
           "bool":{
              "must":[
                {"range" : {
                    "created_at" : {
                      "from" : "$startDatetime",
                      "to" : "$endDatetime"
                    }
                }},
                {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "or"}},
                {"terms": {"tweet_text_array_tokens" : ["rt"]}}
              ],
              "must_not":
                { "terms" : {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}}
            }
         }
        }
      }
    }
    """
  }

  def getTotalFilteredTweets(includeTerms:String, excludeTerms:String, startDatetime: String, endDatetime: String, includeCondition: String): String =
  {
    s"""
    {
      "query": {
        "filtered":{
         "filter":{
           "bool":{
              "must":[
                {"range" : {
                    "created_at" : {
                      "from" : "$startDatetime",
                      "to" : "$endDatetime"
                    }
                }},
                {"terms": {"tweet_text_array_tokens" : [$includeTerms], "execution" : "$includeCondition"}}
              ],
              "must_not":
                { "terms" : {"tweet_text_array_tokens" : [$excludeTerms], "execution" : "or"}}
            }
         }
        }
      }
    }
    """
  }
}
