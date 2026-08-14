package com.kejelah.pencarikeje.status;

import com.kejelah.pencarikeje.status.dto.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statuses")
@Tag(name = "Statuses")
public class StatusController {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    /**
     * The catalogue the frontend must read its dropdowns from. Hardcoding status
     * values client-side is a review-blocking defect (MVP.md 8.2).
     */
    @GetMapping
    @Operation(summary = "Active status catalogue, ordered for presentation")
    public List<StatusResponse> list() {
        return statusService.listActive();
    }
}
