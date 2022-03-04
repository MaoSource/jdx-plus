package cn.yiidii.jdx.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.yiidii.jdx.config.prop.SystemConfigProperties;
import cn.yiidii.jdx.config.prop.SystemConfigProperties.QLConfig;
import cn.yiidii.jdx.model.ex.BizException;
import cn.yiidii.jdx.util.JDXUtil;
import cn.yiidii.jdx.util.ScheduleTaskUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * QLService
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QLService {

    private final static Map<String, String> QL_TOKEN_CACHE = new ConcurrentHashMap<>(16);

    public final JdService jdService;
    public final SystemConfigProperties systemConfigProperties;
    private final ScheduleTaskUtil scheduleTaskUtil;

    public void submitCk(String displayName, String cookie, String remark) throws Exception {
        // 校验
        QLConfig qlConfig = systemConfigProperties.getQLConfigByDisplayName(displayName);
        if (Objects.isNull(qlConfig)) {
            throw new BizException(StrUtil.format("青龙节点【{}】不存在", displayName));
        }
        // 禁用校验
        if (qlConfig.getDisabled() == 1) {
            throw new BizException(StrUtil.format("节点【{}】已禁用", displayName));
        }
        // 最大数量校验
        Integer used = qlConfig.getUsed();
        List<JSONObject> normalEnvs = this.searchEnv(displayName, "JD_COOKIE", 0);
        if (normalEnvs.size() >= qlConfig.getMax()) {
            qlConfig.setUsed(used);
            throw new BizException(StrUtil.format("节点【{}】车位已满", displayName));
        }
        qlConfig.setUsed(normalEnvs.size());

        // 获取存在的env, env存在就不更新备注
        String ptPin = JDXUtil.getPtPinFromCK(cookie);
        JSONObject existEnv = this.getExistCK(displayName, StrUtil.format("pt_pin={};", ptPin));
        if (!existEnv.isEmpty()) {
            remark = existEnv.getString("remarks");
        }
        // 保存并启用
        this.saveAndEnableEnv(displayName, "JD_COOKIE", cookie, remark);
    }

    public void saveAndEnableEnv(String displayName, String name, String value, String remark) {
        QLConfig qlConfig = systemConfigProperties.getQLConfigByDisplayName(displayName);
        if (Objects.isNull(qlConfig)) {
            throw new BizException(StrUtil.format("青龙节点【{}】不存在", displayName));
        }
        // 获取存在的env
        String ptPin = JDXUtil.getPtPinFromCK(value);
        JSONObject existEnv = this.getExistCK(displayName, StrUtil.format("pt_pin={};", ptPin));

        // 推送青龙
        if (existEnv.isEmpty()) {
            // 新增逻辑
            JSONObject envJo = new JSONObject();
            envJo.put("name", name);
            envJo.put("value", value);
            envJo.put("remarks", remark);
            JSONArray paramJa = new JSONArray();
            paramJa.add(envJo);
            try {
                log.debug(StrUtil.format("[青龙 - {}] 添加环境变量, 参数: {}", displayName, envJo.toJSONString()));
                HttpResponse response = HttpRequest.post(qlConfig.getUrl().concat("open/envs"))
                        .bearerAuth(this.getQLToken(displayName))
                        .body(paramJa.toJSONString())
                        .execute();
                log.debug(StrUtil.format("[青龙 - {}] 添加环境变量, 响应: {}", displayName, response.body()));
            } catch (Exception e) {
                log.error(StrUtil.format("[青龙 - {}] 添加环境变量发生异常: {}", displayName, e));
                throw new BizException("连接青龙发生异常, 请联系系统管理员");
            }
        } else {
            // 更新并且启用
            // 更新
            JSONObject envJo = new JSONObject();
            envJo.put("name", name);
            envJo.put("value", value);
            envJo.put("remarks", remark);
            envJo.put("_id", existEnv.getString("id"));
            try {
                // 第一次，用_id
                log.debug(StrUtil.format("[青龙 - {}] 第一次尝试更新环境变量, 参数: {}", displayName, envJo.toJSONString()));
                HttpResponse response = HttpRequest.put(qlConfig.getUrl().concat("open/envs"))
                        .bearerAuth(this.getQLToken(displayName))
                        .body(envJo.toJSONString())
                        .execute();
                log.debug(StrUtil.format("[青龙 - {}] 更新环境变量, 状态码: {}, 响应: {}", displayName, response.getStatus(), response.body()));

                // 如果第一次异常，第二次用id
                if (response.getStatus() == HttpStatus.HTTP_INTERNAL_ERROR) {
                    envJo.remove("_id");
                    envJo.put("id", existEnv.getString("id"));
                    log.debug(StrUtil.format("[青龙 - {}] 第二次尝试更新环境变量, 参数: {}", displayName, envJo.toJSONString()));
                    response = HttpRequest.put(qlConfig.getUrl().concat("open/envs"))
                            .bearerAuth(this.getQLToken(displayName))
                            .body(envJo.toJSONString())
                            .execute();
                    log.debug(StrUtil.format("[青龙 - {}] 第二次尝试更新环境变量, 状态码: {}, 响应: {}", displayName, response.getStatus(), response.body()));
                }

                // 启用
                if (existEnv.getInteger("status") == 1) {
                    JSONArray paramJa = new JSONArray();
                    paramJa.add(existEnv.getString("id"));
                    log.debug(StrUtil.format("[青龙 - {}] 启用环境变量, 参数: {}", displayName, paramJa.toJSONString()));
                    response = HttpRequest.put(qlConfig.getUrl().concat("open/envs/enable"))
                            .bearerAuth(this.getQLToken(displayName))
                            .body(paramJa.toJSONString())
                            .execute();
                    log.debug(StrUtil.format("[青龙 - {}] 启用环境变量, 响应: {}", displayName, response.body()));
                }
            } catch (Exception e) {
                log.error(StrUtil.format("[青龙 - {}] 更新并启用环境变量发生异常: {}", displayName, e));
                throw new BizException("连接青龙发生异常, 请联系系统管理员");
            }
        }

    }

    /**
     * 搜索环境变量
     *
     * @param displayName
     * @param searchValue
     * @param status      0：正常；1：禁用
     * @return
     */
    public List<JSONObject> searchEnv(String displayName, String searchValue, Integer status) {
        List<JSONObject> envs = this.searchEnv(displayName, searchValue);
        return envs.stream().filter(e -> e.getInteger("status").compareTo(status) == 0).collect(Collectors.toList());
    }

    public List<JSONObject> searchEnv(String displayName, String searchValue) {
        QLConfig qlConfig = systemConfigProperties.getQLConfigByDisplayName(displayName);
        if (Objects.isNull(qlConfig)) {
            throw new BizException(StrUtil.format("青龙节点【{}】不存在", displayName));
        }
        try {
            HttpResponse response = HttpRequest.get(StrUtil.format("{}open/envs?searchValue={}", qlConfig.getUrl(), searchValue))
                    .bearerAuth(this.getQLToken(displayName))
                    .execute();
            log.debug(StrUtil.format("[青龙 - {}] 搜索环境变量, 参数: {}, 响应: {}", displayName, searchValue, response.body()));
            JSONObject respJo = JSONObject.parseObject(response.body());
            JSONArray data = respJo.getJSONArray("data");

            // 返回
            return data.stream().map(d -> {
                JSONObject tmp = (JSONObject) d;
                JSONObject jo = new JSONObject();
                String _id = tmp.getString("_id");
                String id = tmp.getString("id");
                jo.put("id", StrUtil.isNotBlank(_id) ? _id : StrUtil.isNotBlank(id) ? id : "");
                jo.put("name", tmp.getString("name"));
                jo.put("value", tmp.getString("value"));
                jo.put("status", tmp.getInteger("status"));
                jo.put("remarks", tmp.getString("remarks"));
                return jo;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.debug(StrUtil.format("连接青龙发生异常, e: {}", e));
            throw new BizException("连接青龙发生异常, 请联系系统管理员");
        }
    }

    public void disableEnv(String displayName, List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        QLConfig qlConfig = systemConfigProperties.getQLConfigByDisplayName(displayName);
        if (Objects.isNull(qlConfig)) {
            throw new BizException(StrUtil.format("青龙节点【{}】不存在", displayName));
        }
        try {
            log.debug(StrUtil.format("[青龙 - {}] 禁用环境变量, 参数: {}", displayName, JSON.toJSONString(ids)));
            HttpResponse response = HttpRequest.put(qlConfig.getUrl().concat("open/envs/disable"))
                    .bearerAuth(this.getQLToken(displayName))
                    .body(JSON.toJSONString(ids))
                    .execute();
            log.debug(StrUtil.format("[青龙 - {}] 禁用环境变量, 响应: {}", displayName, response.body()));
        } catch (Exception e) {
            log.debug(StrUtil.format("连接青龙发生异常, e: {}", e));
            throw new BizException("连接青龙发生异常, 请联系系统管理员");
        }
    }

    public String getQLToken(String displayName) {
        QLConfig qlConfig = systemConfigProperties.getQLConfigByDisplayName(displayName);
        if (Objects.isNull(qlConfig)) {
            throw new BizException(StrUtil.format("青龙节点【{}】不存在", displayName));
        }
        String token = QL_TOKEN_CACHE.get(displayName);
        if (StrUtil.isNotBlank(token)) {
            return token;
        }
        return this.refreshToken(qlConfig);
    }

    public String refreshToken(QLConfig ql) {
        HttpResponse response = HttpRequest.get(StrUtil.format("{}open/auth/token?client_id={}&client_secret={}", ql.getUrl(), ql.getClientId(), ql.getClientSecret())).execute();
        if (response.getStatus() == HttpStatus.HTTP_OK) {
            JSONObject respJo = JSONObject.parseObject(response.body());
            Integer code = respJo.getInteger("code");
            if (code == HttpStatus.HTTP_OK) {
                String token = respJo.getJSONObject("data").getString("token");
                QL_TOKEN_CACHE.put(ql.getDisplayName(), token);
                return token;
            }
        }
        return null;
    }

//    @Override
//    public void startTimerTask() {
//        scheduleTaskUtil.startCron("QL_timerRefreshToken", () -> this.timerRefreshToken(), "0 0/1 * * * ?");
//    }

    private void timerRefreshToken() {
        List<QLConfig> qlConfigs = systemConfigProperties.getQls();
        qlConfigs.forEach(this::refreshToken);
    }

    /**
     * 获取存在的CK
     *
     * @param displayName displayName
     * @param cookie      cookie
     * @return JSONObject
     */
    private JSONObject getExistCK(String displayName, String cookie) {
        log.debug(StrUtil.format("[青龙 - {}], 获取存在的Cookie: {}", displayName, cookie));
        JSONObject result;
        List<JSONObject> envs = this.searchEnv(displayName, cookie);
        if (envs.size() == 0) {
            result = new JSONObject();
        } else if (envs.size() == 1) {
            result = envs.get(0);
        } else {
            // 通过【pt_pin=xxx;】搜索出来有多个的话, 返回正常状态(status=0)的第一个， 如果没有正常的, 就返回禁用状态(status=1)的第一个
            Map<Integer, List<JSONObject>> statusMap = envs.stream().collect(Collectors.groupingBy(e -> e.getInteger("status")));
            if (statusMap.get(0).size() >= 1) {
                result = statusMap.get(0).get(0);
            } else if (statusMap.get(1).size() >= 1) {
                result = statusMap.get(1).get(0);
            } else {
                result = new JSONObject();
            }
        }
        log.debug(StrUtil.format("[青龙 - {}], 获取存在的Cookie: {}, 最终返回结果: {}", displayName, cookie, result.toJSONString()));
        return result;
    }
}
