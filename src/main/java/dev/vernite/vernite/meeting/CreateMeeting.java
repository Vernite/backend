/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2023, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
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

package dev.vernite.vernite.meeting;

import java.util.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class containing information needed to create new meeting entity.
 * Has required constraints annotated using Java Bean Validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeeting {

    /**
     * Name for new meeting. Must contain at least one non-whitespace character.
     */
    @Size(min = 1, max = 50, message = "meeting name must be shorter than 50 characters")
    @NotBlank(message = "meeting name must contain at least one non-whitespace character")
    private String name;

    /**
     * Description for new meeting.
     */
    @NotNull(message = "meeting description must not be null")
    @Size(max = 1000, message = "meeting description must be shorter than 1000 characters")
    private String description;

    /**
     * Location for new meeting.
     */
    @Size(max = 1000, message = "meeting location must be shorter than 1000 characters")
    private String location;

    /**
     * Start date for new meeting.
     */
    @NotNull(message = "meeting start date must not be null")
    private Date startDate;

    /**
     * End date for new meeting.
     */
    @NotNull(message = "meeting end date must not be null")
    private Date endDate;

    /**
     * List of participants for new meeting.
     */
    private List<Long> participantIds;

}
