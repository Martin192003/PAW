package ar.edu.itba.paw.webapp.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
public class DotenvConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer dotenvPropertySources() {
        Properties props = new Properties();

        // 1. Intentamos con system property dotenv.dir (útil para definir en IntelliJ/Tomcat VM options)
        String configured = System.getProperty("dotenv.dir");

        // 2. Tomcat catalina.base/conf
        String catalinaBase = System.getProperty("catalina.base");

        String userDir = System.getProperty("user.dir");

        String[] dirs = new String[] {
                configured,
                catalinaBase == null ? null : catalinaBase + "/conf",
                userDir,
                "."
        };

        for (String d : dirs) {
            if (d == null) continue;
            Dotenv env = Dotenv.configure()
                    .directory(d)
                    .filename(".env")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            env.entries().forEach(e -> props.put(e.getKey(), e.getValue()));

            if (!props.isEmpty()) {
                break;
            }
        }

        props.forEach((k,v) -> System.setProperty(k.toString(), v.toString()));

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(props);
        configurer.setIgnoreUnresolvablePlaceholders(false); // así si falta algo tira error y no queda ${DB_USER}
        return configurer;
    }
}
