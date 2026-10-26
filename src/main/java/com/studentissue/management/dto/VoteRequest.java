package com.studentissue.management.dto;

import jakarta.validation.constraints.NotBlank;

public class VoteRequest {

    @NotBlank(message = "Browser identifier UUID is required")
    private String browserId;

    public VoteRequest() {
    }

    public VoteRequest(String browserId) {
        this.browserId = browserId;
    }

    public String getBrowserId() {
        return browserId;
    }

    public void setBrowserId(String browserId) {
        this.browserId = browserId;
    }
}
