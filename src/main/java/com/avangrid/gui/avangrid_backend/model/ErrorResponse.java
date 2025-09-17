package com.avangrid.gui.avangrid_backend.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String message;
    private String timestamp;



}
