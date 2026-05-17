package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.AITaskSuggestionResponse;
import com.eulerity.taskmanager.dto.SubtaskSuggestion;
import com.eulerity.taskmanager.dto.TaskBreakdownResponse;
import com.eulerity.taskmanager.dto.TaskRequest;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiAIService implements AIProvider {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiAIService(@Value("${gemini.api.key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini API key is not configured. Set the GEMINI_API_KEY environment variable.");
        }
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AITaskSuggestionResponse suggestTask(String description) {
        String requestBody = buildRequestBody(description);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI request was interrupted", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to connect to AI service: " + ex.getMessage(), ex);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "AI service returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseGeminiResponse(response.body());
    }

    @Override
    public TaskBreakdownResponse breakdownTask(TaskResponse task) {
        String requestBody = buildBreakdownRequestBody(task);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI request was interrupted", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to connect to AI service: " + ex.getMessage(), ex);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "AI service returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseBreakdownResponse(response.body(), task);
    }

    private String buildRequestBody(String description) {
        String prompt = """
                You are a task management assistant. Based on the description below, suggest a structured task.
                Return ONLY valid JSON — no markdown, no code fences, no commentary outside the JSON object.
                The JSON must contain exactly these fields:
                  "title"       – concise task title, max 200 characters
                  "description" – detailed task description, max 1000 characters
                  "dueDate"     – ISO-8601 LocalDateTime string, e.g. 2027-06-15T09:00:00, must be a future date
                  "priority"    – one of: LOW, MEDIUM, HIGH
                  "status"      – always TODO
                  "reasoning"   – brief explanation of your suggestions

                Description: """ + description;

        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize AI request body", ex);
        }
    }

    private String buildBreakdownRequestBody(TaskResponse task) {
        String taskDetails = "Title: " + task.getTitle()
                + "\nDescription: " + (task.getDescription() != null ? task.getDescription() : "none")
                + "\nPriority: " + task.getPriority()
                + "\nDue date: " + task.getDueDate();

        String prompt = """
                Break down this task into 3-5 actionable subtasks: %s
                Return ONLY valid JSON — no markdown, no code fences, no commentary outside the JSON object.
                The JSON must contain exactly these fields:
                  "suggestedSubtasks" – array of 3-5 subtask objects, each with:
                      "title"       – concise subtask title, max 200 characters
                      "description" – detailed subtask description, max 1000 characters
                      "priority"    – one of: LOW, MEDIUM, HIGH
                      "status"      – always TODO
                  "reasoning" – brief explanation of why you chose these subtasks
                """.formatted(taskDetails);

        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize AI request body", ex);
        }
    }

    private AITaskSuggestionResponse parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.at("/candidates/0/content/parts/0/text").asText();

            text = stripCodeFences(text.strip());

            JsonNode taskJson = objectMapper.readTree(text);

            String title       = taskJson.get("title").asText();
            String description = taskJson.has("description") ? taskJson.get("description").asText() : null;
            String dueDateStr  = taskJson.get("dueDate").asText();
            Priority priority  = Priority.valueOf(taskJson.get("priority").asText().toUpperCase());
            Status status      = taskJson.has("status")
                    ? Status.valueOf(taskJson.get("status").asText().toUpperCase())
                    : Status.TODO;
            String reasoning   = taskJson.has("reasoning") ? taskJson.get("reasoning").asText() : "";

            LocalDateTime dueDate = LocalDateTime.parse(dueDateStr);

            TaskRequest suggestedTask = new TaskRequest(title, description, dueDate, priority, status);
            return new AITaskSuggestionResponse(suggestedTask, reasoning);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse AI response: " + ex.getMessage(), ex);
        }
    }

    private TaskBreakdownResponse parseBreakdownResponse(String responseBody, TaskResponse originalTask) {
        try {
            JsonNode root   = objectMapper.readTree(responseBody);
            String text     = root.at("/candidates/0/content/parts/0/text").asText();
            text            = stripCodeFences(text.strip());
            JsonNode parsed = objectMapper.readTree(text);

            JsonNode subtasksNode = parsed.get("suggestedSubtasks");
            if (subtasksNode == null || !subtasksNode.isArray()) {
                throw new RuntimeException("AI response missing suggestedSubtasks array");
            }

            List<SubtaskSuggestion> subtasks = new ArrayList<>();
            for (JsonNode node : subtasksNode) {
                String title       = node.get("title").asText();
                String description = node.has("description") ? node.get("description").asText() : null;
                Priority priority  = Priority.valueOf(node.get("priority").asText().toUpperCase());
                Status status      = node.has("status")
                        ? Status.valueOf(node.get("status").asText().toUpperCase())
                        : Status.TODO;
                subtasks.add(new SubtaskSuggestion(title, description, priority, status));
            }

            String reasoning = parsed.has("reasoning") ? parsed.get("reasoning").asText() : "";
            return new TaskBreakdownResponse(originalTask, subtasks, reasoning);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse AI breakdown response: " + ex.getMessage(), ex);
        }
    }

    private String stripCodeFences(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        text = text.replaceFirst("^```(?:json)?\\s*", "");
        int closingFence = text.lastIndexOf("```");
        if (closingFence != -1) {
            text = text.substring(0, closingFence);
        }
        return text.strip();
    }
}
