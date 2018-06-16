package com.silong.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.statemachine.config.EnableWithStateMachine;
import org.springframework.web.reactive.config.EnableWebFlux;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 服务启动类
 *
 * @author louis sin
 * @version 1.0
 * @since 20180601
 */
@EnableWebFlux
//@EnableCircuitBreaker
@EnableWithStateMachine
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication(scanBasePackages = {"com.silong"})
public class AcheronApplication {

  /**
   * Main Entry
   *
   * @param args 应用参数
   */
  public static void main(String[] args) {
    SpringApplication.run(AcheronApplication.class, args);
  }
}
