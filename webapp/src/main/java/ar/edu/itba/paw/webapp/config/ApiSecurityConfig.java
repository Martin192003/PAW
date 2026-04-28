package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.webapp.auth.JwtAuthenticationFilter;
import ar.edu.itba.paw.webapp.auth.PawUserDetailsService;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Order(1)
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private PawUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/api/**")
                .cors().and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .antMatchers("/api/photos/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/clubs").permitAll()
                .antMatchers(HttpMethod.GET, "/api/clubs/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/clubs/**/cover").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/clubs/**/cover").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/clubs/**/layout").permitAll()
                .antMatchers(HttpMethod.POST, "/api/clubs/**/photos").permitAll()
                .antMatchers(HttpMethod.DELETE, "/api/clubs/**/photos/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/users").permitAll()
                .antMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                .antMatchers(HttpMethod.POST, "/api/users/verification-requests").permitAll()
                .antMatchers(HttpMethod.GET, "/api/users/verifications").permitAll()
                .antMatchers(HttpMethod.POST, "/api/users/verifications").permitAll()
                .antMatchers(HttpMethod.POST, "/api/users/password-reset-requests").permitAll()
                .antMatchers(HttpMethod.POST, "/api/users/password-resets").permitAll()
                .anyRequest().authenticated().and()
                .addFilterBefore(jwtAuthenticationFilter(), AnonymousAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }
}
