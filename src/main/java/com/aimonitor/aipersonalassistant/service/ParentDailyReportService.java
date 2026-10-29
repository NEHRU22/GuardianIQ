package com.aimonitor.aipersonalassistant.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aimonitor.aipersonalassistant.entity.ChatMessage;
import com.aimonitor.aipersonalassistant.entity.User;
import com.aimonitor.aipersonalassistant.repository.ChatMessageRepository;
import com.aimonitor.aipersonalassistant.repository.UserRepository;

@Service
public class ParentDailyReportService {

    private static final Logger log =
            LoggerFactory.getLogger(ParentDailyReportService.class);

    private static final ZoneId REPORT_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final JavaMailSender mailSender;
    private final String senderEmail;

    public ParentDailyReportService(
            UserRepository userRepository,
            ChatMessageRepository chatMessageRepository,
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail) {

        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    /**
     * Runs every day at 9:00 PM India Standard Time.
     */
    @Scheduled(
            cron = "0 21 20 * * *",
            zone = "Asia/Kolkata"
    )
    public void sendDailyParentReports() {

        LocalDate today = LocalDate.now(REPORT_ZONE);
        LocalDateTime startTime = today.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        List<User> eligibleUsers =
                userRepository
                        .findAllByParentReportsConsentTrueAndParentEmailIsNotNull();

        log.info(
                "Starting parent daily reports for {}. Eligible accounts: {}",
                today,
                eligibleUsers.size()
        );

        for (User user : eligibleUsers) {

            String parentEmail = user.getParentEmail();

            if (parentEmail == null || parentEmail.isBlank()) {
                continue;
            }

            try {
                sendReportForUser(
                        user,
                        parentEmail.trim(),
                        today,
                        startTime,
                        endTime
                );
            } catch (MailException exception) {
                // Continue processing other users if one email fails.
                log.error(
                        "Could not send parent report for user id {}",
                        user.getId(),
                        exception
                );
            } catch (RuntimeException exception) {
                log.error(
                        "Could not prepare parent report for user id {}",
                        user.getId(),
                        exception
                );
            }
        }

        log.info("Finished parent daily reports for {}", today);
    }

    private void sendReportForUser(
            User user,
            String parentEmail,
            LocalDate today,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<ChatMessage> prompts =
                chatMessageRepository.findDailyMessages(
                        user.getId(),
                        ChatMessage.Role.USER,
                        startTime,
                        endTime
                );

        SimpleMailMessage email = new SimpleMailMessage();

        email.setFrom(senderEmail);
        email.setTo(parentEmail);
        email.setSubject(
                "Daily AI Assistant Report - " + today
        );
        email.setText(buildEmailBody(user, today, prompts));

        mailSender.send(email);

        log.info(
                "Sent parent daily report for user id {}. Prompt count: {}",
                user.getId(),
                prompts.size()
        );
    }

    private String buildEmailBody(
            User user,
            LocalDate today,
            List<ChatMessage> prompts) {

        StringBuilder body = new StringBuilder();

        body.append("Daily AI Assistant Report\n");
        body.append("=========================\n\n");

        body.append("Date: ").append(today).append('\n');
        body.append("User: ").append(user.getFullName()).append("\n\n");

        if (prompts.isEmpty()) {
            body.append("No conversation today.\n");
        } else {
            body.append("Prompts submitted to the chatbot today:\n\n");

            for (int i = 0; i < prompts.size(); i++) {
                body.append(i + 1)
                        .append(". ")
                        .append(prompts.get(i).getContent())
                        .append("\n\n");
            }
        }

        body.append("-------------------------\n");
        body.append(
                "This report contains prompts submitted by the user. "
                + "AI responses are not included.\n"
        );

        return body.toString();
    }
}