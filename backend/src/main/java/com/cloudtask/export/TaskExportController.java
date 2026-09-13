package com.cloudtask.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/tasks")
public class TaskExportController {

    private static final Logger log = LoggerFactory.getLogger(TaskExportController.class);

    private final TaskExportService exportService;
    // Resolves the authenticated user's internal ID from the principal —
    // adapt to however CloudTask's security config exposes it.
    private final AuthenticatedUserResolver userResolver;

    public TaskExportController(TaskExportService exportService,
                                 AuthenticatedUserResolver userResolver) {
        this.exportService = exportService;
        this.userResolver = userResolver;
    }

    @GetMapping(value = "/export/zip", produces = "application/zip")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StreamingResponseBody> exportTasksAsZip(Authentication authentication) {

        // Resolved eagerly, before returning the streaming body, so an
        // auth/authorization failure here still produces a normal 401/403
        // instead of a broken/partial stream.
        Long ownerId = userResolver.resolveOwnerId(authentication);
        String filename = "tasks-export-" + LocalDate.now() + ".zip";

        StreamingResponseBody body = outputStream -> {
            try {
                exportService.streamTasksAsZip(ownerId, outputStream);
            } catch (IOException e) {
                // Client likely disconnected mid-stream (e.g. tab closed) — expected, log at debug.
                log.debug("Client aborted ZIP export stream for owner {}", ownerId, e);
            } catch (Exception e) {
                // At this point headers/status 200 are already committed, so we
                // can't switch to a 500. Best we can do is stop cleanly and log loudly.
                log.error("Unexpected failure while streaming ZIP export for owner {}", ownerId, e);
            }
        };

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.valueOf("application/zip"))
                .body(body);
    }
}
