package com.comeon.assignment.realitycheck.job;

import com.comeon.assignment.realitycheck.event.RealityCheckEvent;
import com.comeon.assignment.realitycheck.event.RealityCheckEventSender;
import com.comeon.assignment.realitycheck.service.RealityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealityCheckRefreshJob {
    private final RealityCheckService realityCheckService;
    private final RealityCheckEventSender realityCheckEventSender;

    @Scheduled(fixedDelayString = "${reality-check.refresh-delay}")
    public void refreshActiveSessions() {
        List<Long> playerIds = realityCheckService.activePlayerIds();
        log.info("Refreshing {} active reality check sessions", playerIds.size());

        for (Long playerId : playerIds) {
            try {
                realityCheckService.refresh(playerId)
                        .map(RealityCheckEvent::realityCheckEventMapper)
                        .ifPresent(realityCheckEventSender::publish);
            } catch (RuntimeException exception) {
                log.error("Failed to refresh reality check for player {}", playerId, exception);
            }
        }

    }
}
