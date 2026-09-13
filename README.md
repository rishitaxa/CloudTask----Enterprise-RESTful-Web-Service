# CloudTask — Task ZIP Export Feature

Streaming `/api/tasks/export/zip` endpoint (Spring Boot + PostgreSQL) plus a
matching TypeScript frontend service that triggers a browser download.

## Contents

```
backend/src/main/java/com/cloudtask/
├── export/
│   ├── dto/TaskExportDto.java          Projection DTO for export rows
│   ├── AuthenticatedUserResolver.java  Placeholder: wire to your auth setup
│   ├── TaskExportService.java          Builds the ZIP, streams DB -> zip entries
│   └── TaskExportController.java       GET /api/tasks/export/zip
└── task/
    ├── Task.java                       Minimal entity (merge into your existing one)
    └── TaskRepository.java             Streaming query (Hibernate cursor)

frontend/src/services/
├── taskExportService.ts                fetch()-based download function
├── taskExportService.axios.ts          axios equivalent
└── exportButtonExample.ts              Example call site
```

## Integration steps

1. **Merge `Task.java` / `TaskRepository.java`** into your existing task
   entity/repository rather than dropping them in wholesale if you already
   have a `Task` class — only the `streamAllByOwnerId` query and its
   `@QueryHints` fetch-size annotation are new.
2. **Wire up `AuthenticatedUserResolver.resolveOwnerId()`** to however
   CloudTask currently identifies the logged-in user (JWT claim, custom
   `UserDetails`, session attribute, etc.). It currently throws
   `UnsupportedOperationException` as a placeholder.
3. **Drop in `TaskExportService` and `TaskExportController`** as-is — no
   changes needed unless your package structure differs.
4. **Frontend**: import `downloadTaskExportZip` (fetch) or
   `downloadTaskExportZipAxios` (axios) from `taskExportService`, wire it to
   an "Export" button's `onClick`.
5. **Security**: confirm `/api/tasks/export/zip` isn't excluded from your
   auth filter chain, and check CSRF settings if you're using session-based
   auth (most Spring Security configs exempt `GET` by default).

## Design notes

- Rows stream from Postgres via a Hibernate cursor (`Stream<TaskExportDto>`,
  fetch size 50) instead of loading the full result set into memory.
- Each task is serialized straight into its own ZIP entry
  (`objectMapper.writeValue(zos, task)`) — no per-task byte-array buffering.
- `StreamingResponseBody` writes directly to the servlet output stream, so
  memory usage stays flat regardless of how many tasks are exported.
- Zip entry names are server-generated and sanitized (defense in depth
  against zip-slip), never derived from user input.
- Because the HTTP response is committed (200 + headers) before streaming
  starts, a mid-stream failure can't be turned into a 500 — the code logs
  loudly instead. Resolve auth/ownership *before* returning the streaming
  body so permission errors still produce a clean 401/403.
"# CloudTask----Enterprise-RESTful-Web-Service" 
