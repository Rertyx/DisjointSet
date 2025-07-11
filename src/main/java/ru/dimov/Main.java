package ru.dimov;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private static final Path OUTPUT_PATH = Paths.get("output.txt");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Error: Input file path not specified.");
            System.err.println("Usage: java -jar line-grouper.jar <file_path>");
            return;
        }

        try {
            Path inputPath = Paths.get(args[0]);
            if (!Files.exists(inputPath) || !Files.isReadable(inputPath)) {
                System.err.println("Error: File does not exist or cannot be read: " + inputPath);
                return;
            }

            processFile(inputPath);

        } catch (InvalidPathException e) {
            System.err.println("Error: Invalid file path: " + args[0]);
        } catch (Exception e) {
            System.err.println("An unexpected error has occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Главный метод, управляющий процессом обработки файла.
     */
    private static void processFile(Path inputPath) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.println("Starting file processing: " + inputPath);

        List<String> uniqueLines = readUniqueLines(inputPath);

        if (uniqueLines.isEmpty()) {
            System.out.println("The input file does not contain any valid lines for processing.");
            writeGroupsToFile(Collections.emptyList(), OUTPUT_PATH);
            return;
        }


        List<List<String>> groups = groupLines(uniqueLines);

        groups.sort((g1, g2) -> Integer.compare(g2.size(), g1.size()));

        writeGroupsToFile(groups, OUTPUT_PATH);

        long endTime = System.currentTimeMillis();
        System.out.println("Processing completed. The result is in the file " + OUTPUT_PATH);
        System.out.printf("Time spent (in seconds): %.2f \n", (endTime - startTime) / 1000.0);
    }

    /**
     * Метод отвечающий за чтение текстового файла.
     * Возвращает найденные в файле уникальные строки.
     */
    private static List<String> readUniqueLines(Path inputPath) throws IOException {
        Set<String> seenLines = new HashSet<>();
        List<String> uniqueLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || isLineStructurallyInvalid(line)) {
                    continue;
                }
                if (seenLines.add(line)) {
                    uniqueLines.add(line);
                }
            }
        }
        return uniqueLines;
    }

    private static String unquote(String s) {
        if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static boolean isLineStructurallyInvalid(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '"') {

                boolean isOpenQuote = (i == 0) || (line.charAt(i - 1) == ';');
                if (isOpenQuote) {
                    continue;
                }

                boolean isCloseQuote = (i == line.length() - 1) || (line.charAt(i + 1) == ';');
                if (isCloseQuote) {
                    continue;
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Метод отвечающий за группировку строк на основе общих непустых значений в колонках.
     * Использует алгоритм Disjoint Set Union (DSU).
     */
    private static List<List<String>> groupLines(List<String> uniqueLines) {
        int n = uniqueLines.size();
        DisjointSetUnion dsu = new DisjointSetUnion(n);
        Map<String, Integer> valuePositionToLineIndex = new HashMap<>();
        StringBuilder keyBuilder = new StringBuilder();

        for (int i = 0; i < n; i++) {
            String line = uniqueLines.get(i);
            String[] parts = line.split(";", -1);
            for (int j = 0; j < parts.length; j++) {
                String value = unquote(parts[j]);
                if (value != null && !value.isEmpty()) {
                    keyBuilder.setLength(0);
                    String key = keyBuilder.append(j).append(":").append(value).toString();
                    if (valuePositionToLineIndex.containsKey(key)) {
                        dsu.union(i, valuePositionToLineIndex.get(key));
                    } else {
                        valuePositionToLineIndex.put(key, i);
                    }
                }
            }
        }

        Map<Integer, List<String>> resultGroups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = dsu.find(i);
            resultGroups.computeIfAbsent(root, k -> new ArrayList<>()).add(uniqueLines.get(i));
        }
        return new ArrayList<>(resultGroups.values());
    }

    /**
     * Метод отвечающий за запись итоговых групп в выходной файл.
     */
    private static void writeGroupsToFile(List<List<String>> groups, Path outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            long groupsWithMoreThanOneElement = groups.stream().filter(g -> g.size() > 1).count();
            writer.write("Число групп более чем с одним элементом: " + groupsWithMoreThanOneElement);
            writer.newLine();
            writer.newLine();

            int groupNumber = 1;
            for (List<String> group : groups) {
                writer.write("Группа " + groupNumber++);
                writer.newLine();
                for (String line : group) {
                    writer.write(line);
                    writer.newLine();
                }
                writer.newLine();
            }
        }
    }
}