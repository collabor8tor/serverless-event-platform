package com.example.todo;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    @Test
    void testCreatesTodo() {
        App app = new App();

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");  // be explicit
        event.setBody("{\"title\":\"Buy milk\"}");

        APIGatewayProxyResponseEvent response = app.handleRequest(event, null);

        assertEquals(201, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"title\":\"Buy milk\""));
        assertTrue(body.contains("\"status\":\"OPEN\""));
    }

    @Test
    void testRequiresTitle() {
        App app = new App();

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod("POST");  // still POST, but invalid body
        event.setBody("{}");

        APIGatewayProxyResponseEvent response = app.handleRequest(event, null);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("title is required"));
    }
}