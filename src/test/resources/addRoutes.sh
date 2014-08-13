#!/bin/bash

ROUTE=$1
if [ "x$ROUTE" == "x" ]; then
  ROUTE='127.0.0.1:9090'
fi

VHOST=$2
if [ "x$VHOST" == "x" ]; then
  VHOST='lol.localdomain'
fi

BACKEND1=$3
if [ "x$BACKEND1" == "x" ]; then
  BACKEND1_HOST='127.0.0.1'
  BACKEND1_PORT='8081'
else
  BACKEND1_HOST=${BACKEND1%%:*}
  BACKEND1_PORT=${BACKEND1##*:}
fi

BACKEND2=$4
if [ "x$BACKEND2" == "x" ]; then
  BACKEND2_HOST='127.0.0.1'
  BACKEND2_PORT='8082'
else
  BACKEND2_HOST=${BACKEND2%%:*}
  BACKEND2_PORT=${BACKEND2##*:}
fi

LOADBALANCE=$5
if [ "x$LOADBALANCE" == "x" ]; then
  LOADBALANCE="RandomPolicy"
fi

curl -XPOST "http://$ROUTE/virtualhost" -d '
{
  "name": "'$VHOST'",
  "properties": {
    "loadBalancePolicy": "'$LOADBALANCE'"
  },
  "backends": []}' \
  && curl -XPOST "http://$ROUTE/version" -d '
  {
    "version": 28031976
  }'

curl -XPOST "http://$ROUTE/backend" -d '
{
  "name": "'$VHOST'",
  "backends": [
    {
        "host":"'$BACKEND1_HOST'",
        "port": '$BACKEND1_PORT'
    }
    ]}' \
    && curl -XPOST "http://$ROUTE/version" -d '
    {
      "version": 28031978
    }'


curl -XPOST "http://$ROUTE/backend" -d '
{
  "name": "'$VHOST'",
  "backends": [
    {
        "host":"'$BACKEND2_HOST'",
        "port": '$BACKEND2_PORT'
    }
    ]}' \
    && curl -XPOST "http://$ROUTE/version" -d '
    {
      "version": 28031978
    }'

#curl -XPOST "http://127.0.0.1:9090/backend" -d '{"name": "lol.localdomain", "backends":[{"host":"127.0.0.1", "port": 8082}]}' \
#  && curl -XPOST "http://$ROUTE/version" -d '{"version": 28031978}'
#curl -XPOST "http://127.0.0.1:9090/backend" -d '{"name": "lol.localdomain", "backends":[{"host":"127.0.0.1", "port": 8083}]}' \
#  && curl -XPOST "http://$ROUTE/version" -d '{"version": 28031978}'

