package com.studentissue.management.service;

import com.studentissue.management.dto.IssueCreateRequest;
import com.studentissue.management.dto.IssueResponse;
import com.studentissue.management.dto.IssueStatsDto;
import com.studentissue.management.exception.DuplicateActionException;
import com.studentissue.management.exception.InvalidOperationException;
import com.studentissue.management.exception.ResourceNotFoundException;
import com.studentissue.management.model.Issue;
import com.studentissue.management.model.IssueSummary;
import com.studentissue.management.model.IssueVote;
import com.studentissue.management.model.ResolutionConfirmation;
import com.studentissue.management.repository.IssueRepository;
import com.studentissue.management.repository.IssueSummaryRepository;
import com.studentissue.management.repository.IssueVoteRepository;
import com.studentissue.management.repository.ResolutionConfirmationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class IssueServiceImpl implements IssueService {

    private final IssueRepository issueRepository;
    private final IssueVoteRepository issueVoteRepository;
    private final ResolutionConfirmationRepository resolutionConfirmationRepository;
    private final IssueSummaryRepository issueSummaryRepository;

    public IssueServiceImpl(IssueRepository issueRepository,
                            IssueVoteRepository issueVoteRepository,
                            ResolutionConfirmationRepository resolutionConfirmationRepository,
                            IssueSummaryRepository issueSummaryRepository) {
        this.issueRepository = issueRepository;
        this.issueVoteRepository = issueVoteRepository;
        this.resolutionConfirmationRepository = resolutionConfirmationRepository;
        this.issueSummaryRepository = issueSummaryRepository;
    }

    @Override
    @Transactional
    public IssueResponse createIssue(IssueCreateRequest request, String browserId) {
        if (request == null) {
            throw new IllegalArgumentException("Issue details are required");
        }

        Issue issue = new Issue();
        issue.setTitle(requireText(request.getTitle(), "Title"));
        issue.setDescription(requireText(request.getDescription(), "Description"));
        issue.setCategory(normalizeCategory(request.getCategory()));
        issue.setLocation(requireText(request.getLocation(), "Location"));
        issue.setStatus(IssueRules.STATUS_OPEN);
        issue.setCreatedAt(OffsetDateTime.now());

        Issue saved = issueRepository.save(issue);

        return new IssueResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getCategory(),
                saved.getLocation(),
                saved.getStatus(),
                calculatePriority(0L),
                0L,
                0L,
                false,
                false,
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueResponse> getAllIssues(String search, String category, String status, String sortBy, String browserId) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanCategory = (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category.trim())) ? category.trim() : null;
        String cleanStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim() : null;

        boolean sortByNewest = sortBy != null && "newest".equalsIgnoreCase(sortBy.trim());

        Specification<IssueSummary> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (cleanSearch != null) {
                String pattern = "%" + cleanSearch.toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate catLike = cb.like(cb.lower(root.get("category")), pattern);
                Predicate locLike = cb.like(cb.lower(root.get("location")), pattern);
                predicates.add(cb.or(titleLike, descLike, catLike, locLike));
            }

            if (cleanCategory != null) {
                predicates.add(cb.equal(cb.lower(root.get("category")), cleanCategory.toLowerCase()));
            }

            if (cleanStatus != null) {
                predicates.add(cb.equal(cb.lower(root.get("status")), cleanStatus.toLowerCase()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = sortByNewest
                ? Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.desc("voteCount"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

        List<IssueSummary> summaries = issueSummaryRepository.findAll(spec, sort);

        // Two queries in total (instead of 2-3 queries per issue) to know what this browser already did
        Set<Long> votedIssueIds = Collections.emptySet();
        Set<Long> confirmedIssueIds = Collections.emptySet();
        UUID voterToken = tryParseVoterToken(browserId);
        if (voterToken != null && !summaries.isEmpty()) {
            votedIssueIds = new HashSet<>(issueVoteRepository.findIssueIdsByVoterToken(voterToken));
            confirmedIssueIds = new HashSet<>(resolutionConfirmationRepository.findIssueIdsByVoterToken(voterToken));
        }

        List<IssueResponse> responses = new ArrayList<>(summaries.size());
        for (IssueSummary summary : summaries) {
            responses.add(new IssueResponse(
                    summary.getId(),
                    summary.getTitle(),
                    summary.getDescription(),
                    summary.getCategory(),
                    summary.getLocation(),
                    summary.getStatus(),
                    calculatePriority(summary.getVoteCount()),
                    summary.getVoteCount(),
                    summary.getResolutionConfirmations(),
                    votedIssueIds.contains(summary.getId()),
                    confirmedIssueIds.contains(summary.getId()),
                    summary.getCreatedAt()
            ));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public IssueResponse getIssueById(Long id, String browserId) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + id));
        return buildIssueResponse(issue, tryParseVoterToken(browserId));
    }

    @Override
    @Transactional
    public IssueResponse voteIssue(Long issueId, String browserId) {
        UUID voterToken = parseVoterToken(browserId);

        // Row lock: concurrent votes on the same issue are handled one after another
        Issue issue = issueRepository.findByIdForUpdate(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + issueId));

        if (issueVoteRepository.existsByIssueIdAndVoterToken(issueId, voterToken)) {
            throw new DuplicateActionException("You have already voted for this issue from this browser identifier.");
        }

        issueVoteRepository.save(new IssueVote(issueId, voterToken));

        return buildIssueResponse(issue, voterToken);
    }

    @Override
    @Transactional
    public IssueResponse confirmResolution(Long issueId, String browserId) {
        UUID voterToken = parseVoterToken(browserId);

        // Row lock: guarantees the 5-confirmation check is correct even with simultaneous clicks
        Issue issue = issueRepository.findByIdForUpdate(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + issueId));

        if (IssueRules.STATUS_RESOLVED.equalsIgnoreCase(issue.getStatus())) {
            throw new InvalidOperationException("This issue has already been marked as RESOLVED.");
        }

        if (resolutionConfirmationRepository.existsByIssueIdAndVoterToken(issueId, voterToken)) {
            throw new DuplicateActionException("You have already confirmed resolution for this issue from this browser identifier.");
        }

        resolutionConfirmationRepository.save(new ResolutionConfirmation(issueId, voterToken));

        long confirmationCount = resolutionConfirmationRepository.countByIssueId(issueId);
        if (confirmationCount >= IssueRules.CONFIRMATIONS_REQUIRED_FOR_RESOLUTION) {
            issue.setStatus(IssueRules.STATUS_RESOLVED);
            issueRepository.saveAndFlush(issue);
        }

        return buildIssueResponse(issue, voterToken);
    }

    @Override
    @Transactional(readOnly = true)
    public IssueStatsDto getStatistics() {
        long totalIssues = issueRepository.count();
        long openIssues = issueRepository.countByStatusIgnoreCase(IssueRules.STATUS_OPEN);
        long resolvedIssues = issueRepository.countByStatusIgnoreCase(IssueRules.STATUS_RESOLVED);
        long totalVotes = issueVoteRepository.count();

        return new IssueStatsDto(totalIssues, openIssues, resolvedIssues, totalVotes);
    }

    @Override
    public List<String> getCategories() {
        return IssueRules.CATEGORIES;
    }

    @Override
    public String calculatePriority(long votes) {
        return IssueRules.priorityFor(votes);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /** Accepts only the predefined categories (case-insensitive) and stores the canonical spelling. */
    private String normalizeCategory(String category) {
        String value = requireText(category, "Category");
        for (String allowed : IssueRules.CATEGORIES) {
            if (allowed.equalsIgnoreCase(value)) {
                return allowed;
            }
        }
        throw new IllegalArgumentException("Invalid category '" + value + "'. Allowed categories: "
                + String.join(", ", IssueRules.CATEGORIES));
    }

    /** Strict parsing for write operations: missing or malformed id is a 400 error. */
    private UUID parseVoterToken(String browserId) {
        if (browserId == null || browserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Browser identifier (UUID) is required");
        }
        try {
            return UUID.fromString(browserId.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format for browser identifier: " + browserId);
        }
    }

    /** Lenient parsing for read operations: a missing or malformed id just means "not voted yet". */
    private UUID tryParseVoterToken(String browserId) {
        if (browserId == null || browserId.trim().isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(browserId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private IssueResponse buildIssueResponse(Issue issue, UUID voterToken) {
        long voteCount = issueVoteRepository.countByIssueId(issue.getId());
        long confirmationCount = resolutionConfirmationRepository.countByIssueId(issue.getId());
        boolean hasVoted = false;
        boolean hasConfirmed = false;

        if (voterToken != null) {
            hasVoted = issueVoteRepository.existsByIssueIdAndVoterToken(issue.getId(), voterToken);
            hasConfirmed = resolutionConfirmationRepository.existsByIssueIdAndVoterToken(issue.getId(), voterToken);
        }

        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getCategory(),
                issue.getLocation(),
                issue.getStatus(),
                calculatePriority(voteCount),
                voteCount,
                confirmationCount,
                hasVoted,
                hasConfirmed,
                issue.getCreatedAt()
        );
    }
}
