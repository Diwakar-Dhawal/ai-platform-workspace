package com.aiservice.platform.identity.config;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        // Add depends-on="flywayInitializer" to the EntityManagerFactory bean
        // This ensures Flyway runs BEFORE Hibernate tries to use the database
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
        // No-op — all work done in postProcessBeanDefinitionRegistry
    }

    @Bean
    public FlywayInitializer flywayInitializer(DataSource dataSource) {
        return new FlywayInitializer(dataSource);
    }

    /**
     * Separate bean that runs Flyway migrations during construction.
     * Because entityManagerFactory depends on this bean, it will be created first.
     */
    static class FlywayInitializer {

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
