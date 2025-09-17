package com.avangrid.gui.avangrid_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.avangrid.gui.avangrid_backend.model.TalkdeskMetadataRequest;
import com.avangrid.gui.avangrid_backend.model.TalkdeskMetadataResponse;
import com.avangrid.gui.avangrid_backend.service.TalkdeskService;


@RestController
@RequestMapping("/talkdesk")
public class TalkdeskController {

    @Autowired
    private TalkdeskService talkdeskService;

    @GetMapping("/metadata")
    public ResponseEntity<TalkdeskMetadataResponse> getTalkdeskMetadata(
            @Parameter(description = "Start date in format 'yyyy-MM-dd HH:mm:ss'", required = true)
            @RequestParam("start_date") String startDate,
            
            @Parameter(description = "End date in format 'yyyy-MM-dd HH:mm:ss'", required = true)
            @RequestParam("end_date") String endDate,
            
            @Parameter(description = "Continuation token for pagination")
            @RequestParam(value = "continuation_token", required = false) String continuationToken,
            
            @Parameter(description = "Page size (1-100)")
            @RequestParam(value = "page_size", defaultValue = "50") 
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size cannot exceed 100") Integer pageSize,
            
            @Parameter(description = "Filter by Interaction ID")
            @RequestParam(value = "interaction_id", required = false) String interactionId,
            
            @Parameter(description = "Filter by Customer Phone Number")
            @RequestParam(value = "customer_phone_number", required = false) String customerPhoneNumber,
            
            @Parameter(description = "Filter by Talkdesk Phone Number")
            @RequestParam(value = "talkdesk_phone_number", required = false) String talkdeskPhoneNumber,
            
            @Parameter(description = "Filter by Call Type")
            @RequestParam(value = "call_type", required = false) String callType) {



        TalkdeskMetadataRequest request = TalkdeskMetadataRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .continuationToken(continuationToken)
                .pageSize(pageSize)
                .interactionId(interactionId)
                .customerPhoneNumber(customerPhoneNumber)
                .talkdeskPhoneNumber(talkdeskPhoneNumber)
                .callType(callType)
                .build();
        
        
        TalkdeskMetadataResponse response = talkdeskService.getTalkdeskMetadata(request);
        
        return ResponseEntity.ok(response);
    }
    
}
