package com.researchassistant.usermanagement.folder;

import com.researchassistant.usermanagement.dto.FolderRequest;
import com.researchassistant.usermanagement.dto.FolderResponse;
import com.researchassistant.usermanagement.error.DuplicateFolderNameException;
import com.researchassistant.usermanagement.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final FolderRepository folders;

    public FolderService(FolderRepository folders) {
        this.folders = folders;
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> listFor(UUID ownerId) {
        return folders.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(FolderService::toResponse)
                .toList();
    }

    @Transactional
    public FolderResponse create(UUID ownerId, FolderRequest request) {
        String name = request.name().trim();
        if (folders.existsByOwnerIdAndNameIgnoreCase(ownerId, name)) {
            throw new DuplicateFolderNameException(name);
        }
        return toResponse(folders.save(new Folder(ownerId, name)));
    }

    @Transactional
    public FolderResponse rename(UUID ownerId, UUID folderId, FolderRequest request) {
        Folder folder = requireOwned(ownerId, folderId);
        String name = request.name().trim();

        if (!folder.getName().equalsIgnoreCase(name)
                && folders.existsByOwnerIdAndNameIgnoreCase(ownerId, name)) {
            throw new DuplicateFolderNameException(name);
        }
        folder.setName(name);
        return toResponse(folder);
    }

    @Transactional
    public void delete(UUID ownerId, UUID folderId) {
        folders.delete(requireOwned(ownerId, folderId));
    }

    /**
     * Looks up by id AND owner. A folder owned by somebody else is therefore
     * indistinguishable from one that does not exist - a 404, never a 403,
     * which would confirm the id is real.
     */
    private Folder requireOwned(UUID ownerId, UUID folderId) {
        return folders.findByIdAndOwnerId(folderId, ownerId)
                .orElseThrow(() -> new NotFoundException("Folder not found"));
    }

    private static FolderResponse toResponse(Folder folder) {
        return new FolderResponse(folder.getId(), folder.getName(), folder.getCreatedAt());
    }
}
