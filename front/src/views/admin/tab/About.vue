<template>
  <div style="padding: 16px ">
    <!-- 版本信息 -->
    <div style="text-align: center">
      <van-divider
        :style="{ color: '#1989fa', borderColor: '#1989fa', padding: '0 16px' }"
        >版本信息
      </van-divider>
      <div style="color: #909399; font-size: 14px;margin-bottom: 16px">
        当前版本: v{{ versionInfo.appVersion }}
      </div>
      <div
        v-if="versionInfo.needUpgrade && versionInfo.latestVersion"
        style="color: #F56C6C; font-size: 14px;margin-bottom: 16px"
      >
        检测到可用版本: v{{ versionInfo.latestVersion }}
      </div>
      <van-button
        color="linear-gradient(to right, #ff6034, #ee0a24)"
        @click="checkUpgrade()"
        size="small"
      >
        检查更新
      </van-button>
    </div>
    <!-- 声明 -->
    <div>
      <van-divider
        :style="{ color: '#1989fa', borderColor: '#1989fa', padding: '0 16px' }"
        >声明
      </van-divider>
      <ul>
        <li><p>如果您使用了本项目，代表您默认同意该声明。</p></li>
        <li>
          <p>
            项目和代码仅供参考学习，禁止商用和非法用途，否则，一切后果请用户自负。
          </p>
        </li>
      </ul>
    </div>

    <!-- 更新日志 -->
    <div class="block">
      <van-divider
        :style="{ color: '#1989fa', borderColor: '#1989fa', padding: '0 16px' }"
        >更新日志
      </van-divider>
      <div>
        <div
          v-for="log in updateLog"
          :key="log.date"
          style="margin: 16px 0 0 16px"
        >
          <div style="color: #909399">{{ log.date }}</div>
          <div v-for="l in log.logs" :key="l">
            <div>{{ l }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="block">
      <van-divider
        :style="{ color: '#1989fa', borderColor: '#1989fa', padding: '0 16px' }"
        >打赏开发者
      </van-divider>
      <div style="text-align: center; margin: 16px 0;color: #909399">
        <div>打赏开发者喝杯咖啡吧~</div>
        <van-button
          style="margin-top: 16px"
          color="linear-gradient(to right, #ff6034, #ee0a24)"
          @click="show = true"
          size="small"
        >
          打赏开发者
        </van-button>
      </div>

      <van-image-preview v-model="show" :images="images" :closeable="true">
      </van-image-preview>
    </div>
  </div>
</template>

<script>
import { getVersion, checkUpgrade } from "@/api/admin";

export default {
  name: "About",
  data() {
    return {
      show: false,
      images: ["https://blog-bucket-cdn.yiidii.cn/donate.jpg"],
      updateLog: [
        {
          date: "2022-2-18 11:00",
          logs: ["新增：短信获取验证码，复制CK"]
        },
        {
          date: "2022-2-20 20:00",
          logs: [
            "新增：网站标题、公告定制化",
            "新增：对接青龙，多青龙对接",
            "新增：推送Cookie根据pt_key自动更新、启用",
            "新增：后台登录，支持Gitee、Github、BAIDU登录",
            "新增：后台青龙信息管理",
            "新增：后台社交登录配置"
          ]
        },
        {
          date: "2022-2-24 11:30",
          logs: [
            "新增：WxPusher配置管理页",
            "新增：支持wxPusher一对一推送 (配置文档待补充…)",
            "新增：公告模式支持：顶部、html自定义",
            "修复：获取Cookie后不弹框",
            "修改：提交的备注为空，且ql有该ck，不修改备注",
            "优化：启动后对配置文件检查"
          ]
        },
        {
          date: "2022-02-26",
          logs: [
            "新增：内置检测所有QL Cookie",
            "新增：检测Cookie结果通知管理员 wxPusher UID"
          ]
        },
        {
          date: "2022-02-27",
          logs: [
            "修复：提交QL，token失效",
            "修复：pt_pin中文问题导致更新不上的问题",
            "调整：移除社交登录，改为用户名密码登录"
          ]
        },
        {
          date: "2022-03-01",
          logs: [
            "修复：兼容最新版QL（解决QL 2.11.3等版本更新Cookie异常的情况）",
            "新增：QL配置可编辑",
            "新增：QL最大车位和禁用QL配置",
            "新增：检测系统版本"
          ]
        }
      ],
      versionInfo: {
        appVersion: "",
        latestVersion: "",
        needUpgrade: false
      }
    };
  },
  mounted() {
    this.getVersion();
  },
  methods: {
    getVersion: function() {
      getVersion().then(resp => {
        this.versionInfo = resp.data;
      });
    },
    checkUpgrade: function() {
      checkUpgrade().then(resp => {
        this.versionInfo = resp.data;
      });
    }
  }
};
</script>

<style scoped>
.block {
  margin-top: 48px;
}

ul {
  margin-left: 16px;
}

li {
  margin-top: 16px;
}
</style>
