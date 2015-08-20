#!/usr/local/bin/python

import sys, json, time
import resources, GetClusterAndDistance

termsInclude = sys.argv[1].split(',')
termsExclude = sys.argv[2].split(',')

resources.loadDistanceClusterData()

################## distance and cluster ##################
distanceCluster = GetClusterAndDistance.getWordDistanceAndCluster(termsInclude, termsExclude)

print json.dumps({'cluster':  distanceCluster[1], 'distance':  distanceCluster[0]})