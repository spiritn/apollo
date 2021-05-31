package com.ctrip.framework.apollo.spring.config;

import com.ctrip.framework.apollo.Config;
import com.google.common.collect.Lists;

import java.util.List;

public class ConfigPropertySourceFactory {

  // 保存了所有的ConfigPropertySource
  private final List<ConfigPropertySource> configPropertySources = Lists.newLinkedList();

  /**
   * 这里方法名字叫getAndSet比较合适
   */
  public ConfigPropertySource getConfigPropertySource(String name, Config source) {
    // 把config转成ConfigPropertySource
    ConfigPropertySource configPropertySource = new ConfigPropertySource(name, source);

    configPropertySources.add(configPropertySource);

    return configPropertySource;
  }

  public List<ConfigPropertySource> getAllConfigPropertySources() {
    return Lists.newLinkedList(configPropertySources);
  }
}
