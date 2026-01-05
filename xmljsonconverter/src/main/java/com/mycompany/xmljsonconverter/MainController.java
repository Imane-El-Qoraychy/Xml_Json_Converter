package com.mycompany.xmljsonconverter;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MainController {

    // Services for the three conversion modes
    private final LocalConverterService localService = new LocalConverterService();
    private final ManualConverterService manualService = new ManualConverterService();
    private final ApiConverterService apiService =
            new ApiConverterService("http://localhost:8080/api");

    private File currentInputFile;
    private File currentOutputFile;

    @FXML private ComboBox<String> modeCombo;
    @FXML private ComboBox<String> conversionCombo;
    @FXML private TextArea inputArea;
    @FXML private TextArea outputArea;

    @FXML
    public void initialize() {
        modeCombo.getItems().addAll(
                "LOCAL (Jackson)",
                "LOCAL (Manual)",
                "API"
        );
        modeCombo.setValue("LOCAL (Jackson)");

        conversionCombo.getItems().addAll(
                "XML To JSON",
                "JSON To XML"
        );
        conversionCombo.setValue("XML To JSON");
    }

    @FXML
    private void loadFile() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose XML or JSON file");
            
            File initialDir = new File(System.getProperty("user.dir"), "data");
            if (initialDir.exists() && initialDir.isDirectory()) {
                chooser.setInitialDirectory(initialDir);
            }
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml"),
                    new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json"),
                    new FileChooser.ExtensionFilter("All files (*.*)", "*.*")
            );

            File file = chooser.showOpenDialog(null);
            if (file == null) return;

            currentInputFile = file;
            currentOutputFile = null;

            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            inputArea.setText(content);
            outputArea.clear();

            String name = file.getName().toLowerCase();
            if (name.endsWith(".xml")) conversionCombo.setValue("XML To JSON");
            if (name.endsWith(".json")) conversionCombo.setValue("JSON To XML");

        } catch (Exception e) {
            outputArea.setText("Load error: " + e.getMessage());
        }
    }

    @FXML
    private void saveFile() {
        try {
            String output = outputArea.getText();
            if (output == null || output.isBlank()) {
                outputArea.setText("Nothing to save (output is empty).");
                return;
            }

            if (currentOutputFile != null) {
                Files.writeString(currentOutputFile.toPath(), output, StandardCharsets.UTF_8);
                outputArea.appendText("\n\n(Saved: " + currentOutputFile.getAbsolutePath() + ")");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save result");

            if (looksLikeXml(output)) {
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml")
                );
                chooser.setInitialFileName("result.xml");
            } else if (looksLikeJson(output)) {
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json")
                );
                chooser.setInitialFileName("result.json");
            } else {
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt")
                );
                chooser.setInitialFileName("result.txt");
            }

            File file = chooser.showSaveDialog(null);
            if (file == null) return;

            currentOutputFile = file;
            Files.writeString(file.toPath(), output, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            outputArea.setText("Save error: " + e.getMessage());
        }
    }

    @FXML
    private void convert() {
        try {
            String input = inputArea.getText();
            if (input == null || input.isBlank()) {
                outputArea.setText("Error: input field is empty.");
                return;
            }

            boolean xmlToJson =
                    "XML To JSON".equals(conversionCombo.getValue());

            if (xmlToJson && !looksLikeXml(input)) {
                outputArea.setText("Input does not look like XML (must start with '<').");
                return;
            }

            if (!xmlToJson && !looksLikeJson(input)) {
                outputArea.setText("Input does not look like JSON (must start with '{' or '[').");
                return;
            }

            String mode = modeCombo.getValue();
            String result;
            boolean outputIsJson;

            if (xmlToJson) {
                switch (mode) {
                    case "LOCAL (Manual)" ->
                            result = manualService.xmlToJson(input);
                    case "LOCAL (Jackson)" ->
                            result = localService.xmlToJson(input);
                    case "API" ->
                            result = apiService.xmlToJson(input);
                    default ->
                            throw new IllegalStateException("Unknown mode: " + mode);
                }
                outputIsJson = true;
            } else {
                switch (mode) {
                    case "LOCAL (Manual)" ->
                            result = manualService.jsonToXmlAutoRoot(input);
                    case "LOCAL (Jackson)" ->
                            result = localService.jsonToXmlAutoRoot(input);
                    case "API" ->
                            result = apiService.jsonToXmlAutoRoot(input);
                    default ->
                            throw new IllegalStateException("Unknown mode: " + mode);
                }
                outputIsJson = false;
            }

            outputArea.setText(result);
            currentOutputFile = defaultOutputFile(outputIsJson);

        } catch (Exception e) {
            outputArea.setText("Conversion error: " + e.getMessage());
        }
    }

    private boolean looksLikeXml(String s) {
        return s != null && s.stripLeading().startsWith("<");
    }

    private boolean looksLikeJson(String s) {
        String t = (s == null) ? "" : s.stripLeading();
        return t.startsWith("{") || t.startsWith("[");
    }

    private File defaultOutputFile(boolean outputIsJson) {
        if (currentInputFile == null) return null;

        String name = currentInputFile.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext = outputIsJson ? ".json" : ".xml";

        File target = new File(currentInputFile.getParentFile(), base + ext);
        return makeUniqueIfExists(target);
    }

    private File makeUniqueIfExists(File file) {
        if (!file.exists()) return file;

        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext = (dot > 0) ? name.substring(dot) : "";

        File parent = file.getParentFile();
        int i = 1;
        File candidate;

        do {
            candidate = new File(parent, base + "(" + i + ")" + ext);
            i++;
        } while (candidate.exists());

        return candidate;
    }

    @FXML
    private void clearAll() {
        inputArea.clear();
        outputArea.clear();
        currentInputFile = null;
        currentOutputFile = null;
        conversionCombo.setValue("XML To JSON");
        modeCombo.setValue("LOCAL (Jackson)");
    }
}
