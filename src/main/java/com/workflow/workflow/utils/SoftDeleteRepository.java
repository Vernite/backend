package com.workflow.workflow.utils;

import java.util.Optional;

import org.springframework.data.repository.NoRepositoryBean;

/**
 * Interface for generic operations on a repository for soft delete entities.
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends SoftDeleteEntity, I> extends NotFoundRepository<T, I> {

    /**
     * Retrieves an entity with given id if its not soft deleted.
     * 
     * @param id must not be {@literal null}.
     * @return the entity with the given id or {@literal Optional#empty()} if none
     *         found.
     * @throws IllegalArgumentException if {@literal id} is {@literal null}.
     */
    Optional<T> findByIdAndActiveNull(I id);

    /**
     * Retrives an entity by its id if its not soft deleted.
     * 
     * @param id must not be {@literal null}
     * @return the entity with the given id.
     * @throws IllegalArgumentException if {@literal id} is {@literal null}.
     * @throws ObjectNotFoundException  if entity with given id is not found or is
     *                                  soft deleted.
     */
    @Override
    default T findByIdOrThrow(I id) {
        return findByIdAndActiveNull(id).orElseThrow(ObjectNotFoundException::new);
    }
}
