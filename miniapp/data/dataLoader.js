/**
 * dataLoader.js - 本地诗词数据加载器
 * 使用 wx.getFileSystemManager 读取代码包中的 JSON 文件
 * 完全离线，无需网络请求
 */

var EMOJIS = ['📜','🌸','🌙','🎋','🏮','🌊','🍂','❄️','🌺','🪷','🌿','🐎','🏯','🌻','🖋️','🌟'];
var DATA_DIR = '/data';

var DYNASTY_TAG_MAP = {
  '先秦': 'qin', '春秋': 'qin', '春秋战国': 'qin',
  '魏晋': 'qin', '五代': 'wudai',
  '唐代': 'tang', '宋代': 'song', '元代': 'yuan', '明代': 'ming', '清代': 'qing',
  '近现代': 'modern',
};

function getTag(dynasty) { return DYNASTY_TAG_MAP[dynasty] || 'default'; }
function getEmoji(idx) { return EMOJIS[idx % EMOJIS.length]; }

/**
 * 读取单个文件
 */
function readFile(path) {
  return new Promise(function(resolve, reject) {
    try {
      var fs = wx.getFileSystemManager();
      fs.readFile({
        filePath: path,
        encoding: 'utf-8',
        success: function(res) { resolve(res.data); },
        fail: function(err) { reject(err); },
      });
    } catch (e) {
      reject(e);
    }
  });
}

/**
 * 解析单个 JSON 文件中的诗词数组
 */
function parsePoemFile(content, dynastyName, tag) {
  var items = JSON.parse(content);
  var poems = [];
  for (var i = 0; i < items.length; i++) {
    var item = items[i];
    if (!item || !item.t) continue;
    poems.push({
      id: dynastyName + ':' + poems.length,
      title: item.t || '',
      author: item.a || '',
      dynasty: item.d || dynastyName,
      category: item.c || '',
      tag: tag,
      emoji: getEmoji(poems.length),
      lines: item.p || [],
      explanation: null,
    });
  }
  return poems;
}

/**
 * 加载全部诗词（按朝代顺序）
 */
function loadAll() {
  return new Promise(function(resolve, reject) {
    readFile(DATA_DIR + '/nav.json').then(function(navContent) {
      var nav = JSON.parse(navContent);
      var allPoems = [];
      var index = 0;

      function loadNextDynasty(idx) {
        if (idx >= nav.length) {
          // 全部加载完成
          console.log('[DataLoader] 全部加载完成: ' + allPoems.length + ' 首诗词');
          resolve(allPoems);
          return;
        }

        var entry = nav[idx];
        var dynastyName = entry.dynasty;
        var tag = getTag(dynastyName);
        var files = entry.files || [];
        var fileIdx = 0;

        function loadNextFile() {
          if (fileIdx >= files.length) {
            loadNextDynasty(idx + 1);
            return;
          }

          var filePath = DATA_DIR + '/' + files[fileIdx];
          readFile(filePath).then(function(content) {
            var poems = parsePoemFile(content, dynastyName, tag);
            for (var i = 0; i < poems.length; i++) {
              poems[i].id = dynastyName + ':' + (index++);
              allPoems.push(poems[i]);
            }
            fileIdx++;
            loadNextFile();
          }).catch(function(err) {
            console.log('[DataLoader] 文件读取失败: ' + filePath);
            fileIdx++;
            loadNextFile();
          });
        }

        loadNextFile();
      }

      loadNextDynasty(0);
    }).catch(function(err) {
      console.log('[DataLoader] nav.json 读取失败', err);
      reject(err);
    });
  });
}

module.exports = {
  loadAll: loadAll,
};
