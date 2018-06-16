package com.silong.cloud.core.configure;

import static com.silong.common.Utilities.instantiate;

import com.silong.common.enu.ConnectorType;
import com.silong.common.enu.Protocol;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 * 配置属性注入时使用的类型转换器，同时注册数据库使用的类型Converter
 *
 * @author louis sin
 * @version 1.0
 * @since 20170530
 */
@Configuration
public class CoreConfigurationConverterAutoConfiguration {

  private static final Pattern DIGITS = Pattern.compile("(^[1-9]\\d*)");
  private static int milliToNanoConst = 1000000;

  /**
   * 注册类型转换器
   *
   * @return 转换器
   */
  @Bean
  ConversionService coreConversionService() {
    ConversionServiceFactoryBean bean = new ConversionServiceFactoryBean();
    bean.setConverters(getConverters());
    bean.afterPropertiesSet();
    return bean.getObject();
  }

  /**
   * 定制类型转换
   *
   * @return 转换器集合
   */
  private Set<Converter<?, ?>> getConverters() {
    Set<Converter<?, ?>> converters = new HashSet<>();

    converters.add(new Converter<String, Protocol>() {
      @Override
      public Protocol convert(String source) {
        return Protocol.valueOf(source);
      }
    });

    converters.add(new Converter<String, ConnectorType>() {
      @Override
      public ConnectorType convert(String source) {
        return ConnectorType.valueOf(source);
      }
    });

    converters.add(new Converter<String, char[]>() {
      @Override
      public char[] convert(String source) {
        return source.toCharArray();
      }
    });

    converters.add(new Converter<String, URL>() {
      @Override
      public URL convert(String source) {
        try {
          return new URL(source);
        } catch (MalformedURLException e) {
          throw new IllegalArgumentException(e);
        }
      }
    });

    converters.add(new Converter<String, Protocol>() {
      @Override
      public Protocol convert(String source) {
        return Protocol.valueOf(source);
      }
    });

    converters.add(new Converter<String, TimeUnit>() {
      @Override
      public TimeUnit convert(String source) {
        return TimeUnit.valueOf(source);
      }
    });

    converters.add(new Converter<String, HostnameVerifier>() {
      @Override
      public HostnameVerifier convert(String source) {
        return instantiate(source, "hostnameVerifier");
      }
    });
    return converters;
  }
}
