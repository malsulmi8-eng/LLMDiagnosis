package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class GenerateLLMDiagnoses {
    public static void main(String[] args) {
        String clinicalCasesFile = "cases.xml";
        String apiKey = "";
        OpenAiChatModel model = null;
        ChatLanguageModel ollamaModel null;
        String modelName = "";
        boolean isOllamaModel = false;

        if (modelName.equals("gpt-6-astra")) {
            model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gpt-6-astra")
                    .build();
        }

        if (modelName.equals("gemini-3.8-flash")) {
            model = OpenAiChatModel.builder()
                    .baseUrl("https://openrouter.ai/api/v1")
                    .apiKey(apiKey)
                    .modelName("google/gemini-3.8-flash")
                    .build();
        }

        if (modelName.equals("anthropic/claude-fable-5.1")) {
            model = OpenAiChatModel.builder()
                    .baseUrl("https://openrouter.ai/api/v1")
                    .apiKey(apiKey)
                    .modelName("anthropic/claude-fable-5.1")
                    .build();
        }
        if (modelName.equals("grok-4.6")) {
            model = OpenAiChatModel.builder()
                    .baseUrl("https://openrouter.ai/api/v1")
                    .apiKey(apiKey)
                    .modelName("x-ai/grok-4.6")
                    .build();
        }
        if (modelName.equals("qwen3.8-max")) {
            model = OpenAiChatModel.builder()
                    .baseUrl("https://openrouter.ai/api/v1")
                    .apiKey(apiKey)
                    .modelName("qwen/qwen3.8-max-0902")
                    .build();
        }
        if (modelName.equals("deepseek-v4-pro")) {
            model = OpenAiChatModel.builder()
                    .baseUrl("https://api.deepseek.com")
                    .apiKey(apiKey)
                    .modelName("deepseek-v4-pro")
                    .build();
        }else if (modelName.contains("ollama")) {

            ollamaModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                 .modelName(modelName.replace("ollama:", ""))
                 .timeout(Duration.ofMinutes(2))
                .build(); 
        }

        int numQueries = 30;
        String descriptions[] = new String[numQueries];
        String summaries[] = new String[numQueries];
        String qnos[] = new String[numQueries];

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();

            Document document = builder.parse(new File(clinicalCasesFile));

            document.getDocumentElement().normalize();

            NodeList list = document.getElementsByTagName("topic");

            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    qnos[i] = element.getAttribute("number");

                    descriptions[i] = element.getElementsByTagName("description").item(0).getTextContent();
                    summaries[i] = element.getElementsByTagName("summary").item(0).getTextContent();

                    // System.out.println("qno = " + qnos[i] + " " + descriptions[i]);
                }
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

            String prompt = "Given the following patient clinical case, identify the potential differential diagnoses.\t";
            prompt += "Provide the diagnoses as a ranked list, assigning each diagnosis a confidence score from 0 to 100 that reflects how strongly the findings in the clinical case support each diagnosis. Do not provide any explanation\n";
            prompt += "Clinical case:\n ";
            double numDiagnosis = 0;
            for (int i = 0; i < summaries.length; i++) {
                String response = null;
                if (isOllamaModel) {
                    response = ollamaModel.generate(prompt + summaries[i]);
                } else {
                    response = model.chat(prompt + summaries[i]);
                }
                System.out.println(summaries[i]);
                numDiagnosis += response.split("\n").length;
                for (String diagnosis : response.split("\n")) {
                    writer.write(qnos[i] + "," + diagnosis + "\n");
                }
                System.out.println("QNO = " + qnos[i] + "\n" + response + "\n");
            }

            writer.flush();
            writer.close();
            System.out.println(numDiagnosis / 30.0);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void processResult() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(
                    new FileReader("diagnosisFile.txt"));

            HashMap<String, Integer> qnoToOriginalDi = new HashMap<String, Integer>();
            HashMap<String, LinkedList<String>> qnoToDiagnostics = new HashMap<String, LinkedList<String>>();

            String line;
            while ((line = reader.readLine()) != null) {
                String splitArray[] = line.split(",");
                String qno = splitArray[0];
                String diagnosis = splitArray[1];
                if (qnoToOriginalDi.containsKey(qno)) {
                    qnoToOriginalDi.put(qno, qnoToOriginalDi.get(qno) + 1);
                    qnoToDiagnostics.get(qno).add(diagnosis);
                } else {
                    qnoToOriginalDi.put(qno, 1);
                    LinkedList<String> diagnoses = new LinkedList<String>();
                    diagnoses.add(diagnosis);
                    qnoToDiagnostics.put(qno, diagnoses);
                }
            }

            reader = new BufferedReader(new FileReader("output.txt"));
            HashMap<String, LinkedList<Item>> qnoToDi = new HashMap<String, LinkedList<Item>>();
            BufferedWriter writer = new BufferedWriter(new FileWriter("newOutputWithRank.txt"));

            while ((line = reader.readLine()) != null) {
                String splitArray[] = line.split(",");
                String qno = splitArray[0];
                String potentialDiagnosis = splitArray[1];
                LinkedList<String> originalDiList = qnoToDiagnostics.get(qno);
                for (int i = 0; i < originalDiList.size(); i++) {
                    if (originalDiList.get(i).toLowerCase().contains(potentialDiagnosis.toLowerCase())
                            || potentialDiagnosis.toLowerCase().contains(originalDiList.get(i).toLowerCase())) {
                        writer.write(line + ", DI" + (i + 1) + "\n");
                    } else {
                        writer.write(line + "\n");
                    }
                }
                writer.flush();
            }
            writer.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
