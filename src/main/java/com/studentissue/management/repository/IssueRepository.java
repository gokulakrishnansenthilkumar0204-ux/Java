package com.studentissue.management.repository;

import com.studentissue.management.model.Issue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    long countByStatusIgnoreCase(String status);

    /**
     * Loads the issue with a row lock (SELECT ... FOR UPDATE).
     * Votes / confirmations for the same issue are processed one at a time,
     * so double clicks or simultaneous requests cannot create duplicates or
     * skip the 5-confirmation rule.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Issue i where i.id = :id")
    Optional<Issue> findByIdForUpdate(@Param("id") Long id);
}
