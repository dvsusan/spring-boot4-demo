package com.susan.training.integration.job;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按需注册 Outbox 扫描任务。
 */
@Configuration
@ConditionalOnProperty(
        prefix = "demo.chapter4.jobs",
        name = "enabled",
        havingValue = "true"
)
public class QuartzIntegrationConfiguration {

    @Bean
    public JobDetail outboxRelayJobDetail() {
        return JobBuilder.newJob(OutboxRelayJob.class)
                .withIdentity("outboxRelayJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger outboxRelayTrigger(JobDetail outboxRelayJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(outboxRelayJobDetail)
                .withIdentity("outboxRelayTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(30)
                        .repeatForever())
                .build();
    }
}
