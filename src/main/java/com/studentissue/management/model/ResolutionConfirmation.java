package com.studentissue.management.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resolution_confirmations", uniqueConstraints = {
    @UniqueConstraint(name = "uq_issue_confirmation", columnNames = {"issue_id", "voter_token"})
})
public class ResolutionConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "voter_token", nullable = false)
    private UUID voterToken;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public ResolutionConfirmation() {
    }

    public ResolutionConfirmation(Long issueId, UUID voterToken) {
        this.issueId = issueId;
        this.voterToken = voterToken;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public UUID getVoterToken() {
        return voterToken;
    }

    public void setVoterToken(UUID voterToken) {
        this.voterToken = voterToken;
    }

    public String getBrowserId() {
        return voterToken != null ? voterToken.toString() : null;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
