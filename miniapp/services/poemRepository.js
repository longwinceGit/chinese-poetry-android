/**
 * poemRepository.js - 诗词数据仓库
 * 基于 poemLoader 的双层缓存：55 首即时显示 → 后台加载全量
 */
var poemLoader = require('./poemLoader');

var CATEGORY_ORDER = ['全部(📜)', '唐代(🎋)', '宋代(🏮)', '元代(🐎)', '清代(🏯)', '明代(🏮)', '两汉(📖)', '魏晋(🌿)', '五代(🍂)', '先秦(📖)', '春秋(📖)', '近现代(📚)', '其他(📚)'];
var CATEGORY_ICONS = { '全部': '📜', '唐代': '🎋', '宋代': '🏮', '元代': '🐎', '清代': '🏯', '明代': '🏮', '两汉': '📖', '魏晋': '🌿', '五代': '🍂', '先秦': '📖', '春秋': '📖', '近现代': '📚', '其他': '📚' };

/**
 * 获取当前所有诗词（同步）
 * 初始返回 55 首经典，全量加载完成后自动升级
 */
function loadAllPoems() {
  return poemLoader.loadAll();
}

/**
 * 加载全量数据集（异步）
 * 完成后通过 callback 通知
 */
function loadFullDataset(callback) {
  poemLoader.loadFullDataset(callback);
}

/**
 * 搜索
 */
function search(query) {
  var poems = loadAllPoems();
  if (!query || !query.trim()) return poems;
  var q = query.trim().toLowerCase();
  var results = [];
  for (var i = 0; i < poems.length; i++) {
    if (results.length >= 500) break;
    var p = poems[i];
    if ((p.title && p.title.toLowerCase().indexOf(q) >= 0) ||
        (p.author && p.author.toLowerCase().indexOf(q) >= 0) ||
        (p.dynasty && p.dynasty.indexOf(q) >= 0) ||
        (p.lines && p.lines.join('').indexOf(q) >= 0)) {
      results.push(p);
    }
  }
  return results;
}

/**
 * 获取分类列表
 */
function getCategories() {
  var poems = loadAllPoems();
  var dynasties = [];
  var seen = {};
  for (var i = 0; i < poems.length; i++) {
    var d = poems[i].dynasty;
    if (d && !seen[d]) { seen[d] = true; dynasties.push(d); }
  }
  var result = ['全部(📜)'];
  for (var j = 0; j < CATEGORY_ORDER.length; j++) {
    var cat = CATEGORY_ORDER[j];
    if (cat === '全部(📜)') continue;
    var dyn = cat.replace(/\(.*\)/, '').replace(/[📚📜🎋🏮🐎🏯📖🌿🍂]/g, '');
    for (var k = 0; k < dynasties.length; k++) {
      if (dynasties[k].indexOf(dyn) >= 0 || dyn.indexOf(dynasties[k]) >= 0) {
        if (result.indexOf(cat) < 0) result.push(cat);
        break;
      }
    }
  }
  // Add remaining dynasties not in CATEGORY_ORDER
  for (var m = 0; m < dynasties.length; m++) {
    var found = false;
    for (var n = 1; n < result.length; n++) {
      if (result[n].indexOf(dynasties[m]) >= 0) { found = true; break; }
    }
    if (!found) result.push(dynasties[m] + '📚');
  }
  return result;
}

/**
 * 获取每日推荐
 */
function getDailyPoem(famousFirst) {
  var poems = loadAllPoems();
  if (poems.length === 0) return null;
  var pool = poems;
  if (famousFirst !== false) {
    var famous = [];
    for (var i = 0; i < poems.length; i++) {
      if (poems[i].explanation) famous.push(poems[i]);
    }
    if (famous.length > 0) pool = famous;
  }
  return pool[Math.floor(Math.random() * pool.length)];
}

/**
 * 按 ID 查找诗词
 */
function findPoemById(id) {
  var poems = loadAllPoems();
  for (var i = 0; i < poems.length; i++) {
    if (poems[i].id === id) return poems[i];
  }
  return null;
}

/**
 * 按分类获取诗词
 */
function getPoemsByCategory(categoryDisplay) {
  var poems = loadAllPoems();
  if (!categoryDisplay || categoryDisplay.indexOf('全部') >= 0) return poems;
  var dyn = categoryDisplay.replace(/\(.*\)/, '').replace(/[📚📜🎋🏮🐎🏯📖🌿🍂]/g, '');
  var result = [];
  for (var i = 0; i < poems.length; i++) {
    if (poems[i].dynasty && poems[i].dynasty.indexOf(dyn) >= 0) {
      result.push(poems[i]);
    }
  }
  return result;
}

function paginate(poems, page, pageSize) {
  pageSize = pageSize || 30;
  var start = page * pageSize;
  var end = start + pageSize;
  return poems.slice(0, end);
}

function sortPoems(poems) {
  var sorted = poems.slice();
  sorted.sort(function(a, b) {
    if (a.explanation && !b.explanation) return -1;
    if (!a.explanation && b.explanation) return 1;
    return 0;
  });
  return sorted;
}

function getPoemCount() { return loadAllPoems().length; }
function isDataLoaded() { return poemLoader.isFullLoaded(); }

module.exports = {
  loadAllPoems: loadAllPoems,
  loadFullDataset: loadFullDataset,
  search: search,
  getCategories: getCategories,
  getDailyPoem: getDailyPoem,
  findPoemById: findPoemById,
  getPoemsByCategory: getPoemsByCategory,
  paginate: paginate,
  sortPoems: sortPoems,
  getPoemCount: getPoemCount,
  isDataLoaded: isDataLoaded,
  CATEGORY_ORDER: CATEGORY_ORDER,
  CATEGORY_ICONS: CATEGORY_ICONS,
};
