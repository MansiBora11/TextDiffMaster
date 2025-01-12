package com.ubs.mf.service;

import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class FileDifferenceService {

    public class FileDiffHighlighter {

        public static void highlightDifferences(String filePath1, String filePath2) {
            try {
                if (filePath1.endsWith(".xlsx") && filePath2.endsWith(".xlsx")) {
                    compareExcelFiles(filePath1, filePath2);
                } else if (filePath1.endsWith(".csv") && filePath2.endsWith(".csv")) {
                    compareCsvFiles(filePath1, filePath2);
                } else {
                    compareTextFiles(filePath1, filePath2);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }





        private static void compareCsvFiles(String filePath1, String filePath2) throws IOException {
            try (CSVReader reader1 = new CSVReader(new FileReader(filePath1));
                 CSVReader reader2 = new CSVReader(new FileReader(filePath2));
                 CSVWriter writer = new CSVWriter(new FileWriter("highlighted_diff.csv"))) {

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
        }

        private static void compareExcelFiles(String filePath1, String filePath2) throws IOException {
            try (FileInputStream fis1 = new FileInputStream(filePath1);
                 FileInputStream fis2 = new FileInputStream(filePath2);
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

                try (FileOutputStream fos = new FileOutputStream("highlighted_diff.xlsx")) {
                    diffWorkbook.write(fos);
                }
            }
        }

        private static void compareTextFiles(String filePath1, String filePath2) throws IOException {
            List<String> lines1 = Files.readAllLines(Paths.get(filePath1));
            List<String> lines2 = Files.readAllLines(Paths.get(filePath2));

            DiffMatchPatch dmp = new DiffMatchPatch();
            StringBuilder htmlOutput = new StringBuilder();

            htmlOutput.append("<!DOCTYPE html>")
                    .append("<html>")
                    .append("<head>")
                    .append("<style>")
                    .append("body { font-family: Arial, sans-serif; }")
                    .append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }")
                    .append("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; vertical-align: top; }")
                    .append("th { background-color: #f2f2f2; }")
                    .append(".added { background-color: #b2f0b2; }") // Green for additions
                    .append(".deleted { background-color: #f0b2b2; }") // Red for deletions
                    .append("</style>")
                    .append("</head>")
                    .append("<body>")
                    .append("<h2>File Comparison</h2>")
                    .append("<table>")
                    .append("<thead><tr><th>File 1 (Deleted Content)</th><th>File 2 (Added Content)</th></tr></thead>")
                    .append("<tbody>");

            // Two-pointer approach for line-by-line comparison
            int i = 0, j = 0;
            while (i < lines1.size() || j < lines2.size()) {
                String line1 = i < lines1.size() ? lines1.get(i) : "";
                String line2 = j < lines2.size() ? lines2.get(j) : "";

                String highlightedLine1 = "";
                String highlightedLine2 = "";

                if (line1.isEmpty() && !line2.isEmpty()) {
                    // Line exists only in File 2
                    highlightedLine1 = "&nbsp;"; // Leave File 1 column empty
                    highlightedLine2 = "<span class='added'>" + escapeHtml(line2) + "</span>";
                    j++;
                } else if (!line1.isEmpty() && line2.isEmpty()) {
                    // Line exists only in File 1
                    highlightedLine1 = "<span class='deleted'>" + escapeHtml(line1) + "</span>";
                    highlightedLine2 = "&nbsp;"; // Leave File 2 column empty
                    i++;
                } else if (!line1.equals(line2)) {
                    // Lines exist in both files but differ, highlight differences
                    LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(line1, line2);
                    dmp.diffCleanupSemantic(diffs);

                    highlightedLine1 = highlightDifferences(diffs, DiffMatchPatch.Operation.DELETE);
                    highlightedLine2 = highlightDifferences(diffs, DiffMatchPatch.Operation.INSERT);
                    i++;
                    j++;
                } else {
                    // Skip identical lines
                    i++;
                    j++;
                    continue;
                }

                // Append the rows with highlighted differences or exclusive lines
                htmlOutput.append("<tr>")
                        .append("<td>").append(highlightedLine1).append("</td>")
                        .append("<td>").append(highlightedLine2).append("</td>")
                        .append("</tr>");
            }

            htmlOutput.append("</tbody>")
                    .append("</table>")
                    .append("</body>")
                    .append("</html>");

            // Write the output to an HTML file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("highlighted_diff.html"))) {
                writer.write(htmlOutput.toString());
            }

            System.out.println("Differences have been saved to 'highlighted_diff.html'");
        }

        private static String highlightDifferences(LinkedList<DiffMatchPatch.Diff> diffs, DiffMatchPatch.Operation targetOperation) {
            StringBuilder result = new StringBuilder();

            for (DiffMatchPatch.Diff diff : diffs) {
                if (diff.operation == targetOperation) {
                    String cssClass = targetOperation == DiffMatchPatch.Operation.DELETE ? "deleted" : "added";
                    result.append("<span class='").append(cssClass).append("'>").append(escapeHtml(diff.text)).append("</span>");
                } else if (diff.operation == DiffMatchPatch.Operation.EQUAL) {
                    result.append(escapeHtml(diff.text));
                }
            }

            return result.toString();
        }

        private static String escapeHtml(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
