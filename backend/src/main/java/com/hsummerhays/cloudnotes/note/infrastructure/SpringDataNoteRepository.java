package com.hsummerhays.cloudnotes.note.infrastructure;

import com.hsummerhays.cloudnotes.note.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataNoteRepository extends JpaRepository<Note, UUID> {
    
    @Query("SELECT n FROM Note n WHERE n.archived = false AND n.ownerEmail = :ownerEmail ORDER BY n.updatedAt DESC")
    List<Note> findAllActive(@Param("ownerEmail") String ownerEmail);

    @Query("SELECT n FROM Note n WHERE n.archived = true AND n.ownerEmail = :ownerEmail ORDER BY n.updatedAt DESC")
    List<Note> findAllArchived(@Param("ownerEmail") String ownerEmail);

    @Query("SELECT n FROM Note n WHERE n.archived = false AND n.ownerEmail = :ownerEmail AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY n.updatedAt DESC")
    List<Note> searchActive(@Param("query") String query, @Param("ownerEmail") String ownerEmail);
}
