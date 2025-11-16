cd /Users/harrievanrijn/project/aicon-monitoring-tos-control/aicon-monitoring-tos-control

curl -X DELETE http://localhost:8081/subjects/AICON_TOS_CONNECTION_STATUS-value
curl -X DELETE http://localhost:8081/subjects/AICON_TOS_CONTROL-value
curl -X DELETE http://localhost:8081/subjects/AICON_USER_CONTROL-value

kafka-topics --bootstrap-server localhost:9091 --delete --topic AICON_TOS_CONTROL
kafka-topics --bootstrap-server localhost:9091 --delete --topic AICON_TOS_CONNECTION_STATUS
kafka-topics --bootstrap-server localhost:9091 --delete --topic AICON_USER_CONTROL

kafka-topics --bootstrap-server localhost:9091 --create --topic AICON_TOS_CONTROL --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9091 --create --topic AICON_TOS_CONNECTION_STATUS --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9091 --create --topic AICON_USER_CONTROL --partitions 1 --replication-factor 1

-- Eventually docker start
open /Applications/Docker.app

docker logs broker-1

docker compose down


docker stop zookeeper-1
docker stop zookeeper-2
docker stop zookeeper-3
docker stop broker-1
docker stop broker-2
docker stop broker-3
docker stop schema-registry
docker stop rest-proxy


docker rm zookeeper-1
docker rm zookeeper-2
docker rm zookeeper-3
docker rm broker-1
docker rm broker-2
docker rm broker-3
docker rm schema-registry
docker rm rest-proxy

docker compose up -d

confluent local services schema-registry start
