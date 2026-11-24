package com.blift.aiservice.config;

import com.blift.common.scheduling.TrackedJobAspect;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * The nightly context build runs on one instance at a time, so AI calls are not paid for twice.
 * The shedlock table is created on startup (blift.jobs.create-shedlock-table).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H", order = TrackedJobAspect.SHEDLOCK_ORDER)
public class SchedulingConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .withTableName("shedlock")
                .usingDbTime()
                .build());
    }
}
