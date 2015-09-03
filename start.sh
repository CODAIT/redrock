# Current dir
CURDIR="$(cd "`dirname "$0"`"; pwd)"

# Export this as SPARK_HOME
#commit spark 1.5 69930310115501f0de094fe6f5c6c60dade342bd
export SPARK_HOME="/Users/barbaragomes/opt/spark-1.5-hadoop2.6"

# generates the new .jar considering new configurations. 
# Run this command separated on cluster, before push code to all nodes. Comment it out on cluster
echo " ==========  Compiling code and generating .jar ============"
sbt compile package assembly

echo "============ running spark =============="
#run program on cluster
$SPARK_HOME/bin/spark-submit --master spark://barbaras-mbp.usca.ibm.com:7077 --driver-memory=2g --class com.redRock.Boot target/scala-2.10/redRock.jar
#run program locally
#$SPARK_HOME/bin/spark-submit --master local[*] --driver-memory=8g --executor-memory=8g --class com.redRock.Boot target/scala-2.10/redRock.jar 