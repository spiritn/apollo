package com.ctrip.framework.apollo.spring.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * 所有的BeanPostProcessor的抽象父类
 * Create by zhangzheng on 2018/2/6
 */
public abstract class ApolloProcessor implements BeanPostProcessor, PriorityOrdered {

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    Class clazz = bean.getClass();
    // 每个bean都要去判断是否含有要处理的注解，这样启动速度是不是慢很多
    for (Field field : findAllField(clazz)) {
      processField(bean, beanName, field);
    }
    for (Method method : findAllMethod(clazz)) {
      processMethod(bean, beanName, method);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  /**
   * subclass should implement this method to process field
   */
  protected abstract void processField(Object bean, String beanName, Field field);

  /**
   * subclass should implement this method to process method
   */
  protected abstract void processMethod(Object bean, String beanName, Method method);


  @Override
  public int getOrder() {
    //make it as late as possible
    return Ordered.LOWEST_PRECEDENCE;
  }

  /**
   * 发现类所有的字段
   * @param clazz
   * @return
   */
  private List<Field> findAllField(Class clazz) {
    final List<Field> res = new LinkedList<>();
    // Spring提供的ReflectionUtils工具类
    ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
      @Override
      public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
        res.add(field);
      }
    });
    return res;
  }

  private List<Method> findAllMethod(Class clazz) {
    final List<Method> res = new LinkedList<>();
    ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
      @Override
      public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
        res.add(method);
      }
    });
    return res;
  }
}
