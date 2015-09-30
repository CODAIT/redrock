
# RedRock Server

## Overview

How to configure environment and **RedRock** code to run in standalone mode

### Environment Setup

#### Hadoop 

Install hadoop 2.6+

For information about how to configure and execute hadoop on standalone mode see: <http://amodernstory.com/2014/09/23/installing-hadoop-on-mac-osx-yosemite/>

Create hdfs directory that will be used by RedRock

- /user/hadoop/decahose_historical
- /user/hadoop/decahose_streaming
- /user/hadoop/spark_streaming_checkpoint

Use command **hadoop fs -mkdir -p directory_path** to create a hdfs directory
 
#### Elasticsearch

Download Elasticsearch 1.7.1 (<https://www.elastic.co/downloads/elasticsearch>) and decompress it.

Install Marvel in order to easily use Elasticsearch (<https://www.elastic.co/downloads/marvel>)
#### Spark 1.5.0

Download pre-built Spark 1.5.0 for Hadoop 2.6 and later and decompress it.

###### Configuring Spark standalone mode

1. Configure environment variable SPARK_HOME with the path to your Spark directory, for example: **/Users/barbaragomes/opt/spark-1.5.0-bin-hadoop2.6** 
2. Save file conf/slaves.template as conf/slaves
3. Edit file conf/spark-env.sh.template and add the following lines (Save it as onf/spark-env.sh):  
    * **HADOOP_CONF_DIR**=/usr/local/Cellar/hadoop/2.7.0/libexec/etc/hadoop/ (Hadoop home path)
    * **SPARK_WORKER_DIR**=/Users/barbaragomes/opt/SparkData (Directory to save Spark logs)
    * **SPARK_WORKER_INSTANCES**=3
    * **SPARK_EXECUTOR_MEMORY**=3g

4. Edit file conf/log4j.properties.template and change log4j.rootCategory=WARN. Save it as  conf/log4j.properties

#### SBT plugin

Install sbt plugin. More information at <http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Mac.html>

### Starting applications

Before running **RedRock** you must start all the following applications:

1. **Hadoop**: Use command --> hstart
2. **Elasticsearch**: Inside elasticsearch home path use command --> nohup ./bin/elasticsearch & tail -f nohup.out 
3. **Spark**: Inside Spark home path use command --> ./sbin/start-all.sh

### Configuring RedRock

Edit file **src/main/scala/com/redRock/Config.scala** in RedRock:

1. **redRockHomePath**: RedRock home path
2. **streamingBatchTime**: batch time for spark streaming
2. Check if the directory specified in the variables below exists (you can use any directory path you want to, just keep mind what each one of them means):
    * twitterHistoricalDataPath
    * twitterStreamingDataPath
    * checkPointDirForStreaming
    
### Understanding Data Directories

The pre-process of the tweets is going to happen through Spark. Spark will use the following directories configured in Config.scala:

1. **twitterHistoricalDataPath**: All the data inside it will be processed before the streaming starts. You can use the variable **loadHistoricalData** to control when do you actually want to process the historical data.
2. **twitterStreamingDataPath**: Spark streaming will monitor this directory in order to process the new data. The streaming batch time can be configured in **streamingBatchTime**
3. **checkPointDirForStreaming**: Check point directory for spark streaming. Always clean this directory if you clean the streaming directory.
 
### Running RedRock

Before running RedRock, make sure that the elasticsearch mapping is created. Execute the script CreateElasticsearchTable.sh to create the tweet mapping on ES. Use argument --delete to delete the existing mapping and then create (obs: all the processed data will also be deleted).

To start RedRock run **./start.sh**

All the historical data will be processed and written to Elasticsearch at redrock/processed_tweets, as well as all the new data coming from streaming.

The REST API will be started.

### Making requests

URL: http://localhost:16666/ss/search?user=Barbara&termsInclude=RT&termsExclude=&top=100

Specified include and exclude terms separated by comma.