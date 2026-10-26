package com.studentissue.management.repository;

import com.studentissue.management.model.IssueSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Read-only access to the issue_summary view.
 * Filtering and sorting are done with Specifications in IssueServiceImpl.
 */
@Repository
public interface IssueSummaryRepository extends JpaRepository<IssueSummary, Long>, JpaSpecificationExecutor<IssueSummary> {
}
