package datatypes;

import com.google.gson.JsonObject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Persona {
    private String name;
    private String personalityDescription;
    private String appearenceDescription;
    private String rules;

    public Persona(String filePath){
        loadCharacterFromFile(filePath);
    }

    public Persona setAppearenceDescription(String appearenceDescription) {
        this.appearenceDescription = appearenceDescription;
        return this;
    }

    public Persona setPersonalityDescription(String personalityDescription) {
        this.personalityDescription = personalityDescription;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getPersonalityDescription() {
        return personalityDescription;
    }

    public String getAppearenceDescription() {
        return appearenceDescription;
    }

    public String getRules() {
        return rules;
    }


    public boolean loadCharacterFromFile(String path){
        File f = new File(path);
        if(!f.exists()) return false;
        try {
            Object obj = new JSONParser().parse(new FileReader(path));
            JSONObject jo = (JSONObject) obj;
            this.name = (String) jo.get("name");
            this.personalityDescription = (String) jo.get("personality");
            this.appearenceDescription = (String) jo.get("appearence");
            this.rules = (String) jo.get("rules");

        }
        catch(IOException ex){
            System.out.println("[Error] An error occurred while reading character file");
            return false;
        }
        catch(ParseException ex){
            System.out.println("[Error] Ensure character file is in JSON format");
            return false;
        }
        System.out.println("[Info] Character file loaded successfully");
        return true;
    }

    public boolean writeCharacterToFile(String path){
        File f = new File(path);
        if(!f.exists()){
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.out.println("[Error] An error occurred while creating the character file");
                return false;
            }
        }
        JsonObject characterObject = new JsonObject();
        characterObject.addProperty("name", name);
        characterObject.addProperty("personality", personalityDescription);
        characterObject.addProperty("appearence", appearenceDescription);
        characterObject.addProperty("rules", rules);
        try {
            Files.write(Paths.get(path), characterObject.toString().getBytes());
        } catch (IOException e) {
            System.out.println("[Error] Unable to write character data to given path");
            return false;
        }
        return true;
    }





}
