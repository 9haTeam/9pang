package com._hateam.auth.infrastructure.repository;

import com._hateam.auth.domain.model.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}