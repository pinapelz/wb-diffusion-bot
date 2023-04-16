package stablediff;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import okhttp3.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StableDiffusionAPI {
    private String baseUrl;
    private String negativePrompt;
    private int stepCount;
    private OkHttpClient client;

    public StableDiffusionAPI(String baseUrl) {
        this.baseUrl = baseUrl;
        this.stepCount = 20;
        OkHttpClient.Builder clientBuilder =
                new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).writeTimeout(600, TimeUnit.SECONDS);
        this.client = clientBuilder.build();
        negativePrompt = setDefaultNegatives();

    }
    public String generateImage(String prompt) throws IOException {
        Map<String, Object> payload = Map.of(
                "prompt", prompt,
                "negative_prompt", negativePrompt,
                "steps", stepCount);
        Request request = new Request.Builder()
                .url(baseUrl+"/sdapi/v1/txt2img")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new ObjectMapper().writeValueAsString(payload)))
                .build();
        Map<String, Object> responseMap = new ObjectMapper().readValue(client.newCall(request).execute().body().string(),
                new TypeReference<>() {});
        List<String> images = (List<String>) responseMap.get("images");
        for (String imageString : images) {
            byte[] imageData = Base64.getDecoder().decode(imageString.split(",", 2)[0]);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            Map<String, Object> pngPayload = Map.of("image", "data:image/png;base64," + imageString.split(",", 2)[0]);
            Request pngRequest = new Request.Builder()
                    .url(baseUrl+"/sdapi/v1/png-info")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new ObjectMapper().writeValueAsString(pngPayload)))
                    .build();
            Map<String, Object> pngResponseMap = new ObjectMapper().readValue(client.newCall(pngRequest).execute().body().string(),
                    new TypeReference<>() {});
            ImageIO.write(image, "png", new File("output.png"));
        }
        return "Return the path to the image";
    }

    private static String setDefaultNegatives(){
        File file = new File("negatives.txt");
        try (Scanner scanner = new Scanner(file).useDelimiter("\\Z")) {
            return scanner.next();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public StableDiffusionAPI setStepCount(int stepCount) {
        this.stepCount = stepCount;
        return this;
    }

    public List<String> getAvailableModels() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl+"/sdapi/v1/sd-models")
                .build();
        Response response = client.newCall(request).execute();
        JsonNode responseNode = new ObjectMapper().readTree(response.body().string());
        List<String> models = new ArrayList<>();
        responseNode.forEach(model -> models.add(model.get("title")+","+model.get("model_name")));
        return models;
    }
}
