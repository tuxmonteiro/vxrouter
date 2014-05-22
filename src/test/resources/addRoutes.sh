#!/bin/bash

#curl -XPOST 'http://127.0.0.1:9090/route' -d '{"routes":[{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8081}]}], "version": 28031974}'
#curl -XDELETE $router -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8081}], "version": 28031974}'
curl -XPOST 'http://127.0.0.1:9090/virtualhost' -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8082}], "version": 28031974}'
curl -XPOST 'http://127.0.0.1:9090/virtualhost' -d '{"name": "lol2.localdomain", "version": 28031975}'
curl -XPOST 'http://127.0.0.1:9090/real' -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8082}], "version": 28031976}'
curl -XDELETE 'http://127.0.0.1:9090/real/127.0.0.1%3A8082' -d '{"name": "lol.localdomain", "endpoints":[{"host":"127.0.0.1", "port": 8082}], "version": 28031977}'
