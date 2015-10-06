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
package com.redRock

object GetJSONRequest
{
	def getLocationJSONRequest(includeTerms:String, excludeTerms:String): String =
	{
		s"""
		{
		 "query": {
		    "bool": {
		      "must": [
		        {"match": {
		          "tweet_text_tokens": {
		            "query": "$includeTerms", 
		            "operator": "and"
		          }
		        }},
		        {"range" : {"created_at" : {"gte" : "Fri Jan 01 07:12:43 +0000 2015"}}},
		        {"range" : {"created_at" : {"lte" : "Fri Dec 01 07:12:43 +0000 2015"}}}
		      ],
		      "must_not": [
		        {"match": {
		          "tweet_text_tokens": {
		            "query": "$excludeTerms", 
		            "operator": "or"
		          }
		        }},
		        {"match": {
		          "tweet_location": ""
		        }}
		      ]
		    }
		  },
		  "aggs": {
		    "tweet_cnt": {
		      "terms": {
		        "field": "created_at_timestamp",
		        "order" : { "_term" : "asc" }
		      },
		      "aggs": {
		        "tweet_locat": {
		          "terms": {
		            "field": "tweet_location"
		          }
		        }
		      }
		    }
		  }
		}
		"""
	}

	def getSentimentJSONRequest(includeTerms:String, excludeTerms:String): String =
	{
		s"""
		{
		 "query": {
		    "bool": {
		      "must": [
		        {"match": {
		          "tweet_text_tokens": {
		            "query": "$includeTerms", 
		            "operator": "and"
		          }
		        }},
		        {"range" : {"created_at" : {"gte" : "Fri Jan 01 07:12:43 +0000 2015"}}},
		        {"range" : {"created_at" : {"lte" : "Fri Dec 01 07:12:43 +0000 2015"}}}
		      ],
		      "must_not": {
		        "match": {
		          "tweet_text_tokens": {
		            "query": "$excludeTerms", 
		            "operator": "or"
		          }
		        }
		      }
		    }
		  },
		  "aggs": {
		    "tweet_cnt": {
		      "terms": {
		        "field": "created_at_timestamp",
		        "order" : { "_term" : "asc" }
		      },
		      "aggs": {
		        "tweet_sent": {
		          "terms": {
		            "field": "tweet_sentiment",
		            "order" : { "_term" : "asc" }
		          }
		        }
		      }
		    }
		  }
		}
		"""
	}

	def getProfessionJSONRequest(includeTerms:String, excludeTerms:String): String =
	{
		s"""
		{
		  "query": {
		    "bool": {
		      "must": [
		        {
		          "match": {
		            "tweet_text_tokens": {
		              "query": "$includeTerms",
		              "operator": "and"
		            }
		          }
		        },
		        {"range": {"created_at": {"gte": "Fri Jan 01 07:12:43 +0000 2015"}}},
		        {"range": {"created_at": {"lte": "Fri Dec 01 07:12:43 +0000 2015"}}}
		      ],
		      "must_not": [
		        {
		          "match": {
		            "tweet_text_tokens": {
		              "query": "$excludeTerms",
		              "operator": "or"
		            }
		          }
		        }
		      ]
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
		            "field": "tweet_professions._1"
		          },
		          "aggs": {
		            "keywords": {
		              "terms": {
		                "field": "tweet_professions._2"
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

	def getTotalTweetsJSONRequest(): String =
	{
		s"""
		{
		    "query" : {
		        "filtered" : {
		            "filter" : {
		                "range" : {
		                    "created_at": {
		                        "gte" : "Fri Jan 01 07:12:43 +0000 2015",
		                        "lte"  : "Fri Dec 01 07:12:43 +0000 2015"
		                    }
		                }
		            }
		        }
		    }
		}
		"""
	}

	def getTotalFilteredTweetsAndTotalUserJSONRequest(includeTerms:String, excludeTerms:String): String =
	{
		s"""
		{
		  "query": {
		    "bool": {
		      "must": [
		        {"match": {
		          "tweet_text_tokens": {
		            "query": "$includeTerms",
		            "operator": "and"
		          }
		        }},
		        {"range" : {"created_at" : {"gte" : "Fri Jan 01 07:12:43 +0000 2015"}}},
		        {"range" : {"created_at" : {"lte" : "Fri Dec 01 07:12:43 +0000 2015"}}}
		      ],
		      "must_not": {
		        "match": {
		          "tweet_text_tokens": {
		            "query": "$excludeTerms",
		            "operator": "or"
		          }
		        }
		      }
		    }
		  },
		  "aggs": {
		    "distinct_users_by_id": {
		      "cardinality": {
		        "field": "user_id"
		      }
		    }
		  }
		}
		"""
	}

	def getTopTweetsJSONRequest(includeTerms:String, excludeTerms:String, top: Int): String =
	{
		s"""
		{
		  "query": {
		    "bool": {
		      "must": [
		        {"match": {
		          "tweet_text_tokens": {
		            "query": "$includeTerms",
		            "operator": "and"
		          }
		        }},
		        {"match": {
		          "language": "en"
		        }},
		        {"range" : {"created_at" : {"gte" : "Fri Jan 01 07:12:43 +0000 2015"}}},
		        {"range" : {"created_at" : {"lte" : "Fri Dec 01 07:12:43 +0000 2015"}}}
		      ],
		      "must_not": {
		        "match": {
		          "tweet_text_tokens": {
		            "query": "$excludeTerms",
		            "operator": "or"
		          }
		        }
		      }
		    }
		  },
		  "size" : 100,
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
}
