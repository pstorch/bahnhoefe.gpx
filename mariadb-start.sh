#!/bin/bash
# creates and runs a local mariadb for testing

docker rm -f mariadb

docker run -d --net host --name mariadb -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=rsapi -e MYSQL_USER=rsapi -e MYSQL_PASSWORD=rsapi -d mariadb:10.1
