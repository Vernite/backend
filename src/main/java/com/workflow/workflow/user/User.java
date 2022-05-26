package com.workflow.workflow.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.utils.SoftDeleteEntity;
import com.workflow.workflow.workspace.Workspace;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;

@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

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

    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(cascade = { CascadeType.PERSIST }, optional = false)
    private CounterSequence counterSequence;

    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @Where(clause = "active is null")
    @OrderBy("name, id")
    private List<Workspace> workspaces = new ArrayList<>();

    public User() {
    }

    public User(String name, String surname, String username, String email, String password) {
        this.setName(name);
        this.setSurname(surname);
        this.setUsername(username);
        this.setEmail(email);
        this.setPassword(password);
        this.setCounterSequence(new CounterSequence());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
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
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUsername() {
        return username;
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

    public CounterSequence getCounterSequence() {
        return counterSequence;
    }

    public void setCounterSequence(CounterSequence counterSequence) {
        this.counterSequence = counterSequence;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) // TODO: use more fields?
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (getId() != other.getId())
            return false;
        if (getUsername() == null)
            return other.getUsername() == null;
        return getUsername().equals(other.getUsername());
    }
}
