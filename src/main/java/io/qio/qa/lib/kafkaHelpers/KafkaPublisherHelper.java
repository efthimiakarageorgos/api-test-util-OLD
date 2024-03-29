/**
 * © Qio Technologies Ltd. 2016. All rights reserved.
 * CONFIDENTIAL AND PROPRIETARY INFORMATION OF QIO TECHNOLOGIES LTD.
 */
package io.qio.qa.lib.kafkaHelpers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.json.simple.JSONObject;

public class KafkaPublisherHelper {
    public void publishMessagesContainedInFile(String[] args) throws IOException, ExecutionException, InterruptedException {
        OptionParser parser = new OptionParser();

        parser.acceptsAll(Lists.newArrayList("s", "servers"), "kafka servers").withRequiredArg().ofType(String.class);
        parser.acceptsAll(Lists.newArrayList("t", "topic"), "kafka topic").withRequiredArg().ofType(String.class);
        parser.acceptsAll(Lists.newArrayList("m", "messages"), "file with messages").withRequiredArg().ofType(String.class);
        parser.acceptsAll(Lists.newArrayList("d", "delay"), "seconds delay between messages").withRequiredArg().ofType(Integer.class).defaultsTo(0);
        parser.acceptsAll(Lists.newArrayList("h", "help"), "show help");

        OptionSet options = parser.parse(args);

        if (!options.hasArgument("servers")) {
            parser.printHelpOn(System.out);
            System.out.println("\nservers is required");
            System.exit(-1);
        }

        if (!options.hasArgument("topic")) {
            parser.printHelpOn(System.out);
            System.out.println("\ntopic is required");
            System.exit(-1);
        }

        if (!options.hasArgument("messages")) {
            parser.printHelpOn(System.out);
            System.out.println("\nmessages is required");
            System.exit(-1);
        }

        File file = new File((String) options.valueOf("messages"));
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter objectWriter = objectMapper.writer();

        JsonNode node = objectMapper.readTree(file);

        if (!node.isArray()) {
            System.out.println("JSON file doesn't contain an array of objects");
        }

        String topic = (String) options.valueOf("topic");

        QioKafkaPublisher publisher = new QioKafkaPublisher((String) options.valueOf("servers"));
        try {
            Iterator<JsonNode> objects = node.iterator();
            while (objects.hasNext()) {
                JsonNode object = objects.next();
                if (!object.isObject()) {
                    System.out.println("JSON file doesn't contain an array of objects");
                    System.exit(-1);
                }

                if (!object.has("key")) {
                    System.out.println("object is missing the field 'key'");
                    System.exit(-1);
                }

                if (!object.get("key").isTextual()) {
                    System.out.println("key is not a string");
                    System.exit(-1);
                }

                if (!object.has("message")) {
                    System.out.println("object is missing the field 'message'");
                    System.exit(-1);
                }

                JsonNode message = object.get("message");

                if (!message.isObject() && !message.isTextual()) {
                    System.out.println("message is not a JSON object or a string");
                    System.exit(-1);
                }

                String key = object.get("key").asText();
                String payload;

                if (message.isObject()) {
                    payload = objectWriter.writeValueAsString(message);
                } else if (message.isTextual()) {
                    payload = message.asText();
                } else {
                    throw new RuntimeException("message is wrong type");
                }

                publisher.publish(topic, key, payload);

                System.out.println("Wrote " + payload);
            }
        } finally {
            publisher.close();
        }
    }


    public void publishJSONMessages(String[] args, ArrayList<JSONObject> JSONObjectsToBePublished) throws IOException, ExecutionException, InterruptedException {
        OptionParser parser = new OptionParser();

        parser.acceptsAll(Lists.newArrayList("s", "servers"), "kafka servers").withRequiredArg().ofType(String.class);
        parser.acceptsAll(Lists.newArrayList("t", "topic"), "kafka topic").withRequiredArg().ofType(String.class);
        parser.acceptsAll(Lists.newArrayList("d", "delay"), "seconds delay between messages").withRequiredArg().ofType(Integer.class).defaultsTo(0);
        parser.acceptsAll(Lists.newArrayList("h", "help"), "show help");

        OptionSet options = parser.parse(args);

        if (!options.hasArgument("servers")) {
            parser.printHelpOn(System.out);
            System.out.println("\nservers is required");
            System.exit(-1);
        }

        if (!options.hasArgument("topic")) {
            parser.printHelpOn(System.out);
            System.out.println("\ntopic is required");
            System.exit(-1);
        }

        if (JSONObjectsToBePublished.isEmpty()) {
            parser.printHelpOn(System.out);
            System.out.println("\nJSON object list is empty");
            System.exit(-1);
        }

        String topic = (String) options.valueOf("topic");

        QioKafkaPublisher publisher = new QioKafkaPublisher((String) options.valueOf("servers"));
        try {
            for (JSONObject ObjectForPublish : JSONObjectsToBePublished) {
                if (ObjectForPublish.get("key") == null) {
                    System.out.println("object is missing the field 'key'");
                    System.exit(-1);
                }

                if (ObjectForPublish.get("message") == null) {
                    System.out.println("object is missing the field 'message'");
                    System.exit(-1);
                }

                publisher.publish(topic, ObjectForPublish.get("key").toString(), ObjectForPublish.get("message").toString());

                System.out.println("Wrote key " + ObjectForPublish.get("message").toString() + " and message " + ObjectForPublish.get("message").toString());
            }
        } finally {
            publisher.close();
        }
    }
}