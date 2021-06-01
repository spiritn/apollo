package com.ctrip.framework.apollo.demo.spring.springBootDemo.refresh;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.demo.spring.springBootDemo.config.SampleRedisConfig;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@ConditionalOnProperty("redis.cache.enabled")
@Component
public class SpringBootApolloRefreshConfig {
    private static final Logger logger = LoggerFactory.getLogger(SpringBootApolloRefreshConfig.class);

    private final SampleRedisConfig sampleRedisConfig;
    private final RefreshScope refreshScope;

    @Value("${my.open}")
    private Boolean open;

    public SpringBootApolloRefreshConfig(
            final SampleRedisConfig sampleRedisConfig,
            final RefreshScope refreshScope) {
        this.sampleRedisConfig = sampleRedisConfig;
        this.refreshScope = refreshScope;
    }

    @ApolloConfigChangeListener(value = {ConfigConsts.NAMESPACE_APPLICATION})
    public void onChange(ConfigChangeEvent changeEvent) {
        logger.info("before refresh {}", sampleRedisConfig.toString());
        logger.info("my.open,{}", open);
        Config config = ConfigService.getAppConfig();
        String timeout = config.getProperty("timeout", "23");
        Integer integer = config.getIntProperty("timeout", 78);
        refreshScope.refresh("sampleRedisConfig");
        logger.info("after refresh {}", sampleRedisConfig.toString());
    }
}
