package cn.yiidii.jdx.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * WXPushUtil
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class WXPushUtil {

    private static final String PUSH_URL = "http://wxpusher.zjiecode.com/api/send/message";

    public void send(String appToken, List<String> uids, String title, String content, String contentType) {
        JSONObject reqParamJo = new JSONObject();
        reqParamJo.put("appToken", appToken);
        reqParamJo.put("content", content);
        reqParamJo.put("summary", title);
        reqParamJo.put("contentType", contentType);
        reqParamJo.put("uids", uids);
        log.debug(StrUtil.format("wxPusher发送消息, 参数: {}", reqParamJo.toJSONString()));
        HttpResponse resp = HttpRequest.post(PUSH_URL)
                .body(reqParamJo.toJSONString())
                .execute();
        log.debug(StrUtil.format("wxPusher发送消息, 响应: {}", resp.body()));
    }

}
