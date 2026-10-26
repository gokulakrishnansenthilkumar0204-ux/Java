package com.studentissue.management.dto;

public class IssueStatsDto {

    private long totalIssues;
    private long openIssues;
    private long resolvedIssues;
    private long totalVotes;

    public IssueStatsDto() {
    }

    public IssueStatsDto(long totalIssues, long openIssues, long resolvedIssues, long totalVotes) {
        this.totalIssues = totalIssues;
        this.openIssues = openIssues;
        this.resolvedIssues = resolvedIssues;
        this.totalVotes = totalVotes;
    }

    public long getTotalIssues() {
        return totalIssues;
    }

    public void setTotalIssues(long totalIssues) {
        this.totalIssues = totalIssues;
    }

    public long getOpenIssues() {
        return openIssues;
    }

    public void setOpenIssues(long openIssues) {
        this.openIssues = openIssues;
    }

    public long getResolvedIssues() {
        return resolvedIssues;
    }

    public void setResolvedIssues(long resolvedIssues) {
        this.resolvedIssues = resolvedIssues;
    }

    public long getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(long totalVotes) {
        this.totalVotes = totalVotes;
    }
}
