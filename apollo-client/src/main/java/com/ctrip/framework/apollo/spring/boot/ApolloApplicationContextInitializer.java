package com.ctrip.framework.apollo.spring.boot;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.List;

/**
 * Initialize apollo system properties and inject the Apollo config in Spring Boot bootstrap phase
 * 在容器启动阶段 初始化apollo系统属性和注入配置文件值
 *
 * <p>Configuration example:</p>
 * <pre class="code">
 *   # set app.id
 *   app.id = 100004458
 *   # enable apollo bootstrap config and inject 'application' namespace in bootstrap phase
 *   apollo.bootstrap.enabled = true
 * </pre>
 * <p>
 * or
 *
 * <pre class="code">
 *   # set app.id
 *   app.id = 100004458
 *   # enable apollo bootstrap config
 *   apollo.bootstrap.enabled = true
 *   # will inject 'application' and 'FX.apollo' namespaces in bootstrap phase
 *   apollo.bootstrap.namespaces = application,FX.apollo
 * </pre>
 * <p>
 * <p>
 * 开启apollo.bootstrap.eagerLoad.enabled = true可以在日志系统加载之前就注入属性值
 * If you want to load Apollo configurations even before Logging System Initialization Phase,
 * add
 * <pre class="code">
 *   # set apollo.bootstrap.eagerLoad.enabled
 *   apollo.bootstrap.eagerLoad.enabled = true
 * </pre>
 * <p>
 * This would be very helpful when your logging configurations is set by Apollo.
 * <p>
 * for example, you have defined logback-spring.xml in your project, and you want to inject some attributes into logback-spring.xml.
 */
public class ApolloApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, EnvironmentPostProcessor, Ordered {
    public static final int DEFAULT_ORDER = 0;

    private static final Logger logger = LoggerFactory.getLogger(ApolloApplicationContextInitializer.class);
    private static final Splitter NAMESPACE_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

    // 这些属性都属于apollo系统属性
    private static final String[] APOLLO_SYSTEM_PROPERTIES = {"app.id", ConfigConsts.APOLLO_CLUSTER_KEY,
            "apollo.cacheDir", "apollo.accesskey.secret", ConfigConsts.APOLLO_META_KEY, PropertiesFactory.APOLLO_PROPERTY_ORDER_ENABLE};

    private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector
            .getInstance(ConfigPropertySourceFactory.class);

    private int order = DEFAULT_ORDER;

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();

        if (!environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, Boolean.class, false)) {
            logger.debug("Apollo bootstrap config is not enabled for context {}, see property: ${{}}", context, PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED);
            return;
        }
        logger.debug("Apollo bootstrap config is enabled for context {}", context);

        initialize(environment);
    }


    /**
     * Initialize Apollo Configurations Just after environment is ready.
     *
     * @param environment
     */
    protected void initialize(ConfigurableEnvironment environment) {
        if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
            //already initialized
            return;
        }

        // 去获取配置文件里的apollo.bootstrap.namespaces，没有配置就是默认application
        String namespaces = environment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, ConfigConsts.NAMESPACE_APPLICATION);
        logger.debug("Apollo bootstrap namespaces: {}", namespaces);
        List<String> namespaceList = NAMESPACE_SPLITTER.splitToList(namespaces);

        CompositePropertySource composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
        for (String namespace : namespaceList) {
            // 经典的ConfigService.getConfig 获取某个配置文件的值
            Config config = ConfigService.getConfig(namespace);

            composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
        }

        // 把所有的配置值添加到Spring的environment的PropertySources，这样通过Spring的PropertySources就可以方便的获取所有值，当然@value也没问题
        environment.getPropertySources().addFirst(composite);
    }

    /**
     * To fill system properties from environment config
     */
    void initializeSystemProperty(ConfigurableEnvironment environment) {
        for (String propertyName : APOLLO_SYSTEM_PROPERTIES) {
            fillSystemPropertyFromEnvironment(environment, propertyName);
        }
    }

    /**
     * 系统属性是优先于环境变量的，环境变量是系统级别的，系统属性是JVM级别的，范围小于环境变量
     */
    private void fillSystemPropertyFromEnvironment(ConfigurableEnvironment environment, String propertyName) {
        // 如果系统属性已经有了，就不再填充
        if (System.getProperty(propertyName) != null) {
            return;
        }

        // 从环境变量environment中把值填充进SystemProperty，为啥呢，直接从environment获取不好吗
        String propertyValue = environment.getProperty(propertyName);

        if (Strings.isNullOrEmpty(propertyValue)) {
            return;
        }

        System.setProperty(propertyName, propertyValue);
    }

    /**
     * 本方法是postProcessEnvironment处理器，比上面的initialize方法先执行
     * 是在容器启动前执行处理Environment，比较急切，所以也就实现了在日志系统加载前就先执行
     * <p>
     * In order to load Apollo configurations as early as even before Spring loading logging system phase,
     * this EnvironmentPostProcessor can be called Just After ConfigFileApplicationListener has succeeded.
     * <p>
     * <br />
     * The processing sequence would be like this: <br />
     * Load Bootstrap properties and application properties -----> load Apollo configuration properties ----> Initialize Logging systems
     *
     * @param configurableEnvironment
     * @param springApplication
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment configurableEnvironment, SpringApplication springApplication) {

        // 因为是在容器未完全启动结束时执行本方法，所以需要先把一些apollo必需的属性先加载进来
        // should always initialize system properties like app.id in the first place
        initializeSystemProperty(configurableEnvironment);

        // 如果开启了apollo.bootstrap.eagerLoad.enabled = true就先去执行initialize方法初始化apollo配置，（可实现在日志系统加载之前就注入属性值）
        Boolean eagerLoadEnabled = configurableEnvironment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED, Boolean.class, false);
        //EnvironmentPostProcessor should not be triggered if you don't want Apollo Loading before Logging System Initialization
        // 如果没有开启，就不先执行initialize方法了
        if (!eagerLoadEnabled) {
            return;
        }

        Boolean bootstrapEnabled = configurableEnvironment.getProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, Boolean.class, false);
        if (bootstrapEnabled) {
            initialize(configurableEnvironment);
        }
    }

    /**
     * @since 1.3.0
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * @since 1.3.0
     */
    public void setOrder(int order) {
        this.order = order;
    }
}
