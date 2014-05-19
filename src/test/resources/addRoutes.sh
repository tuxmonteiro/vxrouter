#!/bin/bash

router='http://127.0.0.1:9090/route'
curl -XPOST $router -d '{"vhost": "lol.localdomain", "host":"127.0.0.1", "port": 8081, "version": 28031974}'
