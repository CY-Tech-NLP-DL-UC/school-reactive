# Rest

## Table of contents
* [General info](#general-info)
* [Technologies](#technologies)
* [Routes](#routes)
* [Setup](#setup)

## General info

Client-server interactions using akka.
On the client side, it is possible to create / delete / update an event, and it is managed by the server side.

Students used the logger system developed in the Philosophers' problem to get the wordcount results stored.

## Technologies

This project was created using:

* Scala 2.13.3
* Akka 2.6.12

## Routes

Available routes are listed here

|url|methods|description|
|-|-|-|
|"/events"|GET|Gets all the available events|
|"/events/name"|POST|Creates an event based on the name and the tickets provided using json|
|"/events/name"|DELETE|Deletes an event based on its name|
|"/events/name/tickets"|GET|Buys tickets from the event name|

## Setup

Please install sbt on your device.
Then, run ```sbt run```.
To visualize the routes, you can use postman.
