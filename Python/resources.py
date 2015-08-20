# coding=utf-8
import numpy as np

###############################################################################
# Data files for distance and cluster
###############################################################################
def loadDistanceClusterData():
  global Feat, words, freqs, clusters

  ## read model data
  Feat  = np.load('./Python/distance/w2v_may1_may19_june1_june11.npy')
  words = np.load('./Python/distance/word_may1_may19_june1_june11.npy')
  freqs = np.load('./Python/distance/freq_may1_may19_june1_june11.npy')
  clusters = np.load('./Python/cluster/clusterK5_may1_may19_june1_june11.npy')
