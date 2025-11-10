package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class PawUserDetails implements UserDetails {
    private final User user;
    private final String loginIdentifier;

    public PawUserDetails(User user, String loginIdentifier) {
        this.user = user;
        this.loginIdentifier = loginIdentifier;
    }

    public User getUser() {
        return user;
    }

    public UUID getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                (GrantedAuthority) () -> "ROLE_" + user.getRole().name()
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return loginIdentifier;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isVerified();
    }
}