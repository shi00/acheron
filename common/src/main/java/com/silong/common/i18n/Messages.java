package com.silong.common.i18n;

import java.util.Locale;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

/**
 * 错误码对应的描述信息
 *
 * @author louis sin
 * @version 1.0
 * @since 20180602
 */
@Component
public class Messages {

  /**
   * 文本资源源
   */
  private final MessageSourceAccessor accessor;

  /**
   * 构造方法
   *
   * @param messageSource 资源
   */
  @Autowired
  public Messages(MessageSource messageSource) {
    this.accessor = new MessageSourceAccessor(messageSource);
  }

  /**
   * 获取错误码对应的资源描述，默认ENGLISH
   *
   * @param code 错误码
   * @return 资源描述
   */
  public String get(String code) {
    return accessor.getMessage(code);
  }

  /**
   * 获取错误码对应的资源描述，默认ENGLISH
   *
   * @param code 错误码
   * @param args 描述参数
   * @return 资源描述
   */
  public String get(String code, Object[] args) {
    return accessor.getMessage(code, args);
  }

  /**
   * 获取错误码对应的资源描述，默认ENGLISH
   *
   * @param code 错误码
   * @param defualtMessage 默认描述，如果错误码没有对应的描述
   * @return 资源描述
   */
  public String get(String code, String defualtMessage) {
    return accessor.getMessage(code, defualtMessage, Locale.ENGLISH);
  }

  /**
   * 获取错误码对应的资源描述
   *
   * @param code 错误码
   * @param locale 语言
   * @return 资源描述
   */
  public String get(String code, Locale locale) {
    return accessor.getMessage(code, locale);
  }

  /**
   * 读取错误码对应的描述信息
   *
   * @param code 错误码
   * @param args 错误描述参数
   * @param defaultMessage 默认值
   * @param locale 语言
   * @return 错误描述
   */
  public String get(String code, @Nullable Object[] args, String defaultMessage, Locale locale) {
    return accessor.getMessage(code, args, defaultMessage, locale);
  }

}
