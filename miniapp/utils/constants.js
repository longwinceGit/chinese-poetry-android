/**
 * constants.js - 共享常量与工具函数
 */
var POEM_PER_PAGE = 30;

var CATEGORY_ORDER = ['全部(📜)', '唐代(🎋)', '宋代(🏮)', '元代(🐎)', '清代(🏯)', '先秦(📖)', '魏晋(🌿)', '五代(🍂)', '其他(📚)'];

var DYNASTY_TAGS = {
  '唐代': { cls: 'tag-tang', color: '#C62828', short: '唐' },
  'tang': { cls: 'tag-tang', color: '#C62828', short: '唐' },
  '宋代': { cls: 'tag-song', color: '#2E7D32', short: '宋' },
  'song': { cls: 'tag-song', color: '#2E7D32', short: '宋' },
  '先秦': { cls: 'tag-qin', color: '#5D4037', short: '先秦' },
  'qin': { cls: 'tag-qin', color: '#5D4037', short: '先秦' },
};
var TAG_CLASS_DEFAULT = 'tag-default';

function formatDate(date) {
  var d = date || new Date();
  return d.getFullYear() + '-' + pad(d.getMonth()+1) + '-' + pad(d.getDate());
}
function getToday() { return formatDate(new Date()); }
function pad(n) { return n < 10 ? '0' + n : '' + n; }
function shuffleArray(arr) {
  for (var i = arr.length - 1; i > 0; i--) {
    var j = Math.floor(Math.random() * (i + 1));
    var temp = arr[i]; arr[i] = arr[j]; arr[j] = temp;
  }
  return arr;
}
function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}
function truncate(str, len) {
  if (!str) return '';
  if (str.length <= len) return str;
  return str.substring(0, len) + '...';
}
function debounce(fn, delay) {
  var timer = null;
  return function() {
    var args = arguments;
    var ctx = this;
    if (timer) clearTimeout(timer);
    timer = setTimeout(function() { fn.apply(ctx, args); }, delay);
  };
}
function getTagClass(tag) {
  var entry = DYNASTY_TAGS[tag];
  return entry ? entry.cls : TAG_CLASS_DEFAULT;
}
function getTagColor(tag) {
  var entry = DYNASTY_TAGS[tag];
  return entry ? entry.color : '#8D6E63';
}
function getCategoryDynasty(categoryDisplay) {
  var map = {
    '全部(📜)': null, '唐代(🎋)': '唐代', '宋代(🏮)': '宋代',
    '元代(🐎)': '元代', '清代(🏯)': '清代', '先秦(📖)': '先秦',
    '魏晋(🌿)': '魏晋', '五代(🍂)': '五代', '其他(📚)': '其他'
  };
  return map[categoryDisplay] || null;
}

module.exports = {
  POEM_PER_PAGE: POEM_PER_PAGE,
  CATEGORY_ORDER: CATEGORY_ORDER,
  DYNASTY_TAGS: DYNASTY_TAGS,
  TAG_CLASS_DEFAULT: TAG_CLASS_DEFAULT,
  formatDate: formatDate, getToday: getToday,
  shuffleArray: shuffleArray, randomInt: randomInt,
  truncate: truncate, debounce: debounce,
  getTagClass: getTagClass, getTagColor: getTagColor,
  getCategoryDynasty: getCategoryDynasty,
};
