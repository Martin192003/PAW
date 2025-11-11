package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Component
public class PawUserDetailsService implements UserDetailsService {
    @Autowired
    private UserService us;

    @Override
    public UserDetails loadUserByUsername(final String usernameOrEmail) throws UsernameNotFoundException {
        User user = us.findByUserName(usernameOrEmail);
        if (user == null) {
            user = us.findByEmail(usernameOrEmail);
            if (user == null) {
                throw new UsernameNotFoundException("No user by the name " + usernameOrEmail);
            }
        }

        if (!user.isVerified()) {
            throw new UsernameNotFoundException("User is not verified");
        }

        return new PawUserDetails(user, usernameOrEmail);    }
}
