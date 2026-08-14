package com.kejelah.pencarikeje.profile;

import com.kejelah.pencarikeje.auth.User;
import com.kejelah.pencarikeje.auth.UserRepository;
import com.kejelah.pencarikeje.auth.dto.UserResponse;
import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.profile.dto.ChangePasswordRequest;
import com.kejelah.pencarikeje.profile.dto.UpdateProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public UserResponse updateName(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setName(request.name().trim());
        return UserResponse.from(user);
    }

    /**
     * PRO-03. The caller's existing JWT stays valid until it expires — there is no
     * session invalidation in the MVP, which MVP.md 12 records as an accepted
     * limitation with token versioning as the deferred fix.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest(ErrorCodes.INCORRECT_PASSWORD, "Current password is incorrect.");
        }
        if (!request.passwordsMatch()) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED, "New passwords do not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized(
                        ErrorCodes.UNAUTHENTICATED, "Authenticated user no longer exists."));
    }
}
