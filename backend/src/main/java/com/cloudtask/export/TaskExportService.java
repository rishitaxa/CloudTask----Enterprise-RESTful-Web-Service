package com.cloudtask.export;

import com.cloudtask.export.dto.TaskExportDto;
import com.cloudtask.task.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class TaskExportService {

    private static final Logger log = LoggerFactory.getLogger(TaskExportService.class);

    // Only allow safe characters in generated entry names — defense in depth
    // against zip-slip / path traversal, even though names are server-generated.
    private static final Pattern SAFE_ENTRY_NAME = Pattern.compile("[^a-zA-Z0-9._-]");

    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public TaskExportService(TaskRepository taskRepository, ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper.copy()
                .findAndRegisterModules(); // ensures JavaTimeModule etc. are active
    }

    /**
     * Streams every task belonging to {@code ownerId} into a ZIP archive,
     * one JSON file per task, written directly to {@code outputStream}.
     *
     * Must run inside a read-only transaction so the underlying Stream
     * (a live DB cursor) stays open for the duration of the write.
     */
    @Transactional(readOnly = true)
    public void streamTasksAsZip(Long ownerId, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream);
             Stream<TaskExportDto> tasks = taskRepository.streamAllByOwnerId(ownerId)) {

            zos.setLevel(6); // balanced compression vs CPU

            long count = 0;
            for (TaskExportDto task : (Iterable<TaskExportDto>) tasks::iterator) {
                writeTaskEntry(zos, task);
                count++;
            }

            if (count == 0) {
                // Still return a valid (empty-ish) archive with a marker file
                // rather than a zero-byte response that some clients mishandle.
                writeEmptyMarker(zos);
            }

            zos.finish();
            log.info("Exported {} task(s) to ZIP for owner {}", count, ownerId);
        }
    }

    private void writeTaskEntry(ZipOutputStream zos, TaskExportDto task) throws IOException {
        String safeId = SAFE_ENTRY_NAME.matcher(String.valueOf(task.id())).replaceAll("_");
        ZipEntry entry = new ZipEntry("task-" + safeId + ".json");
        zos.putNextEntry(entry);
        objectMapper.writeValue(zos, task); // streams JSON straight into the zip entry
        zos.closeEntry();
    }

    private void writeEmptyMarker(ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry("README.txt"));
        zos.write("No tasks found for export.".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
