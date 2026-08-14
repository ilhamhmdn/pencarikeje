package com.kejelah.pencarikeje.dashboard;

import com.kejelah.pencarikeje.application.ApplicationRepository;
import com.kejelah.pencarikeje.application.dto.ApplicationListItemResponse;
import com.kejelah.pencarikeje.dashboard.dto.DashboardResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private static final int RECENT_LIMIT = 5;

    /** DASH-01 interview family. */
    private static final List<String> INTERVIEW_CODES =
            List.of("INTERVIEW", "TECHNICAL_INTERVIEW", "FINAL_INTERVIEW");

    private static final List<String> OFFER_CODES = List.of("OFFER", "ACCEPTED");

    private final ApplicationRepository applicationRepository;

    public DashboardService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    /**
     * Every figure is computed only from the authenticated user's applications.
     *
     * <p>A user with no applications gets zeros and empty arrays — never a 404.
     */
    @Transactional(readOnly = true)
    public DashboardResponse forUser(Long userId) {
        long total = applicationRepository.countByUserId(userId);
        long interviews = applicationRepository.countByUserIdAndStatusCodeIn(userId, INTERVIEW_CODES);
        long offers = applicationRepository.countByUserIdAndStatusCodeIn(userId, OFFER_CODES);

        // Already ordered by display_order and already excludes zero counts,
        // since a status with no applications produces no group.
        List<DashboardResponse.StatusCount> breakdown = applicationRepository.countByStatusForUser(userId).stream()
                .map(row -> new DashboardResponse.StatusCount(
                        (String) row[0], (String) row[1], ((Number) row[2]).longValue()))
                .toList();

        List<ApplicationListItemResponse> recent =
                applicationRepository.findRecentForUser(userId, PageRequest.of(0, RECENT_LIMIT)).stream()
                        .map(ApplicationListItemResponse::from)
                        .toList();

        return new DashboardResponse(total, interviews, offers, breakdown, recent);
    }
}
