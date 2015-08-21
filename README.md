- Download Scala 2.10.4
- Install sbt and sbt assembly
=============================================================

Run start.sh
===========================================================
run project with spark

~/Projects/ApacheSparkFork/spark/bin/spark-submit --master local --class com.redRock.Boot target/scala-2.10/redRock.jar

generate jar

sbt compile package assembly

run with java

java -jar target/scala-2.10/redRock.jar com.redRock.Boot
=============================================================

Sample URL = http://localhost:16666/ss/search?user=Barbara&termsInclude=RT&termsExclude=&top=100
