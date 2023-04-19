package openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import datatypes.Persona;
import okhttp3.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class OpenAIAPI {
    public String CHAT_RESPONSE = "chat";
    public String APPEARENCE_GEN_RESPONSE = "appearence_gen";
    public String INSTRUCT_RESPONSE = "instruct";
    private String COMPLETION_ENDPOINT = "/chat/completions";
    private String INSTRUCT_ENDPOINT = "/completions";

    private OkHttpClient client;
    private String apiKey;
    private String baseUrl;
    private String model;
    private int maxTokens;
    private Persona persona;
    private List<Map<String,String>> prompts;
    private String instructPrompt;
    private String appearenceGenSettings;
    private Map<String,String> characterData;
    private Map<String, String> rules;
    private String appearenceGenPrompt;
    private String animeRandomGenerationPrompt;
    private String instructModel = "text-davinci-003";

    public OpenAIAPI(String apiKey, String baseUrl) {
        setOpenAISettings();
        OkHttpClient.Builder clientBuilder =
                new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).writeTimeout(600, TimeUnit.SECONDS);
        client = clientBuilder.build();
        prompts = List.of(characterData, rules);
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.maxTokens = 100;
        this.model = "gpt-3.5-turbo";
    }

    public String queryGPT(String endpoint, Map<String, Object> requestBodyMap) throws IOException {
        System.out.println("[OpenAI] Querying OpenAI API");
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, new ObjectMapper().writeValueAsString(requestBodyMap));
        System.out.println("[OpenAI] Request Body: " + new ObjectMapper().writeValueAsString(requestBodyMap));
        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
        Response response = client.newCall(request).execute();
        JsonNode responseNode = new ObjectMapper().readTree(response.body().string());
        JsonNode messageJson;
        String content = "";
        switch (endpoint){
            case "/chat/completions":
                messageJson = responseNode.get("choices").get(0).get("message");
                content = messageJson.get("content").asText();
                break;
            case "/completions":
                messageJson = responseNode.get("choices").get(0).get("text");
                content = messageJson.asText();
                break;
            default:
                messageJson = null;
        }
        JsonNode usage = responseNode.get("usage");
        System.out.println("[Info] OpenAI API Usage: " +
                usage.get("prompt_tokens") + " prompt tokens, " +
                usage.get("completion_tokens") + " completion tokens " +
                "Total: " + usage.get("total_tokens") + " tokens");
        response.close();
        return content;
    }

    public String query(String responseType) throws IOException {
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("max_tokens", maxTokens);
        requestBodyMap.put("temperature", 0.5);
        switch (responseType){
            case "instruct":
                requestBodyMap.put("model", instructModel);
                requestBodyMap.put("prompt", instructPrompt);
                return queryGPT(INSTRUCT_ENDPOINT, requestBodyMap);
            case "chat":
                if (!checkPersonaLoaded()) return null;
                requestBodyMap.put("model", model);
                requestBodyMap.put("messages", prompts);
                return queryGPT(COMPLETION_ENDPOINT, requestBodyMap);
            case "appearence_gen":
                if (!checkPersonaLoaded()) return null;
                requestBodyMap.put("model", instructModel);
                requestBodyMap.put("prompt", appearenceGenSettings + "\n" + appearenceGenPrompt + "\n");
                return persona.getAppearenceDescription() + " ," + queryGPT(INSTRUCT_ENDPOINT, requestBodyMap);
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
        appearenceGenPrompt = prompt;
        return this;
    }

    public OpenAIAPI setInstructModel(String model){
        this.instructModel = model;
        return this;
    }

    public OpenAIAPI setInstructPrompt(String prompt){
        System.out.println("[Info] Instruction prompt set to " + prompt);
        instructPrompt = prompt;
        return this;
    }

    public OpenAIAPI loadAnimeRandomGenerationPrompt(){
        instructPrompt = animeRandomGenerationPrompt;
        return this;
    }

    private boolean checkPersonaLoaded(){
        if (persona == null){
            System.out.println("[Error] No persona set");
            return false;
        }
        return true;
    }

    public Persona getPersona() {
        return persona;
    }

    private void setOpenAISettings(){
        // Sets the prompts for the chat and appearance generation
        File settingsFile = new File("settings/openai_prompts.json");
        JsonObject settings = new JsonObject();
        try {
            settings = JsonParser.parseReader(new FileReader(settingsFile)).getAsJsonObject();
            animeRandomGenerationPrompt = settings.get("randomWaifuPrompt").getAsString();
            appearenceGenSettings = settings.get("appearanceGenPrompt").getAsString();
            rules = Map.of("role", "system", "content", settings.get("defaultChatRules").getAsString());
            characterData = Map.of("role", "system", "content", settings.get("defaultCharacter").getAsString());
            System.out.println("[Info] OpenAI settings loaded");
        } catch (FileNotFoundException e) {
            System.out.println("[Error] Could not find settings file");
        }
        catch (JsonParseException e){
            System.out.println("[Error] Could not OpenAI settings file. Check if it is valid JSON");
        }
    }



}
