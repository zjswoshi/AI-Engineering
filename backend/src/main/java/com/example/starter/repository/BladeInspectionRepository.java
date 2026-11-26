package com.example.starter.repository;

import com.example.starter.model.BladeInspection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@Profile("blade & postgres")
public class BladeInspectionRepository {
    private static final String VISIBLE = """
            (:admin OR owner_id = (SELECT id FROM app_user WHERE username = :username))
            """;
    private final JdbcClient jdbc;

    public BladeInspectionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public BladeInspection create(UUID id, String username, String farm, String turbine, String originalName) {
        return jdbc.sql("""
                INSERT INTO blade_inspection (id, owner_id, farm_name, turbine_number, original_name)
                VALUES (:id, (SELECT id FROM app_user WHERE username = :username), :farm, :turbine, :name)
                RETURNING *
                """).param("id", id).param("username", username).param("farm", farm)
                .param("turbine", turbine).param("name", originalName).query(BladeInspectionRepository::task).single();
    }

    public List<BladeInspection> list(String username, boolean admin, long offset, int limit) {
        return jdbc.sql("SELECT * FROM blade_inspection WHERE " + VISIBLE
                        + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset")
                .param("username", username).param("admin", admin).param("offset", offset).param("limit", limit)
                .query(BladeInspectionRepository::task).list();
    }

    public Optional<BladeInspection.Execution> find(UUID id, String username, boolean admin) {
        return jdbc.sql("SELECT * FROM blade_inspection WHERE id = :id AND " + VISIBLE)
                .param("id", id).param("username", username).param("admin", admin)
                .query(BladeInspectionRepository::execution).optional();
    }

    public Optional<BladeInspection.Execution> claim() {
        return jdbc.sql("""
                UPDATE blade_inspection SET status = 'RUNNING', attempt_id = gen_random_uuid()
                WHERE id = (SELECT id FROM blade_inspection WHERE status = 'PENDING'
                            ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED)
                RETURNING *
                """).query(BladeInspectionRepository::execution).optional();
    }

    public void recoverInterrupted() {
        jdbc.sql("UPDATE blade_inspection SET status = 'PENDING', attempt_id = NULL WHERE status = 'RUNNING'").update();
    }

    public void finish(UUID id, UUID attempt, String failure) {
        jdbc.sql("""
                UPDATE blade_inspection SET status = :status, failure_message = :failure
                WHERE id = :id AND attempt_id = :attempt AND status = 'RUNNING'
                """).param("status", failure == null ? "COMPLETED" : "FAILED")
                .param("failure", failure).param("id", id).param("attempt", attempt).update();
    }

    private static BladeInspection.Execution execution(ResultSet row, int number) throws SQLException {
        return new BladeInspection.Execution(task(row, number), row.getObject("attempt_id", UUID.class));
    }

    private static BladeInspection task(ResultSet row, int number) throws SQLException {
        return new BladeInspection(row.getObject("id", UUID.class), row.getString("farm_name"),
                row.getString("turbine_number"), row.getString("original_name"), row.getString("status"),
                row.getObject("created_at", OffsetDateTime.class), row.getString("failure_message"));
    }
}
