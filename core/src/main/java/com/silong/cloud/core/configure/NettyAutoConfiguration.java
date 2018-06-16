package com.silong.cloud.core.configure;

import static com.silong.common.Aes256Utils.decrypt;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Netty Reactive web Server定制配置
 *
 * @author louis sin
 * @version 1.0
 * @since 20180601
 */
@Configuration
public class NettyAutoConfiguration {

  @Bean
  ReactiveWebServerFactory reactiveWebServerFactory(ServerProperties props) {
    NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();

    // factory.addServerCustomizers(builder ->
    // builder.compression(props.getCompression().getEnabled()));

    Ssl ssl = props.getSsl();
    ssl.setKeyStorePassword(decrypt(ssl.getKeyStorePassword()));
    ssl.setKeyPassword(decrypt(ssl.getKeyPassword()));
    factory.setSsl(ssl);
    return factory;
  }
}
