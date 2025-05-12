package dev.abozhik.repository;

import dev.abozhik.model.entity.ProcessedRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProcessedRequestRepository extends JpaRepository<ProcessedRequest, String> {

    void deleteByCreationDateTimeBefore(LocalDateTime dateTime);

} 