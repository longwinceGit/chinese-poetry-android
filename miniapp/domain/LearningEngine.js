/**
 * LearningEngine.js - 学习引擎
 * Port from Android LearningEngine.java
 * 等级系统 / 积分规则 / 连续学习算法
 */
var LEVELS = [
  { level: 1, name: '诗词小学徒', pointsRequired: 0 },
  { level: 2, name: '小秀才', pointsRequired: 100 },
  { level: 3, name: '小举人', pointsRequired: 300 },
  { level: 4, name: '小进士', pointsRequired: 600 },
  { level: 5, name: '小翰林', pointsRequired: 1000 },
  { level: 6, name: '大诗仙', pointsRequired: 2000 },
  { level: 7, name: '诗词宗师', pointsRequired: 3500 },
  { level: 8, name: '一代文豪', pointsRequired: 5000 },
  { level: 9, name: '千古诗圣', pointsRequired: 8000 },
];

function getLevel(totalPoints) {
  var lvl = LEVELS[0];
  for (var i = LEVELS.length - 1; i >= 0; i--) {
    if (totalPoints >= LEVELS[i].pointsRequired) {
      lvl = LEVELS[i];
      break;
    }
  }
  return lvl;
}

function getNextLevel(totalPoints) {
  for (var i = 0; i < LEVELS.length - 1; i++) {
    if (totalPoints < LEVELS[i + 1].pointsRequired) {
      return LEVELS[i + 1];
    }
  }
  return null;
}

function getLevelProgress(totalPoints) {
  var current = getLevel(totalPoints);
  var next = getNextLevel(totalPoints);
  if (!next) return 1;
  var currentMin = current.pointsRequired;
  var nextMin = next.pointsRequired;
  return (totalPoints - currentMin) / (nextMin - currentMin);
}

function calcStreak(userProfile) {
  if (!userProfile || !userProfile.lastActiveDate) return 1;
  var today = getTodayStr();
  var yesterday = getYesterdayStr();
  var lastDate = userProfile.lastActiveDate;
  if (lastDate === today) return userProfile.streak || 1;
  if (lastDate === yesterday) return (userProfile.streak || 1) + 1;
  return 1;
}

function getTodayStr() {
  var d = new Date();
  return d.getFullYear() + '-' + pad(d.getMonth()+1) + '-' + pad(d.getDate());
}
function getYesterdayStr() {
  var d = new Date();
  d.setDate(d.getDate() - 1);
  return d.getFullYear() + '-' + pad(d.getMonth()+1) + '-' + pad(d.getDate());
}
function pad(n) { return n < 10 ? '0' + n : '' + n; }

var POINT_RULES = {
  LEARN_POEM: 10,
  QUIZ_ALL_CORRECT: 20,
  QUIZ_80: 15,
  QUIZ_50: 10,
  QUIZ_LOW: 5,
  STREAK_3: 10,
  STREAK_7: 30,
  STREAK_14: 50,
  STREAK_21: 80,
  STREAK_30: 100,
};

function calcQuizPoints(correctCount, totalCount) {
  if (totalCount === 0) return 0;
  var ratio = correctCount / totalCount;
  if (ratio >= 1) return POINT_RULES.QUIZ_ALL_CORRECT;
  if (ratio >= 0.8) return POINT_RULES.QUIZ_80;
  if (ratio >= 0.5) return POINT_RULES.QUIZ_50;
  return POINT_RULES.QUIZ_LOW;
}

function getStreakBonus(streak) {
  if (streak >= 30) return POINT_RULES.STREAK_30;
  if (streak >= 21) return POINT_RULES.STREAK_21;
  if (streak >= 14) return POINT_RULES.STREAK_14;
  if (streak >= 7) return POINT_RULES.STREAK_7;
  if (streak >= 3) return POINT_RULES.STREAK_3;
  return 0;
}

module.exports = {
  LEVELS: LEVELS,
  POINT_RULES: POINT_RULES,
  getLevel: getLevel,
  getNextLevel: getNextLevel,
  getLevelProgress: getLevelProgress,
  calcStreak: calcStreak,
  calcQuizPoints: calcQuizPoints,
  getStreakBonus: getStreakBonus,
};
