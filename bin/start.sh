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

# Current dir
CURDIR="$(cd "`dirname "$0"`"; pwd)"

# using environment variable to find Spark home directory
if [ -z "$SPARK_HOME" ]; then echo "SPARK_HOME is NOT set"; else echo "SPARK_HOME defined as '$SPARK_HOME'"; fi

# generates the new .jar considering new configurations.
# Run this command separated on cluster, before push code to all nodes. Comment it out on cluster
echo " ==========  Compiling code and generating .jar ============"
sbt compile package assembly

echo "============ running spark =============="
#run program on cluster
HOSTNAME="$(/bin/hostname -f)"
$SPARK_HOME/bin/spark-submit --master spark://$HOSTNAME:7077 --packages org.elasticsearch:elasticsearch-spark_2.10:2.1.1 --class com.redRock.Boot target/scala-2.10/redRock.jar
#com.datastax.spark:spark-cassandra-connector_2.10:1.4.0-M2
