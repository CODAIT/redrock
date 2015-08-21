ps aux |grep "com.redRock.Boot"      | tr -s " " |  cut -d " " -f 2 | xargs kill >/dev/null 2>&1
# ps aux |grep "redrock-server/main.py"     | tr -s " " |  cut -d " " -f 2 | xargs kill >/dev/null 2>&1
# ps aux |grep "python -m pyspark.daemon"   | tr -s " " |  cut -d " " -f 2 | xargs kill >/dev/null 2>&1
#ps aux |grep "SparkSubmit pyspark-shell" | tr -s " " |  cut -d " " -f 2 | xargs kill >/dev/null 2>&1
