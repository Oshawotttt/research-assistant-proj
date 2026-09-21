package com.researchassistant.usermanagement.folder;

import com.researchassistant.usermanagement.dto.FolderRequest;
import com.researchassistant.usermanagement.dto.FolderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/folders")
@Tag(name = "Folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @GetMapping
    @Operation(summary = "List the folders belonging to the caller")
    public List<FolderResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return folderService.listFor(callerId(jwt));
    }

    @PostMapping
    @Operation(summary = "Create a folder")
    public ResponseEntity<FolderResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                 @Valid @RequestBody FolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(folderService.create(callerId(jwt), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a folder")
    public FolderResponse rename(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody FolderRequest request) {
        return folderService.rename(callerId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a folder")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        folderService.delete(callerId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The owner always comes from the verified token, never from the request
     * body. A client-supplied ownerId would let any user write into another
     * account.
     */
    private static UUID callerId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
