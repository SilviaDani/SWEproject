package com.sweproject.analysis2;

import java.io.FileReader;
import java.io.IOException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
public class JsonFileReader {

    public static JsonObject readJsonFromFile(String filePath) {
        JsonObject jsonObject = null;
        try (FileReader reader = new FileReader(filePath)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (jsonElement.isJsonObject()) {
                jsonObject = jsonElement.getAsJsonObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static void main(String[] args) {
        String filePath = "path/to/your/file.json";
        JsonObject jsonObject = readJsonFromFile(filePath);
        if (jsonObject != null) {
            System.out.println(jsonObject);
        } else {
            System.out.println("Failed to read JSON from file.");
        }
    }
}