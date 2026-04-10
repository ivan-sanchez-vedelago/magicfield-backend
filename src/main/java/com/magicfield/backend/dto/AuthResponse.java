package com.magicfield.backend.dto;

public class AuthResponse {

    private String id;
    private String email;
    private String name;
    private String lastName;
    private Long phone;
    private String token;

    public AuthResponse() {}

    public AuthResponse(String id, String email, String name, String lastName, Long phone, String token) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.lastName = lastName;
        this.phone = phone;
        this.token = token;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getPhone() {
        return phone;
    }

    public void setPhone(Long phone) {
        this.phone = phone;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
