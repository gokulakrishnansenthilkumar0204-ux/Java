package com.studentissue.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.studentissue.management.service.IssueRules;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "issue_summary")
@Immutable
public class IssueSummary {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    private String location;

    private String status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "vote_count")
    private Long voteCount;

    private String priority;

    @Column(name = "resolution_confirmations")
    private Long resolutionConfirmations;

    public IssueSummary() {
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVoteCount() {
        return voteCount != null ? voteCount : 0L;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }

    /** Priority is always derived from the vote count using the single rule in IssueRules. */
    public String getPriority() {
        return IssueRules.priorityFor(getVoteCount());
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getResolutionConfirmations() {
        return resolutionConfirmations != null ? resolutionConfirmations : 0L;
    }

    public void setResolutionConfirmations(Long resolutionConfirmations) {
        this.resolutionConfirmations = resolutionConfirmations;
    }
}
