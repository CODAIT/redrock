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
  echo " ==========  Deleting ES Redrock Index ============"
  curl -XDELETE 'http://localhost:9200/redrock/'
  echo ""
fi

echo " ==========  Creating ES Redrock Index ============"
curl -XPUT 'http://localhost:9200/redrock/' -d '
{
  "settings": {
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
  }
}
'

echo ""

echo " ==========  Creating ES Types ============"

$REDROCK_HOME/dev/create-elasticsearch-decahose-type.sh

$REDROCK_HOME/dev/create-elasticsearch-powertrack-type.sh
