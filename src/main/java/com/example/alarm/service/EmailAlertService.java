package com.example.alarm.service;

import com.example.alarm.dto.AlertDto;
import com.example.alarm.dto.UserMailDto;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAlertService implements AlertService {
    private JavaMailSenderImpl mailSender;
    private final SpringTemplateEngine templateEngine;


    // RabbitMQ 사용
    private final Gson gson;
    private static final String EMAIL_TITLE_PREFIX = "[STAGE ALARM] 알림 : 새 공연이 등록되었습니다. ";

    private Session session;
    private Transport transport;
    @PostConstruct
    private void initializeMailSender() throws MessagingException {
        mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("youshin.dev@gmail.com");
        mailSender.setPassword("tdin xdoe suwi fbok");

        Properties props = mailSender.getJavaMailProperties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", 587);
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.connectiontimeout", 10000);
        props.put("mail.smtp.timeout", 10000);
        // SMTP Keep-Alive 활성화
        props.put("mail.smtp.keepalive", true); // Keep-Alive 활성화




        session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailSender.getUsername(), mailSender.getPassword());
            }
        });
        transport = session.getTransport("smtp");
    }

    @RabbitListener(queues = "boot.amqp.alarm2-queue")
    @Retryable(value = MessagingException.class, maxAttempts = 3, backoff = @Backoff(delay = 10000))
    @Async("threadPoolTaskExecutor")
    public void sendMail(String rabbitMessage) throws MessagingException {
        try {
            log.info("===== email sending start =====");
            if (!transport.isConnected()) {
                transport.connect();
            }
            AlertDto alertDto = gson.fromJson(rabbitMessage, AlertDto.class);
            log.info(alertDto.toString());

            sendMail(alertDto);

        } finally {
            if (rabbitMessage == null || rabbitMessage.isEmpty()) {
                // 메시지 큐가 비어있으면 연결을 닫습니다.
                transport.close();
            }
        }
    }



    public void sendMail(AlertDto alertDto) throws MessagingException {


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

        transport.sendMessage(message, message.getAllRecipients());
        log.info("===== email sending end =====");
    }

    @Override
    @RabbitListener(queues = "boot.amqp.auth2-queue")
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
