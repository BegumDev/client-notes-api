package com.clientnotes.resource;

import com.clientnotes.model.EngagementNote;
import com.clientnotes.model.NoteStatus;
import com.clientnotes.repository.EngagementNoteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Path("/api/v1/notes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EngagementNoteResource {

    @Inject
    EngagementNoteRepository repository;

    @Inject
    MeterRegistry registry;

    @POST
    public Response createNote(EngagementNote request) throws SQLException {
        if (request == null || isBlank(request.clientCode) || isBlank(request.author) || isBlank(request.summary)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "clientCode, author, and summary are required"))
                    .build();
        }
        if (request.status == null) {
            request.status = NoteStatus.IN_PROGRESS;
        }

        EngagementNote created = repository.create(request);
        registry.counter("client_notes_created_total", "status", created.status.name()).increment();

        return Response.created(URI.create("/api/v1/notes/" + created.id))
                .entity(created)
                .build();
    }

    @GET
    @Path("/{clientCode}")
    public Response getClientNotes(@PathParam("clientCode") String clientCode,
                                   @QueryParam("status") String statusParam) throws SQLException {
        NoteStatus statusFilter = null;
        if (statusParam != null && !statusParam.isBlank()) {
            try {
                statusFilter = NoteStatus.valueOf(statusParam.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid status filter: " + statusParam))
                        .build();
            }
        }

        List<EngagementNote> notes = repository.findByClient(clientCode, statusFilter);
        if (notes.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "No notes found for client: " + clientCode))
                    .build();
        }
        return Response.ok(notes).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateNote(@PathParam("id") long id, EngagementNote request) throws SQLException {
        if (request == null || request.status == null || isBlank(request.summary)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "status and summary are required"))
                    .build();
        }

        return repository.update(id, request.status, request.summary)
                .map(updated -> Response.ok(updated).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Note not found with id: " + id))
                        .build());
    }

    @DELETE
    @Path("/{id}")
    public Response deleteNote(@PathParam("id") long id) throws SQLException {
        if (repository.deleteById(id)) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Note not found with id: " + id))
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
