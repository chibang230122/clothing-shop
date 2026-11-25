package com.example.clothingshop.service;

import com.example.clothingshop.entity.User;
import com.example.clothingshop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration VALIDITY = Duration.ofMinutes(10);
    private static final Duration RESEND_DELAY = Duration.ofMinutes(1);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_PENDING = 2_000;

    private final Map<String, Challenge> challenges = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mail;
    private final TaskExecutor executor;
    private final TransactionTemplate transaction;
    private final SessionRegistry sessions;
    private final String host;
    private final String from;
    private final boolean smtpAuth;
    private final String smtpUsername;
    private final String smtpPassword;

    public PasswordResetService(UserRepository users, PasswordEncoder encoder,
                                ObjectProvider<JavaMailSender> mail,
                                @Qualifier("passwordResetExecutor") TaskExecutor executor,
                                TransactionTemplate transaction, SessionRegistry sessions,
                                @Value("${spring.mail.host:}") String host,
                                @Value("${app.password-reset.from:}") String from,
                                @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
                                @Value("${spring.mail.username:}") String smtpUsername,
                                @Value("${spring.mail.password:}") String smtpPassword) {
        this.users = users;
        this.encoder = encoder;
        this.mail = mail;
        this.executor = executor;
        this.transaction = transaction;
        this.sessions = sessions;
        this.host = host;
        this.from = from;
        this.smtpAuth = smtpAuth;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
    }

    public boolean isAvailable() {
        return !host.isBlank() && !from.isBlank()
                && (!smtpAuth || (!smtpUsername.isBlank() && !smtpPassword.isBlank()))
                && mail.getIfAvailable() != null;
    }

    public synchronized void requestCode(String email, String sessionId) {
        Instant now = Instant.now();
        challenges.values().removeIf(challenge -> !now.isBefore(challenge.expiresAt));
        Challenge previous = challenges.get(email);
        if (previous != null && now.isBefore(previous.createdAt.plus(RESEND_DELAY))) {
            return;
        }
        if (previous == null && challenges.size() >= MAX_PENDING) {
            return;
        }

        Challenge challenge = new Challenge(sessionId, now);
        challenges.put(email, challenge);
        try {
            executor.execute(() -> prepareAndSend(email, challenge));
        } catch (TaskRejectedException ex) {
            challenges.remove(email, challenge);
        }
    }

    private void prepareAndSend(String email, Challenge challenge) {
        User user = users.findByEmailIgnoreCase(email).filter(PasswordResetService::isActive).orElse(null);
        if (user == null) {
            return;
        }

        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String codeHash = encoder.encode(code);
        synchronized (this) {
            if (challenges.get(email) != challenge || !Instant.now().isBefore(challenge.expiresAt)) {
                return;
            }
            challenge.userId = user.getUserId();
            challenge.oldHash = user.getPasswordHash();
            challenge.codeHash = codeHash;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Clothing Shop - Mã khôi phục mật khẩu");
        message.setText("Mã OTP của bạn là " + code + ". Mã có hiệu lực trong 10 phút. "
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");
        try {
            mail.getObject().send(message);
        } catch (MailException ex) {
            synchronized (this) {
                challenge.codeHash = null;
            }
            log.warn("Không thể gửi OTP qua SMTP ({})", ex.getClass().getSimpleName());
        }
    }

    public boolean resetPassword(String email, String sessionId, String code, String newPassword) {
        PasswordPolicy.require(newPassword);
        Challenge challenge = consumeCode(email, sessionId, code);
        if (challenge == null) {
            return false;
        }

        String newHash = encoder.encode(newPassword);
        Integer updated = transaction.execute(status ->
                users.replacePassword(challenge.userId, challenge.oldHash, newHash));
        if (updated == null || updated != 1) {
            return false;
        }

        for (Object principal : sessions.getAllPrincipals()) {
            if ((principal instanceof UserDetails user && email.equalsIgnoreCase(user.getUsername()))
                    || (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser google
                    && email.equalsIgnoreCase(google.getName()))) {
                sessions.getAllSessions(principal, false).forEach(session -> session.expireNow());
            }
        }
        return true;
    }

    private synchronized Challenge consumeCode(String email, String sessionId, String code) {
        Challenge challenge = challenges.get(email);
        if (challenge == null || !challenge.sessionId.equals(sessionId) || challenge.codeHash == null
                || !Instant.now().isBefore(challenge.expiresAt) || challenge.attempts >= MAX_ATTEMPTS) {
            return null;
        }
        challenge.attempts++;
        if (!encoder.matches(code, challenge.codeHash)) {
            return null;
        }
        challenge.codeHash = null;
        return challenge;
    }

    private static boolean isActive(User user) {
        return Boolean.TRUE.equals(user.getIsActive());
    }

    private static final class Challenge {
        private final String sessionId;
        private final Instant createdAt;
        private final Instant expiresAt;
        private Integer userId;
        private String oldHash;
        private String codeHash;
        private int attempts;

        private Challenge(String sessionId, Instant createdAt) {
            this.sessionId = sessionId;
            this.createdAt = createdAt;
            this.expiresAt = createdAt.plus(VALIDITY);
        }
    }
}
