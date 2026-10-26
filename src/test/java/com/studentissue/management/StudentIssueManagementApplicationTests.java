package com.studentissue.management;

import com.studentissue.management.dto.IssueCreateRequest;
import com.studentissue.management.dto.IssueResponse;
import com.studentissue.management.exception.DuplicateActionException;
import com.studentissue.management.service.IssueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class StudentIssueManagementApplicationTests {

    @Autowired
    private IssueService issueService;

    @Test
    void contextLoads() {
        assertThat(issueService).isNotNull();
    }

    @Test
    @DisplayName("Should create issue with initial LOW priority and OPEN status")
    void shouldCreateIssue() {
        IssueCreateRequest request = new IssueCreateRequest(
                "Wi-Fi down in CS Lab 3",
                "Students cannot connect to Eduroam or CS internal network during practical exams.",
                "Infrastructure",
                "CS Lab 3"
        );

        IssueResponse created = issueService.createIssue(request, UUID.randomUUID().toString());

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Wi-Fi down in CS Lab 3");
        assertThat(created.getStatus()).isEqualTo("OPEN");
        assertThat(created.getPriority()).isEqualTo("LOW");
        assertThat(created.getVoteCount()).isEqualTo(0L);
        assertThat(created.getConfirmationCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should record vote and prevent duplicate vote from same browser identifier")
    void shouldPreventDuplicateVote() {
        String browserId = UUID.randomUUID().toString();
        IssueCreateRequest request = new IssueCreateRequest(
                "Library AC not working",
                "2nd floor central reading hall AC unit is making loud noise and blowing warm air.",
                "Infrastructure",
                "Central Library"
        );
        IssueResponse created = issueService.createIssue(request, browserId);

        // First vote should succeed
        IssueResponse voted = issueService.voteIssue(created.getId(), browserId);
        assertThat(voted.getVoteCount()).isEqualTo(1L);
        assertThat(voted.getHasVoted()).isTrue();

        // Second vote from the SAME browser identifier should throw DuplicateActionException
        assertThrows(DuplicateActionException.class, () -> {
            issueService.voteIssue(created.getId(), browserId);
        });

        // Vote from a DIFFERENT browser identifier should succeed
        String secondBrowserId = UUID.randomUUID().toString();
        IssueResponse votedAgain = issueService.voteIssue(created.getId(), secondBrowserId);
        assertThat(votedAgain.getVoteCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should calculate priority: 0-9 LOW, 10-19 MEDIUM, 20+ HIGH")
    void shouldCalculatePriorityCorrectly() {
        assertThat(issueService.calculatePriority(0)).isEqualTo("LOW");
        assertThat(issueService.calculatePriority(9)).isEqualTo("LOW");
        assertThat(issueService.calculatePriority(10)).isEqualTo("MEDIUM");
        assertThat(issueService.calculatePriority(19)).isEqualTo("MEDIUM");
        assertThat(issueService.calculatePriority(20)).isEqualTo("HIGH");
        assertThat(issueService.calculatePriority(55)).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should automatically change status from OPEN to RESOLVED after 5 distinct browser confirmations")
    void shouldResolveAfterFiveConfirmations() {
        IssueCreateRequest request = new IssueCreateRequest(
                "Water dispenser empty in Block B",
                "RO water dispenser has been empty since morning.",
                "Hostel & Mess",
                "Block B 1st Floor"
        );
        IssueResponse created = issueService.createIssue(request, UUID.randomUUID().toString());
        Long issueId = created.getId();

        // 4 confirmations
        for (int i = 1; i <= 4; i++) {
            String browserId = UUID.randomUUID().toString();
            IssueResponse resp = issueService.confirmResolution(issueId, browserId);
            assertThat(resp.getStatus()).isEqualTo("OPEN");
            assertThat(resp.getConfirmationCount()).isEqualTo((long) i);
        }

        // 5th confirmation: triggers status transition to RESOLVED
        String fifthBrowserId = UUID.randomUUID().toString();
        IssueResponse resolved = issueService.confirmResolution(issueId, fifthBrowserId);
        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");
        assertThat(resolved.getConfirmationCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should reject unknown categories and store the canonical category name")
    void shouldValidateCategory() {
        IssueCreateRequest invalid = new IssueCreateRequest("Title", "Description", "Not A Category", "Block A");
        assertThrows(IllegalArgumentException.class, () -> issueService.createIssue(invalid, null));

        IssueCreateRequest lowerCase = new IssueCreateRequest("Fan broken", "Ceiling fan not working", "academic", "Room 101");
        assertThat(issueService.createIssue(lowerCase, null).getCategory()).isEqualTo("Academic");
    }

    @Test
    @DisplayName("Listing should mark which issues the current browser has voted for")
    void shouldMarkVotedIssuesInList() {
        String browserId = UUID.randomUUID().toString();
        IssueResponse created = issueService.createIssue(
                new IssueCreateRequest("Lift not working", "Main block lift stuck on 3rd floor", "Infrastructure", "Main Block"),
                null);
        issueService.voteIssue(created.getId(), browserId);

        IssueResponse fromList = issueService.getAllIssues(null, null, null, "votes", browserId).stream()
                .filter(i -> i.getId().equals(created.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(fromList.getHasVoted()).isTrue();
        assertThat(fromList.getVoteCount()).isEqualTo(1L);

        IssueResponse otherBrowser = issueService.getAllIssues(null, null, null, "votes", UUID.randomUUID().toString()).stream()
                .filter(i -> i.getId().equals(created.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(otherBrowser.getHasVoted()).isFalse();
    }
}
