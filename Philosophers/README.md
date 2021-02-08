# Philosophers

## Table of contents
* [General info](#general-info)
* [Technologies](#technologies)
* [Setup](#setup)

## General info

Dining philosophers' problem using akka-scala.
There were two required ways of implementation :

* [X] A centralised version, with an actor called supervisor. It had to look-up for philosophers who wanted to eat and allow them if possible.
* [X] A distributed version, where each philosopher (monk) had to communicate with others to see if he could take a spoon (hashi).

Moreover, students were asked to create a logger. I splitted logs into two files (log / error) and only displayed warnings / debugs in console.

## Technologies

This project was created using:

* Scala 2.13.3
* Akka 2.6.12

## Setup

Please install sbt on your device.
Then, run ```sbt run```.