package ru.dimov;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static final Path OUTPUT_PATH = Paths.get("output.txt");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Ошибка: Не указан путь к входному файлу.");
            System.err.println("Использование: java -jar line-grouper.jar <путь_к_файлу>");
            return;
        }

        try {
            Path inputPath = Paths.get(args[0]);
            if (!Files.exists(inputPath) || !Files.isReadable(inputPath)) {
                System.err.println("Ошибка: Файл не существует или не может быть прочитан: " + inputPath);
                return;
            }

            processFile(inputPath);

        } catch (InvalidPathException e) {
            System.err.println("Ошибка: Некорректный путь файла: " + args[0]);
        } catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Главный метод, управляющий процессом обработки файла.
     */
    private static void processFile(Path inputPath) throws IOException {
        long startTime = System.currentTimeMillis();
        System.out.println("Начало обработки файла: " + inputPath);

        Map<List<String>, String> parsedToRawLineMap = readAndParseUniqueLines(inputPath);

        if (parsedToRawLineMap.isEmpty()) {
            System.out.println("Входной файл не содержит корректных строк для обработки.");
            writeGroupsToFile(Collections.emptyList(), OUTPUT_PATH);
            return;
        }

        List<List<String>> uniqueLines = new ArrayList<>(parsedToRawLineMap.keySet());

        List<List<String>> groups = groupLines(uniqueLines, parsedToRawLineMap);

        groups.sort((g1, g2) -> Integer.compare(g2.size(), g1.size()));

        writeGroupsToFile(groups, OUTPUT_PATH);

        long endTime = System.currentTimeMillis();
        System.out.println("Обработка завершена. Результат в файле " + OUTPUT_PATH);
        System.out.printf("Затраченное время в секундах: %.2f \n", (endTime - startTime) / 1000.0);
    }

    /**
     * Метод отвечающий за чтение текстового файла и парсинг строк.
     * Возвращает карту, где ключ - разобранная строка, значение - исходная строка.
     */
    private static Map<List<String>, String> readAndParseUniqueLines(Path inputPath) throws IOException {
        Map<List<String>, String> parsedToRawLineMap = new LinkedHashMap<>();

        try (Stream<String> lines = Files.lines(inputPath, StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                if (line.trim().isEmpty() || isLineStructurallyInvalid(line)) {
                    return;
                }


                String[] parts = line.split(";", -1);

                List<String> parsed = Arrays.stream(parts)
                        .map(Main::unquote) // Применяем новый метод unquote
                        .toList();
                parsedToRawLineMap.putIfAbsent(parsed, line);
                /*
                List<String> parsed = Arrays.asList(parts);
                 */
            });
        }
        return parsedToRawLineMap;
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
    private static List<List<String>> groupLines(List<List<String>> uniqueLines, Map<List<String>, String> parsedToRawLineMap) {
        int n = uniqueLines.size();
        DisjointSetUnion dsu = new DisjointSetUnion(n);
        Map<String, Integer> valuePositionToLineIndex = new HashMap<>();

        for (int i = 0; i < n; i++) {
            List<String> lineParts = uniqueLines.get(i);
            for (int j = 0; j < lineParts.size(); j++) {
                String value = lineParts.get(j);
                if (value != null && !value.isEmpty()) {
                    String key = j + ":" + value;
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
            String rawLine = parsedToRawLineMap.get(uniqueLines.get(i));
            resultGroups.computeIfAbsent(root, k -> new ArrayList<>()).add(rawLine);
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