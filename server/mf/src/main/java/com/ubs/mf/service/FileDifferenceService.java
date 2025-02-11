package com.ubs.mf.service;

import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class FileDifferenceService {

    public class FileDiffHighlighter {

        public static File highlightDifferences(File file1, File file2) {
            try {
                String outputFilePath;

                if (file1.getName().endsWith(".xlsx") && file2.getName().endsWith(".xlsx")) {
                    outputFilePath = compareExcelFiles(file1, file2);
                } else if (file1.getName().endsWith(".csv") && file2.getName().endsWith(".csv")) {
                    outputFilePath = compareCsvFiles(file1, file2);
                } else {
                    outputFilePath = compareTextFiles(file1, file2);
                }

                return new File(outputFilePath);

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private static String compareCsvFiles(File file1, File file2) throws IOException {
            File outputCsv = new File("highlighted_diff.csv");

            try (CSVReader reader1 = new CSVReader(new FileReader(file1));
                 CSVReader reader2 = new CSVReader(new FileReader(file2));
                 CSVWriter writer = new CSVWriter(new FileWriter(outputCsv))) {

                List<String[]> file1Content = reader1.readAll();
                List<String[]> file2Content = reader2.readAll();

                int maxRows = Math.max(file1Content.size(), file2Content.size());

                for (int i = 0; i < maxRows; i++) {
                    String[] row1 = i < file1Content.size() ? file1Content.get(i) : new String[0];
                    String[] row2 = i < file2Content.size() ? file2Content.get(i) : new String[0];

                    String[] diffRow = new String[Math.max(row1.length, row2.length)];
                    for (int j = 0; j < diffRow.length; j++) {
                        String cell1 = j < row1.length ? row1[j] : "";
                        String cell2 = j < row2.length ? row2[j] : "";

                        if (!cell1.equals(cell2)) {
                            diffRow[j] = "DIFF: " + cell1 + " -> " + cell2;
                        } else {
                            diffRow[j] = cell1;
                        }
                    }
                    writer.writeNext(diffRow);
                }
            } catch (CsvException e) {
                throw new RuntimeException(e);
            }

            return outputCsv.getAbsolutePath();
        }

        private static String compareExcelFiles(File file1, File file2) throws IOException {
            File outputExcel = new File("output/" + file1.getName().replace("_old.xlsx", "_diff.xlsx"));
            System.out.println("Saving Excel diff file to: " + outputExcel.getAbsolutePath());

            try (FileInputStream fis1 = new FileInputStream(file1);
                 FileInputStream fis2 = new FileInputStream(file2);
                 Workbook workbook1 = new XSSFWorkbook(fis1);
                 Workbook workbook2 = new XSSFWorkbook(fis2);
                 Workbook diffWorkbook = new XSSFWorkbook()) {

                Sheet sheet1 = workbook1.getSheetAt(0);
                Sheet sheet2 = workbook2.getSheetAt(0);
                Sheet diffSheet = diffWorkbook.createSheet("Differences");

                int maxRows = Math.max(sheet1.getLastRowNum(), sheet2.getLastRowNum());

                for (int i = 0; i <= maxRows; i++) {
                    Row row1 = sheet1.getRow(i);
                    Row row2 = sheet2.getRow(i);
                    Row diffRow = diffSheet.createRow(i);

                    int maxCols = Math.max(row1 != null ? row1.getLastCellNum() : 0,
                            row2 != null ? row2.getLastCellNum() : 0);

                    for (int j = 0; j < maxCols; j++) {
                        Cell cell1 = row1 != null ? row1.getCell(j) : null;
                        Cell cell2 = row2 != null ? row2.getCell(j) : null;
                        Cell diffCell = diffRow.createCell(j);

                        String value1 = cell1 != null ? cell1.toString() : "";
                        String value2 = cell2 != null ? cell2.toString() : "";

                        if (!value1.equals(value2)) {
                            diffCell.setCellValue("DIFF: " + value1 + " -> " + value2);
                            CellStyle style = diffWorkbook.createCellStyle();
                            style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                            diffCell.setCellStyle(style);
                        } else {
                            diffCell.setCellValue(value1);
                        }
                    }
                }
                Files.createDirectories(outputExcel.toPath().getParent());
                try (FileOutputStream fos = new FileOutputStream(outputExcel)) {
                    diffWorkbook.write(fos);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return outputExcel.getAbsolutePath();
        }

        private static String compareTextFiles(File file1, File file2) throws IOException {
            List<String> lines1 = Files.readAllLines(file1.toPath());
            List<String> lines2 = Files.readAllLines(file2.toPath());

            DiffMatchPatch dmp = new DiffMatchPatch();
            StringBuilder htmlOutput = new StringBuilder();

            // Generate HTML header
            htmlOutput.append("<!DOCTYPE html>")
                    .append("<html><head>")
                    .append("<meta charset='UTF-8'>")
                    .append("<style>")
                    .append("body { font-family: Arial, sans-serif; margin: 20px; }")
                    .append("table { border-collapse: collapse; width: 100%; }")
                    .append("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; vertical-align: top; }")
                    .append("th { background-color: #f2f2f2; }")
                    .append(".added { background-color: #b2f0b2; }")
                    .append(".deleted { background-color: #f0b2b2; }")
                    .append("</style></head><body>")
                    .append("<h2>Text File Comparison</h2>")
                    .append("<table><thead>")
                    .append("<tr><th>Original File</th><th>Modified File</th></tr>")
                    .append("</thead><tbody>");

            // Compare files line by line
            int i = 0, j = 0;
            while (i < lines1.size() || j < lines2.size()) {
                String line1 = i < lines1.size() ? lines1.get(i) : "";
                String line2 = j < lines2.size() ? lines2.get(j) : "";

//                // Check for lines that exist only in file1
//                if (i < lines1.size() && !lines1.get(i).trim().isEmpty()) {
//                    boolean foundMatch = false;
//                    // Look ahead in file2 to see if this line appears later
//                    for (int k = j; k < lines2.size(); k++) {
//                        if (isSimilarEnough(lines1.get(i), lines2.get(k))) {
//                            foundMatch = true;
//                            break;
//                        }
//                    }
//
//                    if (!foundMatch) {
//                        // If no match found, this line exists only in file1
//                        appendRow(htmlOutput, "<span class='deleted'>" + escapeHtml(line1) + "</span>", "&nbsp;");
//                        i++;
//                        continue;
//                    }
//                }
//
                // First check for lines that exist only in the file2
                if (j < lines2.size() && !lines2.get(j).trim().isEmpty()) {
                    boolean foundMatch = false;
                    // Look ahead in file1 to see if this line appears later
                    for (int k = i; k < lines1.size(); k++) {
                        if (isSimilarEnough(lines1.get(k), lines2.get(j))) {
                            foundMatch = true;
                            break;
                        }
                    }

                    if (!foundMatch) {
                        // If no match found, this line exists only in file2
                        appendRow(htmlOutput, "&nbsp;", "<span class='added'>" + escapeHtml(line2) + "</span>");
                        j++;
                        continue;
                    }
                }
                // Check if current line in file2 has a better match earlier in file1
                if (j < lines2.size()) {
                    int bestMatchIndex = -1;
                    for (int k = i; k < lines1.size(); k++) {
                        if (isSimilarEnough(lines1.get(k), lines2.get(j))) {
                            bestMatchIndex = k;
                            break;
                        }
                    }

                    if (bestMatchIndex != -1 && bestMatchIndex != i) {
                        // Found a better match, so handle lines before that match as file1-only lines
                        while (i < bestMatchIndex) {
                            appendRow(htmlOutput, "<span class='deleted'>" + escapeHtml(lines1.get(i)) + "</span>", "&nbsp;");
                            i++;
                        }
                        line1 = lines1.get(i);
                    }
                }


                // Handle empty cases first
                if (line1.isEmpty() && !line2.isEmpty()) {
                    appendRow(htmlOutput, "&nbsp;",
                            "<span class='added'>" + escapeHtml(line2) + "</span>");
                    j++;
                    continue;
                }
                if (!line1.isEmpty() && line2.isEmpty()) {
                    appendRow(htmlOutput,
                            "<span class='deleted'>" + escapeHtml(line1) + "</span>",
                            "&nbsp;");
                    i++;
                    continue;
                }

                // Skip identical lines
                if (line1.equals(line2)) {
                    appendRow(htmlOutput, escapeHtml(line1), escapeHtml(line2));
                    i++;
                    j++;
                    continue;

                }

                // Compare differing lines
                if (isSimilarEnough(line1, line2)) {
                    LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(line1, line2);
                    dmp.diffCleanupSemantic(diffs);
                    appendRow(htmlOutput,
                            highlightDifferences(diffs, DiffMatchPatch.Operation.DELETE),
                            highlightDifferences(diffs, DiffMatchPatch.Operation.INSERT));
                    i++;
                    j++;
                }
                else {
                    appendRow(htmlOutput,
                            "<span class='deleted'>" + escapeHtml(line1) + "</span>",
                            "<span class='added'>" + escapeHtml(line2) + "</span>");
                    i++;
                    j++;
                }
//                else {
//                    // Lines are too different - show with empty cells
//                    // If current lines aren't similar enough, move forward in file1
//                    appendRow(htmlOutput,
//                            "<span class='deleted'>" + escapeHtml(line1) + "</span>",
//                            "&nbsp;");
//                    i++;
//                }
            }

            htmlOutput.append("</tbody></table></body></html>");

            // Create output file in system temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            String outputFileName = "text_diff_" + System.currentTimeMillis() + ".html";
            String outputFilePath = tempDir + File.separator + outputFileName;

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write(htmlOutput.toString());
            }

            return outputFilePath;
        }

        private static void appendRow(StringBuilder htmlOutput, String col1, String col2) {
            htmlOutput.append("<tr>")
                    .append("<td>").append(col1).append("</td>")
                    .append("<td>").append(col2).append("</td>")
                    .append("</tr>");
        }

        private static boolean isSimilarEnough(String str1, String str2) {
            if (str1 == null || str2 == null) {
                return false;
            }

            // Split strings into words and convert to lowercase for comparison
            Set<String> words1 = new HashSet<>(Arrays.asList(str1.toLowerCase().split("\\s+")));
            Set<String> words2 = new HashSet<>(Arrays.asList(str2.toLowerCase().split("\\s+")));

            // Find the intersection of words
            Set<String> intersection = new HashSet<>(words1);
            intersection.retainAll(words2);

            // Calculate similarity threshold (60% of the smaller set size)
            int threshold = (int) Math.ceil(Math.min(words1.size(), words2.size()) * 0.6);

            return intersection.size() >= threshold;
        }

        private static String escapeHtml(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }

        private static String highlightDifferences(LinkedList<DiffMatchPatch.Diff> diffs,
                                                   DiffMatchPatch.Operation targetOperation) {
            StringBuilder result = new StringBuilder();

            for (DiffMatchPatch.Diff diff : diffs) {
                if (diff.operation == targetOperation) {
                    String cssClass = targetOperation == DiffMatchPatch.Operation.DELETE ? "deleted" : "added";
                    result.append("<span class='").append(cssClass).append("'>")
                            .append(escapeHtml(diff.text)).append("</span>");
                } else if (diff.operation == DiffMatchPatch.Operation.EQUAL) {
                    result.append(escapeHtml(diff.text));
                }
            }

            return result.toString();
        }
    }
}
