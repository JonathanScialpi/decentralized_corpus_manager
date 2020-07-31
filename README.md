<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Decentralized Corpus Manager
A CorDapp designed to facilitate the crowd sourcing of data used to build machine learning models.

In my [medium article](https://medium.com/corda/machine-learning-on-corda-558cadc8ba05) I discuss:
- **What** common issues data scientists face when trying to build these models
- **How** I solved these issues using Corda
- **Why** Corda is the right choice for decentralized machine learning applications

## Starting the Nodes
From the */decentralized_corpus_manager/CorDapp/* directory:
- Create your nodes by running `./gradlew deployNodes`.
- Start your nodes by running the `build\nodes\runnodes` command.

## Starting the Spring Webserver
- Build the Spring jar file by running `./gradlew clients::bootJar` command fromt the */CorDapp* directory.
- Start the server with: `java -jar .\clients-0.1.jar --server.port:8080 --config.rpc.host=localhost --config.rpc.port=10006 --config.rpc.username=user1 --config.rpc.password=test` from the */CorDapp/clients/build/libs* directory

## Starting the Flask Server
- If you don't have Flask (and using a Windows machine), watch [this video](https://www.youtube.com/watch?v=Nvz7wN23-hw).
- Copy the [Classification API code](https://github.com/JonathanScialpi/decentralized_corpus_manager/blob/master/flask/app.py) to your flask application.
- Run the `activate` command from the *MyProject/Scripts/* directory to start up your virtual environment.
- Start the server with `flask run`

## Using Anaconda to start the flask application
- If you have Anaconda Prompt already installed, you can use anaconda prompt to start the flask application. When you open Anaconda Prompt, you should be in the home directory, which is `C:\Users\John`
- Step 1: Create a Virtual environment
    - If you don't have virtualenv installed, run the command `pip install virtualenv`
    - Run the command `virtualenv venv`. Note that `venv` is the name of our virtual environment. Feel free to replace `venv` with a more suitable name.
    - To activate the virtual environment, navigate to the `Scripts` folder by typing `cd venv\Scripts`. After typing that command, type `activate`.
    - If you activate your virtual environment correctly, you should see something like: `(venv) (base) C:\Users\John\venv\Scripts`
- Step 2: Running the Flask Application
    - We need to install flask. Now that you are in the virtual environment through Anaconda, run the command  `pip install flask`
    - To see what version of flask you have, run the command `flask --version`
    - In the virtual environment, we need to create a directory called `demoapp`. Run the command `mkdir demoapp` while in `(venv) (base) C:\Users\John\venv\Scripts`
    - Open up a code editor. I used Sublime Text. Drag the folder you created in the previous step into the Sublime text window.
    - In Sublime Text, you see on the left hand side an icon called "Folders" with `demoapp` already there. Right click the folder and select `New File`.
    - Go to where you have the `decentralized_corpus_manager` folder in your computer. Click on the folder called `flask` and open the file called `app.py`.
    - Copy and paste that code in the new file you created in Sublime Text. Save that file as `app.py`.
    - Go to Anaconda and run the command `flask run`. If you execute this command correctly, you will see `Running on 127.0.0.1:5000/`. That is the localhost on your browser.
    - If you execute `flask run` and Anaconda wants you to install additional libraries, run `pip install <libraryName>` for each library you are told. For example, to install scikit learn and numpy, run `pip install sklearn` and `pip install numpy` respectively. Feel free to google the pip install command to make sure you install the library correctly.
    - Go to your browser and type `localhost:5000` and you should see `Hello from Flask>app.py`. If you examine the `app.py` file carefully, you will see `app.route()` in the documentation. That tells you what to enter into the browser to execute certain functions.


## Starting the Node Server
- Run `npm start` from the *decentralized-corpus-management\react_app* directory

## Spring Endpoints
1. **issueCorpus:** An endpoint to issue a corpus using JSON.
  - @Param: `corpus` is a LinkedHashMap<String, String> where the Key is the data row and the value is the classification label
  - @Param: `algorithmUsed` is a String describing the type of algo used to produce the model
  - @Param: `classificationURL` is a String which represents the Flask endpoint where the classification report can be created from.
  - @Param: `participants` is the list (Strings) of CordaX500 names for each party included on the TX.
2. **updateCorpus:** An endpoint that allows a user to propose a new corpus for the model with the intent to improve it.
  - @Param: `proposedCorpus` is a LinkedHashMap<String, String> where the Key is the data row and the value is the label.
  - @Param: `corpusLinearId` is the LinearPointer used to query for the corpus state.
3. **updateClassificationURL:** An endpoint for strictly modifying the URL that is used to build the classification report.
  - @Param: `newURL` is a String representing the new endpoint used to produce the classification report.
  - @Param: `corpusLinearId` is the LinearPointer used to query for the corpus state.
4. **transferOwnership:** Only the owner of a corpus can "close" it or update its classification URL. This endpoint allows an owner to re-assign the ownership of a corpus.
  - @Param: `newOwner` is the party representing the new owner of the corpus.
  - @Param: `corpusLinearId` is the LinearPointer used to query for the corpus state.
5. **closeCorpus:** An endpoint for the corpus owner to prevent any further changes to a corpus by pointing to the exit state.
  - @Param: `corpusLinearId` is the LinearPointer used to query for the corpus state.
6. **issueCorpusWithCSV:** This endpoint gives the user has the option of using a CSV file delimited by "|" to use as a corpus for corpus state creation.
  - @Param: `csvFile` is a multipart file that is a "|" delimited utterance (data|label).
  - @Param: `algorithmUsed` is a String describing the type of algo used to produce the model
  - @Param: `classificationURL` is a String which represents the Flask endpoint where the classification report can be created from.
  - @Param: `participants` is the list (Strings) of CordaX500 names for each party included on the TX.
7. **corpusLookup:** Retrieve the most recent version of a corpus state.
  - @Param: `corpusLinearId` is the LinearPointer used to query for the corpus state.



## Postman Collection
Import the sample [Postman requests](https://github.com/JonathanScialpi/decentralized_corpus_manager/blob/master/postman/Decentralized%20Corpus%20Manager.postman_collection.json)

If you don't have Postman installed, go to this [link](https://www.postman.com/downloads/). Follow the download instructions and open Postman.

Once you open Postman, click `Import` on the top left hand corner and navigate to the folder `decentralized_corpus_manager/postman/postman-collection.json`. If you do it correctly, you should see 9 requests with two `GET` requests in green while the rest yellow.

You should be ready for the demo video. We hope you enjoy learning about machine learning with Corda!

## Demo Video
[IssueCorpus, UpdateCorpus, IssueCorpusWithCSV](https://youtu.be/JVLjxeZrz5U)
