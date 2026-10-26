package com.studentissue.management.repository;

import com.studentissue.management.model.IssueVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueVoteRepository extends JpaRepository<IssueVote, Long> {

    boolean existsByIssueIdAndVoterToken(Long issueId, UUID voterToken);

    long countByIssueId(Long issueId);

    /** All issue ids this browser has voted for - one query for the whole list page. */
    @Query("select v.issueId from IssueVote v where v.voterToken = :voterToken")
    List<Long> findIssueIdsByVoterToken(@Param("voterToken") UUID voterToken);
}
