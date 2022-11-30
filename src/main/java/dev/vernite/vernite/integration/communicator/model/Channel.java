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
 * Model representing a chat channel.
 */
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Channel {

    /**
     * ID of the channel.
     */
    @Setter
    @Getter
    @NotBlank
    private String id;

    /**
     * Name of channel. Null if channel is a DM.
     */
    @Setter
    @Getter
    private String name;

    /**
     * ID of user that the channel is a DM with. Null if channel is not a DM.
     */
    @Setter
    @Getter
    private String user;

    /**
     * Whether the channel is a DM. If true channel is not a DM.
     */
    @Setter
    @Getter
    private boolean channel;

    /**
     * ID of team / server the channel is in. Might be null if channel is a DM.
     */
    @Setter
    @Getter
    private String team;

    /**
     * Service the channel is associated with.
     */
    @Setter
    @Getter
    @NotBlank
    private String provider;

}
