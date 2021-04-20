package com.ctrip.framework.apollo.demo.spring.springBootDemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@SpringBootApplication(scanBasePackages = {"com.ctrip.framework.apollo.demo.spring.common",
    "com.ctrip.framework.apollo.demo.spring.springBootDemo"
})
public class SpringBootSampleApplication {

  public static void main(String[] args) throws IOException {
    SpringApplication.run(SpringBootSampleApplication.class, args);
  }
}
