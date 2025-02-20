package com.ubs.mf.controllers;

import com.ubs.mf.service.FileDifferenceService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.ubs.mf.service.FileDifferenceService.FileDiffHighlighter.highlightDifferences;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class FileDiffController {

    @PostMapping(value = "/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> compareFiles(@RequestParam("file1") MultipartFile file1,
                                                 @RequestParam("file2") MultipartFile file2) {
        try {
            // Save uploaded files locally
            File tempFile1 = saveMultipartFile(file1, getFileExtension(file1.getOriginalFilename()));
            File tempFile2 = saveMultipartFile(file2, getFileExtension(file2.getOriginalFilename()));

            // Perform comparison and generate output file
            File outputFile = highlightDifferences(tempFile1, tempFile2);

            // Convert file to a Resource
            Path outputPath = outputFile.toPath();
            Resource resource = new UrlResource(outputPath.toUri());

            // Prepare response
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFile.getName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping(value = "/compareFolders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> compareFolders(
            @RequestParam("folder1Files") List<MultipartFile> folder1Files,
            @RequestParam("folder2Files") List<MultipartFile> folder2Files) {
        try {
            System.out.println("Received " + folder1Files.size() + " files in Folder 1:");
            for (MultipartFile file : folder1Files) {
                System.out.println("   - " + file.getOriginalFilename());
            }

            System.out.println("Received " + folder2Files.size() + " files in Folder 2:");
            for (MultipartFile file : folder2Files) {
                System.out.println("   - " + file.getOriginalFilename());
            }

            if (folder1Files.isEmpty() || folder2Files.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonList("Both folders must contain files."));
            }

            // Save and match files
            Map<String, File> folder1Map = saveFiles(folder1Files, "_old");
            Map<String, File> folder2Map = saveFiles(folder2Files, "_new");

            System.out.println("Folder 1 processed: " + folder1Map.keySet());
            System.out.println("Folder 2 processed: " + folder2Map.keySet());

            List<String> diffFileLinks = new ArrayList<>();

            for (String baseName : folder1Map.keySet()) {
                if (folder2Map.containsKey(baseName)) {
                    File file1 = folder1Map.get(baseName);
                    File file2 = folder2Map.get(baseName);

                    System.out.println("Comparing: " + file1.getName() + " with " + file2.getName());

                    File outputFile = FileDifferenceService.FileDiffHighlighter.highlightDifferences(file1, file2);

                    if (outputFile == null || !outputFile.exists()) {
                        System.out.println("Comparison failed for: " + baseName);
                        continue;
                    }

                    // Generate output with .html extension for non-spreadsheet files
                    String fileType = getFileExtension(file1.getName());
                    String outputFileName = baseName + (fileType.equals(".xlsx") || fileType.equals(".csv") ? "_diff" + fileType : "_diff.html");
                    Path outputPath = Paths.get("output/", outputFileName);
                    Files.createDirectories(outputPath.getParent());
                    Files.move(outputFile.toPath(), outputPath, StandardCopyOption.REPLACE_EXISTING);

                    String fileLink = "https://textdiffmaster.onrender.com/downloads/" + outputFileName;
                    diffFileLinks.add(fileLink);
                    System.out.println("Generated diff file: " + fileLink);
                } else {
                    System.out.println("No matching file for: " + baseName + "_old in Folder 2");
                }
            }

            if (diffFileLinks.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonList("No differences found."));
            }

            return ResponseEntity.ok(diffFileLinks);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Collections.singletonList("An error occurred during file comparison."));
        }
    }

    private Map<String, File> saveFiles(List<MultipartFile> files, String suffix) throws IOException {
        Map<String, File> fileMap = new HashMap<>();
        for (MultipartFile multipartFile : files) {
            String originalName = multipartFile.getOriginalFilename();

            if (originalName != null) {
                String baseName = Paths.get(originalName).getFileName().toString().replace(suffix, "").replaceAll("\\..*", "");
                String extension = getFileExtension(originalName);

                // Treat non-xlsx and non-csv files as txt
                String tempSuffix = (extension.equals(".xlsx") || extension.equals(".csv")) ? extension : ".txt";

                File file = saveMultipartFile(multipartFile, tempSuffix);
                fileMap.put(baseName, file);
                System.out.println("Saved file: " + file.getAbsolutePath());
            } else {
                System.out.println("Skipping file: " + originalName);
            }
        }
        return fileMap;
    }

    private File saveMultipartFile(MultipartFile multipartFile, String tempSuffix) throws IOException {
        File file = File.createTempFile("upload_", tempSuffix);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        System.out.println("Saved file as: " + file.getAbsolutePath());
        return file;
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf(".");
        return (lastIndex > 0) ? fileName.substring(lastIndex) : "";
    }





}
