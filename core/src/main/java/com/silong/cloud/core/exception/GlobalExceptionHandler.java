package com.silong.cloud.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 全局异常处理器
 *
 * @author louis sin
 * @version 1.0
 * @since 20180610
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理WebClient异常
   *
   * @param ex 异常
   * @return 响应
   */
  @ExceptionHandler(WebClientResponseException.class)
  public ResponseEntity<String> handleWebClientResponseException(WebClientResponseException ex) {
    log.error("Error from WebClient - Status {}, Body {}", ex.getRawStatusCode(),
        ex.getResponseBodyAsString(), ex);
    return ResponseEntity.status(ex.getRawStatusCode()).body(ex.getResponseBodyAsString());
  }
}
