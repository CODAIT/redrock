#!/bin/bash

rm -rf log.txt

baseUrl=https://cde-archive.services.dal.bluemix.net
user="rmurphy"
password="kjwrfndc77zDq"

sameday=0
currentdate=$1
loopenddate=$(/bin/date --date "$4 1 day" +%Y-%m-%d)

folder=$7
echo $1 $2 $3 $4 $5 $6 $7 >> log.txt

do_download()
{
	if [ "$s_hh" -lt 10 ]; then
		s_hh1=0$s_hh
	else
		s_hh1=$s_hh
	fi
		
	if [ "$s_mm" -eq 0 ]; then
		s_mm1=0$s_mm
	else
		s_mm1=$s_mm
	fi
	if [ "${arr[1]}" -le 06 ]; then	
		filename=$(printf "%s_%s_%s_%s_%s_activity.gson.gz" "${arr[0]}" "${arr[1]}" "${arr[2]}" "$s_hh1" "$s_mm1")
	else
		filename=$(printf "%s_%s_%s_%s_%s_activity.json.gz" "${arr[0]}" "${arr[1]}" "${arr[2]}" "$s_hh1" "$s_mm1")
	fi
	fullname=$url$filename
	echo $fullname >> log.txt
	wget -O $folder$filename --user $user --password $password --no-check-certificate $fullname

}

generate_for_day()
{
	s_date=$currentdate
	OIFS=$IFS
	IFS='-'
	read -ra arr <<< "$s_date"
	declare -i s_hh
	s_hh=$tmp_s_hh
	declare -i s_mm
	s_mm=$tmp_s_mm
	declare -i e_hh
	e_hh=$tmp_hh
	declare -i e_mm
	e_mm=$tmp_mm	
	IFS=OIFS
	url=$(printf "%s/%s/%s/%s/" "$baseUrl" "${arr[0]}" "${arr[1]}" "${arr[2]}")
	while [[ ( "$s_hh" != "$e_hh" ) || ( "$s_mm" != "$e_mm") ]]; do
		do_download $s_hh $s_mm $url ${arr[0]} ${arr[1]} ${arr[2]}
		s_mm+=10
		if [ "$s_mm" -eq 60 ]; then
			s_mm=0
			s_hh+=1
		fi
		
		if [[ ( "$s_hh" == "$e_hh" ) && ( "$s_mm" == "$e_mm" ) ]]; then
			do_download $s_hh $s_mm $url ${arr[0]} ${arr[1]} ${arr[2]}
		fi
	done
}

if [ "$currentdate" == "$4" ]; then
  sameday=1
fi

if [ "$sameday" == 1 ]; then
	tmp_s_hh=$2
	tmp_s_mm=$3
	tmp_hh=$5
	tmp_mm=$6
	generate_for_day $currentdate $tmp_s_hh $tmp_s_mm $tmp_hh $tmp_mm
else
	until [ "$currentdate" == "$loopenddate" ]
		do
  			echo $currentdate >> log.txt
			if [ "$currentdate" == "$1" ]; then
				tmp_s_hh=$2
				tmp_s_mm=$3
				tmp_hh=23
				tmp_mm=50
			elif [ "$currentdate" == "$4" ]; then
				tmp_s_hh=00
				tmp_s_mm=00
				tmp_hh=$5
				tmp_mm=$6
			else
				tmp_s_hh=00
				tmp_s_mm=00
				tmp_hh=23
				tmp_mm=50
			fi	
			generate_for_day $currentdate $tmp_s_hh $tmp_s_mm $tmp_hh $tmp_mm	
  			currentdate=$(/bin/date --date "$currentdate 1 day" +%Y-%m-%d)
		done
fi

