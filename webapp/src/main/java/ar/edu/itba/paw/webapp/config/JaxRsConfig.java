package ar.edu.itba.paw.webapp.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/api")
@ComponentScan("ar.edu.itba.paw.webapp.rest")
public class JaxRsConfig extends ResourceConfig {
    public JaxRsConfig() {
        packages("ar.edu.itba.paw.webapp.rest");
        register(MoxyJsonFeature.class);
        property("eclipselink.json.date-format", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }
}
