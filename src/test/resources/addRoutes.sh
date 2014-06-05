#!/bin/bash

ROUTE=$1
if [ "x$ROUTE" == "x" ]; then
  ROUTE='127.0.0.1:9090'
fi

curl -XPOST "http://$ROUTE/virtualhost" -d '
{
  "name": "teste.qa02.globoi.com",
  "endpoints": []}' \
  && curl -XPOST "http://$ROUTE/version" -d '
  {
    "version": 28031976
  }'

curl -XPOST "http://$ROUTE/real" -d '
{
  "name": "teste.qa02.globoi.com",
  "endpoints": [
    {
        "host":"cittamp03ld03.globoi.com",
        "port": 80
    }
    ]}' \
    && curl -XPOST "http://$ROUTE/version" -d '
    {
      "version": 28031978
    }'


curl -XPOST "http://$ROUTE/real" -d '
{
  "name": "teste.qa02.globoi.com",
  "endpoints": [
    {
        "host":"cittamp03ld04.globoi.com",
        "port": 80
    }
    ]}' \
    && curl -XPOST "http://$ROUTE/version" -d '
    {
      "version": 28031978
    }'

#curl -XPOST "http://$ROUTE/real" -d '{"name": "teste.qa02.globoi.com", "endpoints":[{"host":"127.0.0.1", "port": 8082}]}' \
#  && curl -XPOST "http://$ROUTE/version" -d '{"version": 28031978}'
#curl -XPOST "http://$ROUTE/real" -d '{"name": "teste.qa02.globoi.com", "endpoints":[{"host":"127.0.0.1", "port": 8083}]}' \
#  && curl -XPOST "http://$ROUTE/version" -d '{"version": 28031978}'

