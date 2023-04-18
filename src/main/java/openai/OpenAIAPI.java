package openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import datatypes.Persona;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class OpenAIAPI {
    public String CHAT_RESPONSE = "chat";
    public String APPEARENCE_GEN_RESPONSE = "appearence_gen";
    private OkHttpClient client;
    private String apiKey;
    private String baseUrl;
    private String model;
    private int maxTokens;
    private Persona persona;
    private List<Map<String,String>> appearenceGenPrompts;
    private List<Map<String,String>> prompts;
    private Map<String, String> appearenceGenSettings = Map.of("role", "system", "content", "Be creative. List the scenery, objects, and what you think the person is doing, wearing, and their expression in the prompt below. Use only single words separated by commas in a list. Make it up if you're not sure or if its unknown. All 3 fields must be filled");
    private Map<String,String> characterData = Map.of("role", "system", "content", "You are a helpful assistant");
    private Map<String, String> rules = Map.of("role", "system", "content", "You should not break character at any time and always respond as the character themselves.");
    public OpenAIAPI(String apiKey, String baseUrl) {
        OkHttpClient.Builder clientBuilder =
                new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).writeTimeout(600, TimeUnit.SECONDS);
        client = clientBuilder.build();
        prompts = List.of(characterData, rules);
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.maxTokens = 100;
        this.model = "gpt-3.5-turbo";
    }

    public String queryGPT(List<Map<String,String>> prompt) throws IOException {
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("messages", prompt);
        requestBodyMap.put("max_tokens", maxTokens);
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
        JsonNode usage = responseNode.get("usage");
        System.out.println("[Info] OpenAI API Usage: " + usage.get("prompt_tokens") + " prompt tokens, " + usage.get("completion_tokens") + " completion tokens " +
        "Total: " + usage.get("total_tokens") + " tokens");
        response.close();
        return content;
    }

    public String query(String responseType) throws IOException {
        if (persona == null){
            System.out.println("[Error] No persona set");
            return null;
        }
        switch (responseType){
            case "chat":
                return queryGPT(prompts);
            case "appearence_gen":
                return persona.getAppearenceDescription() + " ," + queryGPT(appearenceGenPrompts);
            default:
                return null;
        }

    }

    public OpenAIAPI setPersona(Persona persona){
        this.persona = persona;
        this.rules = Map.of("role", "system", "content", persona.getRules());
        this.characterData = Map.of("role", "system", "content", persona.getPersonalityDescription());
        this.prompts = List.of(characterData, rules);
        System.out.println("[Info] Set persona to " + persona.getName());
        return this;
    }

    public OpenAIAPI setChatRules(String rules){
        this.rules = Map.of(
                "role", "system",
                "content", rules);
        return this;
    }

    public OpenAIAPI setPrompt(String prompt) {
        prompts = List.of(characterData, rules, Map.of(
                "role", "user",
                "content", prompt));
        return this;
    }

    public OpenAIAPI setAppearenceGenPrompts(String prompt){
        appearenceGenPrompts = List.of(appearenceGenSettings, Map.of("role", "user", "content", prompt));
        return this;
    }


}
