<template>
  <div>
    <van-field
      v-model="form.mobile"
      name="mobile"
      label="手机号"
      placeholder="手机号"
      clearable
    ></van-field>
    <van-field
      v-model="form.qq"
      name="mobile"
      label="QQ"
      placeholder="QQ"
      clearable
    ></van-field>
    <van-field
      v-model="form.code"
      name="code"
      label="验证码"
      placeholder="验证码"
    >
      <template #button>
        <van-count-down
          v-if="Number(expireTime) > 0"
          ref="countDown"
          millisecond
          :time="expireTime"
          format="ss:SSS"
        />
        <van-button
          :disabled="!form.mobile"
          round
          v-else
          size="small"
          plain
          type="info"
          @click="smsCode"
          >发送验证码
        </van-button>
      </template>
    </van-field>

    <div class="contact">
      <van-button
        style="background: rgb(7,193,96);"
        native-type="submit"
        type="primary"
        id="but"
        :disabled="!form.code"
        @click="gitCk"
        >获取CK
      </van-button>
      <div style="margin-left: 10px">
        <van-button
          style="width: 100%"
          type="default"
          :disabled="!form.code"
          @click="ifOnBus"
          >直接上车
        </van-button>
      </div>
    </div>
    <!--    <div style="margin: 16px; display: flex; justify-content: space-around;">-->
    <!--      <van-button-->
    <!--        style="width: 200px"-->
    <!--        round-->
    <!--        block-->
    <!--        :disabled="!form.code"-->
    <!--        type="primary"-->
    <!--        @click="gitCk"-->
    <!--      >-->
    <!--        获取CK-->
    <!--      </van-button>-->
    <!--      <van-button-->
    <!--        style="width: 200px"-->
    <!--        round-->
    <!--        block-->
    <!--        :disabled="!form.code"-->
    <!--        type="info"-->
    <!--        @click="ifOnBus"-->
    <!--      >-->
    <!--        直接上车-->
    <!--      </van-button>-->
    <!--    </div>-->
  </div>
</template>
<script>
import { baseInfo, jdSmsCode, jdLogin } from "@/api";
import { Dialog, Notify, Toast } from "vant";

export default {
  data() {
    return {
      expireTime: 0,
      form: {
        mobile: "",
        qq: "",
        code: "",
        ifBus: 0
      }
    };
  },
  mounted() {
    this.renderBase();
  },
  methods: {
    renderBase: function() {
      baseInfo()
        .then(resp => {
          this.title = resp.data.title;
          this.notice = resp.data.notice;
          this.noticeModel = resp.data.noticeModel;
          this.qls = resp.data.qls;
          if (this.qls.length > 0) {
            this.form.displayName = this.qls[0];
          }
        })
        .catch(err => {
          console.log(err);
        });
    },
    smsCode: function() {
      if (this.form.mobile <= 11) {
        Toast({ message: "请输入正确的手机号~", position: "top" });
      } else {
        this.form.code = "";
        jdSmsCode(this.form.mobile).then(resp => {
          this.expireTime = resp.data.expireTime * 1000;
        });
      }
    },
    ifOnBus() {
      this.form.ifBus = 1;
      this.checkQQ();
    },
    gitCk() {
      this.form.ifBus = 0;
      this.checkQQ();
    },
    checkQQ() {
      console.log(this.form);
      if (this.form.qq <= 5) {
        let _this = this;
        Dialog.confirm({
          message: "确定不添加QQ?<br>机器人查询是通过QQ绑定的。"
        })
          .then(() => {
            _this.login();
          })
          .catch(() => {
            // on cancel
          });
      } else {
        this.login();
      }
    },
    login: function() {
      let _this = this;
      if (_this.model === "ck") {
        this.form.displayName = "";
        this.form.remark = "";
      }
      jdLogin(this.form)
        .then(function(response) {
          // 计时器清零
          _this.expireTime = 0;
          localStorage.setItem("ptPin", response.data.ptPin);
          if (_this.form.ifBus === 1) {
            Notify({ type: "success", message: "上车成功" });
          } else {
            // 弹框
            _this.$dialog
              .alert({
                title: "获取cookie成功,添加成功",
                message: response.data.cookie,
                confirmButtonText: "点击复制"
              })
              .then(() => {
                _this
                  .$copyText(response.data.cookie)
                  .then(() => {
                    _this.$toast.success("复制成功");
                    setTimeout(() => {
                      _this.$toast.success("如有需要，请选择直接上车。");
                      _this.$emit("change-tab", "submitQL");
                    }, 1000);
                  })
                  .catch(() => {
                    _this.$toast.fail("复制失败，请手动复制");
                  });
              });
          }

          if (_this.model === "ck") {
            // ignore
          }
          // ql模式
          if (_this.model === "ql") {
            // ignore
          }
        })
        .catch(function(error) {
          console.error(error);
        });
    }
  }
};
</script>

<style scoped>
.contact {
  margin-top: 20px;
  display: flex;
  flex: 1;
  justify-content: center;
}
.contact button {
  width: 70%;
  height: 40px;
  border-radius: 8px;
  background: rgba(43, 142, 251, 1);
  font-family: PingFang SC;
  color: rgba(254, 254, 254, 1);
  border: none 0;
}
</style>
