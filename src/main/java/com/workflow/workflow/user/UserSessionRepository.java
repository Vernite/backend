package com.workflow.workflow.user;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface UserSessionRepository extends CrudRepository<UserSession, Integer> {
    Optional<UserSession> findBySession(String session);
}