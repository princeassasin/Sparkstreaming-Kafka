/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kakfa.spark;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;


import scala.Tuple2;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.examples.streaming.StreamingExamples;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.apache.spark.streaming.kafka.*;

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 *
 * Usage: JavaKafkaWordCount <zkQuorum> <group> <topics> <numThreads>
 *   <zkQuorum> is a list of one or more zookeeper servers that make quorum
 *   <group> is the name of kafka consumer group
 *   <topics> is a list of one or more kafka topics to consume from
 *   <numThreads> is the number of threads the kafka consumer should use
 *
 * To run this example:
 *   `$ bin/run-example org.apache.spark.examples.streaming.JavaKafkaWordCount zoo01,zoo02, \
 *    zoo03 my-consumer-group topic1,topic2 1`
 */

public final class JavaKafkaWordCount {
 // private static final Pattern SPACE = Pattern.compile(" ");

  protected static final Pattern SPACE = Pattern.compile(" ");

private JavaKafkaWordCount() {
  }

  public static void main(String[] args) {
    if (args.length < 4) {
      System.err.println("Usage: JavaKafkaWordCount <zkQuorum> <group> <topics> <numThreads>");
      System.exit(1);
    }

    StreamingExamples.setStreamingLogLevels();
    //SparkConf sparkConf = new SparkConf().setAppName("JavaKafkaWordCount");
    //sparkConf.setMaster("spark://60f81dc6426c:7077");
   // SparkConf sparkConf = new SparkConf().setAppName("JavaKafkaWordCount").setMaster("spark://60f81dc6426c:7077");

    // Create the context with a 1 second batch size
    JavaStreamingContext jssc = new JavaStreamingContext("local[4]","JavaKafkaWordCount", new Duration(2000));

    int numThreads = Integer.parseInt(args[3]);
    Logger.getLogger("org").setLevel(Level.OFF);
    Logger.getLogger("akka").setLevel(Level.OFF);
    Map<String, Integer> topicMap = new HashMap<String, Integer>();
    String[] topics = args[2].split(",");
    for (String topic: topics) {
        topicMap.put(topic, numThreads);
      }
    /* for(String t: topic)
    {
        topicMap.put(t, new Integer(3));
    }*/
    // NotSerializable notSerializable = new NotSerializable();
     //JavaRDD<String> rdd = sc.textFile("/tmp/myfile");

    // rdd.map(s -> notSerializable.doSomething(s)).collect();
    JavaPairReceiverInputDStream<String, String> messages =
        KafkaUtils.createStream(jssc, args[0], args[1], topicMap);
    //JavaPairReceiverInputDStream<String, String> kafkaStream = 
    	//   KafkaUtils.createStream(jssc, "localhost:2181","streamingContext", 
    	  //		  topicMap);
    
    System.out.println("Connection !!!!");
   /*JavaDStream<String> data = messages.map(new Function<Tuple2<String, String>, String>() 
                                            {
                                                public String call(Tuple2<String, String> message)
                                                {
                                                    return message._2();
                                                }
                                            }
                                            );*/

    JavaDStream<String> lines = messages.map(new Function<Tuple2<String, String>, String>() {
      @Override
      public String call(Tuple2<String, String> tuple2) {
        return tuple2._2();
      }
    });

    JavaDStream<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
      @Override
      public Iterable<String> call(String x) {
        return Lists.newArrayList(SPACE.split(x));
      }
    });

    JavaPairDStream<String, Integer> wordCounts = words.mapToPair(
      new PairFunction<String, String, Integer>() {
        @Override
        public Tuple2<String, Integer> call(String s) {
          return new Tuple2<String, Integer>(s, 1);
        }
      }).reduceByKey(new Function2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer i1, Integer i2) {
          return i1 + i2;
        }
      });

    wordCounts.print();
    jssc.start();
    jssc.awaitTermination();
  }
}