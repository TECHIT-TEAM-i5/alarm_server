package com.example.alarm.service;

import com.example.alarm.dto.AlertDto;
import com.example.alarm.dto.UserMailDto;
import com.google.gson.Gson;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAlertService implements AlertService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;


    // RabbitMQ 사용
    private final Gson gson;
    private static final String EMAIL_TITLE_PREFIX = "[STAGE ALARM] 알림 : 새 공연이 등록되었습니다. ";


    @Override
    @RabbitListener(queues = "boot.amqp.alarm-queue")
    @Async("threadPoolTaskExecutor")
    public void sendMail(String rabbitMessage) throws MessagingException {
        log.info("===== email sending start =====");
        AlertDto alertDto = gson.fromJson(rabbitMessage, AlertDto.class);
        log.info(alertDto.toString());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setSubject(EMAIL_TITLE_PREFIX + alertDto.getTitle()); //제목
        helper.setFrom("stage alarm <noreply@stagealarm.com>");
        helper.setTo(alertDto.getEmail());
        HashMap<String, String> emailValues = new HashMap<>();
        emailValues.put("content", alertDto.getMessage());
        String text = setContext(emailValues);
        helper.setText(text, true);
        helper.addInline("logo", new ClassPathResource("static/images/logo.png"));
        helper.addInline("notice-icon", new ClassPathResource("static/images/image-1.png"));

        mailSender.send(message);
        log.info("===== email sending end =====");
    }

    @Override
    @RabbitListener(queues = "boot.amqp.auth-queue")
    @Async("threadPoolTaskExecutor")
    public void sendAuthMail(String authMessage) throws MessagingException {
        log.info("===== auth email sending start =====" );
        UserMailDto userMailDto = gson.fromJson(authMessage, UserMailDto.class);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom("noreply@stagealarm.com");
        helper.setTo(userMailDto.getEmail());
        helper.setSubject(userMailDto.getSubject());
        helper.setText(userMailDto.getText());

        mailSender.send(message);
        log.info("===== auth email sending end =====");
    }


    private String setContext(Map<String, String> emailValues) {
        Context context = new Context();
        emailValues.forEach(context::setVariable);
        return templateEngine.process("email/index.html", context);
    }
}
