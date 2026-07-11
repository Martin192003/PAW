package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.webapp.auth.PawUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan(basePackages = "ar.edu.itba.paw.webapp.auth")
@EnableWebSecurity
public class WebAuthConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private PawUserDetailsService userDetailsService;

    /**
     * Orígenes permitidos para CORS. Configurable en application.properties como:
     *   {@code cors.allowed-origins=http://localhost:4200,https://mi-dominio.com}
     * Si se despliega con frontend y backend en el mismo WAR, usualmente no se necesita CORS real.
     */
    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.cors().and()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().permitAll();
    }

    @Override
    public void configure(final WebSecurity web) {
        web.ignoring()
                .antMatchers(
                        "/assets/**",
                        "/resources/**",
                        "/favicon.ico",
                        "/*.css",
                        "/*.js",
                        "/*.ico",
                        "/*.png",
                        "/*.jpg",
                        "/*.jpeg",
                        "/*.svg",
                        "/*.webp",
                        "/*.woff",
                        "/*.woff2",
                        "/*.ttf"
                );
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return bcrypt.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword == null) {
                    return false;
                }

                final String candidate = encodedPassword.trim();
                if (candidate.isEmpty()) {
                    return false;
                }

                if (isBcrypt(candidate)) {
                    return bcrypt.matches(rawPassword, unwrapPrefix(candidate));
                }

                return false;
            }

            @Override
            public boolean upgradeEncoding(String encodedPassword) {
                return false;
            }

            private boolean isBcrypt(String value) {
                return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$") || value.startsWith("$2$") || value.startsWith("{bcrypt}");
            }

            private String unwrapPrefix(String value) {
                return value.startsWith("{bcrypt}") ? value.substring("{bcrypt}".length()) : value;
            }
        };
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        final org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        // Soporta múltiples orígenes separados por coma (leídos desde properties)
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(Arrays.asList("Location", "Link", "X-Total-Count"));

        final org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
