package com.vigneshvrt;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Main {

    private final String INDENTATION = "    ";

    private void updatePatch(String sourceJsonPath, String patchJsonPath) throws IOException, ParseException {
        Map<String, String> flattenKVPairs = flattenJson(patchJsonPath);
        System.out.println("Total keys in patch file: " + flattenKVPairs.size());
        ArrayDeque<String> queue = new ArrayDeque<>();
        String sourceJsonString = fileToString(sourceJsonPath);
        String[] lines = sourceJsonString.split("\n");
        int noChange = 0;
        int updatedKeys = 0;
        StringBuilder updateFileContent = new StringBuilder();
        for (String line : lines) {
            String updatedLine = line;
            line = line.trim();
            if (line.endsWith("{")) {
                if (line.length() > 1) {
                    int colonIdx = line.indexOf(":");
                    String key = line.substring(0, colonIdx).replaceAll("\"", "").trim();
                    queue.addLast(key);
                }
            } else if (line.endsWith("}") || line.endsWith("},")) {
                if (!queue.isEmpty()) {
                    queue.removeLast();
                }
            } else if (!line.isEmpty()) {
                int colonIdx = line.indexOf(":");
                String currentKey = line.substring(0, colonIdx).replaceAll("\"", "").trim();
                String fullKey = queue.isEmpty() ? currentKey : joinString(queue).append(currentKey).toString();
                if (flattenKVPairs.containsKey(fullKey)) {

                    StringBuilder newLine = new StringBuilder();
                    // append new key value pair
                    newLine.append("\"");
                    newLine.append(currentKey);
                    newLine.append("\" : \"");

                    //escapes " (double quotes)
                    //todo: For now, only double quotes are escaped. Need to properly escape all required characters
                    String value = flattenKVPairs.get(fullKey).replaceAll("\\\"", "\\\\\"");
                    newLine.append(value);
                    newLine.append("\"");
                    if (line.endsWith(",")) {
                        newLine.append(",");
                    }

                    String newLineStr = newLine.toString();
                    if (!newLineStr.equals(line)) {
                        StringBuilder spaces = new StringBuilder(INDENTATION);
                        int count = queue.size();
                        while (count-- > 0) {
                            spaces.append(INDENTATION);
                        }
                        updatedLine = spaces.append(newLineStr).toString();
                        updatedKeys++;
                    } else {
                        System.out.println("No change : " + line);
                        noChange++;
                    }
                }
            }
            updateFileContent.append(updatedLine);
            updateFileContent.append("\n");
        }
        System.out.println("Key with no change: " + noChange);
        System.out.println("Updated keys: " + updatedKeys);
        stringToFile(sourceJsonPath, updateFileContent.toString().trim());
    }

    private Map<String, String> flattenJson(String filePath) throws FileNotFoundException, ParseException {
        String fileContent = fileToString(filePath);
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(fileContent);
        return flattenJsonHelper("", json);
    }

    private Map<String, String> flattenJsonHelper(String prefix, JSONObject json) throws FileNotFoundException, ParseException {
        Map<String, String> kvPairs = new HashMap<>();
        json.forEach((key, value) -> {
            String updatedPrefix = prefix.isEmpty() ? key.toString() : prefix + "." + key;
            if (value instanceof JSONObject) {
                try {
                    kvPairs.putAll(Objects.requireNonNull(flattenJsonHelper(updatedPrefix, (JSONObject) value)));
                } catch (FileNotFoundException | ParseException e) {
                    throw new RuntimeException(e);
                }
            } else if (value instanceof String) {
                kvPairs.put(updatedPrefix, value.toString());
            } else {
                throw new RuntimeException("Unknown type: " + value.getClass());
            }
        });
        return kvPairs;
    }

    private StringBuilder joinString(ArrayDeque<String> queue) {
        StringBuilder sb = new StringBuilder();
        for (String s : queue) {
            sb.append(s).append(".");
        }
        return sb;
    }

    private String fileToString(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        Scanner in = new Scanner(file);
        StringBuilder fileContent = new StringBuilder();
        while (in.hasNext()) {
            fileContent.append(in.nextLine());
            if (in.hasNext()) {
                fileContent.append("\n");
            }
        }
        in.close();
        return fileContent.toString();
    }

    private void stringToFile(String filePath, String fileContent) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        fileWriter.write(fileContent.toCharArray());
        fileWriter.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 2) {
            String sourceJsonPath = args[0];
            String patchJsonPath = args[1];

            new Main().updatePatch(sourceJsonPath, patchJsonPath);
        } else {
            System.out.println("Command: java -jar <jar_file> <source_json_path> <patch_json_path>");
        }
    }
}