import json
import numpy as np
from flask import Flask, abort, jsonify, request
from numpy import asarray, mean
from sklearn.feature_extraction.text import CountVectorizer, TfidfTransformer
from sklearn.linear_model import PassiveAggressiveClassifier
from sklearn.metrics import (accuracy_score, classification_report, confusion_matrix)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from flask_cors import CORS
import requests

app = Flask(__name__)
CORS(app)

# @DEV: A simple endpoint to make sure that the server is up and running
@app.route("/", methods=['GET'])
def home():
    return "Hello From flask > app.py..."

# @DEV: The main endpoint that produces classification reports for the CorDapp
# @Param: Corpus is the Hashmanp<String,String> where each key is an utterance and each value is a label
@app.route("/issue_corpus_report", methods=['POST'])
def passive_aggressive_classifier():

    #loop through csv to build training_data and target_label_array
    training_data = []
    target_label_array = []
    for k,v in request.json["corpus"].items():    
        training_data.append(k)
        target_label_array.append(v)
    list_of_goals = target_label_array
    target_label_array = asarray(target_label_array) #set to numpy array
    

    #Seperate data into training and testing sets (30%)
    X_train, X_test, y_train, y_test = train_test_split(training_data, target_label_array, test_size = 0.3, random_state = 42)

    #Setup training data to PassiveAgressiveClassifier Pipeline
    text_clf = Pipeline([
        ('vect', CountVectorizer()),
        ('tfidf', TfidfTransformer()),
        ('clf', PassiveAggressiveClassifier()),
    ])

    text_clf.fit(X_train, y_train)  
    predicted = text_clf.predict(X_test)

    classification_report_output = classification_report(y_test, predicted,output_dict=True, target_names=list(set(list_of_goals)))
    
    #normalize accuracy object
    classification_report_output["accuracy"] = {"score": classification_report_output["accuracy"]}

    #cast all scores to doubles/floats
    for key in classification_report_output.keys():
        if "support" in classification_report_output[key].keys():
            classification_report_output[key]["support"] = float(classification_report_output[key]["support"])
    
    return jsonify(classification_report_output)

# @DEV: An endpoint that allows the user to test out the model with some given utterance[s]
# @Param: URL is the spring server and endpoint used to retrive the corpus state's corpus to test with
# @Param: corpusLinearId is the LinearPointer UUID relative to the corpus state
# @Param: testUtterances is a list of user supplied utterances (Strings) to be classified
@app.route("/test_classifier", methods=['POST'])
def test_passive_aggressive_classifier():
    #get corpus
    response = requests.post(
        url = request.json["url"],
        json={
            "corpusLinearId" : request.json["corpusLinearId"],
        }, 
        timeout = None
    )

    response = json.loads(response.text)
   
    #loop through csv to build training_data and target_label_array
    training_data = []
    target_label_array = []
    for k,v in dict(response["corpus"]).items():    
        training_data.append(k)
        target_label_array.append(v)
    target_label_array = asarray(target_label_array) #set to numpy array
    
    #Seperate data into training and testing sets (30%)
    X_train, X_test, y_train, y_test = train_test_split(training_data, target_label_array, test_size = 0.01, random_state = 42)

    #Setup training data to PassiveAgressiveClassifier Pipeline
    text_clf = Pipeline([
        ('vect', CountVectorizer()),
        ('tfidf', TfidfTransformer()),
        ('clf', PassiveAggressiveClassifier()),
    ])

    text_clf.fit(X_train, y_train)  
    predicted = text_clf.predict(request.json["testUtterances"])
    
    return jsonify({"predictions": list(predicted)})

# @DEV: This endpoint is not currently used within the CorDapp. However, its main use is to 
# produce the classification report from a CSV file stored on the Flask server.
# @Param: pathToFile is where the CSV file is stored on the server.
# @Param: listOfGoals is a list of labels to use when scoring.
@app.route("/classify_with_csv", methods=['POST'])
def pac_with_csv():
    orig_file = open(request.json["pathToFile"], "r", encoding="utf8")
    lines = orig_file.readlines()

    training_data = []
    target_label_array = []
    for line in lines:
        line = line.split('|')    
        training_data.append(line[0])
        target_label_array.append(line[1].replace('\n',''))
    target_label_array = asarray(target_label_array) #set to numpy array

    #Seperate data into training and testing sets (30%)
    X_train, X_test, y_train, y_test = train_test_split(training_data, target_label_array, test_size = 0.3, random_state = 42)

    #Setup training data to PassiveAgressiveClassifier Pipeline
    text_clf = Pipeline([
        ('vect', CountVectorizer()),
        ('tfidf', TfidfTransformer()),
        ('clf', PassiveAggressiveClassifier()),
    ])

    text_clf.fit(X_train, y_train)  
    predicted = text_clf.predict(X_test)
    #mean_output = mean(predicted == y_test)

    classification_report_output = classification_report(y_test, predicted,output_dict=True, target_names=request.json["listOfGoals"])

    for key in classification_report_output.keys():
        if isinstance(classification_report_output[key], dict) and "support" in classification_report_output[key].keys():
            classification_report_output[key]["support"] = float(classification_report_output[key]["support"])
                                            
    #confusion_matrix_output = confusion_matrix(y_test, predicted)
    #accuracy_output = accuracy_score(predicted, y_test)

    return jsonify(classification_report_output)

#Classifying with proposed corpus
@app.route("/update_corpus_report", methods=['POST'])
def passive_aggressive_proposed_corpus():
    
    #get both the proposed and original corpus
    currCorpus = request.json["corpus"]
    proposedCorpus = request.json["proposedCorpus"]

    commonCorpus = {}
    insertions = {}

    #find the data rows that are in both corpus sets and the ones that are newly proposed
    for k,v in proposedCorpus.items():
        if k in currCorpus.keys():
            commonCorpus[k] = v
        else:
            insertions[k] = v

    training_data = []
    target_labels = []

    #generate original training set to use as a starting point
    for k,v in commonCorpus.items():
        training_data.append(k)
        target_labels.append(v)
    orig_targets = target_labels
    target_label_arr = asarray(target_labels)

    #train-test-split
    X_train, X_test, y_train, y_test = train_test_split(training_data,
    orig_targets, test_size = 0.3, random_state = 42)

    #add in all the newly proposed data rows to ensure that they are used to train the new model
    for k,v in insertions.items():
         X_train.append(k)
         y_train.append(v)

    text_clf = Pipeline([
        ('vect', CountVectorizer()),
        ('tfidf', TfidfTransformer()),
        ('clf', PassiveAggressiveClassifier()),
    ])

    text_clf.fit(X_train, y_train)
    predicted = text_clf.predict(X_test)

    classification_report_output = classification_report(y_test, predicted,
    output_dict=True, target_names=list(set(target_label_arr)))

    #normalize accuracy object
    classification_report_output["accuracy"] = {"score": classification_report_output["accuracy"]}

    #cast all scores to doubles/floats
    for key in classification_report_output.keys():
        if "support" in classification_report_output[key].keys():
            classification_report_output[key]["support"] = float(classification_report_output[key]["support"])

    return jsonify({"Report": classification_report_output, "Corpus":
    currCorpus})



