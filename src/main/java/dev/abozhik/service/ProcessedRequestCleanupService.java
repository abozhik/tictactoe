package dev.abozhik.service;

import dev.abozhik.repository.ProcessedRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProcessedRequestCleanupService {

    private final ProcessedRequestRepository processedRequestRepository;

    @Value("${game.processed-requests.retention-hours:24}")
    private int retentionHours;

    public ProcessedRequestCleanupService(ProcessedRequestRepository processedRequestRepository) {
        this.processedRequestRepository = processedRequestRepository;
    }

    @Scheduled(cron = "${game.processed-requests.cleanup-cron:0 0 0 * * ?}")
    @Transactional
    public void cleanupOldProcessedRequests() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(retentionHours);
        processedRequestRepository.deleteByCreationDateTimeBefore(cutoffTime);
    }
} 