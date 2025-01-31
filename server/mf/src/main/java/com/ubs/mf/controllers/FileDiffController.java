package com.ubs.mf.controllers;

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

    private File saveMultipartFile(MultipartFile multipartFile) throws IOException {
        File file = File.createTempFile("upload_", "_" + multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        }
        return file;
    }
}
