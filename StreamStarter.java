package com.github.fxchang23.kafkaStreamer.twitterStream;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;

import java.util.Arrays;
import java.util.Properties;

public class StreamStarter {

    public static void main(String[] args) {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "twitter-streaming");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka1:9092,kafka2:9092,kafka3:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        KStreamBuilder builder = new KStreamBuilder();
        // stream from kafka
        KStream<String, String> twitterInput = builder.stream("twitter-2-input");

        KTable<String, Long> twitterSimpleOutput1 = twitterInput
                    //split the data into each key value pair
                    .flatMapValues(value -> Arrays.asList(value.split(" ")))
                    //match the value with "#"
                    .filter((key,value) -> value.matches("#\\w+"))
                    //setting value as the key value
                    .selectKey((key, value) -> value)
                    //group the value with the same key
                    .groupByKey()
                    //count the number of value
                    .count("HashTagCounts");
        
        //pass data to kafka
        twitterSimpleOutput1.to(Serdes.String(), Serdes.Long(), "twitter-simple-output-2");

        KafkaStreams streams = new KafkaStreams(builder, config);
        streams.start();
        System.out.println(streams.toString());

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
