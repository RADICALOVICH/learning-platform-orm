package ru.mifi.learning.mapper;

import org.springframework.stereotype.Component;
import ru.mifi.learning.domain.entity.Profile;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.dto.user.ProfileResponse;
import ru.mifi.learning.dto.user.UserResponse;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getPhone(),
                profile.getCity(),
                profile.getBirthDate());
    }
}
