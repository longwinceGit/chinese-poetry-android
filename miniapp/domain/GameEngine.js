/**
 * GameEngine.js - 游戏引擎
 * Port from Android GameEngine.java (273 lines)
 * 消消乐配对 / 诗词接龙出题 / 积分
 */
function generateMatchGame(poems, pairs) {
  pairs = pairs || 6;
  var pool = [];
  for (var i = 0; i < poems.length; i++) {
    var p = poems[i];
    if (p.lines && p.lines.length >= 2 && p.lines[0] !== '...' && p.lines[1] !== '...') {
      pool.push(p);
    }
  }
  shuffleArray(pool);
  var selected = pool.slice(0, Math.min(pairs, pool.length));
  var cards = [];
  for (var j = 0; j < selected.length; j++) {
    cards.push({ id: selected[j].id + '_0', pairId: j, isFirstHalf: true, text: selected[j].lines[0], poemTitle: selected[j].title, poemAuthor: selected[j].author, matched: false });
    cards.push({ id: selected[j].id + '_1', pairId: j, isFirstHalf: false, text: selected[j].lines[1], poemTitle: selected[j].title, poemAuthor: selected[j].author, matched: false });
  }
  shuffleArray(cards);
  return cards;
}

function checkMatch(cardA, cardB) {
  if (!cardA || !cardB) return false;
  return (cardA.pairId === cardB.pairId && cardA.isFirstHalf !== cardB.isFirstHalf);
}

function isGameComplete(cards) {
  for (var i = 0; i < cards.length; i++) {
    if (!cards[i].matched) return false;
  }
  return true;
}

function calculateScore(attempts, totalPairs) {
  return Math.max(5, 50 - (attempts - totalPairs) * 3);
}

function generateCoupletGame(poems, rounds) {
  rounds = rounds || 5;
  var pool = [];
  for (var i = 0; i < poems.length; i++) {
    var p = poems[i];
    if (p.lines && p.lines.length >= 2) pool.push(p);
  }
  shuffleArray(pool);
  var selected = pool.slice(0, Math.min(rounds, pool.length));
  var questions = [];
  for (var j = 0; j < selected.length; j++) {
    var poem = selected[j];
    var lineIdx = Math.floor(Math.random() * (poem.lines.length - 1));
    var givenLine = poem.lines[lineIdx];
    var correctLine = poem.lines[lineIdx + 1];
    var distractors = [];
    for (var k = 0; k < pool.length && distractors.length < 3; k++) {
      if (pool[k].id === poem.id) continue;
      var lines = pool[k].lines;
      if (lines && lines.length > 0) {
        var dl = lines[Math.floor(Math.random() * lines.length)];
        if (dl !== correctLine && distractors.indexOf(dl) === -1) {
          distractors.push(dl);
        }
      }
    }
    while (distractors.length < 3) {
      distractors.push('⋯⋯');
    }
    var options = [correctLine].concat(distractors);
    shuffleArray(options);
    questions.push({ givenLine: givenLine, correctLine: correctLine, options: options, poemTitle: poem.title, poemAuthor: poem.author });
  }
  return questions;
}

function calculateCoupletScore(base, streak) {
  return base + streak * 2;
}

function shuffleArray(arr) {
  for (var i = arr.length - 1; i > 0; i--) {
    var j = Math.floor(Math.random() * (i + 1));
    var temp = arr[i];
    arr[i] = arr[j];
    arr[j] = temp;
  }
  return arr;
}

module.exports = {
  generateMatchGame: generateMatchGame,
  checkMatch: checkMatch,
  isGameComplete: isGameComplete,
  calculateScore: calculateScore,
  generateCoupletGame: generateCoupletGame,
  calculateCoupletScore: calculateCoupletScore,
};
