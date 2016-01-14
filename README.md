
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

### Downloading Twitter Data

To download the Twitter data you have to have access to Bluemix. Please, talk to the RedRock team to get the sensitive information you are going to need to have access to the data.

#### Powertrack

The Powertrack data is downloaded through the shell script file **REDROCK_HOME/dev/getPowerTrackData.sh**. 

Open the file and comment out the following lines:
    
    read -p 'PowerTrack Username: ' user
    read -sp 'PowerTrack Password: ' password
    read -p 'Track ID: ' trackid
    read -p 'hadoop folder: ' hdfolder
    read -p 'backoff minutes: ' back_min
    
Insert the new lines:

    user="XXXXXXXX" #Bluemix Dev user credential
    password="XXXXXXXX" #Bluemix Dev password credential
    trackid="XXXXXXXX" #powertrack Bluemix trackID
    hdfolder=hdfs://localhost:9000/data/twitter/powertrack/streaming
    back_min=2

In some machines you don't need to sudo access hadoop before executing a hadoop command. In that case, search for **su - hadoop -c** in the script and delete it, executing only the following command between quotes.

To execute the script use: **./getPowerTrackData.sh**. It will generate a log file in the same directory with a name like: 201510272251445912821.txt 

### Decahose Historical

The Decahose historical data is downloaded through the shell script file **REDROCK_HOME/dev/retrieve-decahose-data.sh**. The script is currently running only on Linux plataform. You can execute it on Linux and then transfer the data to your local machine. Feel free to reach out to the team in case you need just a sample file.

Open the file and comment out the following lines:
    
    read -p 'wget Username: ' user
    read -sp 'wget Password: ' password
    
Insert the new lines:

    user="XXXXXXXX" #Bluemix Prod user credential
    password="XXXXXXXX" #Bluemix Prod password credential
    
In some machines you don't need to sudo access hadoop before executing a hadoop command. In that case, search for **su - hadoop -c** in the script and delete it, executing only the following command between quotes.

To execute the script use: **./retrieve-decahose-data.sh 2015-05-01 00 00 2015-07-31 23 50 /opt/tmp-data-decahose/ hdfs://localhost:9000/data/twitter/decahose/historical/**. It will generate a log file in the same directory with a name based on the start date, like: 2015-05-01_1447882385.txt 

The script usage: ./retrieve-decahose-data.sh start-date start-hh start-mm end-date end-hh end-mm dest-folder/ hadoop-folder/
The minutes should be multiple of 10.
        
### Decahose Streaming

The Decahose streaming data is downloaded through the shell script file **REDROCK_HOME/dev/pollDecahoseData.sh**. The script is currently running only on Linux plataform. Every 10 minutes the scripts downloads a new file from Bluemix archieve.

Open the file and comment out the following lines:
    
    read -p 'wget Username: ' user
    read -sp 'wget Password: ' password
    
Insert the new lines:

    user="XXXXXXXX" #Bluemix Prod user credential
    password="XXXXXXXX" #Bluemix Prod password credential

In some machines you don't need to sudo access hadoop before executing a hadoop command. In that case, search for **su - hadoop -c** in the script and delete it, executing only the following command between quotes.

To execute the script use: **./pollDecahoseData.sh 2016-01-13 14 00 /opt/tmp-data-decahose/ hdfs://localhost:9000/data/twitter/decahose/streaming/**. It will generate a log file in the same directory with a name based on the start date, like: 2016-01-13_1452725819.txt

The script usage: ./pollDecahoseData.sh start-date start-hh start-mm dest-folder/ hadoop-folder/
The minutes should be multiple of 10.

### Running RedRock

RedRock code is divided into 3 main applications: Rest-api, Twitter-Powertrack and Twitter-Decahose.

To start all the applications at once, use the script **REDROCK_HOME/bin/start-all.sh**

To stop all the applications at once, use the script **REDROCK_HOME/bin/stop-all.sh**

