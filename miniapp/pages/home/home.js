Page({
  data: {
    poems: [], displayedPoems: [], categories: [], selectedCategory: '全部(📜)',
    dailyPoem: null, searchQuery: '', searchMode: false,
    isLoading: true, page: 0, hasMore: true, pageSize: 30,
    dataLoadHint: '', // 全量加载提示
  },
  onLoad: function() {
    this.loadPoems();
  },
  onShow: function() {
    // 当全量数据后台加载完成后刷新显示
    var app = getApp();
    if (app.globalData.isDataLoaded && !app.globalData.isInitialLoad && this.data.poems.length < 100) {
      this.loadPoems();
    }
  },
  loadPoems: function() {
    var that = this;
    var app = getApp();
    var poemRepo = require('../../services/poemRepository');
    var poems = poemRepo.loadAllPoems();
    app.globalData.poems = poems;
    // 显示加载提示（仅初始 55 首时）
    var hint = '';
    if (poems.length < 500) {
      hint = '正在加载全部诗词数据…';
    }
    that.setData({ isLoading: false, dataLoadHint: hint });
    that.processPoems(poems);
  },
  processPoems: function(poems) {
    var poemRepo = require('../../services/poemRepository');
    var categories = poemRepo.getCategories();
    var dailyPoem = poemRepo.getDailyPoem(true);
    var sorted = poemRepo.sortPoems(poems);
    var displayed = poemRepo.paginate(sorted, 0, 30);
    this.setData({
      poems: sorted, displayedPoems: displayed, categories: categories,
      dailyPoem: dailyPoem, hasMore: displayed.length < sorted.length, page: 0,
    });
  },
  onFilterTap: function(e) {
    var cat = e.currentTarget.dataset.category;
    if (cat === this.data.selectedCategory) return;
    this.setData({ selectedCategory: cat, page: 0, searchMode: false });
    var poemRepo = require('../../services/poemRepository');
    var filtered = poemRepo.getPoemsByCategory(cat);
    var displayed = poemRepo.paginate(filtered, 0, 30);
    this.setData({ poems: filtered, displayedPoems: displayed, hasMore: displayed.length < filtered.length });
  },
  onSearchInput: function(e) {
    var query = e.detail.value;
    this.setData({ searchQuery: query, searchMode: !!query, page: 0 });
    if (!query) {
      this.loadPoems();
      return;
    }
    var poemRepo = require('../../services/poemRepository');
    var filtered = poemRepo.search(query);
    this.setData({ poems: filtered, displayedPoems: filtered, hasMore: false });
  },
  loadMore: function() {
    if (this.data.searchMode || !this.data.hasMore) return;
    var page = this.data.page + 1;
    var poemRepo = require('../../services/poemRepository');
    var displayed = poemRepo.paginate(this.data.poems, page, 30);
    this.setData({ displayedPoems: displayed, page: page, hasMore: displayed.length < this.data.poems.length });
  },
  goToDetail: function(e) {
    var poem = e.currentTarget.dataset.poem;
    wx.navigateTo({ url: '/pages/detail/detail?poem=' + encodeURIComponent(JSON.stringify(poem)) });
  },
});
