package com.example.todo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class TodoWorker implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TABLE_NAME = System.getenv("TODOS_TABLE_NAME");

    private static DynamoDbClient dynamoDbClient;

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = (context != null) ? context.getLogger() : null;

        if (TABLE_NAME == null || TABLE_NAME.isBlank()) {
            if (logger != null) {
                logger.log("TODOS_TABLE_NAME is not set, skipping processing");
            }
            return null;
        }

        try {
            for (SQSEvent.SQSMessage msg : event.getRecords()) {
                String body = msg.getBody();
                if (logger != null) {
                    logger.log("Processing SQS message: " + body);
                }

                ObjectNode todo = (ObjectNode) MAPPER.readTree(body);

                String id = todo.hasNonNull("id") ? todo.get("id").asText() : Instant.now().toString();
                String title = todo.hasNonNull("title") ? todo.get("title").asText() : "Untitled";
                String status = todo.hasNonNull("status") ? todo.get("status").asText() : "OPEN";

                Map<String, AttributeValue> item = new HashMap<>();
                item.put("id", AttributeValue.builder().s(id).build());
                item.put("title", AttributeValue.builder().s(title).build());
                item.put("status", AttributeValue.builder().s(status).build());
                item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());

                getDynamoDbClient().putItem(PutItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .item(item)
                        .build());

                if (logger != null) {
                    logger.log("Wrote todo to DynamoDB table " + TABLE_NAME + ": " + todo.toString());
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.log("Error processing SQS batch: " + e.getMessage());
            }
            // Let Lambda/SQS handle retries via failure behavior
        }

        return null;
    }

    private static DynamoDbClient getDynamoDbClient() {
        if (dynamoDbClient == null) {
            String region = System.getenv("AWS_REGION");
            Region awsRegion = (region != null && !region.isBlank())
                    ? Region.of(region)
                    : Region.US_EAST_1;

            dynamoDbClient = DynamoDbClient.builder()
                    .region(awsRegion)
                    .build();
        }
        return dynamoDbClient;
    }
}