
# RedRock Server

## Overview

How to configure local environment and **RedRock** code to run in standalone mode

In this guide it is assumed you are using a mac, but it can easily translate to any linux distribution

### Download RedRock code

Clone the RedRock Backend code at: <https://github.com/SparkTC/redrock>

In case you can't access the repo, please contact Luciano Resende for authorization.

Configure environment variable REDROCK_HOME at your shell initialization file with the path to your RedRock directory. For example: at your **/home/.profile** add the line: **export REDROCK_HOME=/Users/YOURUSERNAME/Projects/redrock**  


### Environment Setup

#### Hadoop 

Install hadoop 2.6+

Follow this guide to configure and execute hadoop on standalone (for mac) mode: <http://amodernstory.com/2014/09/23/installing-hadoop-on-mac-osx-yosemite>

Create hdfs directory that will be used by RedRock

```
hadoop fs -mkdir -p /data/twitter/decahose/historical
hadoop fs -mkdir -p /data/twitter/decahose/streaming
hadoop fs -mkdir -p /data/twitter/powertrack/streaming
```

#### Elasticsearch

Download Elasticsearch 1.7.3 (<https://www.elastic.co/downloads/past-releases/elasticsearch-1-7-3>) and decompress it.

Install Marvel in order to easily use Elasticsearch. Be sure to install version 1.3 as it is compatible with ES 1.7.3  (<https://www.elastic.co/downloads/marvel>)

#### Spark

Download **pre-built Spark 1.6.0 for Hadoop 2.6 and later** and decompress it (<http://spark.apache.org/downloads.html>).

###### Configuring Spark standalone mode

1. Configure environment variable SPARK_HOME at your shell initialization file with the path to your Spark directory

    * For example: at your **/home/.profile** add the line **export SPARK_HOME=/Users/YOURUSERNAME/Spark/spark-1.6.0-bin-hadoop2.6**
2. Save file _conf/slaves.template_ as _conf/slaves_
3. Save file _conf/spark-env.sh.template_ as _conf/spark-env.sh_ and add the following lines:  
    * **HADOOP_CONF_DIR**=/usr/local/Cellar/hadoop/2.7.0/libexec/etc/hadoop/ 
       * Hadoop home path where you can find the configuration files like hdfs-site.xml  and  core-site.xml
    * **SPARK_WORKER_DIR**=/Users/YOURUSERNAME/opt/SparkData 
       * Create a local directory to save Spark logs
    * **SPARK_WORKER_CORES**=1
       * Define the amount of cores to be used for each worker instance. Keep in mind the number of logical cores your machine has. 
    * **SPARK_WORKER_INSTANCES**=5
       * Define it based on how many logical cores your machine has. Keep in mind that each worker instance is going to use the amount of worker cores you defined. In this current setup, we are using 5 cores at total, which means 5 (workers) * 1 core (worker-cores)
    * **SPARK_WORKER_MEMORY**=2g
       * Define it based on how much memory RAM your machine has. Keep in mind that each worker instance is going to use the amount of worker memory you defined. In this current setup, we are allocating 10g of memory, which means 5 (workers) * 2g (worker-memory)
    * **SPARK_DRIVER_MEMORY**=4g
       * Define it based on how much memory RAM your machine has. In this current setup our total memory RAM is 16g and we have already allocated 10g for workers.
    

4. Save file _conf/log4j.properties.template_ as _conf/log4j.properties_ **log4j.rootCategory=WARN**. Save it as  

Note: The above spark setup is considering a machine with at least:

1. 16gb of memory RAM
2. 4 cores (8 logical cores)

#### SBT plugin

Install sbt plugin. More information at <http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Mac.html>

#### Python
1. Install _Python 2.7_ (<http://docs.python-guide.org/en/latest/starting/install/osx>). The latest version of Mac OS X, El Capitan, comes with Python 2.7 out of the box.
2. Install _numpy_ extension package.
```
sudo easy_install pip
pip install numpy
```

### Starting applications

Before running **RedRock** you must start all the following applications:

1. **Hadoop**: Use command **hstart** (in case you followed the installation instruction at the Hadoop section)
2. **Elasticsearch**: Inside elasticsearch home path use command **nohup ./bin/elasticsearch & tail -f nohup.out** (In case you download the .zip for Elaslicsearch)
3. **Spark**: Inside Spark home path use command **./sbin/start-all.sh**

### Configuring RedRock

All the configurations for RedRock are at: **REDROCK_HOME/conf/redrock-app.conf.template**. Copy this file and save at the same location without the .template extension.

Inside the file, change the following configuration (All the configurations are considering that you followed all the steps above and you haven't changed any configuration for Spark. In case you have a different setup, please take a look at the section **[Explaining RedRock Configuration File](#rrconfig)**):

* **redrock.homepath**: RedRock home path. The same path you used for the environment variable REDROCK_HOME

Unzip the file **REDROCK_HOME/rest-api/python/distance/freq_may1_may19_june1_june11.npy.gz**

    
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

To download the Twitter data you have to have access to Bluemix. In order to get access to Bluemix, follow the instruction in the section **[Getting Access to Bluemix](#bluemix)**

You can play around with the **[IBM Insights for Twitter API](#bluemix-api)** and download a sample of tweets to use as input on RedRock. However, RedRock offers a script that downloads a sample of tweets for you, you just have to pass in your Bluemix **[credentials](#credential)**. Check **[Sample Tweets](#getsample)** to see how you can use RedRock script to download sample data.

#### <a name="getsample"></a> Samples Tweets

To download sample tweets direct from **[IBM Insights for Twitter API](#bluemix-api)** use the script **REDROCK_HOME/dev/retrieve-decahose-sample-bluemix**.

Pass in three arguments when executing the script:

1. **User**: User defined by your Bluemix Twitter Service **[credentials](#credential)**
2. **Password**: Password defined by your Bluemix Twitter Service **[credentials](#credential)**
3. **Destination Folder**: Local destination folder to store the sample tweets

**The script usage**: ./retrieve-decahose-sample-bluemix user password dest-folder

#### Powertrack

We apoligize but Powertrack data is currently only available for free with a developers Bluemix account.

#### Decahose Historical

Make sure your downloaded historical data is inside the hadoop Decahose historical folder (_/data/twitter/decahose/historical_).

If you ran the RedRock script to download **[sample tweets](#getsample)**, make sure you moved the downloaded historical files to the hadoop folder:

```
hadoop fs -put DEST_FOLDER/historical*.json.gz /data/twitter/decahose/historical
``` 
        
#### Decahose Streaming

See **[Simulating Streaming](#simStreaming)** to learn how you can simulate streaming data.

#### <a name="simStreaming"></a> Simulating Streaming

RedRock Streaming uses _Spark Streaming_ application, which monitors an HDFS directory for new files.

You can simulate a streaming processing by pasting a file on the streaming directory being monitored while the streaming application is running. The file should be processed on the next streaming bach.

If you ran the RedRock script to download **[sample tweets](#getsample)**, you will find some files for you to play around. Files name start with _streaming_2016_

### Running RedRock

RedRock code is divided into 3 main applications: Rest-api, Twitter-Powertrack and Twitter-Decahose.

To start all the applications at once, use the script **REDROCK_HOME/bin/start-all.sh**

To stop all the applications at once, use the script **REDROCK_HOME/bin/stop-all.sh**

Each application can be started and stoped individually. Use the start and stop scripts in **REDROCK_HOME/bin/**

The log file for each application file will be at:

1. Decahose: **REDROCK_HOME/twitter-decahose/nohup-decahose.out**
2. Powertrack: **REDROCK_HOME/twitter-decahose/nohup-powertrack.out**
3. Restapi: **REDROCK_HOME/rest-api/nohup-restapi.out**

For each application start script, please configure the parameter **--driver-memory 2g**.
The sum of the values defined should be equal to the amount at the Spark configuration SPARK_DRIVER_MEMORY.

For example, we defined SPARK_DRIVER_MEMORY = 4g, so you could give decahose 1g, powertrack 1g, and rest-api 2g.

**Don't forget to put the files to be analysed into the HDFS directories we defined before.**

### Making requests

To send a request to the REST API just open a browser and use one of the URLs specified below.

#### Live Portion (Powertrack)

Sample URL: <http://localhost:16666/ss/powertrack/wordcount?termsInclude=#SparkInsight&batchSize=60&topTweets=100&user=bmgomes@us.ibm.com&termsExclude=&topWords=10>

Parameters:

1. **termsInclude**: terms to be included in the search
2. **bachSize**: How many minutes ago you would like to retrieve information. For exemple, 60 means that we are getting tweets between now and one hour ago.
3. **topTweets**: How many tweets to be retrieved
4. **user**: user email
5. **topWords**: How many words to be displayed at the word count visualization

#### Decahose

Sample URL: <http://localhost:16666/ss/search?termsInclude=love&user=bmgomes@us.ibm.com&termsExclude=&top=100>

Parameters:

1. **termsInclude**: terms to be included in the search separated by comma
2. **termsExclude**: terms to be excluded in the search separated by comma
3. **user**: user email
4. **top**: How many tweets to be retrieved

#### Sentiment Analysis

Sample URL: <http://localhost:16666/ss/sentiment/analysis?endDatetime=2015-08-14T00:00:00.000Z&user=bmgomes@us.ibm.com&termsInclude=love&termsExclude=&startDatetime=2015-08-13T00:00:00.000Z&top=100&sentiment=1>

Parameters:

1. **termsInclude**: terms to be included in the search separated by comma
2. **termsExclude**: terms to be excluded in the search separated by comma
3. **user**: user email
4. **startDatetime**: start date to be used to filter the tweets
5. **endDatetime**: end date to be used to filter the tweets
6. **sentiment**: Sentiment of the tweets: 1 for positive and -1 for negative

### <a name="rrconfig"></a> Explaining RedRock Configuration File

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
		<td width=15% rowspan=25> <b> rest-api </b> </td>
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
	<tr style="background-color:#f5f5f5" width=100%>
		<td width=15% rowspan=25> <b> spark </b> </td>
		<td width=20% > <b> Key </b> </td>
		<td width=40% > <b> Meanning </b> </td>
		<td width=20% > <b> Default </b> </td>
	</tr>
	<tr >
		<td> partitionNumber </td>
		<td> Number of Spark partitions </td>
		<td> 5 </td>
	</tr>
	<tr style="background-color:#f5f5f5" >
		<th > decahose </th>
		<td colspan=2> Configure Spark for the Decahose application </td>
	</tr>
	<tr>
		<td> loadHistoricalData </td>
		<td> Process historical data before starting streaming </td>
		<td> true </td>
	</tr>
	<tr>
		<td> twitterHistoricalDataPath </td>
		<td> Data path for the historical data </td>
		<td>  </td>
	</tr>
	<tr>
		<td> twitterStreamingDataPath </td>
		<td> Data path for the streaming data </td>
		<td>  </td>
	</tr>
	<tr>
		<td> streamingBatchTime </td>
		<td> Time interval in seconds to process streaming data </td>
		<td> 720 </td>
	</tr>
	<tr>
		<td> timestampFormat </td>
		<td> Timestamp format for ES field. (Hourly) </td>
		<td> yyyy-MM-dd HH </td>
	</tr>
	<tr>
		<td> timestampFormatDay </td>
		<td> Timestamp format for ES field. (Daily) </td>
		<td> yyyy-MM-dd </td>
	</tr>
	<tr>
		<td> tweetTimestampFormat </td>
		<td> Timestamp format on the twitter raw data </td>
		<td> yyyy-MM-dd'T'HH:mm:ss.SSS'Z' </td>
	</tr>
	<tr>
		<td> sparkUIPort </td>
		<td> Port to bind the Decahose UI Spark application </td>
		<td> 4040 </td>
	</tr>
	<tr>
		<td> executorMemory </td>
		<td> Amount of executor memory to be used by the Decahose Spark Application </td>
		<td>  </td>
	</tr>
	<tr>
		<td> fileExtension </td>
		<td> File extension to be processed by spark </td>
		<td> .json.gz </td>
	</tr>
	<tr>
		<td> fileExtensionAuxiliar </td>
		<td> File extension to be processed by spark </td>
		<td> .gson.gz </td>
	</tr>
	<tr>
		<td> deleteProcessedFiles </td>
		<td> Delete file after it has been processed by spark </td>
		<td> false </td>
	</tr>
</table>

## <a name="bluemix"></a> Getting Access to Bluemix

How to create a free trial account on IBM Bluemix and download sample tweets.

### Sign Up for IBM Bluemix
If you still don't have an Bluemix account, follow this steps to sing up for a trial one. 

1. Access IBM Bluemix website on <https://console.ng.bluemix.net>
 
2. Click on **Get Started Free**.

   ![bluemix-home-page](https://www.evernote.com/shard/s709/sh/f8a08eb9-a246-4340-95ae-31a49fe612af/ad4602c6a05068ec/res/db1d6c2b-6cc7-4cd3-851c-d9ada2edea70/skitch.png?resizeSmall&width=832 =600x150)

3. Fill up the registration form and click on **Create Account**.
	
   ![bluemix-form](https://www.evernote.com/shard/s709/sh/6683326c-b47d-4737-80e9-c689e22a7d67/deb1d4135dd83c95/res/8b37ed82-cbe9-4474-bfd9-f605a46c8e89/skitch.png?resizeSmall&width=832 =600x350)

4. Check out the email you use to sign up, look for IBM Bluemix email and click on **Confirm your account**.
    
   ![bluemix-create](https://www.evernote.com/shard/s709/sh/c90bf74c-a69f-43f6-ab18-af75e9b33594/1e577986fed16e0f/res/83c30331-8512-4dad-8ac0-82193b9c9e92/skitch.png?resizeSmall&width=832 =200x150)       ![bluemix-email](https://www.evernote.com/shard/s709/sh/17cc9c86-b7b0-42e2-8764-4a4bcbb86cbb/1552ee3a236a23d3/res/00feece8-464a-471f-b9ed-920f94663e9e/skitch.png?resizeSmall&width832 =400x150)
   
5. Log In to Bluemix with the credentials you just created.
6. Click on **Catalog** at the top left of your screen
7. Search for **Twitter** and click on **View More**

   ![bluemix-twitter](https://www.evernote.com/shard/s709/sh/0c47004d-d5d9-4641-a285-3b01801a9430/b6d79dae573d26b6/res/1c2d8e6c-61d8-491d-8f13-bd9340e0ff9c/skitch.png?resizeSmall&width=832 =600x300)
   
8. On the right side of the page fill in the service name and the credential name. They must be populated but the contents do not matter for this tutorial. Click on **Create**.
   
   **Notes**: Make sure the _Free Plan_ is selected.  
   
   ![bluemix-tt-service](https://www.evernote.com/shard/s709/sh/7bc9d809-e547-477e-a013-3167258d8173/0da1737fdbed779a/res/c22136c1-2ab3-4d04-9b2d-fb2b2c92a3d8/skitch.png?resizeSmall&width=832 =600x250)
   
9. <a name="bluemix-credential"></a> You can find your credentials by clicking on the service at the **Dashboard** and then clicking on **Service Credentials**'

   ![bluemix-tt-credentials](https://www.evernote.com/shard/s709/sh/13f88f83-bb6d-45fd-ae29-661cb35fef5a/ffa747f0ddbbf13f/res/797c7c68-bf62-4adc-a0fe-2e429540c677/skitch.png?resizeSmall&width=832 =600x250)
   
### <a name="bluemix-api"></a> Accesing Bluemix Twitter API

Browse _IBM Insights for Twitter API_ documentation and try the APIs before you use them. 

1. You can learn about the IBM Bluemix Twitter Service at: <https://console.ng.bluemix.net/docs/services/Twitter/index.html#twitter>

2. Try the APIs at: <https://cdeservice.mybluemix.net/rest-api>

   **Note**: Powertrack API is not available on _Free Plan_
  
   ![bluemix-tt-API](https://www.evernote.com/shard/s709/sh/e818c8e5-ffc3-4d4e-a6b4-a69307683396/2780cb0f7a6f542c/res/46ab0653-c3d5-49e3-857a-876b9dc99bae/skitch.png?resizeSmall&width=832 =600x300)