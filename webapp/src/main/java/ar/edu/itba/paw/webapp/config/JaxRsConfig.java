package ar.edu.itba.paw.webapp.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/")
public class JaxRsConfig extends ResourceConfig {
    public JaxRsConfig() {
        packages("ar.edu.itba.paw.webapp.controller");
        register(MoxyJsonFeature.class);
    }
}
