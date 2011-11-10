# This is the main file for authorship detection project
# This version uses vector space model 
import os
import Solution
from time import clock

class Main:
    trainingDataPath="../../prose/train"
    testDataPath="../../prose/test"
    cutoff = -1 # cut first n symbols in each file for speeding up. Use -1 for function off 

        
    def readCollection(self, path):
        docClassPairs=list()
        for author in os.listdir(path):
            for work in os.listdir(path+"/"+author):
                docClassPairs.append([open(path+"/"+author+"/"+work).read()[:self.cutoff],author])
        return docClassPairs    
   
    def test(self,classifier):
        #print "loading model..."
        testingData=self.readCollection(self.testDataPath)
        
        #print "testing..."
        start = clock()
        positive=0
        skipped=0
        
        for documentAndClass in testingData:
            docClass=classifier.classify(documentAndClass[0])
            #print docClass, documentAndClass[1]
            if docClass == None:
                skipped+=1
            elif docClass==documentAndClass[1]:
                positive+=1
        
        precision = float(positive)/(len(testingData)-skipped)
        recall = float(positive)/len(testingData)
        F1 = (2*precision*recall)/(precision+recall)
        return  'precision: {0}\nrecall: {1}\nF1: {2}\ntime: {3}'.format(precision,
                                                                         recall, F1, clock() - start)
    

run = Main()
solution = Solution.Solution()
print run.test(solution)     
     