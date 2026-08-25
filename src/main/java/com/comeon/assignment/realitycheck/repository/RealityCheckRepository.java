package com.comeon.assignment.realitycheck.repository;

import com.comeon.assignment.realitycheck.model.PlayerRecord;
import com.comeon.assignment.realitycheck.model.RealityCheckSession;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RealityCheckRepository {

    private final Jdbi jdbi;

    public Optional<RealityCheckSession> findByPlayerAndStatus(long playerId, String status) {
        try (Handle handle = jdbi.open()) {
            return handle.createQuery("SELECT * FROM reality_check_session WHERE player_id = :playerId AND status = :status")
                    .bind("playerId", playerId)
                    .bind("status", status)
                    .map(SESSION_MAPPER)
                    .findOne();
        }
    }

    private static final RowMapper<RealityCheckSession> SESSION_MAPPER = (rs, ctx) -> {
        RealityCheckSession session = new RealityCheckSession();
        session.setId(rs.getLong("id"));
        session.setPlayerId(rs.getLong("player_id"));
        session.setFranchiseId(rs.getLong("franchise_id"));
        session.setStatus(rs.getString("status"));
        session.setIntervalMinutes(rs.getInt("interval_minutes"));
        session.setStartedAt(rs.getLong("started_at"));
        session.setLastPromptAt(rs.getLong("last_prompt_at"));
        session.setElapsedSeconds(rs.getLong("elapsed_seconds"));
        session.setNetAmountMinor(rs.getLong("net_amount_minor"));
        session.setAcknowledged(rs.getBoolean("acknowledged"));
        session.setNextCheckAt(rs.getLong("next_check_at"));
        return session;
    };

    public List<Long> findActivePlayerIds() {
        try (Handle handle = jdbi.open()) {
            return handle.createQuery("SELECT player_id FROM reality_check_session WHERE status = 'ACTIVE'")
                    .mapTo(Long.class)
                    .list();
        }
    }

    public void insertSession(RealityCheckSession s) {
        try (Handle handle = jdbi.open()) {
            handle.createUpdate("INSERT INTO reality_check_session " +
                            "(player_id, franchise_id, status, interval_minutes, started_at, last_prompt_at, " +
                            " elapsed_seconds, net_amount_minor, acknowledged, next_check_at) " +
                            "VALUES (:playerId, :franchiseId, :status, :intervalMinutes, :startedAt, :lastPromptAt, " +
                            " :elapsedSeconds, :netAmountMinor, :acknowledged, :nextCheckAt)")
                    .bind("playerId", s.getPlayerId())
                    .bind("franchiseId", s.getFranchiseId())
                    .bind("status", s.getStatus())
                    .bind("intervalMinutes", s.getIntervalMinutes())
                    .bind("startedAt", s.getStartedAt())
                    .bind("lastPromptAt", s.getLastPromptAt())
                    .bind("elapsedSeconds", s.getElapsedSeconds())
                    .bind("netAmountMinor", s.getNetAmountMinor())
                    .bind("acknowledged", s.isAcknowledged())
                    .bind("nextCheckAt", s.getNextCheckAt())
                    .execute();
        }
    }

    public void updateSession(RealityCheckSession session) {
        try (Handle handle = jdbi.open()) {
            handle.createUpdate("UPDATE reality_check_session SET status = :status, interval_minutes = :intervalMinutes, " +
                            "last_prompt_at = :lastPromptAt, elapsed_seconds = :elapsedSeconds, " +
                            "net_amount_minor = :netAmountMinor, acknowledged = :acknowledged, next_check_at = :nextCheckAt " +
                            "WHERE id = :id")
                    .bind("status", session.getStatus())
                    .bind("intervalMinutes", session.getIntervalMinutes())
                    .bind("lastPromptAt", session.getLastPromptAt())
                    .bind("elapsedSeconds", session.getElapsedSeconds())
                    .bind("netAmountMinor", session.getNetAmountMinor())
                    .bind("acknowledged", session.isAcknowledged())
                    .bind("nextCheckAt", session.getNextCheckAt())
                    .bind("id", session.getId())
                    .execute();
        }
    }
    private static final RowMapper<PlayerRecord> PLAYER_ROW_MAPPER =
            (resultSet, context) -> MapPlayerRecord(resultSet);

    public Optional<PlayerRecord> findPlayerFull(long playerId) {
        try (Handle handle = jdbi.open()) {
            return handle.createQuery("SELECT * FROM player WHERE id = :playerId")
                    .bind("playerId", playerId)
                    .map(PLAYER_ROW_MAPPER)
                    .findOne();
        }
    }

    private static PlayerRecord MapPlayerRecord(ResultSet rs) throws SQLException {
        PlayerRecord playerRecord = new PlayerRecord();
        playerRecord.id = rs.getLong("id");
        playerRecord.franchiseId = rs.getLong("franchise_id");
        playerRecord.username = rs.getString("username");
        playerRecord.email = rs.getString("email");
        playerRecord.firstName = rs.getString("first_name");
        playerRecord.lastName = rs.getString("last_name");
        playerRecord.gender = rs.getString("gender");
        playerRecord.birthDate = rs.getDate("birth_date") == null ? null : rs.getDate("birth_date").toLocalDate();
        playerRecord.country = rs.getString("country");
        playerRecord.city = rs.getString("city");
        playerRecord.address = rs.getString("address");
        playerRecord.postalCode = rs.getString("postal_code");
        playerRecord.phone = rs.getString("phone");
        playerRecord.currency = rs.getString("currency");
        playerRecord.language = rs.getString("language");
        playerRecord.timezone = rs.getString("timezone");
        playerRecord.registeredAt = rs.getString("registered_at");
        playerRecord.lastLoginAt = rs.getString("last_login_at");
        playerRecord.kycStatus = rs.getString("kyc_status");
        playerRecord.vipLevel = rs.getInt("vip_level");
        playerRecord.marketingOptIn = rs.getBoolean("marketing_opt_in");
        playerRecord.selfExcluded = rs.getBoolean("self_excluded");
        playerRecord.depositLimitMinor = rs.getLong("deposit_limit_minor");
        playerRecord.balanceMinor = rs.getLong("balance_minor");
        playerRecord.bonusBalanceMinor = rs.getLong("bonus_balance_minor");
        playerRecord.loyaltyPoints = rs.getLong("loyalty_points");
        playerRecord.affiliateId = rs.getString("affiliate_id");
        playerRecord.referralCode = rs.getString("referral_code");
        playerRecord.riskScore = rs.getInt("risk_score");
        playerRecord.accountStatus = rs.getString("account_status");
        playerRecord.createdAt = rs.getString("created_at");
        playerRecord.updatedAt = rs.getString("updated_at");
        return playerRecord;
    }
    public Optional<PlayerRecord> findPlayerById(long playerId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT id, franchise_id, timezone FROM player WHERE id = :playerId")
                        .bind("playerId", playerId)
                        .map((resultSet, context) -> new PlayerRecord(
                                resultSet.getLong("id"),
                                resultSet.getLong("franchise_id"),
                                resultSet.getString("timezone")
                        ))
                        .findOne()
        );
    }

}
