/**
 * poemLoader.js - 诗词数据加载器
 *
 * 双层架构：
 *   1. 快速缓存（55 首经典诗词）- 模块加载即就绪，零延迟
 *   2. 全量数据集（~91,000 首）- 从本地 data/ 目录 JSON 文件读取
 *
 * 使用方式：
 *   loadAll() → 立即返回当前缓存（55 首或全量）
 *   loadFullDataset(callback) → 后台加载全量，完成后回调
 */
var dataLoader = require('../data/dataLoader');

var FAMOUS_POEMS = [
  { id:'tang:1', title:'静夜思', author:'李白', dynasty:'唐代', tag:'tang', emoji:'🌙', lines:['床前明月光','疑是地上霜','举头望明月','低头思故乡'], explanation:'秋夜望月思乡。月光洒在床前像白霜，诗人抬头望月，低头思念远方的家乡。' },
  { id:'tang:2', title:'春晓', author:'孟浩然', dynasty:'唐代', tag:'tang', emoji:'🌸', lines:['春眠不觉晓','处处闻啼鸟','夜来风雨声','花落知多少'], explanation:'春天睡得香，天亮了都不知道。到处是鸟鸣，想起昨夜风雨，不知道花儿落了多少。' },
  { id:'tang:3', title:'登鹳雀楼', author:'王之涣', dynasty:'唐代', tag:'tang', emoji:'🏯', lines:['白日依山尽','黄河入海流','欲穷千里目','更上一层楼'], explanation:'夕阳沿山落下，黄河向海奔流。想看更远风景，就要再登一层楼。' },
  { id:'tang:4', title:'咏鹅', author:'骆宾王', dynasty:'唐代', tag:'tang', emoji:'🦢', lines:['鹅鹅鹅','曲项向天歌','白毛浮绿水','红掌拨清波'], explanation:'大白鹅弯着脖子向天唱歌，白毛浮在绿水上，红掌拨动清清的水波。' },
  { id:'tang:5', title:'赋得古原草送别', author:'白居易', dynasty:'唐代', tag:'tang', emoji:'🌿', lines:['离离原上草','一岁一枯荣','野火烧不尽','春风吹又生','远芳侵古道','晴翠接荒城','又送王孙去','萋萋满别情'], explanation:'原野上小草每年枯荣一次，野火烧不完，春风一吹又生长。' },
  { id:'tang:6', title:'望庐山瀑布', author:'李白', dynasty:'唐代', tag:'tang', emoji:'🌊', lines:['日照香炉生紫烟','遥看瀑布挂前川','飞流直下三千尺','疑是银河落九天'], explanation:'水流直下三千尺，好像是银河从天上落下来。' },
  { id:'tang:7', title:'绝句', author:'杜甫', dynasty:'唐代', tag:'tang', emoji:'🐦', lines:['两个黄鹂鸣翠柳','一行白鹭上青天','窗含西岭千秋雪','门泊东吴万里船'], explanation:'黄鹂在柳树上歌唱，白鹭飞上蓝天。窗外的雪山千年不化。' },
  { id:'tang:8', title:'悯农', author:'李绅', dynasty:'唐代', tag:'tang', emoji:'🌾', lines:['锄禾日当午','汗滴禾下土','谁知盘中餐','粒粒皆辛苦'], explanation:'碗里的每一粒米饭，都是农民辛苦换来的。' },
  { id:'tang:9', title:'江雪', author:'柳宗元', dynasty:'唐代', tag:'tang', emoji:'❄️', lines:['千山鸟飞绝','万径人踪灭','孤舟蓑笠翁','独钓寒江雪'], explanation:'千山万径都没有人和鸟的踪迹，只有一个老渔翁在雪中独自钓鱼。' },
  { id:'tang:10', title:'游子吟', author:'孟郊', dynasty:'唐代', tag:'tang', emoji:'🧵', lines:['慈母手中线','游子身上衣','临行密密缝','意恐迟迟归','谁言寸草心','报得三春晖'], explanation:'母亲为远行的孩子缝衣服。小草般的孝心，怎能报答春天阳光般的母爱？' },
  { id:'tang:11', title:'枫桥夜泊', author:'张继', dynasty:'唐代', tag:'tang', emoji:'🌉', lines:['月落乌啼霜满天','江枫渔火对愁眠','姑苏城外寒山寺','夜半钟声到客船'], explanation:'月亮落下，乌鸦啼叫，秋霜满天。苏州城外寒山寺的钟声，半夜传到客船上。' },
  { id:'tang:12', title:'乌衣巷', author:'刘禹锡', dynasty:'唐代', tag:'tang', emoji:'🏘️', lines:['朱雀桥边野草花','乌衣巷口夕阳斜','旧时王谢堂前燕','飞入寻常百姓家'], explanation:'从前贵族住的乌衣巷，如今长满野草。燕子飞进了普通百姓家。' },
  { id:'tang:13', title:'出塞', author:'王昌龄', dynasty:'唐代', tag:'tang', emoji:'⚔️', lines:['秦时明月汉时关','万里长征人未还','但使龙城飞将在','不教胡马度阴山'], explanation:'只要有李广那样的将军在，就不会让敌人越过阴山。' },
  { id:'tang:14', title:'送杜少府之任蜀州', author:'王勃', dynasty:'唐代', tag:'tang', emoji:'🌄', lines:['城阙辅三秦','风烟望五津','与君离别意','同是宦游人','海内存知己','天涯若比邻','无为在歧路','儿女共沾巾'], explanation:'只要心里有彼此，即使在天边也像邻居一样近。' },
  { id:'tang:15', title:'回乡偶书', author:'贺知章', dynasty:'唐代', tag:'tang', emoji:'🏡', lines:['少小离家老大回','乡音无改鬓毛衰','儿童相见不相识','笑问客从何处来'], explanation:'小时候离开家乡老了才回来，小孩们不认识我，笑着问客人从哪里来。' },
  { id:'tang:16', title:'九月九日忆山东兄弟', author:'王维', dynasty:'唐代', tag:'tang', emoji:'🏮', lines:['独在异乡为异客','每逢佳节倍思亲','遥知兄弟登高处','遍插茱萸少一人'], explanation:'独自在外地，每到过节更加思念亲人。' },
  { id:'tang:17', title:'望天门山', author:'李白', dynasty:'唐代', tag:'tang', emoji:'⛰️', lines:['天门中断楚江开','碧水东流至此回','两岸青山相对出','孤帆一片日边来'], explanation:'天门山被长江从中间劈开，一只小船从太阳旁边驶来。' },
  { id:'tang:18', title:'赠汪伦', author:'李白', dynasty:'唐代', tag:'tang', emoji:'🛶', lines:['李白乘舟将欲行','忽闻岸上踏歌声','桃花潭水深千尺','不及汪伦送我情'], explanation:'桃花潭水有千尺深，也比不上汪伦送我的情意深。' },
  { id:'tang:19', title:'黄鹤楼送孟浩然之广陵', author:'李白', dynasty:'唐代', tag:'tang', emoji:'🎋', lines:['故人西辞黄鹤楼','烟花三月下扬州','孤帆远影碧空尽','唯见长江天际流'], explanation:'老朋友在黄鹤楼告别，在繁花似锦的三月去扬州。' },
  { id:'tang:20', title:'山行', author:'杜牧', dynasty:'唐代', tag:'tang', emoji:'🍁', lines:['远上寒山石径斜','白云深处有人家','停车坐爱枫林晚','霜叶红于二月花'], explanation:'经霜的枫叶比二月的花还要红。' },
  { id:'tang:21', title:'清明', author:'杜牧', dynasty:'唐代', tag:'tang', emoji:'🌧️', lines:['清明时节雨纷纷','路上行人欲断魂','借问酒家何处有','牧童遥指杏花村'], explanation:'清明时节细雨纷纷，牧童指着远处的杏花村。' },
  { id:'tang:22', title:'早发白帝城', author:'李白', dynasty:'唐代', tag:'tang', emoji:'🚢', lines:['朝辞白帝彩云间','千里江陵一日还','两岸猿声啼不住','轻舟已过万重山'], explanation:'猿声还在耳边回响，小船已经穿过万重高山。' },
  { id:'tang:23', title:'凉州词', author:'王之涣', dynasty:'唐代', tag:'tang', emoji:'🏜️', lines:['黄河远上白云间','一片孤城万仞山','羌笛何须怨杨柳','春风不度玉门关'], explanation:'春风是吹不到玉门关外的。' },
  { id:'tang:24', title:'相思', author:'王维', dynasty:'唐代', tag:'tang', emoji:'🫘', lines:['红豆生南国','春来发几枝','愿君多采撷','此物最相思'], explanation:'红豆最能寄托相思之情。' },
  { id:'tang:25', title:'登乐游原', author:'李商隐', dynasty:'唐代', tag:'tang', emoji:'🌇', lines:['向晚意不适','驱车登古原','夕阳无限好','只是近黄昏'], explanation:'夕阳的景色无限美好，可惜已接近黄昏。' },
  { id:'tang:26', title:'悯农·其二', author:'李绅', dynasty:'唐代', tag:'tang', emoji:'🌾', lines:['春种一粒粟','秋收万颗子','四海无闲田','农夫犹饿死'], explanation:'春天种一粒种子，秋天收万颗粮食，可农民还是会被饿死。' },
  { id:'tang:27', title:'古朗月行', author:'李白', dynasty:'唐代', tag:'tang', emoji:'🌙', lines:['小时不识月','呼作白玉盘','又疑瑶台镜','飞在青云端'], explanation:'小时候不认识月亮，叫它白玉盘。' },
  { id:'tang:28', title:'风', author:'李峤', dynasty:'唐代', tag:'tang', emoji:'🍃', lines:['解落三秋叶','能开二月花','过江千尺浪','入竹万竿斜'], explanation:'风能吹落秋天的树叶，能吹开春天的花朵。' },
  { id:'tang:29', title:'池上', author:'白居易', dynasty:'唐代', tag:'tang', emoji:'🪷', lines:['小娃撑小艇','偷采白莲回','不解藏踪迹','浮萍一道开'], explanation:'小孩撑着小船偷偷采了白莲回来，浮萍被划开一道痕迹。' },
  { id:'tang:30', title:'寻隐者不遇', author:'贾岛', dynasty:'唐代', tag:'tang', emoji:'🌲', lines:['松下问童子','言师采药去','只在此山中','云深不知处'], explanation:'松树下问小童子，他说师父采药去了，就在这座山里。' },
  { id:'song:1', title:'题西林壁', author:'苏轼', dynasty:'宋代', tag:'song', emoji:'🏔️', lines:['横看成岭侧成峰','远近高低各不同','不识庐山真面目','只缘身在此山中'], explanation:'看不清庐山的真面目，只因为自己就在山里面——旁观者清。' },
  { id:'song:2', title:'春日', author:'朱熹', dynasty:'宋代', tag:'song', emoji:'🌺', lines:['胜日寻芳泗水滨','无边光景一时新','等闲识得东风面','万紫千红总是春'], explanation:'万紫千红的花朵到处都是春天的气息。' },
  { id:'song:3', title:'元日', author:'王安石', dynasty:'宋代', tag:'song', emoji:'🧨', lines:['爆竹声中一岁除','春风送暖入屠苏','千门万户曈曈日','总把新桃换旧符'], explanation:'在爆竹声中旧的一年过去了，大家都把旧桃符换成新的。' },
  { id:'song:4', title:'饮湖上初晴后雨', author:'苏轼', dynasty:'宋代', tag:'song', emoji:'☀️', lines:['水光潋滟晴方好','山色空蒙雨亦奇','欲把西湖比西子','淡妆浓抹总相宜'], explanation:'把西湖比作美女西施，淡妆浓妆都很合适。' },
  { id:'song:5', title:'泊船瓜洲', author:'王安石', dynasty:'宋代', tag:'song', emoji:'🚢', lines:['京口瓜洲一水间','钟山只隔数重山','春风又绿江南岸','明月何时照我还'], explanation:'春风又吹绿了江南岸，明月什么时候才能照我回家。' },
  { id:'song:6', title:'示儿', author:'陆游', dynasty:'宋代', tag:'song', emoji:'📜', lines:['死去元知万事空','但悲不见九州同','王师北定中原日','家祭无忘告乃翁'], explanation:'只悲痛没有看到国家统一。等收复中原的那天，祭祀时别忘了告诉父亲。' },
  { id:'song:7', title:'小池', author:'杨万里', dynasty:'宋代', tag:'song', emoji:'🪷', lines:['泉眼无声惜细流','树阴照水爱晴柔','小荷才露尖尖角','早有蜻蜓立上头'], explanation:'小荷叶刚露出尖尖的角，早有蜻蜓停在了上面。' },
  { id:'song:8', title:'晓出净慈寺送林子方', author:'杨万里', dynasty:'宋代', tag:'song', emoji:'🌅', lines:['毕竟西湖六月中','风光不与四时同','接天莲叶无穷碧','映日荷花别样红'], explanation:'碧绿的荷叶一望无际，在阳光映照下荷花格外红艳。' },
  { id:'song:9', title:'梅花', author:'王安石', dynasty:'宋代', tag:'song', emoji:'🌺', lines:['墙角数枝梅','凌寒独自开','遥知不是雪','为有暗香来'], explanation:'墙角几枝梅花，在寒冷中独自开放。知道那不是雪，因为有淡淡香气。' },
  { id:'song:10', title:'惠崇春江晚景', author:'苏轼', dynasty:'宋代', tag:'song', emoji:'🦆', lines:['竹外桃花三两枝','春江水暖鸭先知','蒌蒿满地芦芽短','正是河豚欲上时'], explanation:'鸭子最先知道江水回暖。' },
  { id:'song:11', title:'三衢道中', author:'曾几', dynasty:'宋代', tag:'song', emoji:'🌿', lines:['梅子黄时日日晴','小溪泛尽却山行','绿阴不减来时路','添得黄鹂四五声'], explanation:'树荫和来时一样浓密，多了几声黄鹂的叫声。' },
  { id:'song:12', title:'六月二十七日望湖楼醉书', author:'苏轼', dynasty:'宋代', tag:'song', emoji:'⛈️', lines:['黑云翻墨未遮山','白雨跳珠乱入船','卷地风来忽吹散','望湖楼下水如天'], explanation:'黑云像打翻的墨汁，白雨像珠子跳进船里。一阵风忽然吹散乌云。' },
  { id:'song:13', title:'春日偶成', author:'程颢', dynasty:'宋代', tag:'song', emoji:'🌳', lines:['云淡风轻近午天','傍花随柳过前川','时人不识余心乐','将谓偷闲学少年'], explanation:'云淡风轻的午前，沿着花柳走到水边。' },
  { id:'qing:1', title:'村居', author:'高鼎', dynasty:'清代', tag:'qing', emoji:'🪁', lines:['草长莺飞二月天','拂堤杨柳醉春烟','儿童散学归来早','忙趁东风放纸鸢'], explanation:'孩子们放学早回家，忙着趁东风放风筝。' },
  { id:'qing:2', title:'所见', author:'袁枚', dynasty:'清代', tag:'qing', emoji:'🌳', lines:['牧童骑黄牛','歌声振林樾','意欲捕鸣蝉','忽然闭口立'], explanation:'牧童骑在黄牛背上，想去捉知了，忽然闭上嘴站住了。' },
  { id:'qing:3', title:'竹石', author:'郑燮', dynasty:'清代', tag:'qing', emoji:'🎋', lines:['咬定青山不放松','立根原在破岩中','千磨万击还坚劲','任尔东西南北风'], explanation:'竹子紧紧咬住青山不放松，经历千万磨炼依然坚韧。' },
  { id:'yuan:1', title:'墨梅', author:'王冕', dynasty:'元代', tag:'yuan', emoji:'🌺', lines:['吾家洗砚池头树','个个花开淡墨痕','不要人夸好颜色','只留清气满乾坤'], explanation:'不要别人夸颜色好看，只愿留下清香充满天地间。' },
  { id:'pre:1', title:'江南', author:'汉乐府', dynasty:'两汉', tag:'qin', emoji:'🪷', lines:['江南可采莲','莲叶何田田','鱼戏莲叶间','鱼戏莲叶东','鱼戏莲叶西','鱼戏莲叶南','鱼戏莲叶北'], explanation:'江南可以采莲了，鱼儿在莲叶间玩耍。' },
  { id:'pre:2', title:'长歌行', author:'汉乐府', dynasty:'两汉', tag:'qin', emoji:'🌅', lines:['青青园中葵','朝露待日晞','阳春布德泽','万物生光辉','常恐秋节至','焜黄华叶衰','百川东到海','何时复西归','少壮不努力','老大徒伤悲'], explanation:'百川向东奔流入海不会西归。年轻时不努力，老了只能白白伤悲。' },
  { id:'pre:3', title:'敕勒歌', author:'北朝民歌', dynasty:'南北朝', tag:'qin', emoji:'🌾', lines:['敕勒川','阴山下','天似穹庐','笼盖四野','天苍苍','野茫茫','风吹草低见牛羊'], explanation:'天空苍茫辽阔，草原一望无际，风吹过草低下头露出成群的牛羊。' },
  { id:'song:14', title:'望月怀远', author:'张九龄', dynasty:'唐代', tag:'tang', emoji:'🌕', lines:['海上生明月','天涯共此时','情人怨遥夜','竟夕起相思'], explanation:'海上升起明月，远方的亲人也在看同一个月亮。' },
  { id:'tang:31', title:'逢雪宿芙蓉山主人', author:'刘长卿', dynasty:'唐代', tag:'tang', emoji:'🏠', lines:['日暮苍山远','天寒白屋贫','柴门闻犬吠','风雪夜归人'], explanation:'风雪夜中有人回来了。' },
];

