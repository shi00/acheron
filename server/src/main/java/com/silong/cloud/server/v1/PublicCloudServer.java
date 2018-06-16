package com.silong.cloud.server.v1;

import com.huawei.openstack4j.model.compute.Server;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * 公有云虚拟机服务
 *
 * @author louis sin
 * @version 1.0
 * @since 20180606
 */
public interface PublicCloudServer {

  /**
   * 查询虚拟机详情
   *
   * @param serverId 虚拟机id
   * @param region 虚拟机区域(可选)
   * @return 响应
   */
  @Nonnull
  Mono<ResponseEntity<Server>> get(@NotBlank String serverId,
      @Nullable String region);

}
