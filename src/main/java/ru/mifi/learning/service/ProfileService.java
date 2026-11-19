package ru.mifi.learning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.learning.domain.entity.Profile;
import ru.mifi.learning.domain.entity.User;
import ru.mifi.learning.dto.user.ProfileResponse;
import ru.mifi.learning.dto.user.UpsertProfileRequest;
import ru.mifi.learning.exception.NotFoundException;
import ru.mifi.learning.mapper.UserMapper;
import ru.mifi.learning.repository.ProfileRepository;
import ru.mifi.learning.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final UserMapper mapper;

    public ProfileResponse get(long userId) {
        return mapper.toResponse(profiles.findById(userId).orElseThrow(() -> NotFoundException.of("Profile", userId)));
    }

    public record UpsertResult(boolean created, ProfileResponse profile) {}

    @Transactional
    public UpsertResult upsert(long userId, UpsertProfileRequest request) {
        // Блокируем существующего родителя даже при первом PUT, когда строки профиля ещё нет.
        User user = users.lockById(userId).orElseThrow(() -> NotFoundException.of("User", userId));
        var existing = profiles.findById(userId);
        Profile profile = existing.orElseGet(() -> profiles.save(new Profile(user)));
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setPhone(request.phone());
        profile.setCity(request.city());
        profile.setBirthDate(request.birthDate());
        return new UpsertResult(existing.isEmpty(), mapper.toResponse(profile));
    }
}
