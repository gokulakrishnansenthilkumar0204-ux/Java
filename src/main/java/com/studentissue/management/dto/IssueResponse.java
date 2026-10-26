package com.studentissue.management.dto;

import com.studentissue.management.service.IssueRules;

import java.time.OffsetDateTime;

public class IssueResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private String location;
    private String status;
    private String priority;
    private Long voteCount;
    private Long confirmationCount;
    private Boolean hasVoted;
    private Boolean hasConfirmed;
    private OffsetDateTime createdAt;
    private Integer confirmationsRequired = IssueRules.CONFIRMATIONS_REQUIRED_FOR_RESOLUTION;

    public IssueResponse() {
    }

    public IssueResponse(Long id, String title, String description, String category,
                         String location, String status, String priority, Long voteCount,
                         Long confirmationCount, Boolean hasVoted, Boolean hasConfirmed,
                         OffsetDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.location = location;
        this.status = status;
        this.priority = priority;
        this.voteCount = voteCount;
        this.confirmationCount = confirmationCount;
        this.hasVoted = hasVoted;
        this.hasConfirmed = hasConfirmed;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }

    public Long getConfirmationCount() {
        return confirmationCount;
    }

    public void setConfirmationCount(Long confirmationCount) {
        this.confirmationCount = confirmationCount;
    }

    public Boolean getHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(Boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public Boolean getHasConfirmed() {
        return hasConfirmed;
    }

    public void setHasConfirmed(Boolean hasConfirmed) {
        this.hasConfirmed = hasConfirmed;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getConfirmationsRequired() {
        return confirmationsRequired;
    }

    public void setConfirmationsRequired(Integer confirmationsRequired) {
        this.confirmationsRequired = confirmationsRequired;
    }
}
