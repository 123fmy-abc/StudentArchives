package com.example.studentarchives.repository.projection;

public interface UserAuthStatus {

    Integer getStatus();

    Integer getTokenVersion();

    Integer getRefreshTokenVersion();
}
