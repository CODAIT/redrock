# Current dir
CURDIR="$(cd "`dirname "$0"`"; pwd)"

# using spark 1.5.0
export SPARK_HOME="/Users/barbaragomes/opt/spark-1.5.0-bin-hadoop2.6"

# generates the new .jar considering new configurations. 
# Run this command separated on cluster, before push code to all nodes. Comment it out on cluster
echo " ==========  Compiling code and generating .jar ============"
sbt compile package assembly

echo "============ running spark =============="
#run program on cluster
$SPARK_HOME/bin/spark-submit --master spark://barbaras-mbp.usca.ibm.com:7077 --packages org.elasticsearch:elasticsearch-spark_2.10:2.1.1 --class com.redRock.Boot target/scala-2.10/redRock.jar
#com.datastax.spark:spark-cassandra-connector_2.10:1.4.0-M2 
