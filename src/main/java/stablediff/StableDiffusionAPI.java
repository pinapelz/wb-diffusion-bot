package stablediff;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private String positivePrompt;
    private OkHttpClient client;

    private int stepCount;
    private int width;
    private int height;


    public StableDiffusionAPI(String baseUrl) {
        OkHttpClient.Builder clientBuilder =
                new OkHttpClient.Builder().readTimeout(600, TimeUnit.SECONDS).writeTimeout(600, TimeUnit.SECONDS);
        this.client = clientBuilder.build();
        negativePrompt = setDefaultNegatives();
        positivePrompt = setDefaultPositives();
        this.baseUrl = baseUrl;
        this.stepCount = 20;
        this.width = 512;
        this.height = 512;
    }

    public String generateImage(String prompt) throws IOException {
        System.out.println("[Stable Diffusion]" + " Generating image with prompt: " + prompt);
        Map<String, Object> payload = Map.of(
                "prompt", positivePrompt + ", " + prompt,
                "negative_prompt", negativePrompt,
                "steps", stepCount,
                "width", width,
                "height", height);
        Request request = new Request.Builder()
                .url(baseUrl + "/sdapi/v1/txt2img")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), new ObjectMapper().writeValueAsString(payload)))
                .build();
        Map<String, Object> responseMap = new ObjectMapper().readValue(client.newCall(request).execute().body().string(),
                new TypeReference<>() {
                });
        List<String> images = (List<String>) responseMap.get("images");
        for (String imageString : images) {
            byte[] imageData = Base64.getDecoder().decode(imageString.split(",", 2)[0]);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            ImageIO.write(image, "png", new File("output.png"));
        }
        return "Return the path to the image";
    }

    private static String setDefaultNegatives() {
        File file = new File("settings/negatives.txt");
        try (Scanner scanner = new Scanner(file).useDelimiter("\\Z")) {
            return scanner.next();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String setDefaultPositives() {
        File file = new File("settings/positives.txt");
        try (Scanner scanner = new Scanner(file).useDelimiter("\\Z")) {
            return scanner.next();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getAvailableModels() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/sdapi/v1/sd-models")
                .build();
        Response response = client.newCall(request).execute();
        JsonNode responseNode = new ObjectMapper().readTree(response.body().string());
        List<String> models = new ArrayList<>();
        responseNode.forEach(model -> models.add(model.get("title") + "," + model.get("model_name")));
        return models;
    }

    public boolean setCheckpoint(String checkpointName) throws IOException {
        Map<String, Object> payload = Map.of("sd_model_checkpoint", checkpointName);
        Request request = new Request.Builder()
                .url(baseUrl + "/sdapi/v1/options")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                        new ObjectMapper().writeValueAsString(payload)))
                .build();
        Response response = client.newCall(request).execute();
        return response.isSuccessful();
    }

    public StableDiffusionAPI setStepCount(int stepCount) {
        this.stepCount = stepCount;
        return this;
    }

    public StableDiffusionAPI setWidth(int width) {
        this.width = width;
        return this;
    }

    public StableDiffusionAPI setHeight(int height) {
        this.height = height;
        return this;
    }
}
