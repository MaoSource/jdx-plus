package cn.yiidii.jdx.util.jd;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yiidii.jdx.util.JDXUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * CheckCk
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class CheckJDCKUtil {

    private static final String JD_BASE_API_URL = "https://api.m.jd.com/client.action";

    public CheckCookieResult checkCookie(String cookie) {
        boolean expired = false;
        String ptPin = JDXUtil.getPtPinFromCK(cookie);
        String nickName = ptPin;
        String remark = "Cookie有效";
        boolean useInterface2 = false;

        HttpResponse response;
        JSONObject respJo;
        try {
            response = HttpRequest.get("https://me-api.jd.com/user_new/info/GetJDUserInfoUnion")
                    .cookie(cookie)
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口1响应: {}", ptPin, respJo));
            if (CollUtil.isNotEmpty(respJo)) {
                String retCode = respJo.getString("retcode");
                if (StrUtil.equals("1001", retCode)) {
                    expired = true;
                    remark = "Cookie已失效";
                } else if (StrUtil.equals("0", retCode)) {
                    JSONObject userInfo = respJo.getJSONObject("data").getJSONObject("userInfo");
                    if (CollUtil.isNotEmpty(userInfo)) {
                        nickName = userInfo.getJSONObject("baseInfo").getString("nickname");
                    }
                } else {
                    // 未知状态
                    useInterface2 = true;
                    remark = "JD返回未知状态";
                }
            } else {
                // 京东接口返回空数据
                useInterface2 = true;
                remark = "京东接口返回空数据";
            }

        } catch (Exception e) {
            useInterface2 = true;
            log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口q响应异常, e: {}", ptPin, e.getMessage()));
        }

        if (useInterface2) {
            log.debug(StrUtil.format("[检查Cookie], 继续采用接口2, pt_pin: {}", ptPin));
            try {
                response = HttpRequest.get("https://plogin.m.jd.com/cgi-bin/ml/islogin")
                        .cookie(cookie)
                        .execute();
                respJo = JSONObject.parseObject(response.body());
                log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口2响应: {}", ptPin, respJo));
                if (CollUtil.isNotEmpty(respJo)) {
                    String islogin = respJo.getString("islogin");
                    if (StrUtil.equals("1", islogin)) {
                        //
                    } else if (StrUtil.equals("0", islogin)) {
                        expired = true;
                    } else {
                        remark = "planB京东接口返回未知状态";
                    }
                } else {
                    remark = "planB京东接口返回空数据";
                }
            } catch (Exception e) {
                // ignore
                log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口2响应异常, e: {}", ptPin, e.getMessage()));
            }
        }

        CheckCookieResult result = new CheckCookieResult().setExpired(expired).setPtPin(ptPin).setNickName(nickName).setRemark(remark);
        log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 最终结果: {}", ptPin, JSONObject.toJSONString(result)));
        return result;
    }

    public JDAssetResult getJDAsset(String cookie) {
        if (checkCookie(cookie).isExpired()) {
            return null;
        }
        String ptPin = JDXUtil.getPtPinFromCK(cookie);
        JDAssetResult jdAssetResult = new JDAssetResult()
                .setPtPin(ptPin);
        HttpResponse response;
        JSONObject respJo;
        // totalBean接口1
        try {
            response = HttpRequest.get("https://me-api.jd.com/user_new/info/GetJDUserInfoUnion")
                    .cookie(cookie)
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            log.debug(StrUtil.format("[京东资产变动], pt_pin: {}, GetJDUserInfoUnion响应: {}", ptPin, respJo));
            if (CollUtil.isNotEmpty(respJo)) {
                String retCode = respJo.getString("retcode");
                if (StrUtil.equals("0", retCode)) {
                    JSONObject userInfo = respJo.getJSONObject("data").getJSONObject("userInfo");
                    if (CollUtil.isNotEmpty(userInfo)) {
                        jdAssetResult.setNickName(userInfo.getJSONObject("baseInfo").getString("nickname"));
                        jdAssetResult.setLevelName(userInfo.getJSONObject("baseInfo").getString("levelName"));
                    }
                    JSONObject assetInfo = respJo.getJSONObject("data").getJSONObject("assetInfo");
                    if (CollUtil.isNotEmpty(assetInfo)) {
                        jdAssetResult.setBeanCount(assetInfo.getInteger("beanNum"));
                    }
                }
            }
        } catch (Exception e) {

        }

        // totalBean接口2
        try {
            response = HttpRequest.post(StrUtil.format("https://wxapp.m.jd.com/kwxhome/myJd/home.json?&useGuideModule=0&bizId=&brandId=&fromType=wxapp&timestamp={}", System.currentTimeMillis() / 1000))
                    .cookie(cookie)
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            if (respJo.containsKey("user")) {
                JSONObject userJo = respJo.getJSONObject("user");
                jdAssetResult.setJingxiang(userJo.getString("uclass"));
                if (StrUtil.isBlank(jdAssetResult.getNickName())) {
                    jdAssetResult.setNickName(userJo.getString("petName"));
                }
            }
        } catch (Exception e) {
        }

        // 汪汪乐园
        try {
            response = HttpRequest.post(StrUtil.format("https://api.m.jd.com/client.action?functionId=joyBaseInfo"))
                    .body("body={\"taskId\":\"\",\"inviteType\":\"\",\"inviterPin\":\"\",\"linkId\":\"LsQNxL7iWDlXUs6cFl-AAg\"}&appid=activities_platform")
                    .header(Header.ORIGIN, "https://joypark.jd.com")
                    .header(Header.REFERER, "https://joypark.jd.com/?activityId=LsQNxL7iWDlXUs6cFl-AAg&lng=113.387899&lat=22.512678&sid=4d76080a9da10fbb31f5cd43396ed6cw&un_area=19_1657_52093_0")
                    .cookie(cookie)
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            if (respJo.getBoolean("success")) {
                jdAssetResult.setJoyLevel(respJo.getJSONObject("data").getInteger("level"));
            }
        } catch (Exception e) {
        }

        // 京东赚赚
        try {
            response = HttpRequest.post(StrUtil.format("{}?functionId=interactTaskIndex&body=\\{\\}&client=wh5&clientVersion=9.1.0", JD_BASE_API_URL))
                    .cookie(cookie)
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            if (StrUtil.equals("0", respJo.getString("code"))) {
                jdAssetResult.setJdzzNum(respJo.getJSONObject("data").getInteger("totalNum"));
            }
        } catch (Exception e) {
        }

        // 京东秒杀
        try {
            response = HttpRequest.post(StrUtil.format("{}?functionId=homePageV2&body=\\{\\}&client=wh5&clientVersion=1.0.0&appid=SecKill2020", JD_BASE_API_URL))
                    .cookie(cookie)
                    .header(Header.ORIGIN, "https://h5.m.jd.com")
                    .header(Header.REFERER, "https://h5.m.jd.com/babelDiy/Zeus/2NUvze9e1uWf4amBhe1AV6ynmSuH/index.html")
                    .contentType(ContentType.FORM_URLENCODED.getValue())
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            if (CollUtil.isNotEmpty(respJo)) {
                jdAssetResult.setJdms(respJo.getJSONObject("result").getJSONObject("assignment").getInteger("assignmentPoints"));
            }
        } catch (Exception e) {

        }
        // 东东农场
        try {
            //{"version":14,"channel":1,"babelChannel":"120"}
            response = HttpRequest.post(StrUtil.format("{}?functionId=taskInitForFarm&appid=wh5&body={\"version\":14,\"channel\":1,\"babelChannel\":\"120\"}", JD_BASE_API_URL))
                    .cookie(cookie)
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            if (StrUtil.equals("0", respJo.getString("code"))) {
                jdAssetResult.setTotalWaterTaskTimes(respJo.getJSONObject("totalWaterTaskInit").getInteger("totalWaterTaskTimes"));
            }
        } catch (Exception e) {
        }
        // 极速金币 TODO
        // 京喜牧场 TODO
        return jdAssetResult;
    }

    @Data
    @Accessors(chain = true)
    public static class CheckCookieResult {

        private String _id;
        private String ptPin;
        private String nickName;
        private boolean expired;
        private String remark;
    }

    @Data
    @Accessors(chain = true)
    public static class JDAssetResult {

        private String _id;
        private String ptPin;
        private String nickName;
        private String levelName;
        private Integer beanCount;
        private String jingxiang;
        private Integer joyLevel;
        private Integer jdzzNum;
        private Integer jdms;
        private Integer totalWaterTaskTimes;

    }
}
