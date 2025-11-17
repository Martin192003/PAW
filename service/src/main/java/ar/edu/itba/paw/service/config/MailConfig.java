package ar.edu.itba.paw.service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@PropertySource("classpath:mail.properties")
public class MailConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailConfig.class);

    @Value("${SMTP_HOST:smtp.gmail.com}")
    private String host;

    @Value("${SMTP_PORT:587}")
    private int port;

    @Value("${SMTP_USER:}")
    private String username;

    @Value("${SMTP_PASS:}")
    private String password;

    @Value("${MAIL_DEBUG:false}")
    private boolean debug;

    @Value("${MAIL_OPTIONAL:false}")
    private boolean optional;

    @Value("${MAIL_TEST_CONNECTION:false}")
    private boolean testConnection;

    @Bean
    public JavaMailSender mailSender() {
        boolean credsPresent = !isBlank(username) && !isBlank(password);

        if (!credsPresent && !optional) {
            throw new IllegalStateException("Missing SMTP_USER / SMTP_PASS (cannot send email)");
        }
        if (!credsPresent) {
            LOGGER.warn("Mail disabled: credentials absent and MAIL_OPTIONAL=true");
        }

        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        impl.setHost(host);
        impl.setPort(port);
        if (credsPresent) {
            impl.setUsername(username);
            impl.setPassword(password);
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
        p.put("mail.debug", String.valueOf(debug));

        LOGGER.info("SMTP config host={} port={} user={} auth={} starttls=true debug={} optional={}",
                host, port, username, credsPresent, debug, optional);

        if (credsPresent && testConnection) {
            try {
                impl.testConnection();
                LOGGER.info("SMTP connection test OK");
            } catch (MessagingException e) {
                LOGGER.error("SMTP connection test FAILED: {}", e.getMessage());
            }
        }
        return impl;
    }

    @Bean("mailMessageSource")
    public MessageSource mailMessageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:mail_messages");
        ms.setDefaultEncoding(StandardCharsets.UTF_8.displayName());
        ms.setCacheSeconds(5);
        return ms;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
