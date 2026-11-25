package com.example.starter.repository;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class PostgresAccountRepository implements AccountRepository {
    private final JdbcClient jdbc;

    PostgresAccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return jdbc.sql("""
                SELECT u.username, u.password_hash, u.enabled, r.code
                FROM app_user u
                JOIN app_user_role ur ON ur.user_id = u.id
                JOIN app_role r ON r.id = ur.role_id
                WHERE u.username = :username
                """)
                .param("username", username)
                .query((row, number) -> User.withUsername(row.getString("username"))
                        .password(row.getString("password_hash"))
                        .disabled(!row.getBoolean("enabled"))
                        .roles(row.getString("code"))
                        .build())
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Override
    @Transactional
    public String replaceRole(String username, String role) {
        var current = jdbc.sql("""
                SELECT user_id, role_id
                FROM app_user_role
                WHERE user_id = (SELECT id FROM app_user WHERE username = :username)
                FOR UPDATE
                """)
                .param("username", username)
                .query((row, number) -> new AssignedRole(row.getObject("user_id", UUID.class), row.getShort("role_id")))
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return jdbc.sql("""
                UPDATE app_user_role
                SET role_id = (SELECT id FROM app_role WHERE code = :role)
                WHERE user_id = :userId
                RETURNING (SELECT code FROM app_role WHERE id = :previousRoleId)
                """)
                .param("role", role)
                .param("userId", current.userId())
                .param("previousRoleId", current.roleId())
                .query(String.class)
                .single();
    }

    public void createDemoAccountIfAbsent(String username, String passwordHash, String role) {
        jdbc.sql("""
                WITH created AS (
                    INSERT INTO app_user (username, password_hash)
                    VALUES (:username, :passwordHash)
                    ON CONFLICT (username) DO NOTHING
                    RETURNING id
                )
                INSERT INTO app_user_role (user_id, role_id)
                SELECT id, (SELECT id FROM app_role WHERE code = :role)
                FROM created
                """)
                .param("username", username)
                .param("passwordHash", passwordHash)
                .param("role", role)
                .update();
    }

    private record AssignedRole(UUID userId, short roleId) {}
}
