<template>
  <div>
    <van-field
      v-model="form.cookie"
      label="Cookie"
      placeholder="pt_key=xxx;pt_pin=xxx;"
      type="textarea"
      autosize
      rows="1"
    />
    <van-field
      readonly
      clickable
      label="选择节点"
      :value="form.displayName"
      placeholder="选择青龙节点"
      @click="showQLPicker = true"
    ></van-field>
    <van-field
      v-model="form.remark"
      label="备注"
      placeholder="备注"
      type="textarea"
      autosize
      size="1"
    />
    <div style="margin: 16px; ">
      <van-button
        round
        block
        :disabled="!(form.cookie && form.displayName)"
        type="warning"
        @click="submitCk"
      >
        提交青龙
      </van-button>
    </div>

    <!-- 选择QL节点的picker -->
    <van-popup v-model="showQLPicker" round position="bottom">
      <van-picker
        show-toolbar
        value-key="displayNameWithDesc"
        :columns="qls"
        @cancel="showQLPicker = false"
        @confirm="conformQL"
      />
    </van-popup>
  </div>
</template>

<script>
import { baseInfo, submitCk } from "@/api";

export default {
  name: "SubmitQL",
  data() {
    return {
      form: {
        cookie: "",
        displayName: "",
        remark: ""
      },
      qls: [],
      showQLPicker: false
    };
  },
  mounted() {
    this.renderBase();
  },
  methods: {
    renderBase: function() {
      baseInfo()
        .then(resp => {
          this.qls = resp.data.qls;
          if (this.qls.length > 0) {
            this.form.displayName = this.qls[0].displayName;
          }
        })
        .catch(err => {
          console.log(err);
        });
    },
    conformQL(ql) {
      this.form.displayName = ql.displayName;
      this.showQLPicker = false;
    },
    submitCk() {
      submitCk(this.form).then(() => {
        let _this = this;
        setTimeout(() => {
          _this.$toast.success("如有需要，请绑定您的WxPusher Uid");
          _this.$emit("change-tab", "bindWxPusher");
        }, 1000);
      });
    }
  }
};
</script>

<style scoped></style>
