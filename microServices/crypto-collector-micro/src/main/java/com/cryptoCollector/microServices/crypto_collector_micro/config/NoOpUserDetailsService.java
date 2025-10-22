package com.cryptoCollector.microServices.crypto_collector_micro.config;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsService dummy para evitar que Spring Security genere uno automático.
 * Este microservicio usa JWT para autenticación, no carga usuarios de una base de datos.
 */
@Service
public class NoOpUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("This service uses JWT authentication only");
    }
}
