//package com.dseme.app.configurations;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.JavaMailSenderImpl;
//
//import java.util.Properties;
//
//@Configuration
//public class MailConfig {
//
//    @Value("${MAIL_HOST}")
//    private String host;
//
//    @Value("${MAIL_PORT}")
//    private int port;
//
//    @Value("${MAIL_USERNAME}")
//    private String username;
//
//    @Value("${MAIL_PASSWORD}")
//    private String password;
//
//    @Value("${MAIL_TLS}")
//    private boolean tls;
//
//    @Bean
//    public JavaMailSender javaMailSender() {
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setHost(host);
//        mailSender.setPort(port);
//        mailSender.setUsername(username);
//        mailSender.setPassword(password);
//
//        Properties props = mailSender.getJavaMailProperties();
//        props.put("mail.transport.protocol", "smtp");
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", String.valueOf(tls));
//        props.put("mail.debug", "true");
//
//        return mailSender;
//    }
//}
