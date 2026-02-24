package com.safewatch.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;

//@Component for debug purposes only
@RequiredArgsConstructor
public class MailConfigDebug {
    private final org.springframework.core.env.Environment env;
    @Autowired
    JavaMailSender mailSender;

    @PostConstruct
    void logMailConfig() {
        System.out.println("MAIL host=" + env.getProperty("spring.mail.host"));
        System.out.println("MAIL port=" + env.getProperty("spring.mail.port"));
        System.out.println("MAIL user=" + env.getProperty("spring.mail.username"));
    }

    @PostConstruct
    public void checkMailSender() {
        System.out.println("MAIL SENDER CLASS = " + mailSender.getClass());
        if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
            System.out.println("MAIL HOST = " + impl.getHost());
            System.out.println("MAIL PORT = " + impl.getPort());
            System.out.println("MAIL USER = " + impl.getUsername());
        }
    }
}
