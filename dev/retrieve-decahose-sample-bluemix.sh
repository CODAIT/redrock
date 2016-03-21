#!/usr/bin/env bash

# (C) Copyright IBM Corp. 2015, 2015
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ "$#" -ne 3 ]; then
	echo "Illegal number of parameters"
	echo "Usage:"
	echo "./retrieve-decahose-sample-bluemix username password \"dest-folder\" "
	echo "Example: ./retrieve-decahose-sample-bluemix XXXXXXXXX XXXXXXXXXX \"/data/sample\" "
	exit
fi

user=$1
password=$2
destinationFolder=$3


baseUrl="https://"$user:$password"@cdeservice.mybluemix.net:443"
restapi="/api/v1/messages/search?"
query_start="q=posted:"
query_hour=( "T13:00:00Z-T13:10:00Z"
             "T15:00:00Z-T15:10:00Z" )
query_date="2016-03-"
query_size="&size=500"
query_from="&from="
total=2500

for day in `seq 1 10`;
do
    for hours in "${query_hour[@]}" ; do
        from=${hours%%-*}
        to=${hours#*-}
        c_from=0
        while [ $c_from -le $total ] ; do
            range=$[$c_from+500]
            url=$baseUrl$restapi$query_start$query_date"0"$day$from","$query_date"0"$day$to$query_size$query_from$c_from
            filename="historical_"$query_date"0"$day${from//":"/"-"}"_"$c_from"-"$range".json"
            if [ $day -eq 10 ];
                then
                    url=$baseUrl$restapi$query_start$query_date$day$from","$query_date$day$to$query_size$query_from$c_from
                    filename="streaming_"$query_date$day${from//":"/"-"}"_"$c_from"-"$range".json"
            fi

            echo "Downloading -> "$filename
            echo "URL -> " $url

            curl $url > $destinationFolder/$filename \
            && gzip $destinationFolder/$filename

            c_from=$[$range+1]

        done
    done
done
