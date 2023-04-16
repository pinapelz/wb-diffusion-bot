package fileutils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class FileDataProcessor {

    public static String getField(String parameter){
        try {
            Object obj = new JSONParser().parse(new FileReader("settings//config.json"));
            JSONObject jo = (JSONObject) obj;
            return (String) jo.get(parameter);
        }
        catch(FileNotFoundException e){
            System.out.println("Credential file could not be found. Please create it at settings//config.json");
        }
        catch(ParseException ex){
            System.out.println("Ensure that your credential file is valid JSON");
        }
        catch(IOException ex){
            System.out.println("An error occurred while reading the credential file");
        }
        return "";

    }


}
