from flask import Flask, request, abort, jsonify
from sklearn.pipeline import Pipeline
from sklearn.linear_model import PassiveAggressiveClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from numpy import asarray, mean
from sklearn.feature_extraction.text import CountVectorizer, TfidfTransformer

app = Flask(__name__)

@app.route("/", methods=['GET'])
def home():
    return "Hello From flask > app.py..."

@app.route("/classify", methods=['POST'])
def passive_aggressive_classifier():
    
    #loop through csv to build training_data and target_label_array
    training_data = []
    target_label_array = []
    list_of_goals = []
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

@app.route("/classify_with_csv", methods=['POST'])
def pac_with_csv():
    orig_file = open(request.json["path_to_file"], "r", encoding="utf8")
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

    classification_report_output = classification_report(y_test, predicted,output_dict=True, target_names=request.json["list_of_goals"])

    for key in classification_report_output.keys():
        if isinstance(classification_report_output[key], dict) and "support" in classification_report_output[key].keys():
            classification_report_output[key]["support"] = float(classification_report_output[key]["support"])
                                            
    #confusion_matrix_output = confusion_matrix(y_test, predicted)
    #accuracy_output = accuracy_score(predicted, y_test)

    return jsonify(classification_report_output)
