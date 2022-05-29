package com.workflow.workflow.user;

import org.springframework.data.repository.CrudRepository;

public interface DeleteAccountRequestRepository extends CrudRepository<DeleteAccountRequest, Long> {
    public DeleteAccountRequest findByToken(String token);
}
