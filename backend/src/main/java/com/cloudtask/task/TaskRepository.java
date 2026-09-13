package com.cloudtask.task;

import com.cloudtask.export.dto.TaskExportDto;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.stream.Stream;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "50"))
    @Query("""
        select new com.cloudtask.export.dto.TaskExportDto(
            t.id, t.title, t.description, t.status, t.assignee, t.createdAt, t.updatedAt)
        from Task t
        where t.ownerId = :ownerId
        order by t.id
        """)
    Stream<TaskExportDto> streamAllByOwnerId(Long ownerId);
}
