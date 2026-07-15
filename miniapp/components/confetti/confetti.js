Component({
  properties: {
    duration: { type: Number, value: 3000 },
    particleCount: { type: Number, value: 60 },
  },
  data: { show: false },
  methods: {
    fire: function() {
      var that = this;
      this.setData({ show: true });
      var ctx = wx.createCanvasContext('confettiCanvas', this);
      var colors = ['#FF6B6B','#FFD93D','#6BCB77','#4D96FF','#FF6B9D','#C084FC'];
      var particles = [];
      var info = wx.getSystemInfoSync();
      var screenWidth = info.windowWidth;
      var screenHeight = info.windowHeight;
      for (var i = 0; i < this.data.particleCount; i++) {
        particles.push({
          x: Math.random() * screenWidth, y: Math.random() * screenHeight * -1,
          vx: (Math.random() - 0.5) * 6, vy: Math.random() * 4 + 2,
          radius: Math.random() * 6 + 3, color: colors[Math.floor(Math.random() * colors.length)],
          alpha: 1,
        });
      }
      var startTime = Date.now();
      function animate() {
        var elapsed = Date.now() - startTime;
        if (elapsed > that.data.duration) {
          that.setData({ show: false });
          return;
        }
        var progress = elapsed / that.data.duration;
        ctx.clearRect(0, 0, screenWidth, screenHeight);
        for (var j = 0; j < particles.length; j++) {
          var p = particles[j];
          p.x += p.vx;
          p.y += p.vy;
          p.vy += 0.15;
          p.alpha = 1 - progress;
          ctx.globalAlpha = p.alpha;
          ctx.setFillStyle(p.color);
          ctx.beginPath();
          ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.draw();
        if (elapsed < that.data.duration) {
          setTimeout(animate, 33);
        }
      }
      animate();
    }
  }
});
