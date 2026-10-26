package com.studentissue.management.dto;

import jakarta.validation.constraints.NotBlank;

public class ResolutionConfirmRequest {

    @NotBlank(message = "Browser identifier UUID is required")
    private String browserId;

    public ResolutionConfirmRequest() {
    }

    public ResolutionConfirmRequest(String browserId) {
        this.browserId = browserId;
    }

    public String getBrowserId() {
        return browserId;
    }

    public void setBrowserId(String browserId) {
        this.browserId = browserId;
    }
}
