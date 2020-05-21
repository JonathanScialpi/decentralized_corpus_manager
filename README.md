<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Decentralized Corpus Manager
A CorDapp designed to facilitate the crowd sourcing of data used to build machine learning models.

In my medium article (coming soon!) I discuss:
- **What** common issues data scientists face when trying to build these models
- **How** I solved these issues using Corda
- **Why** Corda is the right choice for decentralized machine learning applications

## Starting the Nodes
From the */decentralized_corpus_manager/CorDapp/* directory:
- Create your nodes by running `./gradlew deployNodes`.
- Start your nodes by running the `build\nodes\runnodes` command.

## Starting the Spring Webserver
- Build the Spring jar file by running `./gradlew clients::bootJar` command fromt the */CorDapp* directory.
- Start the server with: `java -jar .\clients-0.1.jar --server.port:8080 --config.rpc.host=localhost --config.rpc.port=10006 --config.rpc.username=user1 --config.rpc.password=test` from the */CorDapp/build/libs` directory

## Starting the Flask Server
- If you don't have Flask (and using a Windows machine), watch [this video](https://www.youtube.com/watch?v=Nvz7wN23-hw).
- Copy the [Classification API code](https://github.com/JonathanScialpi/decentralized_corpus_manager/blob/master/flask/app.py) to your flask application.
- Run the `activate` command from the *MyProject/Scripts/* directory to start up your virtual environment.
- Start the server with `flask run`

## Starting the Node Server
- Run `npm start` from the *decentralized-corpus-management\react_app* directory

## Spring Endpoints
1. **issueCorpus:** An endpoint to issue a corpus using JSON.
  - @Param: `corpus` is a LinkedHashMap<String, String> where the Key is the data row and the value is the
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

## Demo Video
[IssueCorpus, UpdateCorpus, IssueCorpusWithCSV](https://r3.webex.com/recordingservice/sites/r3/recording/playback/f533bfc9070a4e7f9a6b2b0aff7c43e7)

