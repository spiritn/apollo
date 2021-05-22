package com.ctrip.framework.apollo.spring.spi;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.spring.annotation.ApolloAnnotationProcessor;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValueProcessor;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.SpringValueProcessor;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import com.ctrip.framework.apollo.spring.property.SpringValueDefinitionProcessor;
import com.ctrip.framework.apollo.spring.util.BeanRegistrationUtil;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.HashMap;
import java.util.Map;

public class DefaultApolloConfigRegistrarHelper implements ApolloConfigRegistrarHelper {

  /**
   * 注册BeanDefinitions，也就是注册bean到容器里
   */
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AnnotationAttributes attributes = AnnotationAttributes
        .fromMap(importingClassMetadata.getAnnotationAttributes(EnableApolloConfig.class.getName()));
    // 解析EnableApolloConfig注解里配置的namespaces。其实生产中一般是在配置文件里指定要获取的namespaces
    String[] namespaces = attributes.getStringArray("value");
    int order = attributes.getNumber("order");
    PropertySourcesProcessor.addNamespaces(Lists.newArrayList(namespaces), order);

    // PropertySourcesPlaceholderConfigurer负责替换PlaceHolder为对应的属性值，要确保先执行
    Map<String, Object> propertySourcesPlaceholderPropertyValues = new HashMap<>();
    // to make sure the default PropertySourcesPlaceholderConfigurer's priority is higher than PropertyPlaceholderConfigurer
    propertySourcesPlaceholderPropertyValues.put("order", 0);
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesPlaceholderConfigurer.class.getName(),
        PropertySourcesPlaceholderConfigurer.class, propertySourcesPlaceholderPropertyValues);

    // 注册PropertySourcesProcessor，将EnableApolloConfig注解里配置的namespaces集成到Spring的PropertySources里
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesProcessor.class.getName(),
        PropertySourcesProcessor.class);

    // 注册ApolloAnnotationProcessor，它负责解析@ApolloConfigChangeListener和@ApolloConfig
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, ApolloAnnotationProcessor.class.getName(),
        ApolloAnnotationProcessor.class);

    // 注册SpringValueProcessor 它负责加载所有的SpringValue，解析@Value
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, SpringValueProcessor.class.getName(),
        SpringValueProcessor.class);

    // 注册SpringValueDefinitionProcessor，用于xml配置的bean
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, SpringValueDefinitionProcessor.class.getName(),
        SpringValueDefinitionProcessor.class);

    // 注册ApolloJsonValueProcessor，解析@ApolloJsonValue
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, ApolloJsonValueProcessor.class.getName(),
        ApolloJsonValueProcessor.class);
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
