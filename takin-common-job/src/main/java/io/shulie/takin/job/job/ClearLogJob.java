package io.shulie.takin.job.job;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import io.shulie.takin.job.annotation.ElasticSchedulerJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author 无涯
 * @Package io.hzq.job.elasticjob.job
 * @date 2021/6/21 5:44 下午
 */
@Component
@ElasticSchedulerJob(jobName = "clearLogJob",cron = "0 0 1 * * ?",
    description = "每天凌晨1点，清理JOB_EXECUTION_LOG、JOB_STATUS_TRACE_LOG的3天前数据")
@Slf4j
public class ClearLogJob implements SimpleJob {
    @Autowired
    private Environment environment;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public void execute(ShardingContext shardingContext) {
        String clearTime = environment.getProperty("job.clear.day.time");
        if(StringUtils.isBlank(clearTime)) {
            clearTime = "3";
        }
        log.info("清理JOB_EXECUTION_LOG、JOB_STATUS_TRACE_LOG");
        jdbcTemplate.update("delete from JOB_EXECUTION_LOG where datediff(curdate(), START_TIME) >= " + clearTime );
        jdbcTemplate.update("delete from JOB_STATUS_TRACE_LOG where datediff(curdate(), CREATION_TIME) >= " + clearTime);
    }
}
