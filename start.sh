#!/bin/bash
mvn clean package
winpty docker cp target/MapReduceTask-1.0-SNAPSHOT-jar-with-dependencies.jar docker-hive_namenode_1:MapReduce-1.0-SNAPSHOT.jar
winpty docker cp docker.sh docker-hive_namenode_1:docker.sh
winpty docker cp 000000 docker-hive_namenode_1:000000
winpty docker exec -it docker-hive_namenode_1 bash docker.sh
read -p "press enter"
