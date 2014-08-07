#!/bin/bash

ROUTE=$1
if [ "x$ROUTE" == "x" ]; then
  ROUTE='127.0.0.1:9090'
fi

VHOST=$2
if [ "x$VHOST" == "x" ]; then
  VHOST='lol.localdomain'
fi

ENDPOINT1=$3
if [ "x$ENDPOINT1" == "x" ]; then
  ENDPOINT1_HOST='127.0.0.1'
  ENDPOINT1_PORT='8081'
else
  ENDPOINT1_HOST=${ENDPOINT1%%:*}
  ENDPOINT1_PORT=${ENDPOINT1##*:}
fi

ENDPOINT2=$4
if [ "x$ENDPOINT2" == "x" ]; then
  ENDPOINT2_HOST='127.0.0.1'
  ENDPOINT2_PORT='8082'
else
  ENDPOINT2_HOST=${ENDPOINT2%%:*}
  ENDPOINT2_PORT=${ENDPOINT2##*:}
fi

LOADBALANCE=$5
if [ "x$LOADBALANCE" == "x" ]; then
  LOADBALANCE="RandomPolicy"
fi

curl -XPOST "http://$ROUTE/virtualhost" -d '
{
  "name": "'$VHOST'",
  "loadBalancePolicy": "'$LOADBALANCE'",
  "endpoints": []}' \
  && curl -XPOST "http://$ROUTE/version" -d '
  {
    "version": 28031976
  }'

curl -XPOST "http://$ROUTE/real" -d '
{
  "name": "'$VHOST'",
  "endpoints": [
    {
        "host":"'$ENDPOINT1_HOST'",
        "port": '$ENDPOINT1_PORT'
    }
    ]}' \
    && curl -XPOST "http://$ROUTE/version" -d '
    {
      "version": 28031978
    }'


curl -XPOST "http://$ROUTE/real" -d '
{
  "name": "'$VHOST'",
  "endpoints": [
    {
        "host":"'$ENDPOINT2_HOST'",
        "port": '$ENDPOINT2_PORT'
    }
    ]}' \
    && curl -XPOST "http://$ROUTE/version" -d '
    {
      "version": 28031978
    }'

#curl -XPOST "http://127.0.0.1:9090/real" -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8082}]}' \
#  && curl -XPOST "http://$ROUTE/version" -d '{"version": 28031978}'
#curl -XPOST "http://127.0.0.1:9090/real" -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8083}]}' \
#  && curl -XPOST "http://$ROUTE/version" -d '{"version": 28031978}'

