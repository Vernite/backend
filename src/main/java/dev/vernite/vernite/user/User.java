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

package dev.vernite.vernite.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.vernite.vernite.common.utils.counter.CounterSequence;
import dev.vernite.vernite.workspace.Workspace;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Date deleted;

    private boolean deletedPermanently;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 32)
    @NotNull
    private byte[] hash;

    @JsonIgnore
    @Column(nullable = false, length = 20)
    private byte[] salt;

    private String avatar;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String username;

    private String language;
    private String dateFormat;

    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(cascade = { CascadeType.PERSIST }, optional = false)
    private CounterSequence counterSequence;

    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @OrderBy("name, id")
    private List<Workspace> workspaces = new ArrayList<>();

    public User() {
    }

    public User(String name, String surname, String username, String email, String password) {
        this(name, surname, username, email, password, null, null);
    }

    public User(String name, String surname, String username, String email, String password, String language,
            String dateFormat) {
        this.setName(name);
        this.setSurname(surname);
        this.setUsername(username);
        this.setEmail(email);
        this.setPassword(password);
        this.setLanguage(language);
        this.setDateFormat(dateFormat);
        this.setCounterSequence(new CounterSequence());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return getDeleted() != null;
    }

    public Date getDeleted() {
        return deleted;
    }

    public void setDeleted(Date deleted) {
        this.deleted = deleted;
    }

    public boolean isDeletedPermanently() {
        return deletedPermanently;
    }

    public void setDeletedPermanently(boolean deletedPermanently) {
        this.deletedPermanently = deletedPermanently;
    }

    public String getEmail() {
        return isDeleted() ? "(deleted)" : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte[] hash) {
        this.hash = hash;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public String getAvatar() {
        return isDeleted() ? null : avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getName() {
        return isDeleted() ? "(deleted)" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return isDeleted() ? "(deleted)" : surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUsername() {
        return isDeleted() ? "(deleted)" : username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Workspace> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(List<Workspace> workspaces) {
        this.workspaces = workspaces;
    }

    public boolean checkPassword(String password) {
        byte[] hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            digest.update(this.getSalt());
            hash = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return MessageDigest.isEqual(hash, this.getHash());
    }

    public void setPassword(String password) {
        byte[] salt = new byte[20];
        new SecureRandom().nextBytes(salt);
        byte[] hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            digest.update(salt);
            hash = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.setHash(hash);
        this.setSalt(salt);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public CounterSequence getCounterSequence() {
        return counterSequence;
    }

    public void setCounterSequence(CounterSequence counterSequence) {
        this.counterSequence = counterSequence;
    }

    @Override
    public String toString() {
        return "U[" + this.getUsername() + "#" + this.getId() + "]";
    }

    // TODO: lombok auto generate
    @Override
    public int hashCode() {
        // final int prime = 31;
        // int result = 1;
        // result = prime * result + Arrays.hashCode(getHash());
        // result = prime * result + Arrays.hashCode(getSalt());
        // result = prime * result + Objects.hash(getAvatar(), getCounterSequence(), getEmail(), getId(), getName(), getSurname(), getUsername(), getWorkspaces(), getDeleted(), getLanguage(), getDateFormat());
        return (int) getId();
    }

    // TODO: lombok auto generate
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof User))
            return false;
        User other = (User) obj;
        return getId() == other.getId();
        // return Objects.equals(getAvatar(), other.getAvatar()) && Objects.equals(getCounterSequence(), other.getCounterSequence())
        //         && Objects.equals(getEmail(), other.getEmail()) && Arrays.equals(getHash(), other.getHash()) && getId() == other.getId()
        //         && Objects.equals(getName(), other.getName()) && Arrays.equals(getSalt(), other.getSalt())
        //         && Objects.equals(getSurname(), other.getSurname()) && Objects.equals(getUsername(), other.getUsername())
        //         && Objects.equals(getWorkspaces(), other.getWorkspaces()) && Objects.equals(getDeleted(), other.getDeleted())
        //         && Objects.equals(getLanguage(), other.getLanguage()) && Objects.equals(getDateFormat(), other.getDateFormat());
    }
}
