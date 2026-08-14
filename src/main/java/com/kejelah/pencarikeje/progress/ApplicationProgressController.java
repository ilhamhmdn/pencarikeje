package com.kejelah.pencarikeje.progress;

import com.kejelah.pencarikeje.progress.dto.ProgressRequest;
import com.kejelah.pencarikeje.progress.dto.ProgressResponse;
import com.kejelah.pencarikeje.security.AuthenticatedUser;
import com.kejelah.pencarikeje.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/progress")
@Tag(name = "Application progress")
public class ApplicationProgressController {

    private final ApplicationProgressService progressService;

    public ApplicationProgressController(ApplicationProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    @Operation(summary = "Timeline, ordered event_date ASC then id ASC")
    public List<ProgressResponse> list(@CurrentUser AuthenticatedUser user,
                                       @PathVariable Long applicationId) {
        return progressService.list(user.id(), applicationId);
    }

    @PostMapping
    @Operation(summary = "Add an event; any status may follow any status")
    public ResponseEntity<ProgressResponse> add(@CurrentUser AuthenticatedUser user,
                                                @PathVariable Long applicationId,
                                                @Valid @RequestBody ProgressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(progressService.add(user.id(), applicationId, request));
    }

    @PutMapping("/{progressId}")
    @Operation(summary = "Edit an event and recompute the application's current status")
    public ProgressResponse update(@CurrentUser AuthenticatedUser user,
                                   @PathVariable Long applicationId,
                                   @PathVariable Long progressId,
                                   @Valid @RequestBody ProgressRequest request) {
        return progressService.update(user.id(), applicationId, progressId, request);
    }

    @DeleteMapping("/{progressId}")
    @Operation(summary = "Delete an event; the last remaining event cannot be deleted")
    public ResponseEntity<Void> delete(@CurrentUser AuthenticatedUser user,
                                       @PathVariable Long applicationId,
                                       @PathVariable Long progressId) {
        progressService.delete(user.id(), applicationId, progressId);
        return ResponseEntity.noContent().build();
    }
}
