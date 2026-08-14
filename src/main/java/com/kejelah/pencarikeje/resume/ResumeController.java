package com.kejelah.pencarikeje.resume;

import com.kejelah.pencarikeje.security.AuthenticatedUser;
import com.kejelah.pencarikeje.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/applications/{applicationId}/resume")
@Tag(name = "Resume")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace the application's single PDF resume")
    public ResumeService.ResumeMetadata upload(@CurrentUser AuthenticatedUser user,
                                               @PathVariable Long applicationId,
                                               @RequestParam("file") MultipartFile file) {
        return resumeService.upload(user.id(), applicationId, file);
    }

    /**
     * RES-04. {@code inline} lets the same endpoint serve in-browser viewing and
     * download, so the frontend needs only one URL.
     */
    @GetMapping
    @Operation(summary = "Stream the resume for inline viewing or download")
    public ResponseEntity<Resource> download(@CurrentUser AuthenticatedUser user,
                                             @PathVariable Long applicationId) {

        ResumeService.ResumeDownload download = resumeService.download(user.id(), applicationId);

        ContentDisposition disposition = ContentDisposition.inline(download.filename());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.value())
                .body(download.resource());
    }

    /**
     * Builds a Content-Disposition value that survives non-ASCII filenames by
     * pairing an ASCII fallback with the RFC 5987 {@code filename*} form.
     */
    private record ContentDisposition(String value) {

        static ContentDisposition inline(String filename) {
            String safe = filename == null ? "resume.pdf" : filename;
            String ascii = safe.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
            String encoded = java.net.URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
            return new ContentDisposition(
                    "inline; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded);
        }
    }
}
