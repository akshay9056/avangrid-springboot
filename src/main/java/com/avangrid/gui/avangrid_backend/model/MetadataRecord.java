package com.avangrid.gui.avangrid_backend.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataRecord {
    @JsonProperty("startTime")
    private String startTime;
    
    @JsonProperty("endTime")
    private String endTime;
    
    private Map<String, Object> additionalProperties;
    
    public MetadataRecord() {}
    
    public MetadataRecord(String startTime, String endTime, Map<String, Object> additionalProperties) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.additionalProperties = additionalProperties;
    }
    
    // Getters and Setters
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
    
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}

