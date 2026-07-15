// app.js - 诗词乐园 微信小程序入口
const poemRepository = require('./services/poemRepository');
const storageService = require('./services/storage');

App({
  globalData: {
    poems: [],           // 当前诗词数据（初始55首，全量加载后更新）
    isDataLoaded: false,  // 全量数据是否加载完成
    isInitialLoad: true,  // 是否处于初始55首展示阶段
    userProfile: null,
    learningRecords: {},
    dailyStats: {},
  },

  onLaunch() {
    // 初始化存储服务
    storageService.init();

    // 加载用户档案
    this.loadUserProfile();

    // 加载诗词数据（双层策略）
    this.loadPoemData();
  },

  loadUserProfile() {
    const profile = storageService.getUserProfile();
    if (!profile) {
      const newProfile = {
        totalPoints: 0,
        level: 1,
        streak: 1,
        lastActiveDate: '',
        unlockedThemes: ['default'],
        currentTheme: 'default',
        achievements: [],
      };
      storageService.saveUserProfile(newProfile);
      this.globalData.userProfile = newProfile;
    } else {
      this.globalData.userProfile = profile;
    }
  },

  loadPoemData() {
    var that = this;

    // 第1层：55 首经典诗词（同步，即时显示）
    var cachedPoems = poemRepository.loadAllPoems();
    this.globalData.poems = cachedPoems;
    this.globalData.isInitialLoad = true;
    console.log('[诗词乐园] 缓存加载完成: ' + cachedPoems.length + ' 首经典诗词');

    // 第2层：全量 ~91,000 首诗词（异步，从本地文件系统读取）
    poemRepository.loadFullDataset(function(err, allPoems) {
      if (!err) {
        that.globalData.poems = allPoems;
        that.globalData.isDataLoaded = true;
        that.globalData.isInitialLoad = false;
        console.log('[诗词乐园] 全量加载完成: ' + allPoems.length + ' 首诗词');
      } else {
        console.log('[诗词乐园] 全量加载失败，保持 55 首缓存');
        that.globalData.isDataLoaded = true;
        that.globalData.isInitialLoad = false;
      }
    });
  },

  getPoems() {
    return this.globalData.poems;
  },

  getUserProfile() {
    return this.globalData.userProfile;
  },

  updateUserProfile(profile) {
    this.globalData.userProfile = profile;
    storageService.saveUserProfile(profile);
  },

  celebrate() {
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    if (currentPage && currentPage.triggerCelebration) {
      currentPage.triggerCelebration();
    }
  },
});
