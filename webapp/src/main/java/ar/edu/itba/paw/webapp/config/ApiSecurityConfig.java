package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.auth.ApiBasicAuthenticationFilter;
import ar.edu.itba.paw.webapp.auth.JwtAuthenticationFilter;
import ar.edu.itba.paw.webapp.auth.ApiAccessDeniedHandler;
import ar.edu.itba.paw.webapp.auth.ApiAuthenticationEntryPoint;
import ar.edu.itba.paw.webapp.auth.PawUserDetailsService;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@Order(1)
@EnableWebSecurity
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private PawUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/api/**")
                .cors().and()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .sessionManagement().sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS).and()
                .exceptionHandling()
                    .authenticationEntryPoint(new ApiAuthenticationEntryPoint())
                    .accessDeniedHandler(new ApiAccessDeniedHandler())
                    .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .antMatchers("/api/photos/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/clubs").permitAll()
                .antMatchers(HttpMethod.GET, "/api/events").permitAll()
                .antMatchers(HttpMethod.POST, "/api/clubs").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.GET, "/api/clubs/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/clubs/**/cover").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/clubs/**/cover").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/clubs/**/layout").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/clubs/**/photos").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/clubs/**/photos/**").hasAnyRole("USER", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/users").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/tokens").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/challenges").permitAll()
                .anyRequest().authenticated().and()
                .addFilterBefore(apiBasicAuthenticationFilter(), AnonymousAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), AnonymousAuthenticationFilter.class);
    }

    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
    }

    public ApiBasicAuthenticationFilter apiBasicAuthenticationFilter() {
        return new ApiBasicAuthenticationFilter(userService);
    }
}
