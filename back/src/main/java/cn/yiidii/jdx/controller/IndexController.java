package cn.yiidii.jdx.controller;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import cn.yiidii.jdx.config.prop.JDUserConfigProperties;
import cn.yiidii.jdx.config.prop.SystemConfigProperties;
import cn.yiidii.jdx.model.R;
import cn.yiidii.jdx.model.dto.JdInfo;
import cn.yiidii.jdx.model.ex.BizException;
import cn.yiidii.jdx.service.JdService;
import cn.yiidii.jdx.service.QLService;
import cn.yiidii.jdx.util.JDXUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JdController
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class IndexController {

    private final JdService jdService;
    private final QLService qlService;
    private final SystemConfigProperties systemConfigProperties;
    private final JDUserConfigProperties jdUserConfigProperties;

    @GetMapping("/jd/smsCode")
    public R<JdInfo> qrCode(@RequestParam @NotNull(message = "请填写手机号") String mobile) throws Exception {
        Assert.isTrue(PhoneUtil.isMobile(mobile), () -> {
            throw new BizException("手机号格式不正确");
        });
        JdInfo jdInfo = jdService.sendSmsCode(mobile);
        log.info(StrUtil.format("{}发送了验证码", DesensitizedUtil.mobilePhone(mobile)));
        return R.ok(jdInfo, "发送验证码成功");
    }

    @PostMapping("/jd/login")
    public R<JdInfo> login(@RequestBody JSONObject paramJo) throws Exception {
        String mobile = paramJo.getString("mobile");
        Long qq = paramJo.getLong("qq");
        int ifBus = paramJo.getIntValue("ifBus");
        String code = paramJo.getString("code");
        Assert.isTrue(StrUtil.isNotBlank(mobile), () -> {
            throw new BizException("手机号不能为空");
        });
        Assert.isTrue(PhoneUtil.isMobile(mobile), () -> {
            throw new BizException("手机号格式不正确");
        });
        Assert.isTrue(StrUtil.isNotBlank(code), () -> {
            throw new BizException("验证码不能为空");
        });

        JdInfo jdInfo = jdService.login(mobile, code);
        if (ifBus == 1) {
            String s = jdService.addXdd(jdInfo.getCookie(), qq);
            log.info(s);
            return R.ok();
        }else {
            log.info(StrUtil.format("{}获取了京东Cookie", DesensitizedUtil.mobilePhone(mobile)));
            return R.ok(jdInfo, "获取cookie成功");
        }
    }

    @PostMapping("/ql/submitCk")
    public R<JdInfo> submitCk(@RequestBody JSONObject paramJo) throws Exception {
        String cookie = paramJo.getString("cookie");
        String displayName = paramJo.getString("displayName");
        String remark = paramJo.getString("remark");
        Assert.isTrue(StrUtil.isNotBlank(cookie), () -> {
            throw new BizException("Cookie不能为空");
        });
        Assert.isTrue(StrUtil.isNotBlank(displayName), () -> {
            throw new BizException("请选择QL节点");
        });

        qlService.submitCk(displayName, cookie, remark);
        log.info(StrUtil.format("ptPin:{}提交Cookie至【{}】成功", JDXUtil.getPtPinFromCK(cookie), displayName));
        return R.ok(null, StrUtil.format("提交至【{}】成功", displayName));
    }

    @GetMapping("info")
    public R<?> getBaseInfo() {
        JSONObject jo = new JSONObject();
        jo.put("title", systemConfigProperties.getTitle());
        jo.put("notice", systemConfigProperties.getNotice());
        jo.put("noticeModel", systemConfigProperties.getNoticeModel());
        List<JSONObject> qls = systemConfigProperties.getQls().stream().map(e -> {
            JSONObject j = JSON.parseObject(JSON.toJSONString(e));
            j.remove("url");
            j.remove("clientId");
            j.remove("clientSecret");
            String desc = e.getDisabled() == 1 ? "（已禁用）" : e.getUsed() >= e.getMax() ? "（车位已满）" : "";
            if (e.getUsed() >= e.getMax()) {
                j.put("disabled", 1);
            }
            j.put("displayNameWithDesc", e.getDisplayName() + desc);
            return j;
        }).collect(Collectors.toList());
        jo.put("qls", qls);

        jo.put("wxPusherQrUrl", jdUserConfigProperties.getWxPusherQrUrl());
        return R.ok(jo);
    }

    /**
     * 绑定wxPusherUid
     *
     * @param paramJo 参数
     * @return R
     */
    @PostMapping("bindWXPusher")
    public R<?> bindWXPush(@RequestBody JSONObject paramJo) {
        String cookie = paramJo.getString("cookie");
        String wxPusherUid = paramJo.getString("wxPusherUid");
        Assert.isTrue(StrUtil.isNotBlank(cookie), () -> {
            throw new BizException("pt_pin不能为空");
        });
        Assert.isTrue(StrUtil.isNotBlank(wxPusherUid), () -> {
            throw new BizException("wxPusherUid不能为空");
        });
        jdUserConfigProperties.bindWXPusherUid(cookie, wxPusherUid);
        return R.ok(null, "绑定成功");
    }
}
