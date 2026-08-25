package com.comeon.assignment.realitycheck.service;

import com.comeon.assignment.realitycheck.exception.RealityCheckException;
import com.comeon.assignment.realitycheck.model.PlayerRecord;
import com.comeon.assignment.realitycheck.model.RealityCheckSession;
import com.comeon.assignment.realitycheck.model.RealityCheckStatus;
import com.comeon.assignment.realitycheck.repository.RealityCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealityCheckService {
    private static final String ACTIVE = "ACTIVE";

    private final RealityCheckRepository repository;

    private final Clock clock;

    private final Map<Long, RealityCheckSession> cache = new ConcurrentHashMap<>();

    public RealityCheckStatus getStatus(long playerId) {
        RealityCheckSession session = Optional.ofNullable(cache.get(playerId))
                .or(() -> repository.findByPlayerAndStatus(playerId, ACTIVE))
                .orElse(null);

        if (session == null) {
            log.warn("No active reality check found for player {}", playerId);
            return new RealityCheckStatus("NO_ACTIVE_CHECK", playerId);
        }

        cache.put(playerId, session);
        return new RealityCheckStatus(session.getStatus(), playerId);
    }

    public Optional<PlayerRecord> findPlayer(long playerId) {
        return repository.findPlayerById(playerId);
       }

    public RealityCheckSession acknowledge(long playerId) throws RealityCheckException {
        RealityCheckSession session = Optional.ofNullable(cache.get(playerId))
                .or(() -> repository.findByPlayerAndStatus(playerId, ACTIVE))
                .orElseThrow(() -> new RealityCheckException("NO_ACTIVE_CHECK"));

        session.setAcknowledged(true);
        repository.acknowledgeSession(playerId, true);
        cache.put(playerId, session);
        return session;
    }

    public List<Long> activePlayerIds() {
        return repository.findActivePlayerIds();
    }

    public Optional<RealityCheckSession> refresh(long playerId) {
        RealityCheckSession realityCheckSession = repository.findByPlayerAndStatus(playerId, ACTIVE).orElse(null);
        if (realityCheckSession == null) {
            return Optional.empty();
        }
        long now = clock.instant().getEpochSecond();
        realityCheckSession.setElapsedSeconds(Math.max(0, now - realityCheckSession.getStartedAt()));
        boolean promptDue = now >= realityCheckSession.getNextCheckAt();
        if (promptDue) {
            realityCheckSession.setAcknowledged(false);
            realityCheckSession.setLastPromptAt(now);
            realityCheckSession.setNextCheckAt(nextCheckAt(now, realityCheckSession.getIntervalMinutes()));
        }
        repository.updateSession(realityCheckSession);
        cache.put(playerId, realityCheckSession);
        return promptDue ? Optional.of(realityCheckSession) : Optional.empty();
    }

    public Optional<RealityCheckSession> getActiveSession(long playerId) {
        RealityCheckSession realityCheckSession = cache.get(playerId);
        if (realityCheckSession == null) {
            realityCheckSession = repository.findByPlayerAndStatus(playerId, ACTIVE).orElse(null);
        }
        return Optional.ofNullable(realityCheckSession);
    }


    public RealityCheckSession getOrStartCheck(long playerId, int intervalMinutes) {
        PlayerRecord player = findPlayer(playerId)
                .orElseThrow(() -> new RealityCheckException("PLAYER_NOT_FOUND"));
        Optional<RealityCheckSession> existingSession = getActiveSession(playerId);
        if (existingSession.isEmpty()) {
            RealityCheckSession session = newSession(player, intervalMinutes);
            repository.insertSession(session);
            return getActiveSession(playerId).orElseThrow(
                    () -> new IllegalStateException("Inserted reality check session could not be read"));
        }
        RealityCheckSession session = existingSession.get();
        if (session.getFranchiseId() != player.getFranchiseId()) {
            throw new RealityCheckException("FRANCHISE_MISMATCH");
        }
        updateTiming(session, intervalMinutes);
        repository.updateSession(session);
        return session;

    }

    private void updateTiming(RealityCheckSession session, int intervalMinutes) {
        long now = clock.instant().getEpochSecond();
        session.setElapsedSeconds(Math.max(0, now - session.getStartedAt()));
        session.setIntervalMinutes(intervalMinutes);
        if (now >= session.getNextCheckAt()) {
            session.setAcknowledged(false);
            session.setLastPromptAt(now);
            session.setNextCheckAt(nextCheckAt(now, intervalMinutes));
        }
    }

    private RealityCheckSession newSession(PlayerRecord player, int intervalMinutes) {
        long now = clock.instant().getEpochSecond();
        RealityCheckSession session = new RealityCheckSession();
        session.setPlayerId(player.getId());
        session.setFranchiseId(player.getFranchiseId());
        session.setStatus(ACTIVE);
        session.setIntervalMinutes(intervalMinutes);
        session.setStartedAt(now);
        session.setLastPromptAt(now);
        session.setElapsedSeconds(0);
        session.setNetAmountMinor(0);
        session.setAcknowledged(false);
        session.setNextCheckAt(nextCheckAt(now, intervalMinutes));
        return session;
    }

    private long nextCheckAt(long now, int intervalMinutes) {
        return Math.addExact(now, Math.multiplyExact((long) intervalMinutes, 60));
    }
}
