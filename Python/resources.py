#
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
#

# coding=utf-8
import numpy as np

###############################################################################
# Data files for distance and cluster
###############################################################################
def loadDistanceClusterData(homePath):
  global Feat, words, freqs, clusters
  ## read model data
  Feat  = np.load(homePath + 'Python/distance/w2v_may1_may19_june1_june11.npy')
  words = np.load(homePath + 'Python/distance/word_may1_may19_june1_june11.npy')
  freqs = np.load(homePath + 'Python/distance/freq_may1_may19_june1_june11.npy')
  clusters = np.load(homePath + 'Python/cluster/clusterK5_may1_may19_june1_june11.npy')
