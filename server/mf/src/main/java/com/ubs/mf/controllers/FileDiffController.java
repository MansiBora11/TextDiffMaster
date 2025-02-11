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
            File tempFile1 = saveMultipartFile(file1);
            File tempFile2 = saveMultipartFile(file2);

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
                    String fileType = getFileExtension(file1);

                    System.out.println(" Comparing: " + file1.getName() + " with " + file2.getName());
                    System.out.println(" Processing file type: " + fileType);

                    // Call file comparison method
                    File outputFile = FileDifferenceService.FileDiffHighlighter.highlightDifferences(file1, file2);

                    if (outputFile == null || !outputFile.exists()) {
                        System.out.println("Comparison failed for: " + baseName + fileType);
                        continue;
                    }

                    // Save the output file
                    String outputFileName = baseName + "_diff" + getFileExtension(outputFile);
                    Path outputPath = Paths.get("output/", outputFileName);
                    Files.createDirectories(outputPath.getParent());
                    Files.move(outputFile.toPath(), outputPath, StandardCopyOption.REPLACE_EXISTING);

                    String fileLink = "http://localhost:8080/downloads/" + outputFileName;
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

            if (originalName != null && (originalName.endsWith(suffix + ".xlsx") ||
                    originalName.endsWith(suffix + ".txt") ||
                    originalName.endsWith(suffix + ".csv"))) {
                // Remove path and keep only the filename
                String baseName = Paths.get(originalName).getFileName().toString()
                        .replace(suffix + ".xlsx", "")
                        .replace(suffix + ".txt", "")
                        .replace(suffix + ".csv", "");

                File file = saveMultipartFile(multipartFile);
                fileMap.put(baseName, file);
                System.out.println(" Saved file: " + file.getAbsolutePath());
            } else {
                System.out.println(" Skipping file: " + originalName);
            }
        }
        return fileMap;
    }



    private File saveMultipartFile(MultipartFile multipartFile) throws IOException {
        // Extract the original filename (excluding any folder structure)
        String originalName = Paths.get(multipartFile.getOriginalFilename()).getFileName().toString();

        if (originalName == null || originalName.isEmpty()) {
            throw new IOException("Invalid file name.");
        }

        // Create a temp file safely
        File file = File.createTempFile("upload_", "_" + originalName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }

        System.out.println("Saved file: " + file.getAbsolutePath()); // Debugging log
        return file;
    }


    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf(".");
        return (lastIndex > 0) ? name.substring(lastIndex) : "";
    }


}
