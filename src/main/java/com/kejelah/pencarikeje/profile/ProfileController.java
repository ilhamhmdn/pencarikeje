package com.kejelah.pencarikeje.profile;

import com.kejelah.pencarikeje.auth.dto.UserResponse;
import com.kejelah.pencarikeje.profile.dto.ChangePasswordRequest;
import com.kejelah.pencarikeje.profile.dto.UpdateProfileRequest;
import com.kejelah.pencarikeje.security.AuthenticatedUser;
import com.kejelah.pencarikeje.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Current user's name and email")
    public UserResponse get(@CurrentUser AuthenticatedUser user) {
        return profileService.get(user.id());
    }

    @PutMapping
    @Operation(summary = "Update name (email is read-only in the MVP)")
    public UserResponse update(@CurrentUser AuthenticatedUser user,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateName(user.id(), request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    public ResponseEntity<Void> changePassword(@CurrentUser AuthenticatedUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(user.id(), request);
        return ResponseEntity.noContent().build();
    }
}
