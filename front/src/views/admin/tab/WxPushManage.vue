<template>
  <div>
    <div style="margin: 16px 0">
      <van-swipe-cell>
        <van-cell-group inset>
          <van-cell title="appToken" :value="appToken" />
          <van-cell title="管理员UID" :value="adminUid" />
          <van-cell title="二维码连接" :value="wxPusherQrUrl" />
        </van-cell-group>

        <template #right>
          <van-button
            square
            type="info"
            class="slide-button"
            text="编辑"
            @click="wxPusherConfigVisible = true"
          />
        </template>
      </van-swipe-cell>

      <div v-if="wxPusherQrUrl" style="margin-top: 24px">
        <div style="text-align: center">
          <div style="margin: 4px 0; ">二维码预览</div>
          <van-image
            width="200"
            height="200"
            fit="contain"
            :src="wxPusherQrUrl"
          >
            <template v-slot:loading>
              <van-loading type="spinner" size="20" />
            </template>
          </van-image>
        </div>
      </div>

      <van-action-sheet v-model="wxPusherConfigVisible" title="编辑WxPusher">
        <van-form>
          <van-field
            v-model="appToken"
            label="appToken"
            placeholder="appToken"
          />
          <van-field
            v-model="adminUid"
            label="管理员UID"
            autosize
            type="textarea"
            placeholder="管理员UID"
          />
          <van-field
            v-model="wxPusherQrUrl"
            label="二维码地址"
            autosize
            type="textarea"
            placeholder="二维码地址"
          />
          <div style="margin: 16px;">
            <van-button round block type="info" @click="updateWxPusher()"
              >提交
            </van-button>
          </div>
        </van-form>
      </van-action-sheet>
    </div>
  </div>
</template>

<script>
import { getWxPusher, updateWxPusher } from "@/api/admin";

export default {
  name: "WxPushManage",
  data() {
    return {
      appToken: "",
      wxPusherQrUrl: "",
      adminUid: "",
      wxPusherConfigVisible: false
    };
  },
  mounted() {
    this.render();
  },
  methods: {
    render: function() {
      getWxPusher().then(resp => {
        this.appToken = resp.data.appToken;
        this.wxPusherQrUrl = resp.data.wxPusherQrUrl;
        this.adminUid = resp.data.adminUid;
      });
    },
    updateWxPusher: function() {
      let param = {
        appToken: this.appToken,
        wxPusherQrUrl: this.wxPusherQrUrl,
        adminUid: this.adminUid
      };
      updateWxPusher(param).then(() => {
        this.wxPusherConfigVisible = false;
      });
    }
  }
};
</script>

<style scoped></style>
