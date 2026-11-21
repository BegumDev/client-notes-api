# Client Engagement Notes API (`client-notes-api`)

## Overview
`client-notes-api` is a lightweight RESTful microservice designed for client-facing developers (Forward Deployed Engineers / Solutions Engineers) to record, query, update, and clean up engagement context notes per client. It provides an immediate, structured view of where work was last left off, active blockers, workarounds, and engagement status across handoffs.

## Technology Stack
- **Language & Runtime:** Java 17
- **Framework:** Quarkus 3.8.3 (RESTEasy Reactive, Hibernate ORM with Panache)
- **Database & Migrations:** PostgreSQL 16 + Flyway SQL Migrations
- **Testing:** JUnit 5, REST Assured, Testcontainers (PostgreSQL)
- **Observability:** SmallRye Health (`/q/health`), Micrometer Prometheus (`/q/metrics`)
- **CI/CD & Containerisation:** Maven Wrapper (`./mvnw`), Docker, Docker Compose, Jenkins Declarative Pipeline

---

## Functional Requirements (Given–When–Then)

### Requirement 1: Create a Client Engagement Note (`POST /api/v1/notes`)
- **Given** a client-facing developer submits a valid note payload (`clientCode`, `author`, `status` [`BLOCKER`, `IN_PROGRESS`, `WORKAROUND`, `RESOLVED`], `summary`),
- **When** `POST /api/v1/notes` is called,
- **Then** the note is persisted in PostgreSQL with an `updatedAt` timestamp and returns `201 Created` with the saved note (or `400 Bad Request` if `clientCode` or `summary` is blank).

### Requirement 2: Retrieve Latest Client Notes & Active Blockers (`GET /api/v1/notes/{clientCode}`)
- **Given** one or more engagement notes exist for `{clientCode}`,
- **When** a developer queries `GET /api/v1/notes/{clientCode}` (optionally filtered with `?status=BLOCKER`),
- **Then** the API returns `200 OK` with matching notes ordered by most recent `updatedAt` first so the developer sees where work was last left off (or `404 Not Found` if no notes exist for that client).

### Requirement 3: Update Status / Summary or Delete a Note (`PUT /api/v1/notes/{id}` & `DELETE /api/v1/notes/{id}`)
- **Given** an existing engagement note `{id}` is stored in PostgreSQL,
- **When** a developer updates its status or summary via `PUT /api/v1/notes/{id}` (or deletes an obsolete note via `DELETE /api/v1/notes/{id}`),
- **Then** PostgreSQL is updated with a refreshed `updatedAt` timestamp and returns `200 OK` (or `204 No Content` on delete, and `404 Not Found` if `{id}` does not exist).
