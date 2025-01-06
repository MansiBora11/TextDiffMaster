package com.ubs.mf.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;




public class FileDifferenceService {


    public class FileDiffHighlighter {

        public static void DiffHighlighterSameFile(String txt1, String txt2) {
            try {
                // Read the file content
//                String txt1="C:\\Users\\Mansi\\Documents\\dummyFile1.txt";
//                String txt2="C:\\Users\\Mansi\\Documents\\dummyFile2.txt";
                String text1 = readFile(txt1);
                String text2 = readFile(txt2);

                // Initialize DiffMatchPatch
                DiffMatchPatch dmp = new DiffMatchPatch();

                // Generate differences
                LinkedList<DiffMatchPatch.Diff> diffs = dmp.diffMain(text1, text2);
                dmp.diffCleanupSemantic(diffs); // Clean up small semantic differences

                // Generate highlighted content
                String highlightedText = highlightDifferences(diffs);
                System.out.println(highlightedText);

                // Write back to the same file
                writeFile("file1.txt", highlightedText);

                System.out.println("File updated with highlighted differences.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static String readFile(String path) throws IOException {
            return new String(Files.readAllBytes(Paths.get(path)));
        }

        private static void writeFile(String path, String content) throws IOException {
            Files.write(Paths.get(path), content.getBytes());
        }

        private static String highlightDifferences(LinkedList<DiffMatchPatch.Diff> diffs) {
            StringBuilder html = new StringBuilder();
            html.append("<html><body><pre>");

            for (DiffMatchPatch.Diff diff : diffs) {
                switch (diff.operation) {
                    case INSERT:
                        html.append("<span style='background-color: #b2f0b2;'>").append(escapeHtml(diff.text)).append("</span>");
                        break;
                    case DELETE:
                        html.append("<span style='background-color: #f0b2b2; text-decoration: line-through;'>").append(escapeHtml(diff.text)).append("</span>");
                        break;
                    case EQUAL:
                        html.append(escapeHtml(diff.text));
                        break;
                }
            }

            html.append("</pre></body></html>");
            return html.toString();
}
//
//        public static void main(String[] args) {
//            DiffHighlighterSameFile();
//        }
    }
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">","&gt;");
    }
}


