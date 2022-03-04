<template>
  <div>
    <div>
      <van-field
        v-model="cookie"
        rows="1"
        autosize
        label="Cookie"
        type="textarea"
        placeholder="pt_key=xxx;pt_pin=xxx;"
      />
      <van-field
        v-model="wxPusherUid"
        rows="2"
        autosize
        label="UID"
        type="textarea"
        placeholder="UID_xxxxxx"
      />
      <div style="padding: 8px">
        <van-button round block type="primary" @click="bindWxPusher()"
          >绑定</van-button
        >
      </div>
    </div>

    <div v-if="wxPusherQrUrl">
      <div style="text-align: center">
        <van-image width="200" height="200" fit="contain" :src="wxPusherQrUrl">
          <template v-slot:loading>
            <van-loading type="spinner" size="20" />
          </template>
        </van-image>
      </div>
      <div style="padding: 16px 0 0 32px">
        1. 扫码关注WxPusher <br />
        2. 公众号页面点击我的 -> 我的UID <br />
        3. 输入Cookie和UID即可绑定 <br />
      </div>
    </div>
  </div>
</template>

<script>
import { baseInfo, bindWXPusher } from "@/api";

export default {
  name: "BindWxPusher",
  data() {
    return {
      cookie: "",
      wxPusherUid: "",
      wxPusherQrUrl: ""
    };
  },
  mounted() {
    this.renderBase();
  },
  methods: {
    renderBase: function() {
      baseInfo()
        .then(resp => {
          this.wxPusherQrUrl = resp.data.wxPusherQrUrl;
        })
        .catch(err => {
          console.log(err);
        });
    },
    bindWxPusher: function() {
      let param = { cookie: this.cookie, wxPusherUid: this.wxPusherUid };
      bindWXPusher(param)
        .then(() => {})
        .catch(err => {
          console.error(err);
        });
    }
  }
};
</script>

<style scoped></style>
