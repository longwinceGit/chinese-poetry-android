Component({
  properties: {
    stats: { type: Array, value: [] },
    barColor: { type: String, value: '#8B4513' },
  },
  data: { chartHeight: 200, barMaxHeight: 160 },
  observers: {
    'stats': function(stats) {
      if (stats && stats.length > 0) this.drawChart();
    }
  },
  methods: {
    drawChart: function() {
      var stats = this.data.stats;
      if (!stats || stats.length === 0) return;
      var info = wx.getSystemInfoSync();
      var canvasWidth = info.windowWidth - 80;
      var canvasHeight = 260;
      var chartPadding = 20;
      var barArea = canvasWidth - chartPadding * 2;
      var barCount = stats.length;
      var barGap = 8;
      var barWidth = (barArea - barGap * (barCount - 1)) / barCount;
      if (barWidth > 40) barWidth = 40;
      var ctx = wx.createCanvasContext('chartCanvas', this);
      var maxVal = 1;
      for (var i = 0; i < stats.length; i++) {
        if (stats[i].poemsLearned > maxVal) maxVal = stats[i].poemsLearned;
      }
      ctx.clearRect(0, 0, canvasWidth, canvasHeight);
      for (var j = 0; j < stats.length; j++) {
        var barH = (stats[j].poemsLearned / maxVal) * 160;
        var x = chartPadding + j * (barWidth + barGap);
        var y = 220 - barH;
        ctx.setFillStyle(this.data.barColor);
        ctx.beginPath();
        ctx.rect(x, y, barWidth, barH);
        ctx.fill();
        ctx.setFontSize(11);
        ctx.setTextAlign('center');
        ctx.setFillStyle('#999');
        var label = stats[j].date ? stats[j].date.slice(-5) : '';
        ctx.fillText(label, x + barWidth / 2, 238);
        if (stats[j].poemsLearned > 0) {
          ctx.setFillStyle('#8B4513');
          ctx.fillText(stats[j].poemsLearned.toString(), x + barWidth / 2, y - 6);
        }
      }
      ctx.draw();
    }
  }
});
