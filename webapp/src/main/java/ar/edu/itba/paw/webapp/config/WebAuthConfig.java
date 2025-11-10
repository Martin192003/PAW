package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.webapp.auth.CustomAuthenticationFailureHandler;
import ar.edu.itba.paw.webapp.auth.PawUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableWebSecurity
@ComponentScan("ar.edu.itba.paw.webapp.auth")
public class WebAuthConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private PawUserDetailsService userDetailsService;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Value("${security.rememberme.key}")
    private String rememberMeKey;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.userDetailsService(userDetailsService)
                .sessionManagement()
                .invalidSessionUrl("/clubs")
                .and().authorizeRequests()

                // Authenticated routes
                .antMatchers("/clubs/new", "/clubs/create", "/reservations/**", "/reviews/**").authenticated()
                .antMatchers("/admin/**").hasRole("ADMIN")

                // Public routes
                .antMatchers("/", "/clubs", "/clubs/{id}", "/contact", "/login", "/register", "/photos/**", "/verify", "/unverified", "/resend-verification", "/forgot-password", "/reset-password","/verifyMailNotice", "/verify-account").permitAll()                .antMatchers(HttpMethod.GET, "/clubs").permitAll()
                //.antMatchers(HttpMethod.GET, "/clubs").permitAll()
                .antMatchers(HttpMethod.GET, "/clubs/{id}").permitAll()
                .antMatchers(HttpMethod.GET, "/clubs/**/layout/**").permitAll()

                .anyRequest().authenticated()
                .and().formLogin()
                .usernameParameter("usernameOrEmail")
                .passwordParameter("password")
                .defaultSuccessUrl("/clubs", false)
                .failureHandler(customAuthenticationFailureHandler)
                .loginPage("/login")
                .and().rememberMe()
                .rememberMeParameter("j_rememberme")
                .userDetailsService(userDetailsService)
                .key(rememberMeKey)
                .tokenValiditySeconds((int) TimeUnit.DAYS.toSeconds(30))
                .and().logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/clubs")
                .and().exceptionHandling()
                .accessDeniedPage("/403")
                .and().csrf().disable();
    }
    @Override
    public void configure(final WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers("/resources/**", "/js/**", "/img/**", "/favicon.ico", "/403");
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}