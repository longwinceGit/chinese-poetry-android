Component({
  properties: {
    text: { type: String, value: '' },
    pinyinList: { type: Array, value: [] },
  },
  data: { cells: [] },
  observers: {
    'text,pinyinList': function(text, list) {
      if (!text) return;
      var cells = [];
      for (var i = 0; i < text.length; i++) {
        cells.push({ char: text[i], pinyin: (list && list[i]) ? list[i] : '' });
      }
      this.setData({ cells: cells });
    }
  }
});
