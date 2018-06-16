package com.silong.common.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

/**
 * 错误详情
 *
 * @author louis sin
 * @version 1.0
 * @since 20180602
 */
@Data
@Builder
public class ErrorDetails {

  /**
   * 错误码
   */
  @NotEmpty
  @JsonProperty("error_code")
  private String code;

  /**
   * 错误详情
   */
  @NotEmpty
  @JsonProperty("error_message")
  private String message;
}
