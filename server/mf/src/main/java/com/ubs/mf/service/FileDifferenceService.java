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

        private static void compareTextFiles(String filePath1, String filePath2) throws IOException {
            String text1 = new String(Files.readAllBytes(Paths.get(filePath1)));
            String text2 = new String(Files.readAllBytes(Paths.get(filePath2)));

            DiffMatchPatch dmp = new DiffMatchPatch();
            LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(text1, text2);
            dmp.diffCleanupSemantic(diffs);

            String highlightedText = highlightDifferences(diffs);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("highlighted_diff.txt"))) {
                writer.write(highlightedText);
            }

            System.out.println("Differences have been saved to 'highlighted_diff.txt'");
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

        private static String highlightDifferences(LinkedList<DiffMatchPatch.Diff> diffs) {
            StringBuilder html = new StringBuilder();

            for (DiffMatchPatch.Diff diff : diffs) {
                switch (diff.operation) {
                    case INSERT:
                        html.append("<span style='background-color: #b2f0b2;'>").append(diff.text).append("</span>");
                        break;
                    case DELETE:
                        html.append("<span style='background-color: #f0b2b2; text-decoration: line-through;'>").append(diff.text).append("</span>");
                        break;
                    case EQUAL:
                        html.append(diff.text);
                        break;
                }
            }

            return html.toString();
        }
    }
}
