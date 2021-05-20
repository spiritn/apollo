package com.ctrip.framework.apollo.spring.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Map;
import java.util.Objects;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class BeanRegistrationUtil {
  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName,
      Class<?> beanClass) {
    return registerBeanDefinitionIfNotExists(registry, beanName, beanClass, null);
  }

  /**
   * 注册BeanDefinition的工具类，可以借鉴下。
   * 目的是为了 动态注册一个bean到容器里
   */
  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName,
                                                          Class<?> beanClass, Map<String, Object> extraPropertyValues) {
    if (registry.containsBeanDefinition(beanName)) {
      return false;
    }

    String[] candidates = registry.getBeanDefinitionNames();

    for (String candidate : candidates) {
      BeanDefinition beanDefinition = registry.getBeanDefinition(candidate);
      if (Objects.equals(beanDefinition.getBeanClassName(), beanClass.getName())) {
        return false;
      }
    }

    BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(beanClass).getBeanDefinition();

    if (extraPropertyValues != null) {
      for (Map.Entry<String, Object> entry : extraPropertyValues.entrySet()) {
        beanDefinition.getPropertyValues().add(entry.getKey(), entry.getValue());
      }
    }

    // 注册一个BeanDefinition
    registry.registerBeanDefinition(beanName, beanDefinition);

    return true;
  }


}
