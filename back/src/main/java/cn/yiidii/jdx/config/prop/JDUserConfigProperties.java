package cn.yiidii.jdx.config.prop;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.yiidii.jdx.model.ex.BizException;
import cn.yiidii.jdx.util.JDXUtil;
import cn.yiidii.jdx.util.ScheduleTaskUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDInfoConfigProperties
 *
 * @author ed w
 * @since 1.0
 */
@Data
@Slf4j
@Component
public class JDUserConfigProperties implements InitializingBean {

    public static final String JD_USER_CONFIG_FILE_PAH = System.getProperty("user.dir") + File.separator + "config" + File.separator + "JDInfoConfig.json";
    private static boolean INIT = false;
    @JSONField(serialize = false, deserialize = false)
    private final ScheduleTaskUtil scheduleTaskUtil;

    private String appToken;
    private String adminUid;
    private String wxPusherQrUrl;
    private List<JDUserConfig> jdUsers = new ArrayList<>();

    @PostConstruct
    public void init() {
        INIT = true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        update(true);
    }

    public String update(boolean throwException) {
        try {
            String configStr = FileUtil.readUtf8String(JD_USER_CONFIG_FILE_PAH);
            JSONObject configJo = JSONObject.parseObject(configStr);
            BeanUtil.copyProperties(configJo, this);
            return configJo.toJSONString();
        } catch (Exception e) {
            if (throwException) {
                throw new IllegalArgumentException(StrUtil.format("{}不存在", JD_USER_CONFIG_FILE_PAH));
            }
            log.error("JDUser更新配置文件[{}]发生异常, e: {}", JD_USER_CONFIG_FILE_PAH, e.getMessage());
            return null;
        }
    }

    @Data
    @Slf4j
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JDUserConfig {

        private String ptPin;
        private String wxPusherUid;
    }


    public JDUserConfig getByPtPin(String ptPin) {
        return this.getJdUsers().stream().filter(e -> e.getPtPin().equals(ptPin)).findFirst().orElse(null);
    }

    public void bindWXPusherUid(String cookie, String wxPusherUid) {
        String ptPin = JDXUtil.getPtPinFromCK(cookie);
        if (StrUtil.isBlank(ptPin)) {
            throw new BizException("Cookie格式不正确");
        }
        JDUserConfig jdUserConfig = this.getByPtPin(ptPin);
        if (Objects.nonNull(jdUserConfig)) {
            jdUserConfig.setWxPusherUid(wxPusherUid);
        } else {
            this.getJdUsers().add(new JDUserConfig(ptPin, wxPusherUid));
        }
    }

//    @Override
//    public void startTimerTask() {
//        scheduleTaskUtil.startCron("SYS_timerPersistSystemConfig", () -> this.timerPersistJDUserConfig(), "0/30 * * * * ?");
//    }

    private void timerPersistJDUserConfig() {
        if (!INIT) {
            return;
        }
        Thread.currentThread().setName(String.format(Thread.currentThread().getName(), "SYS_timerPersistJDUserConfig"));
        String prettyJa = JSONUtil.toJsonPrettyStr(JSONObject.toJSONString(this));
        FileUtil.writeString(prettyJa, JD_USER_CONFIG_FILE_PAH, StandardCharsets.UTF_8);
    }
}
