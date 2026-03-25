---
title: Taro 介绍
categories:
  - Taro
tags:
---

# Taro 介绍

## 简介

[官方网址]: https://taro-docs.jd.com/docs/

**Taro** 是一个开放式跨端跨框架解决方案，支持使用 React/Vue/Nerv 等框架来开发 [微信](https://mp.weixin.qq.com/) / [京东](https://mp.jd.com/?entrance=taro) / [百度](https://smartprogram.baidu.com/) / [支付宝](https://mini.open.alipay.com/) / [字节跳动](https://developer.open-douyin.com/) / [QQ](https://q.qq.com/) / [飞书](https://open.feishu.cn/document/uYjL24iN/ucDOzYjL3gzM24yN4MjN) 小程序 / H5 / RN 等应用。

现如今市面上端的形态多种多样，Web、React Native、微信小程序等各种端大行其道。当业务要求同时在不同的端都要求有所表现的时候，针对不同的端去编写多套代码的成本显然非常高，这时候只编写一套代码就能够适配到多端的能力就显得极为需要。

## 特性

### 多端转换支持

Taro 3 可以支持转换到 H5、ReactNative 以及任意小程序平台。

目前官方支持转换的平台如下：

 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/h5-81f73c447874b6528e84ee395bece16e.png" alt="H5" style="zoom:2%;" /> H5](https://developer.mozilla.org/zh-CN/docs/Web?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/rn-ecec68ba194e4b5e9fc3e853cc00c569.png" alt="React Native" style="zoom:2%;" /> React Native](https://reactnative.dev/?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/weapp-0e8fbe2d5eb3676de4961b54ee7f5ba4.png" alt="微信小程序" style="zoom:2%;" /> 微信小程序](https://developers.weixin.qq.com/miniprogram/dev/framework/?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/jd-03cf3bd618bc6274dd94e14928e325c3.png" alt="京东小程序" style="zoom:2%;" /> 京东小程序](https://mp.jd.com/?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/swan-566f56d360909d0457073b67b8f48958.png" alt="百度小程序" style="zoom:2%;" /> 百度智能小程序](https://smartprogram.baidu.com/developer/index.html?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/alipay-ee5545de747ce1ad6e17faec10358975.png" alt="支付宝小程序" style="zoom:2%;" /> 支付宝小程序](https://opendocs.alipay.com/mini/developer/getting-started?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/tt-f4ec120e570f924e7ef763dcaf7fc69d.png" alt="抖音小程序" style="zoom:2%;" /> 抖音小程序](https://developer.open-douyin.com/docs/resource/zh-CN/mini-app/introduction/overview?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/qq-3f77e6fbb490848ab8aa8183e9399110.png" alt="QQ 小程序" style="zoom:2%;" /> QQ 小程序](https://q.qq.com/wiki/develop/miniprogram/frame/?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/ding-talk-b5a9f3f70aae5365787ac12a294e1535.png" alt="钉钉小程序" style="zoom:8%;" /> 钉钉小程序](https://open.dingtalk.com/document/org/develop-org-mini-programs?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/wework-d23d31eee89d30c4909b90d328ea57eb.png" alt="企业微信小程序" style="zoom:5%;" /> 企业微信小程序](https://developers.weixin.qq.com/miniprogram/dev/devtools/qywx-dev.html?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/alipay-ee5545de747ce1ad6e17faec10358975.png" alt="支付宝 IOT 小程序" style="zoom:2%;" /> 支付宝 IOT 小程序](https://opendocs.alipay.com/iot/multi-platform/vcs0fv?from=taro)
 - [<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/lark-b264e88fd335c5d932313f1f7e612b03.png" alt="飞书小程序" style="zoom:2%;" /> 飞书小程序](https://open.feishu.cn/document/uYjL24iN/uMjNzUjLzYzM14yM2MTN?from=taro)

### 框架支持

在 Taro 3 中可以使用完整的 [**<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/react-81ed438b18e24116794df3148c0e1eaa.png" alt="react" style="zoom:1%;" /> React**](https://react.dev/) / [**<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/vue-be5842d62a326b39e66e79386b9df33b.png" alt="vue" style="zoom:1%;" /> Vue**](https://vuejs.org/) / [**<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/preact-68c69a4cef45e1be5985460257983da3.png" alt="preact" style="zoom:1%;" /> Preact**](https://preactjs.com/) / [**<img src="https://storage.360buyimg.com/pubfree-bucket/taro-docs/f1fdc1a4a18/assets/images/svelte-a7bfb5d80483441bcd32443d1adb0ae6.png" alt="svelte" style="zoom:4%;" /> Svelte**](https://svelte.dev/) / [ Nerv](https://github.com/NervJS/nerv) 开发体验，具体请参考：

- [基础教程——React](https://taro-docs.jd.com/docs/react-overall)
- [基础教程——Vue](https://taro-docs.jd.com/docs/vue-overall)
- [基础教程——Vue3](https://taro-docs.jd.com/docs/vue3)
- [基础教程——Preact](https://taro-docs.jd.com/docs/preact)



- Vue

示例代码

<<<<<<< HEAD
```vue
<template>
  <view class="index">
    <text>msg</text>
=======
```jsx
<template>
  <view class="index">
    <text>{{msg}}</text>
>>>>>>> ad831483737f742e33d7753c30fc23d15f020a6f
  </view>
</template>

<script>
  export default {
    data() {
      return {
        msg: 'Hello World！',
      }
    },
    created() {},
    onShow() {},
    onHide() {},
  }
</script>
```



## Taro UI

https://taro-docs.jd.com/docs/#taro-ui

> Taro 3 只能配合使用 taro-ui@next 版本
>
> 安装命令： `npm i taro-ui@next`

一款基于 `Taro` 框架开发的多端 UI 组件库。

[Taro UI](https://taro-ui.jd.com/) 特性：

- 基于 `Taro` 开发 UI 组件
- 一套组件可以在多端适配运行（`ReactNative` 端暂不支持）
- 提供友好的 API，可灵活的使用组件

## 学习资源

https://taro-docs.jd.com/docs/#学习资源

[【资讯】Taro 团队博客](https://taro-docs.jd.com/blog)

[【教程】5 分钟上手 Taro 开发](https://taro-docs.jd.com/docs/guide)

[【视频】5 分钟快速上手 Taro 开发小程序](https://mp.weixin.qq.com/s?__biz=MzU3NDkzMTI3MA==&mid=2247484205&idx=1&sn=935bb7a35c11c33563eeb7c3aaca3321&chksm=fd2bab04ca5c2212b4cd8aeb5858bd08517aeb31e20727b22d1eee00b394184e7e61359e7dd9&token=1180618535&lang=zh_CN#rd)



