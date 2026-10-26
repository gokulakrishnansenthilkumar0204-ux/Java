package com.studentissue.management.service;

import com.studentissue.management.dto.IssueCreateRequest;
import com.studentissue.management.dto.IssueResponse;
import com.studentissue.management.dto.IssueStatsDto;

import java.util.List;

public interface IssueService {

    IssueResponse createIssue(IssueCreateRequest request, String browserId);

    List<IssueResponse> getAllIssues(String search, String category, String status, String sortBy, String browserId);

    IssueResponse getIssueById(Long id, String browserId);

    IssueResponse voteIssue(Long issueId, String browserId);

    IssueResponse confirmResolution(Long issueId, String browserId);

    IssueStatsDto getStatistics();

    List<String> getCategories();

    String calculatePriority(long votes);
}
