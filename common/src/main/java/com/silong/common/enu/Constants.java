package com.silong.common.enu;

/**
 * 通用常量
 *
 * @author louis sin
 * @version 1.0
 * @since 20180405
 */
public interface Constants {

  /**
   * 华为云对象存储类型
   */
  String OBS = "obs";

  /**
   * 请求头region key
   */
  String REQUEST_HEADER_REGION_KEY = "region";

  /**
   * 主机id
   */
  String HOST_ID = "host-id";

  /**
   * 主机名
   */
  String HOSTNAME = "HOSTNAME";

  /**
   * 计算机名
   */
  String COMPUTERNAME = "COMPUTERNAME";

  /**
   * 部署模式
   */
  String DEPLOYMENT = "deployment";

  /**
   * 节点角色
   */
  String NODE_ROLE = "node-role";

  /**
   * 最大有效端口
   */
  int MAX_PORT = 65535;

  /**
   * 分页查询上限
   */
  int MAX_PAGE_SIZE = 1000;
}
