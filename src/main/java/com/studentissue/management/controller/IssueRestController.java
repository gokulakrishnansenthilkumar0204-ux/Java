package com.studentissue.management.controller;

import com.studentissue.management.dto.*;
import com.studentissue.management.service.IssueRules;
import com.studentissue.management.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueRestController {

    private final IssueService issueService;

    public IssueRestController(IssueService issueService) {
        this.issueService = issueService;
    }

    /** List issues. Supports ?search= (or ?keyword=), ?category=, ?status=, ?sortBy=votes|newest, ?browserId= */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IssueResponse>>> getAllIssues(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "votes") String sortBy,
            @RequestParam(required = false) String browserId) {

        List<IssueResponse> issues = issueService.getAllIssues(searchTerm(keyword, search), category, status, sortBy, browserId);
        return ResponseEntity.ok(ApiResponse.success("Issues retrieved successfully", issues));
    }

    /** Same as GET /api/issues - kept for backwards compatibility with the frontend and tests. */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<IssueResponse>>> searchIssues(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "votes") String sortBy,
            @RequestParam(required = false) String browserId) {

        List<IssueResponse> issues = issueService.getAllIssues(searchTerm(keyword, search), category, status, sortBy, browserId);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", issues));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<IssueResponse>> getIssueById(
            @PathVariable Long id,
            @RequestParam(required = false) String browserId) {

        IssueResponse issue = issueService.getIssueById(id, browserId);
        return ResponseEntity.ok(ApiResponse.success("Issue details retrieved successfully", issue));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IssueResponse>> createIssue(
            @Valid @RequestBody IssueCreateRequest request,
            @RequestParam(required = false) String browserId) {

        IssueResponse created = issueService.createIssue(request, browserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Issue reported successfully", created));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<IssueResponse>> voteIssue(
            @PathVariable Long id,
            @Valid @RequestBody VoteRequest request) {

        IssueResponse updated = issueService.voteIssue(id, request.getBrowserId());
        return ResponseEntity.ok(ApiResponse.success("Vote recorded successfully", updated));
    }

    @PostMapping("/{id}/confirm-resolution")
    public ResponseEntity<ApiResponse<IssueResponse>> confirmResolution(
            @PathVariable Long id,
            @Valid @RequestBody ResolutionConfirmRequest request) {

        IssueResponse updated = issueService.confirmResolution(id, request.getBrowserId());
        int required = IssueRules.CONFIRMATIONS_REQUIRED_FOR_RESOLUTION;
        String msg = IssueRules.STATUS_RESOLVED.equalsIgnoreCase(updated.getStatus())
                ? required + " confirmations reached! Issue has been marked as RESOLVED."
                : "Resolution confirmation recorded (" + updated.getConfirmationCount() + "/" + required + ")";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<IssueStatsDto>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", issueService.getStatistics()));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", issueService.getCategories()));
    }

    private String searchTerm(String keyword, String search) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return keyword.trim();
        }
        return search != null ? search.trim() : null;
    }
}
