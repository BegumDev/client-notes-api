package com.clientnotes.repository;

import com.clientnotes.model.EngagementNote;
import com.clientnotes.model.NoteStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EngagementNoteRepository {

    @Inject
    DataSource dataSource;

    public EngagementNote create(EngagementNote note) throws SQLException {
        String sql = """
            INSERT INTO engagement_notes (client_code, author, status, summary, updated_at)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, client_code, author, status, summary, updated_at
            """;
        Instant now = Instant.now();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.clientCode.trim().toUpperCase());
            ps.setString(2, note.author.trim());
            ps.setString(3, note.status.name());
            ps.setString(4, note.summary.trim());
            ps.setTimestamp(5, Timestamp.from(now));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return mapRow(rs);
            }
        }
    }

    public List<EngagementNote> findByClient(String clientCode, NoteStatus status) throws SQLException {
        String sql = status != null
            ? "SELECT id, client_code, author, status, summary, updated_at FROM engagement_notes WHERE UPPER(client_code) = UPPER(?) AND status = ? ORDER BY updated_at DESC, id DESC"
            : "SELECT id, client_code, author, status, summary, updated_at FROM engagement_notes WHERE UPPER(client_code) = UPPER(?) ORDER BY updated_at DESC, id DESC";

        List<EngagementNote> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientCode.trim());
            if (status != null) {
                ps.setString(2, status.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public Optional<EngagementNote> update(long id, NoteStatus status, String summary) throws SQLException {
        String sql = """
            UPDATE engagement_notes
            SET status = ?, summary = ?, updated_at = ?
            WHERE id = ?
            RETURNING id, client_code, author, status, summary, updated_at
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, summary.trim());
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setLong(4, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM engagement_notes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private EngagementNote mapRow(ResultSet rs) throws SQLException {
        return new EngagementNote(
            rs.getLong("id"),
            rs.getString("client_code"),
            rs.getString("author"),
            NoteStatus.valueOf(rs.getString("status")),
            rs.getString("summary"),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
