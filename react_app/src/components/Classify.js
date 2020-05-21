import React, {Component} from 'react';
import {Card, Form, Button, Col} from 'react-bootstrap';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faSave, faPlusSquare, faUndo} from '@fortawesome/free-solid-svg-icons';
import axios from 'axios';
export default class Classify extends Component{
    constructor(props){
        super(props);
        this.state = this.intialState;
        this.classificationChange = this.classificationChange.bind(this);
        this.submitClassification = this.submitClassification.bind(this);
    }

    intialState = {
        corpusLinearId:'', testUtterances:''
    }

    resetClassification = () => {
        this.setState(() => this.intialState);
    }

    submitClassification = event => {
        alert('URL: http://localhost:8080/corpusLookup , CorpusLinearId: ' 
        + this.state.corpusLinearId + ', testUtterances: ' + this.state.testUtterances
         );

         const classification = {
            url: "http://localhost:8080/corpusLookup",
            corpusLinearId: this.state.corpusLinearId,
            testUtterances: this.state.testUtterances.split("|")
         };

         axios.post("http://localhost:5000/test_classifier", classification)
         .then(response => {
             if(response.data !=null){
                 this.setState(this.intialState);
                 alert(response.data.predictions);
             }else{
                alert("Failed")
             }
         });
        event.preventDefault();

    }

    classificationChange = event => {
        this.setState({
            [event.target.name]:event.target.value
        });
    }
    render(){
        const {corpusLinearId, testUtterances} = this.state;

        return(
            <Card className = {"border border-dark bg-dark text-white"}>
                <Card.Header><FontAwesomeIcon icon={faPlusSquare}/> Corpus Linear ID</Card.Header>
                <Form id = "classificationFormId" onReset = {this.resetClassification}  onSubmit = {this.submitClassification}>
                <Card.Body>
                    <Form.Row>
                        <Form.Group as={Col} controlId = "formGridCorpusLinearId">
                            <Form.Label>Corpus Linear ID</Form.Label>
                            <Form.Control 
                            required
                            value = {corpusLinearId}
                            onChange = {this.classificationChange}
                            name = "corpusLinearId"
                            type="text"
                            className = {"bg-dark text-white"}
                            placeholder="Enter Corpus Linear ID" />
                        </Form.Group>
                        <Form.Group as={Col} controlId = "formGridTestUtterances">
                            <Form.Label>Test Utterances</Form.Label>
                            <Form.Control
                            required
                            value = {testUtterances}
                            onChange = {this.classificationChange}
                            name = "testUtterances"
                            type="text"
                            className = {"bg-dark text-white"}
                            placeholder="Enter Test Utterances" />
                        </Form.Group>
                    </Form.Row>
                    </Card.Body>
                    <Card.Footer style ={{"textAlign":"right"}}>
                        <Button size = "small" variant="success" type="submit">
                        <FontAwesomeIcon icon={faSave}/> Submit
                        </Button>{' '}
                        <Button size = "small" variant="info" type="reset">
                        <FontAwesomeIcon icon={faUndo}/> Reset
                        </Button>
                    </Card.Footer>
                </Form>
            </Card>
        );
    }
}