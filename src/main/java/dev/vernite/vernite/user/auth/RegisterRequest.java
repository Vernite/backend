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

package dev.vernite.vernite.user.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Class containing information needed to create new user entity.
 * Has required constraints annotated using Java Bean Validation.
 */
@Data
public class RegisterRequest {

    /**
     * Email for new user. Must be valid email.
     */
    @NotBlank(message = "email must be specified")
    @Size(max = 320, message = "email must be shorter than 320 characters")
    private String email;

    /**
     * Password for new user. Must be at least 8 characters long.
     */
    @NotBlank(message = "password must be specified")
    @Size(min = 8, max = 100, message = "password must be at least 8 characters long and shorter than 100 characters")
    private String password;

    /**
     * Name for new user. Must be at least 2 characters long.
     */
    @NotBlank(message = "name must be specified")
    @Size(min = 2, max = 100, message = "name must be at least 2 characters long and shorter than 100 characters")
    private String name;

    /**
     * Surname for new user. Must be at least 2 characters long.
     */
    @NotBlank(message = "surname must be specified")
    @Size(min = 2, max = 100, message = "surname must be at least 2 characters long and shorter than 100 characters")
    private String surname;

    /**
     * Username for new user. Must be at least 2 characters long.
     */
    @NotBlank(message = "username must be specified")
    @Size(min = 1, max = 100, message = "username must be at least 2 characters long and shorter than 100 characters")
    private String username;

    /**
     * Language for new user.
     */
    @Size(max = 5, message = "language must be shorter than 5 characters")
    private String language;

    /**
     * Date format for new user.
     */
    @Size(max = 10, message = "date format must be shorter than 10 characters")
    private String dateFormat;

    /**
     * Solving of captcha for new user.
     */
    @NotBlank(message = "captcha must be specified")
    @Size(max = 10000, message = "captcha must be shorter than 10000 characters")
    private String captcha;
}
