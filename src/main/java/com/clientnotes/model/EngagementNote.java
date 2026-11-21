package com.clientnotes.model;

import java.time.Instant;

public class EngagementNote {
    public Long id;
    public String clientCode;
    public String author;
    public NoteStatus status;
    public String summary;
    public Instant updatedAt;

    public EngagementNote() {}

    public EngagementNote(Long id, String clientCode, String author, NoteStatus status, String summary, Instant updatedAt) {
        this.id = id;
        this.clientCode = clientCode;
        this.author = author;
        this.status = status;
        this.summary = summary;
        this.updatedAt = updatedAt;
    }
}
