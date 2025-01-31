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
            File outputExcel = new File("highlighted_diff.xlsx");

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

                try (FileOutputStream fos = new FileOutputStream(outputExcel)) {
                    diffWorkbook.write(fos);
                }
            }

            return outputExcel.getAbsolutePath();
        }

        private static String compareTextFiles(File file1, File file2) throws IOException {
            List<String> lines1 = Files.readAllLines(file1.toPath());
            List<String> lines2 = Files.readAllLines(file2.toPath());

            DiffMatchPatch dmp = new DiffMatchPatch();
            StringBuilder htmlOutput = new StringBuilder();

            htmlOutput.append("<!DOCTYPE html><html><head><style>")
                    .append("body { font-family: Arial, sans-serif; }")
                    .append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }")
                    .append("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; vertical-align: top; }")
                    .append("th { background-color: #f2f2f2; }")
                    .append(".added { background-color: #b2f0b2; }")
                    .append(".deleted { background-color: #f0b2b2; }")
                    .append("</style></head><body><h2>File Comparison</h2><table>")
                    .append("<thead><tr><th>File 1</th><th>File 2</th></tr></thead><tbody>");

            for (int i = 0; i < Math.max(lines1.size(), lines2.size()); i++) {
                String line1 = i < lines1.size() ? lines1.get(i) : "";
                String line2 = i < lines2.size() ? lines2.get(i) : "";

                if (!line1.equals(line2)) {
                    htmlOutput.append("<tr><td class='deleted'>").append(escapeHtml(line1))
                            .append("</td><td class='added'>").append(escapeHtml(line2)).append("</td></tr>");
                } else {
                    htmlOutput.append("<tr><td>").append(escapeHtml(line1)).append("</td><td>")
                            .append(escapeHtml(line2)).append("</td></tr>");
                }
            }

            htmlOutput.append("</tbody></table></body></html>");

            File outputFile = new File("highlighted_diff.html");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write(htmlOutput.toString());
            }

            return outputFile.getAbsolutePath();
        }

        private static String escapeHtml(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
