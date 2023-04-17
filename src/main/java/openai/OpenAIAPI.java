package openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OpenAIAPI {
    private OkHttpClient client;
    private String apiKey;
    private String baseUrl;
    private String model;
    private int maxTokens;
    private List<Map<String,String>> prompts;
    private Map<String,String> defaultCharacter = Map.of(
            "role", "system",
            "content", "You are role playing as an anime girl named Aizawa. You should fully take on this personality and must never break character. You should make up scenarios when you don't know what to do" +
            "Aizawa is a cheerful and optimistic anime girl who loves making new friends and exploring new places. " +
            "She is always eager to try new things and takes on challenges with a can-do attitude. " +
            "Despite her outgoing nature, she also has a sensitive side and is quick to empathize with others. " +
            "Aizawa is a loyal friend and will go out of her way to help those in need. " +
            "She has a passion for music and loves to sing and play the guitar. " +
            "Her positive energy is infectious, and she brightens up any room she enters. Make sure you always talk in the first person in roleplaying mode");

    public OpenAIAPI(String apiKey, String baseUrl) {
        client = new OkHttpClient();
        prompts = List.of(defaultCharacter);
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.maxTokens = 100;
        this.model = "gpt-3.5-turbo";
    }

    public String queryGPT() throws IOException {
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("messages", prompts);
        // requestBodyMap.put("max_tokens", maxTokens);
        requestBodyMap.put("temperature", 0.5);
        requestBodyMap.put("model", model);
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, new ObjectMapper().writeValueAsString(requestBodyMap));
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        Response response = client.newCall(request).execute();
        JsonNode responseNode = new ObjectMapper().readTree(response.body().string());
        JsonNode messageJson = responseNode.get("choices").get(0).get("message");
        String content = messageJson.get("content").asText();
        response.close();
        return content;
    }


    public void setPrompt(String prompt) {
        this.prompts = List.of(defaultCharacter, Map.of(
                "role", "user",
                "content", prompt));
    }

    public void setCharacter(String profile){
        defaultCharacter = Map.of(
                "role", "system",
                "content", profile);
    }
}
