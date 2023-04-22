package llm.ooba;
/*
This if for using oobabooga's gradio Web UI API to run a custom model
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import datatypes.Persona;
import fileutils.FileDataProcessor;
import llm.LargeLanguageModelAPI;
import okhttp3.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OobaAPI implements LargeLanguageModelAPI<OobaAPI> {
    private final String COMPLETION_ENDPOINT = "/run/textgen";
    private final String INSTRUCT_ENDPOINT = "/run/textgen";

    private OkHttpClient client;
    private String baseUrl;
    private int maxTokens;
    private String model;
    private String prompt;
    private JSONObject params;


    public OobaAPI(String baseUrl) {
        System.out.println("[OobaAPI] Initializing Oobabooga Web-UI API");
        OkHttpClient.Builder clientBuilder =
                new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).writeTimeout(600, TimeUnit.SECONDS);
        client = clientBuilder.build();
        this.baseUrl = baseUrl;
        this.maxTokens = 100;
        setParameters();

    }
    @Override
    public String query(String responseType) throws IOException {
        JSONArray payloadDataList = new JSONArray();
        payloadDataList.add(prompt);
        payloadDataList.add(params);
        JSONArray payloadList = new JSONArray();
        String payloadData = payloadDataList.toString();
        payloadList.add(payloadData);
        JSONObject payload = new JSONObject();
        payload.put("data", payloadList);
        String requestBody = payload.toString();
        System.out.println("[OobaAPI] Request Body: " + requestBody);
        Request request = new Request.Builder()
                .url(baseUrl + COMPLETION_ENDPOINT)
                .post(RequestBody.create(null, requestBody))
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        JsonNode jsonNode = new ObjectMapper().readTree(responseBody);
        String completion = jsonNode.get("data").get(0).asText();
        System.out.println(completion);
        return completion;
    }

    @Override
    public OobaAPI setPersona(Persona persona) {
        return null;
    }

    @Override
    public OobaAPI setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }


    @Override
    public OobaAPI setAppearenceGenPrompts(String prompt) {
        return null;
    }

    @Override
    public OobaAPI loadAnimeRandomGenerationPrompt() {
        return null;
    }

    @Override
    public OobaAPI setInstructPrompt(String instruction) {
        return null;
    }

    public OobaAPI setMaxTokens(int maxTokens){
        this.maxTokens = maxTokens;
        setParameters();
        return this;
    }

    private void setParameters(){
        params = new JSONObject();
        params.put("max_new_tokens", 200);
        params.put("do_sample", true);
        params.put("temperature", 0.72);
        params.put("top_p", 0.73);
        params.put("typical_p", 1);
        params.put("repetition_penalty", 1.1);
        params.put("encoder_repetition_penalty", 1.0);
        params.put("top_k", 0);
        params.put("min_length", 0);
        params.put("no_repeat_ngram_size", 0);
        params.put("num_beams", 1);
        params.put("penalty_alpha", 0);
        params.put("length_penalty", 1);
        params.put("early_stopping", false);
        params.put("seed", -1);
        params.put("add_bos_token", true);
        params.put("truncation_length", 2048);
        params.put("ban_eos_token", false);
        params.put("skip_special_tokens", true);
        params.put("stopping_strings", new JSONArray());
    }
}
