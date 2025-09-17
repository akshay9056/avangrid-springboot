package com.avangrid.gui.avangrid_backend.service;

import com.avangrid.gui.avangrid_backend.model.*;

import com.avangrid.gui.avangrid_backend.repository.RecordingsRepo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class TestService {

    private final RecordingsRepo recordingsRepo;

    public TestService(RecordingsRepo recordingsRepo) {
        this.recordingsRepo = recordingsRepo;
    }

    private static final String[] SAMPLE_FILES_WAV = {
            "audio/audioTwo.wav",
            "audio/audioThree.wav",
            "audio/audioFour.wav",
            "audio/audioSix.wav",
            "audio/audioSeven.wav"
    };

    private static final String[] SAMPLE_FILES_MP3 = {
            "audio/audioOne.mp3",
            "audio/audioFive.mp3",
            "audio/audioEight.mp3",
            "audio/audioNine.mp3"
    };

    private static final Random random = new Random();


    public VpiSearchResponse getMetadata( VpiSearchRequest request){

        Specification<Recording> spec = Specification.where(null);

        VpiSearchResponse response = new VpiSearchResponse();

        PaginationResponse pageResponse = new PaginationResponse();

        if (request.getFrom_date() != "")
        {
            LocalDateTime fromDate = parseDateTime(request.getFrom_date());
            LocalDateTime toDate = parseDateTime(request.getTo_date());
            validateDateRange(fromDate, toDate);
            spec = spec.and(RecordingSpecifications.dateBetween(
                    fromDate, toDate
            ));
        }

        int pageNumber = request.getPagination().getPageNumber();
        int pageSize = request.getPagination().getPageSize() > 0 ? request.getPagination().getPageSize() : 20;

        // Access filters
        VpiFiltersRequest filters = request.getFilters();

//        String fileName = filters.getFileName();
//        List<String> extensionNums = filters.getExtensionNum();
//        List<String> objectIDs = filters.getObjectID();
//        List<String> channelNums = filters.getChannelNum();
//        List<String> aniAliDigits = filters.getAniAliDigits();
//        List<String> names = filters.getName();


// 🔹 File name (substring search, case-insensitive)
        if (filters.getFileName() != null && !filters.getFileName().isEmpty()) {
            spec = spec.and(RecordingSpecifications.fileNameContainsAny(filters.getFileName()));
        }

        if (request.getOpco() != null && !request.getOpco().isEmpty()) {
            spec = spec.and(RecordingSpecifications.containsString("opco",request.getOpco()));
        }

// 🔹 Extension number (exact match from list)
        if (filters.getExtensionNum() != null && !filters.getExtensionNum().isEmpty()) {
            spec = spec.and(RecordingSpecifications.extensionNumContainsAny(filters.getExtensionNum()));
        }

// 🔹 Object ID (exact match from list)
        if (filters.getObjectID() != null && !filters.getObjectID().isEmpty()) {
            spec = spec.and(RecordingSpecifications.objectIdContainsAny(filters.getObjectID()));
        }

// 🔹 Channel number (exact match from list)
        if (filters.getChannelNum() != null && !filters.getChannelNum().isEmpty()) {
            spec = spec.and(RecordingSpecifications.channelNumContainsAny( filters.getChannelNum()));
        }

// 🔹 AniAliDigits (substring search, multiple)
        if (filters.getAniAliDigits() != null && !filters.getAniAliDigits().isEmpty()) {
            spec = spec.and(RecordingSpecifications.aniAliDigitsContainsAny( filters.getAniAliDigits()));
        }

// 🔹 Name (substring search, multiple)
        if (filters.getName() != null && !filters.getName().isEmpty()) {
            spec = spec.and(RecordingSpecifications.nameContainsAny(filters.getName()));
        }

// 🔹 Handle pagination safely
        int safePage = (pageNumber > 0 ? pageNumber - 1 : 0);
        int safeSize = (pageSize > 0 ? pageSize : 10);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("dateAdded").descending());

// 🔹 Fetch result
        Page<Recording> pageResult = recordingsRepo.findAll(spec, pageable);
        List<Recording> dbResult = recordingsRepo.findAll();

        // 🔹 Convert entities to List<Map<String,Object>>
        List<Map<String, Object>> records = new ArrayList<>();
        for (Recording rec : pageResult.getContent()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("fileName", rec.getFileName());
            map.put("extensionNum", rec.getExtensionNum());
            map.put("objectId", rec.getObjectId());
            map.put("channelNum", rec.getChannelNum());
            map.put("aniAliDigits", rec.getAniAliDigits());
            map.put("name", rec.getName());
            map.put("dateAdded", rec.getDateAdded());
            map.put("opco",rec.getOpco());
            map.put("agentID",rec.getAgentID());
            map.put("duration",rec.getDuration());
            map.put("direction",rec.getDirection());
            records.add(map);
        }

        // 🔹 Prepare response

        response.setData(records);
        response.setMessage("Success");
        response.setStatus("200");


        pageResponse.setPageNumber(pageResult.getNumber() + 1);
        pageResponse.setPageSize( pageResult.getSize());
        pageResponse.setTotalRecords(pageResult.getTotalElements());
        pageResponse.setTotalPages(pageResult.getTotalPages());

        response.setPagination(pageResponse);

        return response;




    }


    //function needed if any date formatting between frontend and backend
    private LocalDateTime parseDateTime(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
        }
    }

    private void validateDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        if (toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "`to_date` must be after `from_date`");
        }
    }

    public static ResponseEntity<?> testSampleRecording(RecordingRequest request) {
        String filename =  request.getFilename();

        if(Objects.equals(filename, "success")){
            try {



                String sampleFile = SAMPLE_FILES_MP3[random.nextInt(SAMPLE_FILES_MP3.length)];
                ClassPathResource audioFile = new ClassPathResource("audio/audioOne.mp3");
                // Load file from resources (src/main/resources/audio/sample.mp3)

                System.out.println("Picked " + sampleFile + ", exists=" + audioFile.exists());

                // Read as byte[]
                byte[] mp3Data = StreamUtils.copyToByteArray(audioFile.getInputStream());

                // Prepare headers
                String mp3Filename = "audioFile";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentLength(mp3Data.length);
                headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
                headers.setContentDispositionFormData("inline", mp3Filename);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(new ByteArrayResource(mp3Data));

            }
            catch (Exception e) {
                ErrorResponse error = new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        e.getMessage(),
                        LocalDateTime.now().toString()
                );

                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Testing Failure message to fetch Recording",
                    LocalDateTime.now().toString()
            );
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }


    }


    public Recording fetchsampleMetadata(RecordingRequest request) {
        List<Recording> recordingMetadata = recordingsRepo.findAllByOpcoAndFileName(request.getOpco(), request.getFilename());

        if (recordingMetadata.isEmpty()) {
            throw new IllegalArgumentException("No recording found for opco=" + request.getOpco() + " and fileName=" + request.getFilename());
        }

        return recordingMetadata.get(0);
    }

    public ResponseEntity<byte[]> prepareZip(List<RecordingRequest> requests) throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (RecordingRequest req : requests) {
                // Pick random static audio file
                String pickedFile = SAMPLE_FILES_WAV[random.nextInt(SAMPLE_FILES_WAV.length)];

                ClassPathResource resource = new ClassPathResource(pickedFile);

                zos.putNextEntry(new ZipEntry(req.getFilename())); // zip entry = DTO filename
                try (InputStream is = resource.getInputStream()) {
                    StreamUtils.copy(is, zos);
                }
                zos.closeEntry();

            }

            zos.finish();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "recordings.zip");

            return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace(); // log the actual error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Failed to generate ZIP: " + e.getMessage()).getBytes());
        }
    }



}