// 当前缓存（初始为 55 首经典诗词，全量加载完成后替换）
var poemCache = FAMOUS_POEMS.slice();
var fullDatasetLoaded = false;

/**
 * loadAll - 同步返回当前缓存
 * 初始为 55 首经典诗词，全量加载完成后自动升级为全部诗词
 */
function loadAll() {
  return poemCache;
}

/**
 * loadFullDataset - 异步加载全部诗词（本地文件系统）
 * 完成后自动替换缓存，通过 callback 通知
 */
function loadFullDataset(callback) {
  if (fullDatasetLoaded) {
    if (callback) callback(null, poemCache);
    return;
  }

  dataLoader.loadAll().then(function(allPoems) {
    // 将 55 首经典诗词合并到全量数据中（标记为有释义）
    var famousMap = {};
    for (var i = 0; i < FAMOUS_POEMS.length; i++) {
      var key = FAMOUS_POEMS[i].title + '|' + FAMOUS_POEMS[i].author;
      famousMap[key] = FAMOUS_POEMS[i];
    }
    for (var j = 0; j < allPoems.length; j++) {
      var p = allPoems[j];
      var fk = p.title + '|' + p.author;
      if (famousMap[fk]) {
        p.explanation = famousMap[fk].explanation;
        p.emoji = famousMap[fk].emoji;
      }
    }
    poemCache = allPoems;
    fullDatasetLoaded = true;
    console.log('[PoemLoader] 全量数据加载完成: ' + allPoems.length + ' 首诗词');
    if (callback) callback(null, allPoems);
  }).catch(function(err) {
    console.log('[PoemLoader] 全量数据加载失败，使用缓存 55 首', err);
    if (callback) callback(err, poemCache);
  });
}

function getCache() {
  return poemCache;
}

function isFullLoaded() {
  return fullDatasetLoaded;
}

module.exports = {
  loadAll: loadAll,
  loadFullDataset: loadFullDataset,
  getCache: getCache,
  isFullLoaded: isFullLoaded,
  FAMOUS_POEMS: FAMOUS_POEMS,
};
