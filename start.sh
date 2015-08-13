# Current dir
CURDIR="$(cd "`dirname "$0"`"; pwd)"

# Export this as SPARK_HOME
#commit spark 1.5 69930310115501f0de094fe6f5c6c60dade342bd
export SPARK_HOME="/Users/barbaragomes/Projects/ApacheSparkFork/spark"
#"/Users/barbaragomes/opt/spark-1.4.1-bin-hadoop2.6"
#"/Users/barbaragomes/Projects/ApacheSparkFork/spark" 
#
# generates the new .jar considering new configurations
echo " ==========  Compiling code and generating .jar ============"
sbt compile package assembly

echo "============ running spark =============="
#run program
$SPARK_HOME/bin/spark-submit --master local[3] --driver-memory=1g --class com.redRock.Boot target/scala-2.10/redRock.jar