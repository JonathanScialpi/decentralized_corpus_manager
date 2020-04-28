import React, {Component} from 'react';
import {Jumbotron} from 'react-bootstrap';
export default class Welcome extends Component{
    render(){
        return(
            <Jumbotron className = "bg-dark text-white">
          <h1>Welcome to DCM!</h1>
          <p>
              This project uses Corda DLT to decentralize contributions to machine learning data sets.
          </p>
        </Jumbotron>
        );
    }
}