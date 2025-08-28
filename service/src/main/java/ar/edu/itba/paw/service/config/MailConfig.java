package ar.edu.itba.paw.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Properties;

@Configuration
@EnableAsync
public class MailConfig {

    @Bean(name = "mailExecutor")
    public TaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("mail-");
        ex.initialize();
        return ex;
    }

    @Bean
    public JavaMailSender mailSender() {
        String host = System.getenv().getOrDefault("SMTP_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "1025"));
        String user = System.getenv().getOrDefault("SMTP_USER", "");
        String pass = System.getenv().getOrDefault("SMTP_PASS", "");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(user);
        sender.setPassword(pass);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        if ("smtp.gmail.com".equalsIgnoreCase(host)) {
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        }
        props.put("mail.debug", "false");
        return sender;
    }

    @Bean
    public TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
