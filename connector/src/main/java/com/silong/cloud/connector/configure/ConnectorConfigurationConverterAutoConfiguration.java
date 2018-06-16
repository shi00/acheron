package com.silong.cloud.connector.configure;

import static com.silong.common.Utilities.instantiate;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import com.huawei.openstack4j.api.client.CloudProvider;
import com.huawei.openstack4j.api.identity.EndpointURLResolver;
import com.huawei.openstack4j.api.types.Facing;
import com.huawei.openstack4j.model.common.resolvers.LatestServiceVersionResolver;
import com.huawei.openstack4j.model.common.resolvers.ServiceVersionResolver;
import com.huawei.openstack4j.model.common.resolvers.StableServiceVersionResolver;
import com.huawei.openstack4j.openstack.identity.internal.DefaultEndpointURLResolver;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 * 配置属性注入时使用的类型转换器
 *
 * @author louis sin
 * @version 1.0
 * @since 20170530
 */
@Configuration
public class ConnectorConfigurationConverterAutoConfiguration {

  /**
   * 注册类型转换器
   *
   * @return 转换器
   */
  @Bean
  public ConversionService connectorConversionService() {
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

    converters.add(new Converter<String, Facing>() {
      @Override
      public Facing convert(String source) {
        return Facing.valueOf(source.toUpperCase());
      }
    });

    converters.add(new Converter<String, CloudProvider>() {
      @Override
      public CloudProvider convert(String source) {
        return CloudProvider.valueOf(source.toUpperCase());
      }
    });

    converters.add(new Converter<String, EndpointURLResolver>() {
      @Override
      public EndpointURLResolver convert(String source) {
        return equalsIgnoreCase("default", source) ? new DefaultEndpointURLResolver()
            : instantiate(source, "endpointURLResolver");
      }
    });

    converters.add(new Converter<String, ServiceVersionResolver>() {
      @Override
      public ServiceVersionResolver convert(String source) {
        ServiceVersionResolver resolver = StableServiceVersionResolver.INSTANCE;
        // 只有明确指定使用最新版时才使用，否则使用stable
        if (equalsIgnoreCase("latest", source)) {
          resolver = LatestServiceVersionResolver.INSTANCE;
        } else if (!equalsIgnoreCase("stable", source)) {
          resolver = instantiate(source, "serviceVersionResolver");
        }
        return resolver;
      }
    });
    return converters;
  }
}
