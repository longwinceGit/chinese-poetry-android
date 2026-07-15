/**
 * QuizGenerator.js - 填空出题引擎
 * Port from Android QuizGenerator.java
 * 智能挖空 + 干扰项生成
 */
function generateQuiz(poem) {
  if (!poem || !poem.lines || poem.lines.length === 0) return null;
  var validLines = [];
  for (var i = 0; i < poem.lines.length; i++) {
    var line = poem.lines[i];
    if (line && line.length >= 5 && line !== '...') validLines.push(line);
  }
  if (validLines.length === 0) return null;
  var line = validLines[Math.floor(Math.random() * validLines.length)];
  var chars = line.split('');
  var blankCandidates = [];
  for (var j = 0; j < chars.length; j++) {
    var c = chars[j];
    if (isChineseChar(c)) blankCandidates.push(j);
  }
  if (blankCandidates.length === 0) return null;
  var blankIdx = blankCandidates[Math.floor(Math.random() * blankCandidates.length)];
  var answer = chars[blankIdx];
  var distractors = generateDistractors(answer, poem);
  var options = [answer].concat(distractors);
  shuffleArray(options);
  var questionChars = chars.slice();
  questionChars[blankIdx] = '＿';
  var questionText = questionChars.join('');
  return { question: questionText, answer: answer, options: options, blankIndex: blankIdx, line: line };
}

function isChineseChar(c) {
  var code = c.charCodeAt(0);
  return code >= 0x4E00 && code <= 0x9FFF;
}

function generateDistractors(answer, poem) {
  var pool = '天地人日月星山水风云花鸟草木春华夏秋冬雪雨雾烟霞光色香声音影梦心情感思知明行路时年长短高深远近多少前后左右东西南北中内外上下新故旧老病愁苦悲欢离合爱恨情仇';
  var distractors = [];
  var used = {};
  used[answer] = true;
  for (var i = 0; i < 20 && distractors.length < 3; i++) {
    var idx = Math.floor(Math.random() * pool.length);
    var c = pool[idx];
    if (!used[c]) {
      used[c] = true;
      distractors.push(c);
    }
  }
  while (distractors.length < 3) {
    distractors.push('？');
  }
  return distractors;
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
  generateQuiz: generateQuiz,
};
