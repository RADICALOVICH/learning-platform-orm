package ru.mifi.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mifi.learning.domain.entity.Profile;

public interface ProfileRepository extends JpaRepository<Profile, Long> {}
