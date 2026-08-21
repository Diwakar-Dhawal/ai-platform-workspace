package com.aiservice.platform.chatservice.config;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Manual Flyway configuration for Spring Boot 4.1+.
 * Spring Boot 4.1 removed FlywayAutoConfiguration entirely.
 *
 * This config ensures Flyway migrations run BEFORE Hibernate's
 * EntityManagerFactory is created by adding a "depends-on" relationship.
 */
@Slf4j
@Configuration
public class FlywayConfig implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String[] emfNames = {"entityManagerFactory", "jpaSharedEM_entityManagerFactory"};
        for (String name : emfNames) {
            if (registry.containsBeanDefinition(name)) {
                BeanDefinition bd = registry.getBeanDefinition(name);
                bd.setDependsOn(new String[]{"flywayInitializer"});
                log.info("Added depends-on='flywayInitializer' to bean '{}'", name);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No-op
    }

    @Bean
    public FlywayInitializer flywayInitializer(DataSource dataSource, Environment environment) {
        boolean flywayEnabled = environment.getProperty("spring.flyway.enabled", Boolean.class, true);
        if (!flywayEnabled) {
            log.info("Flyway disabled via spring.flyway.enabled=false — skipping migrations");
            return new FlywayInitializer();
        }
        return new FlywayInitializer(dataSource);
    }

    static class FlywayInitializer {

        FlywayInitializer() {
            // Disabled — skip migrations
        }

        FlywayInitializer(DataSource dataSource) {
            log.info("=== Flyway: Starting migrations ===");

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .cleanDisabled(true)
                    .load();

            var result = flyway.migrate();
            log.info("=== Flyway: Migration complete. Applied {} migrations ===", result.migrationsExecuted);
        }
    }
}
