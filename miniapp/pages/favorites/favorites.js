var storage = require('../../services/storage');
Page({
  data: { favorites: [], isEmpty: true },
  onShow: function() { this.loadFavorites(); },
  loadFavorites: function() {
    var favs = storage.getFavorites();
    this.setData({ favorites: favs || [], isEmpty: !favs || favs.length === 0 });
  },
  goToDetail: function(e) {
    var item = e.currentTarget.dataset.item;
    var poem = { id: item.poemId, title: item.title, author: item.author, dynasty: item.dynasty, lines: [], emoji: '❤️' };
    wx.navigateTo({ url: '/pages/detail/detail?poem=' + encodeURIComponent(JSON.stringify(poem)) });
  },
  removeFavorite: function(e) {
    var item = e.currentTarget.dataset.item;
    storage.toggleFavorite(item.poemId, item);
    this.loadFavorites();
    wx.showToast({ title: '已取消收藏', icon: 'none' });
  },
});
