#!/bin/bash

curl -XPOST 'http://127.0.0.1:9090/virtualhost' -d '
{
  "name": "lol.localdomain", 
  "endpoints": [
      {
        "host":"127.0.0.1", 
        "port": 8082
      }
   ]}' \
  && curl -XPOST 'http://127.0.0.1:9090/version' -d '
  {
    "version": 28031976
  }'

curl -XPOST 'http://127.0.0.1:9090/real' -d '
{
  "name": "lol.localdomain", 
  "endpoints": [
    {
      "host":"127.0.0.1", 
      "port": 8081
    }
    ]}' \
    && curl -XPOST 'http://127.0.0.1:9090/version' -d '
    {
      "version": 28031978
    }'

curl -XPOST 'http://127.0.0.1:9090/real' -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8082}]}' \
  && curl -XPOST 'http://127.0.0.1:9090/version' -d '{"version": 28031978}'
curl -XPOST 'http://127.0.0.1:9090/real' -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8083}]}' \
  && curl -XPOST 'http://127.0.0.1:9090/version' -d '{"version": 28031978}'

