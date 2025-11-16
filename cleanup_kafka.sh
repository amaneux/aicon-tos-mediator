#Who uses Kafka?
kafka-consumer-groups --bootstrap-server localhost:9091,localhost:9093,localhost:9094 --list

#Is kafka up and running??

#Which topics are there
kafka-topics --bootstrap-server localhost:9091,localhost:9093,localhost:9094 --list

#Delete topics
kafka-topics --bootstrap-server localhost:9091 --delete --topic AICON_TOS_CONTROL
kafka-topics --bootstrap-server localhost:9091 --delete --topic AICON_TOS_CONNECTION_STATUS
kafka-topics --bootstrap-server localhost:9091 --delete --topic AICON_USER_CONTROL

#Verify deleted
#Verify deleted

#Create new topics
kafka-topics --bootstrap-server localhost:9091,localhost:9093,localhost:9094 --create --topic AICON_TOS_CONNECTION_STATUS --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9091,localhost:9093,localhost:9094 --create --topic AICON_TOS_CONTROL --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9091,localhost:9093,localhost:9094 --create --topic AICON_USER_CONTROL --partitions 1 --replication-factor 1

#Checks logs`
docker logs broker-1
docker logs broker-2
docker logs broker-3
