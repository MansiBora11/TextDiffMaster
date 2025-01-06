package com.ubs.mf.controllers;

import com.ubs.mf.dao.FilePaths;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import static com.ubs.mf.service.FileDifferenceService.FileDiffHighlighter.highlightDifferences;

@RestController
//@RequestMapping("/file-diff")
public class FileDiffController {

    @PostMapping("/compare")
    public ResponseEntity<String> compareFiles(@RequestBody FilePaths filePaths) {
        try {
            highlightDifferences(filePaths.getFilePath1(), filePaths.getFilePath2());
            return ResponseEntity.ok("File comparison completed successfully. Check the generated difference file.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred during file comparison: " + e.getMessage());
        }
    }
}
