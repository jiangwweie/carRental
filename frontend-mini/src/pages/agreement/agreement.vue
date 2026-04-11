<template>
  <view class="container">
    <scroll-view class="agreement-content" scroll-y>
      <view class="title">租车服务协议</view>
      <view class="update-time">更新日期：2026年4月11日</view>

      <view class="section">
        <view class="section-title">一、服务说明</view>
        <view class="paragraph">
          1.1 本协议是您（以下简称"用户"）与本平台（以下简称"平台"）之间关于使用平台提供的汽车租赁服务所订立的协议。
        </view>
        <view class="paragraph">
          1.2 平台通过微信小程序为用户提供车辆浏览、在线预订、租赁管理等服务。用户注册并使用本服务即视为已充分理解并接受本协议全部条款。
        </view>
        <view class="paragraph">
          1.3 平台保留随时修改本协议的权利，修改后的协议将在平台上公示，继续使用服务视为接受修改后的协议。
        </view>
      </view>

      <view class="section">
        <view class="section-title">二、用户权利与义务</view>
        <view class="paragraph">
          2.1 用户有权按照平台公布的规则和流程预订和使用租赁车辆。
        </view>
        <view class="paragraph">
          2.2 用户应提供真实、准确、完整的个人信息，并在信息变更时及时更新。
        </view>
        <view class="paragraph">
          2.3 用户应妥善保管账户信息，因用户个人原因导致的账户泄露及损失由用户自行承担。
        </view>
        <view class="paragraph">
          2.4 用户承诺遵守交通法规，安全文明驾驶，不得将租赁车辆用于违法活动。
        </view>
        <view class="paragraph">
          2.5 用户应按照约定用途使用车辆，不得擅自改装、转租、抵押或交由无驾驶资格人员驾驶。
        </view>
      </view>

      <view class="section">
        <view class="section-title">三、费用与支付</view>
        <view class="paragraph">
          3.1 租赁费用包括租金、押金及平台公示的其他相关费用，具体金额以预订页面显示为准。
        </view>
        <view class="paragraph">
          3.2 用户需在确认订单时完成支付，支付方式以平台支持的微信支付等为准。
        </view>
        <view class="paragraph">
          3.3 超时还车将按平台公布的超时费率加收费用；提前还车不退还已支付的全天费用。
        </view>
        <view class="paragraph">
          3.4 取消订单的退款政策：取车前24小时取消全额退款，24小时内取消收取50%违约金，取车后取消不予退款。
        </view>
        <view class="paragraph">
          3.5 押金将在还车确认无违章及车损后按规定时间原路退还。
        </view>
      </view>

      <view class="section">
        <view class="section-title">四、违约责任</view>
        <view class="paragraph">
          4.1 用户违反本协议约定或法律法规的，平台有权立即终止服务并要求用户承担相应责任。
        </view>
        <view class="paragraph">
          4.2 因用户原因造成车辆损坏的，用户应承担维修费用及由此产生的停运损失。
        </view>
        <view class="paragraph">
          4.3 用户逾期归还车辆的，除支付超时费用外，平台有权要求用户赔偿由此造成的其他损失。
        </view>
        <view class="paragraph">
          4.4 因不可抗力（如自然灾害、政府行为等）导致无法履行协议的，双方互不承担违约责任。
        </view>
      </view>

      <view class="section">
        <view class="section-title">五、隐私政策</view>
        <view class="paragraph">
          5.1 平台承诺按照国家相关法律法规保护用户的个人信息安全。
        </view>
        <view class="paragraph">
          5.2 平台收集的信息包括但不限于：微信昵称、手机号、驾驶证信息、订单记录等，仅用于提供服务和身份验证。
        </view>
        <view class="paragraph">
          5.3 未经用户授权，平台不会将用户个人信息出售或泄露给第三方，法律法规要求或司法机关依法调取的除外。
        </view>
        <view class="paragraph">
          5.4 用户有权查询、更正、删除自己的个人信息，可通过平台客服渠道行使上述权利。
        </view>
      </view>

      <view class="section">
        <view class="section-title">六、争议解决</view>
        <view class="paragraph">
          6.1 本协议的订立、执行和解释及争议的解决均适用中华人民共和国法律。
        </view>
        <view class="paragraph">
          6.2 双方因本协议发生争议的，应首先通过友好协商解决；协商不成的，任何一方均可向平台所在地有管辖权的人民法院提起诉讼。
        </view>
        <view class="paragraph">
          6.3 在争议解决期间，除争议事项外，双方应继续履行本协议的其他条款。
        </view>
      </view>

      <view class="bottom-spacer" />
    </scroll-view>

    <view class="bottom-bar">
      <button class="agree-btn" @click="onAgree">我已阅读</button>
    </view>
  </view>
</template>

<script setup>
function onAgree() {
  uni.navigateBack()
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
}

.agreement-content {
  flex: 1;
  padding: 30rpx;
  box-sizing: border-box;
}

.title {
  font-size: 40rpx;
  font-weight: bold;
  text-align: center;
  margin-bottom: 16rpx;
  color: #333;
}

.update-time {
  font-size: 24rpx;
  color: #999;
  text-align: center;
  margin-bottom: 40rpx;
}

.section {
  margin-bottom: 36rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #eee;
}

.paragraph {
  font-size: 28rpx;
  color: #555;
  line-height: 1.8;
  margin-bottom: 16rpx;
  text-align: justify;
}

.bottom-spacer {
  height: 40rpx;
}

.bottom-bar {
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background-color: #fff;
  border-top: 1rpx solid #eee;
}

.agree-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 32rpx;
  font-weight: 500;
  color: #fff;
  background-color: #07c160;
  border-radius: 12rpx;
  border: none;
}

.agree-btn::after {
  border: none;
}
</style>
