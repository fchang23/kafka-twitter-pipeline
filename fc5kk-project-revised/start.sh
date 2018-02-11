#go into kafka folder, start zookeeper
bin/zookeeper-server-start.sh -daemon config/zookeeper.properties

#start kafka server
bin/kafka-server-start.sh config/server.properties

#check status
nc -vz localhost 2181
nc -vz localhost 9092

#create twitter input topic
bin/kafka-topics.sh --create --zookeeper zookeeper1:2181,zookeeper2:2181,zookeeper3:2181/kafka --replication-factor 3 --partitions 3 --topic twitter-2-input

#create twitter output topic
bin/kafka-topics.sh --create --zookeeper zookeeper1:2181,zookeeper2:2181,zookeeper3:2181/kafka --replication-factor 3 --partitions 3 --topic twitter-simple-output-2

#run kafka source connect, from twitter, standalone mode
bin/connect-standalone.sh config/connect-standalone.properties twitter-source.properties

#run console consumer to check raw input twitter data
bin/kafka-console-consumer.sh --bootstrap-server kafka1:9092,kafka2:9092,kafka3:9092 --topic twitter-2-input --from-beginning

#run kafka streaming using jar file in command line
java -jar kafka-stream-twitter-1.0-SNAPSHOT-jar-with-dependencies.jar

#run console consumer to check hashtag count output
bin/kafka-console-consumer.sh --bootstrap-server kafka1:9092,kafka2:9092,kafka3:9092 --topic twitter-simple-output-2  --formatter kafka.tools.DefaultMessageFormatter --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer