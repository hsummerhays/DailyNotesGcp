package com.hsummerhays.cloudnotes.note.infrastructure;

import com.hsummerhays.cloudnotes.note.domain.NoteDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MongoNoteRepository extends MongoRepository<NoteDocument, UUID> {

    List<NoteDocument> findByOwnerEmailAndArchivedFalseOrderByUpdatedAtDesc(String ownerEmail);

    List<NoteDocument> findByOwnerEmailAndArchivedTrueOrderByUpdatedAtDesc(String ownerEmail);

    @Query("{ 'ownerEmail': ?1, 'archived': false, $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'content': { $regex: ?0, $options: 'i' } } ] }")
    List<NoteDocument> searchActive(String query, String ownerEmail);
}
