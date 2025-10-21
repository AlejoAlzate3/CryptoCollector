package com.cryptoCollector.apiGateWay.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupValidator implements ApplicationRunner {

    private final Environment env;

    public StartupValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String[] activeProfiles = env.getActiveProfiles();
        boolean isProd = false;
        for (String p : activeProfiles) {
            if (p != null && p.equalsIgnoreCase("prod")) {
                isProd = true;
                break;
            }
        }

        if (isProd) {
            String jwtSecret = env.getProperty("JWT_SECRET");
            if (jwtSecret == null || jwtSecret.isBlank()) {
                throw new IllegalStateException("JWT_SECRET must be provided as an environment variable when running in 'prod' profile");
            }
        }
    }
}
