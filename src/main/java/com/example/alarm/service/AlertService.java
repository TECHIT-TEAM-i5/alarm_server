package com.example.alarm.service;

import jakarta.mail.MessagingException;

public interface AlertService {
    void sendMail(String rabbitMessage) throws MessagingException;
    void sendAuthMail(String authMessage) throws MessagingException;
}
