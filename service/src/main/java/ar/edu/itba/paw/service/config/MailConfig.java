package ar.edu.itba.paw.service.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class MailConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailConfig.class);

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    @Bean
    public JavaMailSender mailSender() {
        Properties cp = loadClasspathProps("mail.properties"); // optional

        String user = pick("SMTP_USER", cp);
        String pass = pick("SMTP_PASS", cp);
        String host = orEmpty(pick("SMTP_HOST", cp));
        String portStr = orEmpty(pick("SMTP_PORT", cp));
        String debug = orEmpty(pick("MAIL_DEBUG", cp));
        String optional = orEmpty(pick("MAIL_OPTIONAL", cp)); // MAIL_OPTIONAL=true to not fail

        if (host.isBlank()) host = "smtp.gmail.com";
        int port = parseInt(portStr, 587);
        boolean credsPresent = !user.isBlank() && !pass.isBlank();

        if (!credsPresent && !"true".equalsIgnoreCase(optional)) {
            throw new IllegalStateException("Missing SMTP_USER / SMTP_PASS (cannot send email)");
        }
        if (!credsPresent) {
            LOGGER.warn("Mail disabled: credentials absent and MAIL_OPTIONAL=true");
        }

        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(port);
        if (credsPresent) {
            impl.setUsername(user);
            impl.setPassword(pass);
        }

        Properties p = impl.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", String.valueOf(credsPresent));
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.starttls.required", String.valueOf("smtp.gmail.com".equalsIgnoreCase(host)));
        p.put("mail.smtp.ssl.trust", host);
        p.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.timeout", "10000");
        p.put("mail.smtp.writetimeout", "10000");
        p.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");
        p.put("mail.debug", debug.isBlank() ? "false" : debug);

        LOGGER.info("SMTP config host={} port={} user={} auth={} starttls=true debug={} optional={}",
                host, port, user, credsPresent, p.getProperty("mail.debug"), optional);

        if (credsPresent) {
            try {
                impl.testConnection();
                LOGGER.info("SMTP connection test OK");
            } catch (MessagingException e) {
                LOGGER.error("SMTP connection test FAILED: {}", e.getMessage());
            }
        }
        return impl;
    }

    private String pick(String key, Properties cp) {
        String v = System.getenv(key);
        if (isBlank(v)) v = System.getProperty(key);
        if (isBlank(v)) v = DOTENV.get(key);
        if (isBlank(v) && cp != null) v = cp.getProperty(key);
        return v == null ? "" : v.trim();
    }

    private Properties loadClasspathProps(String name) {
        Properties props = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            if (in != null) {
                props.load(in);
                LOGGER.info("Loaded classpath {}", name);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not load {}: {}", name, e.getMessage());
        }
        return props;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private String orEmpty(String s) {
        return s == null ? "" : s;
    }
    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}