/**
 * tts.js - 语音朗读工具
 * 
 * 使用百度AI语音合成 API（百度智能云）
 * 
 * ⚠️ 使用前需要：
 * 1. 注册百度智能云 https://console.bce.baidu.com/
 * 2. 创建语音合成应用，获取 API Key 和 Secret Key
 * 3. 填入下方 BAIDU_TTS_CONFIG
 * 
 * 免费额度：每月 100 万字符，个人项目完全够用
 * 
 * API 文档：https://ai.baidu.com/ai-doc/SPEECH/Vk38lxily
 */

// ===== 请替换为你的百度AI应用凭证 =====
var BAIDU_TTS_CONFIG = {
  apiKey: '9RSpMQzW0u29vuDk7Sav3xTW',       // 替换为你的 API Key
  secretKey: 'gogDQ4BAdJx60HiZepZQW67J5z1AZEGD', // 替换为你的 Secret Key
};

var innerAudioContext = null;
var isSpeaking = false;
var onStopCallback = null;
var accessToken = '';
var tokenExpireTime = 0;
var tempFilePath = '';

function init() {
  if (!innerAudioContext) {
    // 强制使用旧版实现（lib 3.16.2 的 WebAudio 实现有 decode bug）
    innerAudioContext = wx.createInnerAudioContext({
      useWebAudioImplement: false,
    });
    innerAudioContext.obeyMuteSwitch = false;
    innerAudioContext.onStop(function() {
      isSpeaking = false;
      if (onStopCallback) onStopCallback();
    });
    innerAudioContext.onEnded(function() {
      isSpeaking = false;
      cleanTempFile();
      if (onStopCallback) onStopCallback();
    });
    innerAudioContext.onError(function(res) {
      isSpeaking = false;
      console.error('[TTS] Play error:', res.errCode, res.errMsg);
      cleanTempFile();
      wx.showToast({ title: '语音播放失败', icon: 'none' });
      if (onStopCallback) onStopCallback();
    });
    // 等待音频上下文准备就绪
    innerAudioContext.onCanplay(function() {
      console.log('[TTS] Audio ready');
    });
    innerAudioContext.onWaiting(function() {
      console.log('[TTS] Audio waiting');
    });
  }
}

function cleanTempFile() {
  if (tempFilePath) {
    try {
      var fs = wx.getFileSystemManager();
      fs.accessSync(tempFilePath);
      fs.unlinkSync(tempFilePath);
    } catch(e) {}
    tempFilePath = '';
  }
}

/**
 * 检查 API Key 是否已配置
 */
function checkConfig() {
  if (BAIDU_TTS_CONFIG.apiKey === 'YOUR_API_KEY' || BAIDU_TTS_CONFIG.secretKey === 'YOUR_SECRET_KEY') {
    wx.showModal({
      title: '语音朗读未配置',
      content: '请在 utils/tts.js 中填入百度AI应用的 API Key 和 Secret Key，即可使用语音朗读功能。\n\n注册地址：https://console.bce.baidu.com/',
      showCancel: false,
    });
    return false;
  }
  return true;
}

/**
 * 获取百度AI access_token
 */
function getAccessToken() {
  return new Promise(function(resolve, reject) {
    wx.request({
      url: 'https://aip.baidubce.com/oauth/2.0/token',
      data: {
        grant_type: 'client_credentials',
        client_id: BAIDU_TTS_CONFIG.apiKey,
        client_secret: BAIDU_TTS_CONFIG.secretKey,
      },
      success: function(res) {
        if (res.data && res.data.access_token) {
          accessToken = res.data.access_token;
          // expires_in 单位秒，提前 5 分钟刷新
          tokenExpireTime = Date.now() + (res.data.expires_in - 300) * 1000;
          resolve(accessToken);
        } else {
          reject(new Error('获取 token 失败: ' + JSON.stringify(res.data)));
        }
      },
      fail: reject,
    });
  });
}

