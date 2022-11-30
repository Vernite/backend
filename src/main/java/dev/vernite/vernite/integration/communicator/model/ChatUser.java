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

package dev.vernite.vernite.integration.communicator.model;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Model representing a chat user.
 */
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class ChatUser {

    /**
     * ID of the user.
     */
    @Setter
    @Getter
    @NotBlank
    private String id;

    /**
     * Team associated with the user. Might be null.
     */
    @Setter
    @Getter
    private String team;

    /**
     * Name of the user.
     */
    @Setter
    @Getter
    @NotBlank
    private String name;

    /**
     * Display name of the user.
     */
    @Setter
    @Getter
    @NotBlank
    private String displayName;

    /**
     * Whether the user is a bot.
     */
    @Setter
    @Getter
    private boolean bot;

    /**
     * Link to the user's avatar.
     */
    @Setter
    @Getter
    @NotBlank
    private String avatar;

    /**
     * Service the user is associated with.
     */
    @Setter
    @Getter
    @NotBlank
    private String provider;

}
