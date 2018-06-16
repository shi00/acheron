package com.silong.cloud.server.v1.openstack.controller;

import static com.silong.common.enu.Constants.REQUEST_HEADER_REGION_KEY;
import static com.silong.common.enu.Profiles.HUAWEI;
import static com.silong.common.enu.Profiles.OPENSTACK;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.huawei.openstack4j.model.compute.Server;
import com.silong.cloud.server.v1.PublicCloudServer;
import com.silong.cloud.server.v1.openstack.AbstractHwCloudOpenstackService;
import javax.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


/**
 * 华为云虚拟机服务实现类
 *
 * @author louis sin
 * @version 1.0
 * @since 20180610
 */
@Profile({HUAWEI, OPENSTACK})
@RestController
@Slf4j
@RequestMapping(value = "/v1.0/servers", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
public class HwCloudServerController extends AbstractHwCloudOpenstackService implements
    PublicCloudServer {


  /**
   * 查询虚拟机详情
   *
   * @param serverId 虚拟机id
   * @param region 虚拟机区域(请求头内，可选)
   * @return 响应
   */
  @Override
  @GetMapping(value = "/{server_id}", consumes = ALL_VALUE)
  public Mono<ResponseEntity<Server>> get(@PathVariable("server_id") @NotBlank String serverId,
      @Header(name = REQUEST_HEADER_REGION_KEY) String region) {
    region = getRegion(region);
    if (log.isInfoEnabled()) {
      log.info("Query details of server with serverId[{}] and region[{}].", serverId, region);
    }
    getConnector().getComputeRestClient().get()
        .uri("/v2.1/{tenant_id}/servers/{server_id}", getProjectId(region),
            serverId)
        .header(REQUEST_HEADER_REGION_KEY, region)
        .retrieve()
        .bodyToMono(Server.class);
    return null;
  }


}
