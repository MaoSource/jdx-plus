import request from "@/util/request";

// 登录
export function login(data) {
  return request({
    url: "auth/login",
    method: "post",
    data: data
  });
}

// 青龙列表
export function getQLConfigList() {
  return request({
    url: "admin/ql",
    method: "get"
  });
}

// 删除青龙配置
export function delQLConfig(displayName) {
  return request({
    url: "admin/ql?displayName=" + displayName,
    method: "delete"
  });
}

// 新增青龙配置
export function addQLConfig(data) {
  return request({
    url: "admin/ql",
    method: "post",
    data: data
  });
}

// 编辑青龙配置
export function updateQLConfig(data) {
  return request({
    url: "admin/ql",
    method: "put",
    data: data
  });
}

// 系统配置
export function getSystemConfig() {
  return request({
    url: "admin/config",
    method: "get"
  });
}

// 更新系统配置
export function updateWebsiteConfig(data) {
  return request({
    url: "admin/websiteConfig",
    method: "put",
    data: data
  });
}

export function getWxPusher() {
  return request({
    url: "admin/wxPusher",
    method: "get"
  });
}

export function updateWxPusher(data) {
  return request({
    url: "admin/wxPusher",
    method: "put",
    data: data
  });
}

export function checkCookie() {
  return request({
    url: "admin/checkCookie",
    method: "post"
  });
}

export function updateCheckCookieCron(data) {
  return request({
    url: "admin/updateCheckCookieCron",
    method: "put",
    data: data
  });
}

export function updateAccount(data) {
  return request({
    url: "admin/updateAccount",
    method: "put",
    data: data
  });
}

// 版本信息
export function getVersion() {
  return request({
    url: "admin/version",
    method: "get"
  });
}
export function checkUpgrade() {
  return request({
    url: "admin/checkUpgrade",
    method: "post"
  });
}
