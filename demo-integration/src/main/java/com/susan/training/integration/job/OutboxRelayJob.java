package com.susan.training.integration.job;

import com.susan.training.integration.outbox.OutboxService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * 扫描待投递 Outbox 消息。
 *
 * <p>禁止同一任务并发执行，避免多个调度实例重复搬运同一批消息。</p>
 */
@DisallowConcurrentExecution
public class OutboxRelayJob extends QuartzJobBean {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class);
    private OutboxService outboxService;

    // Quartz 负责创建 Job 实例，Spring 负责给 setter 注入协作者。
    @Autowired
    public void setOutboxService(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        int pending = outboxService.pending().size();
        log.info("outbox relay job: pendingCount={}", pending);
    }
}
