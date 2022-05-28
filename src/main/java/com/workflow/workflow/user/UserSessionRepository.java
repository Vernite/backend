package com.workflow.workflow.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.repository.CrudRepository;

public abstract interface UserSessionRepository extends CrudRepository<UserSession, Long> {
    Optional<UserSession> findBySession(String session);
    
    default List<UserSession> findByUser(User user) {
        return findByUser(user, Sort.by(Direction.DESC, "lastUsed"));
    }

    List<UserSession> findByUser(User user, Sort by);

    void deleteBySession(String session);
}
