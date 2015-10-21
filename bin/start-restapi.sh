#!/usr/bin/env bash
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

# using environment variable to find Spark home directory
if [ -z "$SPARK_HOME" ]; then echo "SPARK_HOME is NOT set"; else echo "SPARK_HOME defined as '$SPARK_HOME'"; fi
# using environment variable to find RedRock home directory
if [ -z "$REDROCK_HOME" ]; then echo "REDROCK_HOME is NOT set"; else echo "REDROCK_HOME defined as '$REDROCK_HOME'"; fi

# generates the new .jar considering new configurations.
# Run this command separated on cluster, before push code to all nodes. Comment it out on cluster
echo " ==========  Compiling code and generating .jar ============"
sbt compile
sbt 'project redrock-restapi' compile package assembly

# Changes the location where spark is being launched so it creates its own metastore_db
cd $REDROCK_HOME/rest-api

echo "============ Starting REST API =============="
HOSTNAME="$(/bin/hostname -f)"
nohup $SPARK_HOME/bin/spark-submit --driver-memory 1g --master spark://$HOSTNAME:7077 --class com.restapi.Application $REDROCK_HOME/rest-api/target/scala-2.10/redrock-rest-api.jar > $REDROCK_HOME/rest-api/nohup-restapi.out&

echo "========== REST API Started - Check nodup_restapi.out ================="

