package com.silong.cloud.connector.configure;

import static com.silong.common.Aes256Utils.decrypt;
import static java.time.Duration.ofDays;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.apache.commons.lang3.reflect.FieldUtils.getField;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.huawei.openstack4j.openstack.identity.v3.domain.KeystoneEndpoint;
import com.huawei.openstack4j.openstack.identity.v3.domain.KeystoneService;
import com.silong.common.validator.annotation.TwovarMutuallyExclusive;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * openstack配置信息
 *
 * @author louis sin
 * @version 1.0
 * @since 20180613
 */
@Data
@Validated
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "connector.openstack")
public class OpenstackProperties {

  /**
   * 默认token缓存的并发访问级别
   */
  private static final int DEFAULT_TOKEN_CONCURRENT_ACCESS_LEVEL = 16;

  /**
   * openstack 区域配置
   */
  @Valid
  @NestedConfigurationProperty
  private Map<@NotBlank String, @NotNull @Valid OpenstackRegion> regions = Maps.newHashMap();

  /**
   * 默认区域
   */
  private String defaultRegion;

  /**
   * token超时时间，默认：1天
   */
  @NotNull
  private Duration tokenTimeout = ofDays(1);

  /**
   * pki超时时间，默认：1年
   */
  @NotNull
  private Duration pkiTimeout = ofDays(360);

  /**
   * token并发访问级别，默认：16
   */
  @Min(1)
  private int tokenConcurrentAccessLevel = DEFAULT_TOKEN_CONCURRENT_ACCESS_LEVEL;


  /**
   * openstack region配置信息
   *
   * @author louis sin
   * @version 1.0
   * @since 20180613
   */
  @Data
  @Validated
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OpenstackRegion {

    /**
     * 扩展服务地址列表
     */
    @Valid
    private Set<@Valid ExtOpenstackService> extServices = Sets.newHashSet();

    /**
     * keystone url
     */
    @NotBlank
    private String endpoint;

    /**
     * 查询signed证书地址，pki token认证需要 <br> /certificates/signing url
     */
    @Nullable
    private String signingEndpoint;

    /**
     * 域
     */
    @Valid
    @NestedConfigurationProperty
    private Domain domain = new Domain();

    /**
     * 租户
     */
    @Valid
    @NestedConfigurationProperty
    private Project project = new Project();

    /**
     * 用户
     */
    @Valid
    @NestedConfigurationProperty
    private User user = new User();

    /**
     * 是否keystone api版本为v3
     *
     * @return v3返回true，否则false
     */
    public boolean isV3() {
      return contains(endpoint, "/v3");
    }

    /**
     * 是否keystone api版本为v2
     *
     * @return v2返回true，否则false
     */
    public boolean isV2() {
      return contains(endpoint, "/v2.0");
    }

    /**
     * 配置的keystone地址是否开启https
     *
     * @return 启用https返回true，否则false
     */
    public boolean isHttps() {
      return startsWithIgnoreCase(endpoint, "https");
    }

    /**
     * 获取配置的租户id
     *
     * @return 租户id
     */
    public String getProjectId() {
      return project.id;
    }
  }

  /**
   * openstack租户信息
   *
   * @author louis sin
   * @version 1.0
   * @since 20161126
   */
  @Data
  @Validated
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Project {

    /**
     * 租户名
     */
    private String name;

    /**
     * 租户 id
     */
    private String id;
  }

  /**
   * openstack域信息
   *
   * @author louis sin
   * @version 1.0
   * @since 20161126
   */
  @Data
  @Validated
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Domain {

    /**
     * 域名
     */
    private String name;

    /**
     * 域id
     */
    private String id;
  }

  /**
   * openstack用户信息
   *
   * @author louis sin
   * @version 1.0
   * @since 20161126
   */
  @Data
  @Validated
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @TwovarMutuallyExclusive(one = "name", other = "id")
  public static class User {

    /**
     * 用户名
     */
    private String name;

    /**
     * 用户id
     */
    private String id;

    /**
     * 用户密码
     */
    @NotBlank
    private transient String password;

    /**
     * 设置用户密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
      this.password = decrypt(password);
    }

    @Override
    public String toString() {
      return reflectionToString(this, SHORT_PREFIX_STYLE, false);
    }
  }

  /**
   * openstack扩展服务配置
   *
   * @author louis sin
   * @version 1.0
   * @since 20180609
   */
  @Validated
  public static class ExtOpenstackService extends KeystoneService {

    private static final long serialVersionUID = -8375595070841350282L;

    /**
     * 服务访问秘钥
     */
    @Nullable
    private String ak;

    /**
     * 服务访问秘钥
     */
    @Nullable
    private String sk;

    public String getAk() {
      return ak;
    }

    public void setAk(String ak) {
      this.ak = ak;
    }

    public String getSk() {
      return sk;
    }

    public void setSk(String sk) {
      this.sk = sk;
    }

    public void setEndpoints(List<ExtServiceEndpoint> endpoints) {
      try {
        getField(KeystoneService.class, "endpoint", true).set(this, endpoints);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setLinks(Map<String, String> links) {
      try {
        getField(KeystoneService.class, "links", true).set(this, links);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setType(String type) {
      try {
        getField(KeystoneService.class, "type", true).set(this, type);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setName(String name) {
      try {
        getField(KeystoneService.class, "name", true).set(this, name);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setDescription(String desc) {
      try {
        getField(KeystoneService.class, "description", true).set(this, desc);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setVersion(Integer version) {
      try {
        getField(KeystoneService.class, "version", true).set(this, version);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setId(String id) {
      try {
        getField(KeystoneService.class, "id", true).set(this, id);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * openstack扩展服务端点配置
   *
   * @author louis sin
   * @version 1.0
   * @since 20180609
   */

  @Validated
  public static class ExtServiceEndpoint extends KeystoneEndpoint {

    private static final long serialVersionUID = -7921463634305905094L;

    public void setUrl(URL url) {
      try {
        getField(KeystoneEndpoint.class, "url", true).set(this, url);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setRegionId(String regionId) {
      try {
        getField(KeystoneEndpoint.class, "regionId", true).set(this, regionId);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setRegion(String region) {
      try {
        getField(KeystoneEndpoint.class, "region", true).set(this, region);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setName(String name) {
      try {
        getField(KeystoneEndpoint.class, "name", true).set(this, name);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void getServiceId(String serviceId) {
      try {
        getField(KeystoneEndpoint.class, "serviceId", true).set(this, serviceId);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setIface(String iface) {
      try {
        getField(KeystoneEndpoint.class, "iface", true).set(this, iface);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setId(String id) {
      try {
        getField(KeystoneEndpoint.class, "id", true).set(this, id);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setDescription(String desc) {
      try {
        getField(KeystoneEndpoint.class, "description", true).set(this, desc);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setType(String type) {
      try {
        getField(KeystoneEndpoint.class, "type", true).set(this, type);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    public void setLinks(Map<String, String> links) {
      try {
        getField(KeystoneEndpoint.class, "links", true).set(this, links);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
