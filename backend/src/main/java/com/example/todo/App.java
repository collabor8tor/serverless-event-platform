package com.example.todo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.Map;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TABLE_NAME = System.getenv("TODOS_TABLE_NAME");
    private static final String QUEUE_URL = System.getenv("TODO_QUEUE_URL");

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Access-Control-Allow-Origin", "*",
            "Access-Control-Allow-Headers", "Content-Type",
            "Access-Control-Allow-Methods", "GET,POST,OPTIONS"
    );

    private static DynamoDbClient dynamoDbClient;
    private static SqsClient sqsClient;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = (context != null) ? context.getLogger() : null;

        try {
            String rawMethod = event != null ? event.getHttpMethod() : null;

            String method = (rawMethod == null || rawMethod.isBlank())
                    ? "POST"
                    : rawMethod.toUpperCase();

            if ("OPTIONS".equals(method)) {
                return corsResponse(200, "{\"message\":\"CORS preflight OK\"}");
            } else if ("POST".equals(method)) {
                return handleCreateTodo(event, logger);
            } else if ("GET".equals(method)) {
                return handleListTodos(logger);
            } else {
                return corsResponse(405, "{\"message\":\"Method not allowed\"}");
            }

        } catch (Exception e) {
            if (logger != null) {
                logger.log("Error: " + e.getMessage());
            }
            return corsResponse(500, "{\"message\":\"Internal server error\"}");
        }
    }

    private APIGatewayProxyResponseEvent handleCreateTodo(APIGatewayProxyRequestEvent event, LambdaLogger logger) throws Exception {
        String body = event.getBody();
        if (body == null || body.isEmpty()) {
            return badRequest("Request body is required");
        }

        ObjectNode json = (ObjectNode) MAPPER.readTree(body);
        String title = json.hasNonNull("title") ? json.get("title").asText() : null;

        if (title == null || title.isBlank()) {
            return badRequest("title is required");
        }

        String id = Instant.now().toString();
        ObjectNode todo = MAPPER.createObjectNode();
        todo.put("id", id);
        todo.put("title", title);
        todo.put("status", "OPEN");

        if (QUEUE_URL != null && !QUEUE_URL.isBlank()) {
            try {
                SendMessageRequest sendReq = SendMessageRequest.builder()
                        .queueUrl(QUEUE_URL)
                        .messageBody(todo.toString())
                        .build();

                getSqsClient().sendMessage(sendReq);

                if (logger != null) {
                    logger.log("Sent todo to SQS queue " + QUEUE_URL + ": " + todo.toString());
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.log("Failed to send message to SQS: " + e.getMessage());
                }
            }
        }

        return corsResponse(201, todo.toString());
    }

    private APIGatewayProxyResponseEvent handleListTodos(LambdaLogger logger) {
        ArrayNode todos = MAPPER.createArrayNode();

        if (TABLE_NAME != null && !TABLE_NAME.isBlank()) {
            try {
                ScanResponse scan = getDynamoDbClient().scan(ScanRequest.builder()
                        .tableName(TABLE_NAME)
                        .build());

                for (Map<String, AttributeValue> item : scan.items()) {
                    ObjectNode t = MAPPER.createObjectNode();
                    t.put("id", item.getOrDefault("id", AttributeValue.builder().s("").build()).s());
                    t.put("title", item.getOrDefault("title", AttributeValue.builder().s("").build()).s());
                    t.put("status", item.getOrDefault("status", AttributeValue.builder().s("").build()).s());
                    todos.add(t);
                }

                if (logger != null) {
                    logger.log("Read " + scan.count() + " items from DynamoDB table " + TABLE_NAME);
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.log("Failed to read from DynamoDB: " + e.getMessage());
                }
            }
        } else {
            ObjectNode sample = MAPPER.createObjectNode();
            sample.put("id", "sample-1");
            sample.put("title", "Sample todo from Lambda");
            sample.put("status", "OPEN");
            todos.add(sample);
        }

        ObjectNode responseBody = MAPPER.createObjectNode();
        responseBody.put("service", "todo-api-lambda-java");
        responseBody.put("message", "GET /todos is working");
        responseBody.set("items", todos);

        return corsResponse(200, responseBody.toString());
    }

    private APIGatewayProxyResponseEvent badRequest(String message) {
        return corsResponse(400, "{\"message\":\"" + message + "\"}");
    }

    private APIGatewayProxyResponseEvent corsResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(CORS_HEADERS)
                .withBody(body);
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

    private static SqsClient getSqsClient() {
        if (sqsClient == null) {
            String region = System.getenv("AWS_REGION");
            Region awsRegion = (region != null && !region.isBlank())
                    ? Region.of(region)
                    : Region.US_EAST_1;

            sqsClient = SqsClient.builder()
                    .region(awsRegion)
                    .build();
        }
        return sqsClient;
    }
}