Each application can be started and stoped individually. Use the start and stop scripts in **REDROCK_HOME/bin/**

The log file for each application file will be at:

1. Decahose: **REDROCK_HOME/twitter-decahose/nohup-decahose.out**
2. Powertrack: **REDROCK_HOME/twitter-decahose/nohup-powertrack.out**
3. Restapi: **REDROCK_HOME/rest-api/nohup-restpi.out**

For each application start script, please configure the parameter **--driver-memory 2g**.
The sum of the values defined should be equal to the amount at the Spark configuration SPARK_DRIVER_MEMORY.

**Don't forget to put the files to be analysed into the HDFS directories we defined before.**

### Making requests

To send a request to the REST API just open a browser and use one of the URLs specified below.

#### Live Portion (Powertrack)

Sample URL: <http://localhost:16666/ss/powertrack/wordcount?termsInclude=#SparkInsight&batchSize=60&topTweets=100&user=bmgomes@us.ibm.com&termsExclude=&topWords=10/>

Parameters:

1. **termsInclude**: terms to be included in the search
2. **bachSize**: How many minutes ago you would like to retrieve information. For exemple, 60 means that we are getting tweets between now and one hour ago.
3. **topTweets**: How many tweets to be retrieved
4. **user**: user email
5. **topWords**: How many words to be displayed at the word count visualization

#### Decahose

Sample URL: <http://localhost:16666/ss/search?termsInclude=love&user=bmgomes@us.ibm.com&termsExclude=&top=100/>

Parameters:

1. **termsInclude**: terms to be included in the search separated by comma
2. **termsExclude**: terms to be excluded in the search separated by comma
3. **user**: user email
4. **top**: How many tweets to be retrieved

#### Sentiment Analysis

Sample URL: <http://localhost:16666/ss/sentiment/analysis?endDatetime=2015-08-14T00:00:00.000Z&user=bmgomes@us.ibm.com&termsInclude=love&termsExclude=&startDatetime=2015-08-13T00:00:00.000Z&top=100&sentiment=1/>

Parameters:

1. **termsInclude**: terms to be included in the search separated by comma
2. **termsExclude**: terms to be excluded in the search separated by comma
3. **user**: user email
4. **startDatetime**: start date to be used to filter the tweets
5. **endDatetime**: end date to be used to filter the tweets
6. **sentiment**: Sentiment of the tweets: 1 for positive and -1 for negative


### Explaining RedRock Configuration File

The RedRock configuration file is located at **REDROCK_HOME/conf/redrock-app.conf.template**.
All the configurations should be located inside the root redrock key. Following an explanation of each key-value pair.

<table width=100% >
	<tr width=100%>
		<td width=20% colspan=4 align=center> <b> redrock </b> </td>
	</tr>
	<tr width=100%>
		<td width=20% colspan=4> <b> Module </b> </td>
	</tr>
	<tr style="background-color:#f5f5f5" width=100%>
		<td width=15% rowspan=40> <b> rest-api </b> </td>
		<td width=20% > <b> Key </b> </td>
		<td width=40% > <b> Meanning </b> </td>
		<td width=20% > <b> Default </b> </td>
	</tr>
	<tr>
		<td > name </td>
		<td > Application name </td>
		<td > redrock-restapi </td>
	</tr>
	<tr>
		<td> actor </td>
		<td> REST API Actor System name </td>
		<td> redrock-actor </td>
	</tr>
	<tr>
		<td> port </td>
		<td> REST API bind port </td>
		<td> 16666 </td>
	</tr>
	<tr>
		<td > validateTweetsBeforeDisplaying </td>
		<td> Defines if the tweets that are going to be displayed need to be validate on Bluemix </td>
		<td> true </td>
	</tr>
	<tr>
		<td > groupByESField </td>
		<td > Which field of ES is going to be used for Sentiment and Location aggregation query. It can be grouped by day: <b>created_at_timestamp_day</b> or by hour: <b>created_at_timestamp</b> </td>
		<td > created_at_timestamp_day </td>
	</tr>
	<tr style="background-color:#f5f5f5" >
		<th > bluemixProduction </th>
		<td colspan=2> Credentials to connect to Bluemix Production Server </td>
	</tr>
	<tr>
		<td> user </td>
		<td> Bluemix user </td>
		<td>  </td>
	</tr>
	<tr>
		<td> password </td>
		<td> Bluemix password </td>
		<td> 16666 </td>
	</tr>
	<tr>
		<td> requestURLforMessagesCheck </td>
		<td> Bluemix request URL to validate tweets </td>
		<td> </td>
	</tr>
	<tr style="background-color:#f5f5f5" >
		<th > python-code </th>
		<td colspan=2> Used to execute Cluster and Distance algorithm </td>
	</tr>
	<tr>
		<td> classPath </td>
		<td> Class path to the main python file </td>
		<td> ${redrock.homePath}"/rest-api/python/main.py" </td>
	</tr>
	<tr>
		<td> pythonVersion </td>
		<td> Python version </td>
		<td> python2.7 </td>
	</tr>
	<tr style="background-color:#f5f5f5" >
		<th > searchParam </th>
		<td colspan=2> Parameters to be used when performing searches</td>
	</tr>
	<tr>
		<td> defaultTopTweets </td>
		<td> Default number of tweets to be returned in the search if not specifyed in the request </td>
		<td> 100 </td>
	</tr>
	<tr>
		<td> tweetsLanguage </td>
		<td> Language of the tweets that is going to be returned in the search </td>
		<td> en </td>
	</tr>
	<tr>
		<td> topWordsToVec </td>
		<td> Number of words to be returned by the Cluster and Distance algorithm </td>
		<td> 20 </td>
	</tr>
	<tr>
		<td> defaulStartDatetime </td>
		<td> Start date to be used in the search range if not specifyed in the request </td>
		<td> 1900-01-01T00:00:00.000Z </td>
	</tr>
	<tr>
		<td> defaultEndDatetime </td>
		<td> End date to be used in the search range if not specifyed in the request </td>
		<td> 2050-12-31T23:59:59.999Z </td>
	</tr>
	<tr style="background-color:#f5f5f5" >
		<th > sentimentAnalysis </th>
		<td colspan=2> Configure the online ML algorithm for the sentiment drill down </td>
	</tr>
	<tr>
		<td> numTopics </td>
		<td> Number of topics to be displayed </td>
		<td> 3 </td>
	</tr>
	<tr>
		<td> termsPerTopic </td>
		<td> Number of terms per topic </td>
		<td> 5 </td>
	</tr>
	<tr style="background-color:#f5f5f5" >
		<th > totalTweetsScheduler </th>
		<td colspan=2> Configure the scheduled task that count the amount of tweets in ES decahose index </td>
	</tr>
	<tr>
		<td> delay </td>
		<td> Defines how many seconds after the application have started will the task begin to be executed </td>
		<td> 10 </td>
	</tr>
	<tr>
		<td> reapeatEvery </td>
		<td> Time interval in seconds that the task will be executed </td>
		<td> 1800 </td>
	</tr>

</table>
