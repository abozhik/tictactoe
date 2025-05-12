package dev.abozhik.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ProcessedRequest {
    @Id
    private String requestId;

    @Column(nullable = false)
    private String response;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;

    @PrePersist
    protected void onCreate() {
        creationDateTime = LocalDateTime.now();
    }
}