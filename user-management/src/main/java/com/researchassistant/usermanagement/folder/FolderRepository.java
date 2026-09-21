package com.researchassistant.usermanagement.folder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    /**
     * Always look up by id AND owner. A folder belonging to someone else then
     * comes back empty and becomes a 404 - never a 403, which would confirm
     * the id exists.
     */
    Optional<Folder> findByIdAndOwnerId(UUID id, UUID ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);
}
