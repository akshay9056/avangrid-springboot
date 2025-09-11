package com.avangrid.gui.avangrid_backend.service;

import com.avangrid.gui.avangrid_backend.model.TalkdeskMetadataRequest;
import com.avangrid.gui.avangrid_backend.model.TalkdeskMetadataResponse;
import com.avangrid.gui.avangrid_backend.repository.AzureTalkdeskRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TalkdeskService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private final AzureTalkdeskRepo talkdeskRepo;

    public TalkdeskMetadataResponse getTalkdeskMetadata(TalkdeskMetadataRequest request) {
        log.info("Processing Talkdesk metadata request for date range: {} to {}",
                request.getStartDate(), request.getEndDate());

        try {
            // Validate date formats
            LocalDateTime startDateTime = validateAndParseDate(request.getStartDate(), "start_date");
            LocalDateTime endDateTime = validateAndParseDate(request.getEndDate(), "end_date");

            // Validate date range
            validateDateRange(startDateTime, endDateTime);

            // Build query with filters
            String query = buildQuery(request);
            log.debug("Generated query: {}", query);

            // Fetch metadata with pagination
            List<Map<String, Object>> metadataList = talkdeskRepo
                    .fetchBlobMetadata(query, request.getPageSize(), request.getContinuationToken());

            // Get continuation token for next page
            String nextToken = talkdeskRepo.getNextContinuationToken();

            log.info("Successfully retrieved {} metadata entries", metadataList.size());

            return TalkdeskMetadataResponse.builder()
                    .data(metadataList)
                    .continuationToken(nextToken)
                    .totalCount(metadataList.size())
                    .build();

        } catch (DateTimeParseException e) {
            log.error("Invalid date format in request: {}", e.getMessage());
            throw new RuntimeException("Invalid date format. Expected format: yyyy-MM-dd HH:mm:ss");
        } catch (Exception e) {
            log.error("Error processing Talkdesk metadata request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Talkdesk metadata: " + e.getMessage());
        }
    }

    private LocalDateTime validateAndParseDate(String dateStr, String fieldName) {
        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date format for " + fieldName + ": " + dateStr);
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("End date must be after start date");
        }
    }

    private String buildQuery(TalkdeskMetadataRequest request) {
        StringBuilder queryBuilder = new StringBuilder();

        // Base date range query
        queryBuilder.append("\"Start_Time\" >= '").append(request.getStartDate()).append("'");
        queryBuilder.append(" AND \"Start_Time\" <= '").append(request.getEndDate()).append("'");

        // Add optional filters
        if (request.getInteractionId() != null && !request.getInteractionId().trim().isEmpty()) {
            queryBuilder.append(" AND \"Interaction_ID\" = '").append(request.getInteractionId()).append("'");
        }

        if (request.getCustomerPhoneNumber() != null && !request.getCustomerPhoneNumber().trim().isEmpty()) {
            queryBuilder.append(" AND \"Customer_Phone_Number\" = '").append(request.getCustomerPhoneNumber()).append("'");
        }

        if (request.getTalkdeskPhoneNumber() != null && !request.getTalkdeskPhoneNumber().trim().isEmpty()) {
            queryBuilder.append(" AND \"Talkdesk_Phone_Number\" = '").append(request.getTalkdeskPhoneNumber()).append("'");
        }

        if (request.getCallType() != null && !request.getCallType().trim().isEmpty()) {
            queryBuilder.append(" AND \"Call_Type\" = '").append(request.getCallType()).append("'");
        }

        return queryBuilder.toString();
    }
}