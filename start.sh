# Current dir
CURDIR="$(cd "`dirname "$0"`"; pwd)"

# Export this as SPARK_HOME
export SPARK_HOME="/Users/barbaragomes/Projects/ApacheSparkFork/spark"

# generates the new .jar considering new configurations
echo " ==========  Compiling code and generating .jar ============"
sbt compile package assembly

echo "============ running spark =============="
#run program
$SPARK_HOME/bin/spark-submit --master local --class com.redRock.Boot target/scala-2.10/redRock.jar