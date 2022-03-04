<template>
  <div>
    <van-nav-bar title="Source" />
    <van-notice-bar
      left-icon="volume-o"
      text="Q查手机：https://top.maosource.com"
      mode="closeable"
      @click="TZ()"
    />
    <van-notice-bar
      v-if="notice && noticeModel === 'TOP'"
      left-icon="volume-o"
      :text="notice"
      mode="closeable"
    />

    <!-- title -->
    <div>
      <div
        v-if="title"
        style="text-align: center; margin: 40px 0 20px 0; font-size: 32px"
      >
        {{ title }}
      </div>

      <!-- sub title -->
      <div v-if="notice && noticeModel === 'HTML'" v-html="notice"></div>
    </div>

    <div>
      <JD @change-tab="changeActive" />
    </div>
  </div>
</template>

<script>
import JD from "./JD";
import { baseInfo } from "@/api";

export default {
  name: "Index",
  components: { JD },
  data() {
    return {
      active: "jd",
      title: "",
      notice: "",
      noticeModel: ""
    };
  },
  mounted() {
    this.renderBase();
  },
  methods: {
    TZ() {
      window.location.href = "https://top.maosource.com";
    },
    renderBase: function() {
      baseInfo()
        .then(resp => {
          this.title = resp.data.title;
          this.notice = resp.data.notice;
          this.noticeModel = resp.data.noticeModel;
        })
        .catch(err => {
          console.log(err);
        });
    },
    changeActive: function(tabName) {
      this.active = tabName;
    }
  }
};
</script>

<style scoped>
body {
  max-width: 750px;
}
</style>
