Component({
  properties: {
    poem: { type: Object, value: {} },
    className: { type: String, value: '' },
  },
  methods: {
    onTap: function() {
      this.triggerEvent('tap', { poem: this.data.poem });
    }
  }
});
