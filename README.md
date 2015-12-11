
# RedRock Server

## Overview

How to configure local environment and **RedRock** code to run in standalone mode

### Download RedRock code

Clone the RedRock Backend code at: <https://github.com/SparkTC/redrock/>

In case you can't access the repo, please contact Luciano Resende for authorization.

Configure environment variable REDROCK_HOME at your shell initialization file with the path to your RedRock directory. For example: at your **/home/.profile** add the line: **export REDROCK_HOME=/Users/barbaragomes/Projects/redrock**  

### Environment Setup

#### Hadoop 

Install hadoop 2.6+

For information about how to configure and execute hadoop on standalone (for mac) mode see: <http://amodernstory.com/2014/09/23/installing-hadoop-on-mac-osx-yosemite/>

Create hdfs directory that will be used by RedRock

- /data/twitter/decahose/historical
- /data/twitter/decahose/streaming
- /data/twitter/powertrack/streaming

Use command **hadoop fs -mkdir -p directory_path** to create a hdfs directory
 
#### Elasticsearch

Download Elasticsearch 1.7.1 (<https://www.elastic.co/downloads/elasticsearch>) and decompress it.

Install Marvel in order to easily use Elasticsearch (<https://www.elastic.co/downloads/marvel>)
#### Spark 1.5.1

Download pre-built Spark 1.5.1 for Hadoop 2.6 and later and decompress it (<http://spark.apache.org/downloads.html/>).

###### Configuring Spark standalone mode

1. Configure environment variable SPARK_HOME at your shell initialization file with the path to your Spark directory
    * For example: at your **/home/.profile** add the line **export SPARK_HOME=/Users/barbaragomes/Spark/spark-1.5.1-bin-hadoop2.6**  
2. Save file conf/slaves.template as conf/slaves
3. Edit file conf/spark-env.sh.template and add the following lines (Save it as conf/spark-env.sh):  
    * **HADOOP_CONF_DIR**=/usr/local/Cellar/hadoop/2.7.0/libexec/etc/hadoop/ 
       * Hadoop home path where you can find the configuration files like hdfs-site.xml  and  core-site.xml
    * **SPARK_WORKER_DIR**=/Users/barbaragomes/opt/SparkData 
       * Create a local directory to save Spark logs
    * **SPARK_WORKER_INSTANCES**=5
       * Define it based on how many cores your machine has.
    * **SPARK_WORKER_MEMORY**=2g
       * Define it based on how much memory RAM your machine has. Keep in mind that each worker instance is going to use the amount of worker memory you defined. In this current setup, we are allocating 10g of memory, which means 5 (workers) * 2g (worker-memory)
    * **SPARK_DRIVER_MEMORY**=4g
       * Define it based on how much memory RAM your machine has. In this current setup our total memory RAM is 16g and we have already allocated 10g for workers.
    * **SPARK_WORKER_CORES**=1
       * Define the amount of cores to be used for each worker instance. Keep in mind the number of cores your machine has. 

4. Edit file conf/log4j.properties.template and change log4j.rootCategory=WARN. Save it as  conf/log4j.properties

Obs: The current spark setup is considering a machine with at least:

1. 16gb of memory RAM
2. 8 cores

#### SBT plugin

Install sbt plugin. More information at <http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Mac.html>

#### Python

Install Pyhton 2.7 and NumPy extension package.

### Starting applications

Before running **RedRock** you must start all the following applications:

1. **Hadoop**: Use command **hstart** (in case you followed the installation instruction at the Hadoop section)
2. **Elasticsearch**: Inside elasticsearch home path use command **nohup ./bin/elasticsearch & tail -f nohup.out&** (In case you download the .zip for Elaslicsearch)
3. **Spark**: Inside Spark home path use command **./sbin/start-all.sh**

### Configuring RedRock

All the configurations for RedRock are at: **REDROCK_HOME/conf/redrock-app.conf.template**. Copy this file and save at the same location without the .template extension.

Inside the file, change the following configurations (All the configurations are considering that you followed all the steps above and you haven't changed any configuration for Spark. In case you have a different setup, please take a look at the section **Explaining RedRock Configuration File**):

1. **redrock.homepath**: RedRock home path. The same path you used for the environment variable REDROCK_HOME
2. **spark.partitionNumber**: 5
3. **spark.decahose.twitterHistoricalDataPath**: hdfs://localhost:9000/data/twitter/decahose/historical
4. **spark.decahose.twitterStreamingDataPath**: hdfs://localhost:9000/data/twitter/decahose/streaming
5. **spark.decahose.totalcores**: 2
6. **spark.decahose.executorMemory**: 1g
7. **spark.powertrack.twitterStreamingDataPath**: hdfs://localhost:9000/data/twitter/powertrack/streaming
8. **spark.powertrack.totalcores**: 1
9. **spark.powetrack.executorMemory**: 1g
10. **spark.restapi.totalcores**: 2
11. **spark.restapi.executorMemory**: 2g

    
### Understanding Data Directories

The pre-process of the tweets is going to happen through Spark. Spark will use the following directories configured in **REDROCK_HOME/conf/redrock-app.conf.template**:

1. **spark.decahose.twitterHistoricalDataPath**: All the data inside it will be processed before the streaming starts. You can use the variable **spark.decahose.loadHistoricalData** to control when do you actually want to process the historical data.
2. **spark.decahose.twitterStreamingDataPath**: Spark streaming will monitor this directory in order to process the new Decahose data. The streaming batch time can be configured in **spark.decahose.streamingBatchTime**
3. **spark.powertrack.twitterStreamingDataPath**: Spark streaming will monitor this directory in order to process the new Powertrack data. The streaming batch time can be configured in **spark.powertrack.streamingBatchTime**

### Creating Elastisearch Indexes

RedRock will write the processed data to Elasticsearch to the correspondent Index. Before start indexing data, make sure you have created the Elasticsearch Index for Decahose and Powertrack data. Use the following scripts:

1. **REDROCK_HOME/dev/create-elasticsearch-powertrack-type.sh**: It will create the index for the Powertrack data
2. **REDROCK_HOME/dev/create-elasticsearch-decahose-type.sh**: It will create the index for the Decahose data

Both of the scripts can be executed with the directive **--delete**. It will delete the existing index, and all of the data in it, before creating the same index.

Make sure the URLs inside the scripts are point to the right instance of Elasticsearch. In this tutorial, we are running elasticsearch in localhost so the URL should be http://localhost:9200

### Getting Twitter Data

### Running RedRock

To start RedRock run **./start.sh**

All the historical data will be processed and written to Elasticsearch at redrock/processed_tweets, as well as all the new data coming from streaming.

The REST API will be started.

### Making requests

URL: http://localhost:16666/ss/search?user=Barbara&termsInclude=RT&termsExclude=&top=100

Specified include and exclude terms separated by comma.