/**
 * 合成语音并播放（百度 AI TTS RPC API）
 * API: https://aip.baidubce.com/rpc/2.0/tts/v1/create
 * 文档: https://ai.baidu.com/ai-doc/SPEECH/Vk38lxily
 *
 * ⚠️ 需要在微信小程序后台「开发设置」→「request合法域名」中添加：
 *    - aip.baidubce.com
 *    （如果未添加，真机会被拦截。开发工具不校验是因为勾选了「不校验合法域名」）
 */
function synthesizeAndPlay(text) {
  // 截断过长文本（限制 1024 字节）
  var utterance = text;
  if (text.length > 200) {
    utterance = text.substring(0, 197) + '。。。';
  }

  var doRequest = function(token) {
    wx.request({
      method: 'POST',
      url: 'https://aip.baidubce.com/rpc/2.0/tts/v1/create?access_token=' + token,
      header: { 'Content-Type': 'application/json' },
      data: {
        text: utterance,
        spd: 5,
        pit: 5,
        vol: 5,
        per: 1,
        aue: 6,
      },
      responseType: 'arraybuffer',
      success: function(res) {
        var contentType = (res.header && res.header['Content-Type']) || '';
        var isAudio = contentType.indexOf('audio') >= 0;

        if (res.data && res.data.byteLength > 200 && isAudio) {
          // 音频 → 写入临时文件播放
          var fs = wx.getFileSystemManager();
          var path = wx.env.USER_DATA_PATH + '/tts_' + Date.now() + '.mp3';
          try {
            fs.writeFileSync(path, res.data);
            tempFilePath = path;
            console.log('[TTS] OK  ', path, 'size:', res.data.byteLength);
            innerAudioContext.src = path;
            innerAudioContext.play();
            isSpeaking = true;
          } catch(e) {
            console.error('[TTS] File write error:', e);
            wx.showToast({ title: '语音播放失败', icon: 'none' });
            if (onStopCallback) onStopCallback();
          }
        } else {
          // 非音频 → JSON 错误
          var errDetail = '';
          try {
            var bytes = new Uint8Array(res.data);
            var str = '';
            for (var i = 0; i < bytes.length; i++) str += String.fromCharCode(bytes[i]);
            var json = JSON.parse(str);
            errDetail = 'code=' + json.error_code + ' msg=' + json.error_msg;
            console.error('[TTS] API error:', json);
          } catch(e) {
            errDetail = 'content-type=' + contentType + ' size=' + (res.data ? res.data.byteLength : 0) + 'B';
            console.error('[TTS] Unknown response:', errDetail);
          }
          wx.showToast({ title: '语音合成失败', icon: 'none', duration: 3000 });
          if (onStopCallback) onStopCallback();
        }
      },
      fail: function(err) {
        console.error('[TTS] Request fail:', err);
        wx.showToast({ title: '网络请求失败', icon: 'none' });
        if (onStopCallback) onStopCallback();
      },
    });
  };

  // 检查 token 是否过期
  if (accessToken && Date.now() < tokenExpireTime) {
    doRequest(accessToken);
  } else {
    getAccessToken().then(function(token) {
      doRequest(token);
    }).catch(function(err) {
      console.error('[TTS] Token error:', err);
      wx.showToast({ title: '语音服务认证失败', icon: 'none' });
      if (onStopCallback) onStopCallback();
    });
  }
}

/**
 * 朗读文本
 * @param {string} text 要朗读的文本
 * @param {function} callback 朗读结束后的回调
 */
function speak(text, callback) {
  init();
  stop();
  if (!text || text.trim() === '') {
    if (callback) callback();
    return;
  }
  if (!checkConfig()) {
    if (callback) callback();
    return;
  }
  onStopCallback = callback || null;
  synthesizeAndPlay(text);
}

function stop() {
  if (innerAudioContext) {
    innerAudioContext.stop();
  }
  isSpeaking = false;
  cleanTempFile();
}

function isCurrentlySpeaking() {
  return isSpeaking;
}

module.exports = {
  speak: speak,
  stop: stop,
  isSpeaking: isCurrentlySpeaking,
};
