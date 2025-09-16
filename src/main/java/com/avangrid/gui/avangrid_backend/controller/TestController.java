package com.avangrid.gui.avangrid_backend.controller;




import com.avangrid.gui.avangrid_backend.model.Recording;
import com.avangrid.gui.avangrid_backend.model.RecordingRequest;
import com.avangrid.gui.avangrid_backend.model.VpiSearchRequest;
import com.avangrid.gui.avangrid_backend.repository.RecordingsRepo;
import com.avangrid.gui.avangrid_backend.model.VpiSearchResponse;

import com.avangrid.gui.avangrid_backend.service.TestService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class TestController {

    private final RecordingsRepo repository;

    private final TestService service;

    public TestController(RecordingsRepo repository, TestService service) {

        this.repository = repository;
        this.service = service;
    }



    @GetMapping("/recordings")
    public List<Recording> getAllRecordings() {
        return repository.findAll();
    }

    @PostMapping("/fetch-metadata")
    public ResponseEntity<VpiSearchResponse> getAllRecordings(@RequestBody VpiSearchRequest request) {
        VpiSearchResponse response = service.getMetadata(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/recording")
    public ResponseEntity<?> getRecording(
            @RequestBody RecordingRequest request) {

        return service.testSampleRecording(request);
    }

    @PostMapping("/recording-metadata")
    public Recording getMetadata(@RequestBody RecordingRequest request){

        Recording response = service.fetchsampleMetadata(request);

        return response;


    }





}
