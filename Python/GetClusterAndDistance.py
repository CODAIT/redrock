import sys, json, time
import resources
import numpy as np
from math import sqrt

###############################################################################
# Distance and Cluster computation
###############################################################################
def getWordDistanceAndCluster(include, exclude):
  ## define some functions
  def w2v(word):
    idx = resources.Feat[np.where(str(word)==resources.words),:]
    if idx.size != 0:
      return idx[0][0]
    else:
      return None

  def dist(v1,v2):
    return np.dot(v1,v2)/sqrt(np.dot(v1,v1)*np.dot(v2,v2))
  
  def findsynonyms(v,n): #take a vector and an integer
    distance = np.zeros(Nw)
    i = 0
    for vec in resources.Feat:
        distance[i] = dist(vec,v)
        i = i + 1
    indexes = np.argpartition(distance,-(n+1))[-(n+1):]
    return {'indexes':indexes,'distances':distance[indexes]}

  ## read model data
  Nw = resources.words.shape[0]
  
  ## get/output results
  vector = np.zeros(resources.Feat.shape[1])
  for i in range(len(include)):
    v = w2v(include[i])
    if v is not None:
      vector = vector + v
  
  if np.dot(vector,vector) <= 0.0000001:
    return [[],[]]

  D = findsynonyms(vector,20)
  out_word = resources.words[D['indexes']]
  out_dist = D['distances']
  ind = D['indexes']
  
  dist_filt_words = []
  cluster_filt_words = []
  for i in range(out_word.shape[0]):
    if (out_word[i] in (include+exclude)):
        1
    else:
        dist_filt_words.append([out_word[i], out_dist[i], resources.freqs[resources.freqs[:,0] == out_word[i]][0][1]])
        cluster_filt_words.append([out_word[i], out_dist[i], resources.clusters[ind[i]], resources.freqs[resources.freqs[:,0] == out_word[i]][0][1]])
  return [dist_filt_words,cluster_filt_words]
  
