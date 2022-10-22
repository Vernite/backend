/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
     * Retrieves an entity by its id if its not soft deleted.
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
