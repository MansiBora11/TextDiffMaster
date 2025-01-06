package com.ubs.mf.controllers;

import com.ubs.mf.dao.FilePaths;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.ubs.mf.service.FileDifferenceService.FileDiffHighlighter.DiffHighlighterSameFile;

@RestController
    public class FileDiffController {

    @PostMapping("/getDiff")
    public ResponseEntity<String> getDifferences(@RequestBody FilePaths filepath){
        DiffHighlighterSameFile(filepath.getFilePath1(),filepath.getFilePath2());
        return ResponseEntity.ok("Diff operation completed successfully");

    }
}


