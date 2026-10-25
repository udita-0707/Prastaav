package com.prastav.auth;

import com.prastav.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A person who can log in. The role is fixed on the account and copied into the JWT.
 * Students also carry the admission year and branch parsed from their institutional email.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "admission_year")
    private Integer admissionYear;

    @Column(length = 20)
    private String branch;

    protected User() {
    }

    public User(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /** A STUDENT account whose email, admission year and branch come from {@code studentEmail}. */
    public static User student(String name, StudentEmail studentEmail, String passwordHash) {
        User user = new User(name, studentEmail.email(), passwordHash, Role.STUDENT);
        user.admissionYear = studentEmail.admissionYear();
        user.branch = studentEmail.branch();
        return user;
    }

    public Actor toActor() {
        return new Actor(getId(), role);
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public String getBranch() {
        return branch;
    }
}
