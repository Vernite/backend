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

package dev.vernite.vernite.status;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.vernite.vernite.common.constraints.NullOrNotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Class containing information needed to update status entity.
 * Has required constraints annotated using Java Bean Validation.
 * It performs partial update using only present fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatus {

    /**
     * New name for status. Must contain at least one non-whitespace character.
     */
    @Size(min = 1, max = 50, message = "status name must be shorter than 50 characters")
    @NullOrNotBlank(message = "status name must contain at least one non-whitespace character")
    private String name;

    /**
     * New color for status. Must be a non negative number.
     */
    @PositiveOrZero(message = "status color must be a non negative number")
    private Integer color;

    /**
     * New order for status. Must be a non negative number.
     */
    @PositiveOrZero(message = "status order must be a non negative number")
    private Integer ordinal;

    /**
     * Flag indicating if status is begin status. Must be a boolean value.
     */
    private Boolean begin;

    /**
     * Flag indicating if status is final status. Must be a boolean value.
     */
    @JsonProperty("final")
    private Boolean isFinal;

}
