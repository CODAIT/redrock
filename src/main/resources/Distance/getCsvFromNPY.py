# pip install kitchen
import numpy as np
import codecs
import sys
import locale
import os
import unicodedata

from kitchen.text.converters import getwriter, to_bytes, to_unicode
from kitchen.i18n import get_translation_object

# Setup stdout
encoding = locale.getpreferredencoding()
Writer = getwriter(encoding)
sys.stdout = Writer(sys.stdout)

w2v = np.load("/Users/barbaragomes/Projects/RedRockInsights-Demo/sparkinsights-server/handlers/distance/w2v_may1_may19_june1_june11.npy")
word = np.load("/Users/barbaragomes/Projects/RedRockInsights-Demo/sparkinsights-server/handlers/distance/word_may1_may19_june1_june11.npy")
freqs = np.load("/Users/barbaragomes/Projects/RedRockInsights-Demo/sparkinsights-server/handlers/distance/freq_may1_may19_june1_june11.npy")
clusters = np.load("/Users/barbaragomes/Projects/RedRockInsights-Demo/sparkinsights-server/handlers/cluster/clusterK5_may1_may19_june1_june11.npy") 

n =  w2v.shape[0]
m = w2v.shape[1]
#print m
#print n

for i in range(n):
    #try:
    if  u'\n' not in word[i] and u'\r' not in word[i]:
        v = w2v[i,:]
        sys.stdout.write("\"%s\"," % to_unicode(word[i], 'utf-8'))
        #sys.stdout.write(codecs.decode(word[i],'UTF-8')+",")
        #sys.stdout.write(word[i]+",")
        docproduct = 0.0
        for j in range(m):
            #if j < m-1:
            docproduct = docproduct + v[j]*v[j]
            sys.stdout.write(str(v[j])+",")
            #else:
        
        sys.stdout.write(str(docproduct)+",")
        sys.stdout.write(str(clusters[i])+",")
        sys.stdout.write(str(freqs[freqs[:,0]==word[i]][0][1])+"\n")
        
    #except:
        #print(i)			
#print codecs.decode(word[i],"utf-8"), ",", w2v[i,:]	
#print(word[i]+","+w2v[i,:])


