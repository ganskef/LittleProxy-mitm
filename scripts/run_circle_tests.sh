#!/usr/bin/env bash

if [ "$CIRCLE_TAG" == "" ]
then
  mvn test -T2C
fi