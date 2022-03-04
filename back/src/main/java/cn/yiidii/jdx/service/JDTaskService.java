package cn.yiidii.jdx.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.yiidii.jdx.config.prop.JDUserConfigProperties;
import cn.yiidii.jdx.config.prop.JDUserConfigProperties.JDUserConfig;
import cn.yiidii.jdx.config.prop.SystemConfigProperties;
import cn.yiidii.jdx.config.prop.SystemConfigProperties.QLConfig;
import cn.yiidii.jdx.model.ex.BizException;
import cn.yiidii.jdx.util.JDXUtil;
import cn.yiidii.jdx.util.ScheduleTaskUtil;
import cn.yiidii.jdx.util.WXPushUtil;
import cn.yiidii.jdx.util.jd.CheckJDCKUtil;
import cn.yiidii.jdx.util.jd.CheckJDCKUtil.CheckCookieResult;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * JDTaskService
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JDTaskService {

    private final SystemConfigProperties systemConfigProperties;
    private final JDUserConfigProperties jdUserConfigProperties;
    private final QLService qlService;
    private final ScheduleTaskUtil scheduleTaskUtil;

    public List<JSONObject> timerCheckCookie() {
        // 所有青龙节点
        List<QLConfig> qlConfigs = systemConfigProperties.getQls();
        ThreadPoolTaskExecutor asyncExecutor = SpringUtil.getBean("asyncExecutor", ThreadPoolTaskExecutor.class);
        List<JSONObject> result = new ArrayList<>(16);
        for (QLConfig qlConfig : qlConfigs) {
            List<JSONObject> envs = qlService.searchEnv(qlConfig.getDisplayName(), "JD_COOKIE")
                    .stream().filter(e -> e.getInteger("status") == 0).collect(Collectors.toList());
            // 执行所有ck
            List<CompletableFuture<CheckCookieResult>> completableFutures = envs.stream().map(env -> CompletableFuture.supplyAsync(() -> {
                String value = env.getString("value");
                Thread.currentThread().setName(StrUtil.format("checkCookie_{}", JDXUtil.getPtPinFromCK(value)));
                CheckCookieResult checkCookieResult = CheckJDCKUtil.checkCookie(value);
                checkCookieResult.set_id(env.getString("id"));
                return checkCookieResult;
            }, asyncExecutor)).collect(Collectors.toList());
            // 等待所有任务执行完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]));
            CompletableFuture<List<CheckCookieResult>> finalResults = allFutures.thenApply(v -> completableFutures.stream().map(accountFindingFuture -> accountFindingFuture.join()).collect(Collectors.toList()));
            try {
                List<CheckCookieResult> checkCookieResults = finalResults.get();
                List<String> expiredPtPins = checkCookieResults.stream().filter(CheckCookieResult::isExpired).peek(r -> {
                    // 通知到微信
                    String ptPin = r.getPtPin();
                    JDUserConfig userConfig = jdUserConfigProperties.getByPtPin(ptPin);
                    if (Objects.isNull(userConfig)) {
                        return;
                    }
                    String wxPusherUid = userConfig.getWxPusherUid();
                    WXPushUtil.send(jdUserConfigProperties.getAppToken(),
                            Arrays.asList(wxPusherUid),
                            "Cookie失效通知",
                            StrUtil.format("{}, {}", r.getPtPin(), r.getRemark()),
                            "1");
                }).map(CheckCookieResult::getPtPin).collect(Collectors.toList());
                List<String> ids = checkCookieResults.stream().filter(CheckCookieResult::isExpired).map(CheckCookieResult::get_id).collect(Collectors.toList());
                try {
                    qlService.disableEnv(qlConfig.getDisplayName(), ids);
                } catch (Exception e) {
                    log.error("定时检查cookie时, 禁用环境变量发生异常, displayName: {}", qlConfig.getDisplayName());
                }
                if (CollUtil.isNotEmpty(expiredPtPins)) {
                    JSONObject jo = new JSONObject();
                    jo.put("displayName", qlConfig.getDisplayName());
                    jo.put("expiredPtPins", expiredPtPins);
                    result.add(jo);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.warn(StrUtil.format("检查Cookie任务异常, e: {}", e.getMessage()));
                throw new BizException("检查Cookie任务异常");
            }

        }

        String adminUid = jdUserConfigProperties.getAdminUid();
        if (CollUtil.isNotEmpty(result) && StrUtil.isNotBlank(adminUid)) {
            String adminContent = result.stream().map(jo -> StrUtil.format("🐉节点：{}\r\n{}\r\n\r\n已自动禁用", jo.getString("displayName"), CollUtil.join(jo.getJSONArray("expiredPtPins"), "\r\n")))
                    .collect(Collectors.joining("\r\n"));
            WXPushUtil.send(jdUserConfigProperties.getAppToken(),
                    Arrays.asList(adminUid),
                    "Cookie失效通知",
                    adminContent,
                    "1");
        }
        return result;
    }

//    @Override
//    public void startTimerTask() {
//        scheduleTaskUtil.startCron("jdTask_checkCookie", this::timerCheckCookie, "0 0 12 * * ?");
//    }
}
