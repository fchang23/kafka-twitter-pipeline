1. create three t2.medium ubuntu 16.04 EC2 instances, setting three different availability zone
   sepretally and assigning three private IP to each instance.

2. attaching 3GB EBS volume to each instance for storing data which was buffered by Kafka broker.

3. opening 2888, 3888 and 2181 port to corresponding region IP for zookeeper. opening 9092 port to 
   corresponding IP for Kafka brokers.

4. adding mock IP to /etc/hosts on each instance

4. download official Kafka binary code into each instance and untar it.

5. setting up zookeeper.properties and server.properties

6. create another EC2 instance to run Kafka connect.
   opening 2181 and 9092 port to kafka cluster. opening 8082 and 8083 to schema registry and REST proxy

7. setting up connect-standalone.properties and twitter-source.properties

8. create maven project for Kafka connector and Kafka streaming and create jar file with dependency for each.

9. running kafka streaming in command line

10. the Kafka connector jar is using for plug.in in connect-standalone.properties
   this Kafka connector is an open connector on GitHub: https://github.com/Eneco/kafka-connect-twitter

11. The start.sh is the shell command for running the service



File category:
connect-standalone.properties   //kafka connector properties
hosts.sh   //hosts file in instance
server.properties   //kafka server properties
StreamStarter.java   //kafka streaming java code
pom.xml   //kafka streaming code maven project dependency
zookeeper.properties   //zookeeper properties
twitter-source.properties   //kafka twitter connector properties
start.sh //shell command for running the service