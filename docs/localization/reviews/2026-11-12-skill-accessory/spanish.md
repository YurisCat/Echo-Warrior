# 西班牙西语／中性拉美西语独立审校（2026-11-12）

审校范围：`source-packet.json` 的 58 个键 × `es_es`、`es_mx` 两种语言，共 116 条。两份候选逐键值相同，以下共用逐条中文回译表；这不代表两个完整语言文件应完全相同。本批没有出现需作地区区分的词语。

审校者未参与本批翻译，未读取 `translator-*.md` 或译者解释。依据为双源文本、`docs/localization/REVIEW_WORKFLOW.md`、`TERMINOLOGY.md`、`TARGET_TERMS_CORE.md`，并对照正式旧 locale 的技能名及状态术语。没有读取实现代码或运行客户端，因此下列玩法判断是译文与给定双源及审校任务明确语义的比对，不是实现审计。

## 逐语言结论

| 语言 | 语义、语法与删减审校 | 必须修改 | 建议修改 | 作者裁决 | 视觉验收 |
|---|---|---|---|---|---|
| `es_es` | 本批 58 条通过 | 无 | 无 | 无需作者决定 | 未执行；长提示与按钮实际换行待验收 |
| `es_mx` | 本批 58 条通过；适合作为中性拉美基准 | 无 | 无 | 无需作者决定 | 未执行；长提示与按钮实际换行待验收 |

未发现新增宣传性语句、无效总结、机械对称句或需要删除的赘述。技能说明以条件、对象、结果和数值为主；同一命题可自然共用两地区译文，不为制造地区差异强行改写。`review-fixes-es_es.json` 与 `review-fixes-es_mx.json` 均为 `{}`。

## 重要术语与高风险语义

| 中文概念 | 本批西语 | 判断 |
|---|---|---|
| 英灵 | Eco / Ecos | 与核心回声概念一致，没有翻成灵魂、鬼魂或亡灵 |
| 主人 | dueño | 指所属玩家，受益者不扩成任意持盾玩家 |
| 攻击力／攻击伤害 | daño de ataque | 属性与公式用词一致 |
| 最大生命值 | salud máxima | 百分比基数一致 |
| 护甲 | armadura | 护甲获得／穿甲语境区分明确 |
| 流血 | hemorragia | 与正式效果名 Hemorragia 一致 |
| 虚弱／缓慢 | Debilidad / Lentitud | 状态名与等级 I／II 保留 |
| 追猎 | Persecución | 与正式技能名一致 |
| 越战越勇 | Valor creciente | 与正式技能名一致 |
| 羽蛇神 | Quetzalcóatl | 与正式技能名 Maldición de Quetzalcóatl 一致；沿用已有西语重音拼写 |
| 增益／减益 | efecto beneficioso / efecto perjudicial | 两类效果计数独立，不译成叠层数或强度 |
| 协助战斗 | Asistencia | 召唤器战斗选项上下文中的简洁分区名 |
| 关闭／保护／全部 | Desactivada / Proteger / Todos | 关闭对应协助功能；Todos 指所有允许攻击的目标 |
| 冷却 | recarga | 沿用本语言现有技能术语 |

三处重点语义均通过：

- 军团阵列：`El dueño recibe además un 15 % menos de daño mientras sostiene un escudo` 明说“主人持盾时少受 15% 伤害”。不是全部友军获得该减伤，也不是举盾格挡才触发。30% 攻击力仍作用于附近友军。
- 关刀连段：`elevan ligeramente a los enemigos y reducen su alcance cuerpo a cuerpo un 80 % durante 2 s` 中“其近战距离”承接敌人，译文不削弱英灵或主人。前三次命中刷新减益、受击减少冷却均另有完整说明。
- 羽蛇神诅咒：`por cada efecto beneficioso de este Eco y por cada efecto perjudicial del objetivo` 两次明确“每种”，意思是英灵增益数与目标减益数之和，每种追加 `{curse}` 点固定魔法伤害。不是两类效果配对、相乘，也不是状态等级乘数。例如两种自身增益＋一种目标减益，句义为三份 `{curse}`，不会读成只算一对。

## 逐条中文回译

下表为保留句义的中文直译回译，并已调整为可直接审阅的自然中文；没有额外设计解释时两者相同。`*` 是资源中的动态结果标记，表内保留以便核对。所有条目均无需作者决定。

### 饰品效果

表中键省略共同前缀 `item.echo_warrior.accessory.`。

| 键后缀 | 中文直译回译／自然中文释义 | 结论 |
|---|---|---|
| `battle_worn_whetstone.effect.1` | 攻击伤害 +20%。 | 通过。 |
| `mountain_burden_blade.effect.1` | 攻击伤害 +40%。 | 通过。 |
| `fractured_crystal_blade.effect.1` | 攻击伤害 +40%。 | 通过。 |
| `twin_oath_badge.effect.1` | 攻击伤害 +10%。 | 通过。 |
| `battle_blindfold.effect.1` | 攻击伤害 +30%。 | 通过。 |
| `blood_pact_fang.effect.1` | 造成伤害的攻击有 30% 概率恢复 2 点生命值＋最大生命值的 5%。 | 通过。 |
| `memory_ritual_knife.effect.1` | 攻击伤害 +20%。 | 通过。 |
| `heart_sprout_amber.effect.1` | 最大生命值 +20%。 | 通过。 |
| `feast_ham.effect.1` | 最大生命值 +35%。 | 通过。 |
| `peacemaker.effect.1` | 最大生命值 +60%。 | 通过。 |
| `peacemaker.effect.2` | 攻击伤害 -40%。 | 通过。 |
| `sunwheel_garland.effect.1` | 白天每 2.5 秒恢复 1 点生命值＋最大生命值的 1%。 | 通过。 |
| `moondew_bottle.effect.1` | 夜间每 2.5 秒恢复 1 点生命值＋最大生命值的 1%。 | 通过。 |
| `hollow_bird_bone.effect.2` | 最大生命值 -25%。 | 通过。 |

### 技能说明

表中键省略共同前缀 `gui.echo_warrior.summoner.skill.`。

| 键后缀 | 中文直译回译／自然中文释义 | 结论 |
|---|---|---|
| `roman.formation.simple.1` | 附近友军获得额外 30% 攻击伤害。主人手持盾牌时，额外少受 15% 伤害。 | 通过；15% 减伤受益者明确为主人。 |
| `roman.formation.full.1` | 附近友军获得额外 30% 攻击伤害。主人手持盾牌时，额外少受 15% 伤害。 | 通过；同简版，受益者无歧义。 |
| `roman.formation.full.2` | 影响半径 8 格内的此英灵、其主人及该主人的其他英灵。 | 通过；8 格范围与同主人所属关系保留。 |
| `roman.formation.full.3` | 不能与相同光环叠加。 | 通过。 |
| `roman.endures.simple.1` | 举盾并嘲讽敌人 5 秒，获得 {armor}* 点护甲。结束时，向附近敌人造成累计伤害的 {release}%*。冷却 20 秒。 | 通过；长提示待视觉验收。 |
| `roman.endures.full.1` | 举盾并嘲讽敌人 5 秒，获得 {armor}* 点护甲（12＋等级×0.1）。结束时，向附近敌人造成累计伤害的 {release}%*（100%＋等级×1%）。冷却 20 秒。 | 通过；双公式与结果对应正确，长提示待视觉验收。 |
| `roman.endures.full.2` | 按减伤生效前受到的伤害累计。 | 通过。 |
| `aztec.quetzalcoatls_curse.simple.1` | 此英灵或其主人被击中时，攻击者可能陷入虚弱。 | 通过。 |
| `aztec.quetzalcoatls_curse.simple.2` | 直接命中时，此英灵的每种有益效果与目标的每种有害效果各追加 {curse}* 点魔法伤害。 | 通过；两次 por cada 明确分别计数，不要求成对，也不按持续时间或效果等级加倍。 |
| `aztec.quetzalcoatls_curse.full.1` | 此英灵或其主人被击中时，攻击者可能陷入虚弱。 | 通过。 |
| `aztec.quetzalcoatls_curse.full.2` | 直接命中时，此英灵的每种有益效果与目标的每种有害效果各追加 {curse}* 点魔法伤害（1＋等级÷100）。 | 通过；单种固定魔法伤害公式正确。 |
| `aztec.quetzalcoatls_curse.full.3` | 60% 概率造成虚弱：75% 为虚弱 I、持续 5 秒；25% 为虚弱 II、持续 3 秒。 | 通过；60% 是总触发率，75%／25% 是触发后的分支比例。 |
| `aztec.quetzalcoatls_curse.full.4` | 日照、有利群系与追猎各算一种有益效果。 | 通过；有利群系指群系强化条件，无新增地区限制。 |
| `aztec.obsidian_wound.simple.1` | 攻击可能造成流血，每秒造成 {bleed}* 点伤害，持续 6 秒。 | 通过。 |
| `aztec.obsidian_wound.full.1` | 攻击可能造成流血，每秒造成 {bleed}* 点伤害（1＋攻击伤害×10%），持续 6 秒。 | 通过。 |
| `aztec.obsidian_wound.full.2` | 概率：普通攻击主目标 30%；横扫次目标和追猎命中 15%。 | 通过；普攻主目标、横扫次目标和追猎对象区分正确。 |
| `aztec.macuahuitl.simple.1` | 普通攻击横扫一个区域，对次目标造成一半伤害。追猎后普通攻击强化 3 秒。 | 通过。 |
| `aztec.macuahuitl.full.1` | 普通攻击横扫一个区域，对次目标造成一半伤害。追猎后普通攻击强化 3 秒。 | 通过。 |
| `aztec.macuahuitl.full.2` | 横扫半径 4.5 格，强化时为 5 格。 | 通过。 |
| `aztec.macuahuitl.full.3` | 次目标伤害 {half}*（攻击伤害×50%）；强化时 {threequarters}*（攻击伤害×75%）。 | 通过；基础／强化次目标伤害均保留。 |
| `egyptian.cat_god.simple.1` | 附近苦力怕惊慌逃跑。埃及弓箭手对亡灵多造成 20% 伤害。 | 通过；没有恢复旧版“苦力怕不能爆炸”文案。 |
| `egyptian.cat_god.full.1` | 附近苦力怕惊慌逃跑。埃及弓箭手对亡灵多造成 20% 伤害。 | 通过；没有恢复旧版“苦力怕不能爆炸”文案。 |
| `egyptian.cat_god.full.2` | 影响半径 30 格内的苦力怕。 | 通过。 |
| `egyptian.leaf_arrow.simple.1` | 命中使目标流血 4 秒，每 2 秒受到 {bleed}* 点伤害，并使目标减速 2 秒。 | 通过。 |
| `egyptian.leaf_arrow.full.1` | 命中使目标流血 4 秒，每 2 秒受到 {bleed}* 点伤害（1＋发射时攻击伤害×10%），并使目标减速 2 秒。 | 通过；发射时攻击力快照没有误成命中时。 |
| `egyptian.leaf_arrow.full.2` | 施加缓慢 I。 | 通过。 |
| `egyptian.leaf_arrow.full.3` | 命中刷新其自身来源的流血。各来源分别到期；只有仍在生效的最强流血造成伤害。 | 通过；刷新来源、分别到期、最强生效三条规则齐全。 |
| `egyptian.cone_arrow.simple.1` | 无视 {penetration}%* 护甲，有 {pierce}%* 概率穿透目标。最多命中 2 个目标。 | 通过。 |
| `egyptian.cone_arrow.full.1` | 无视 {penetration}%* 护甲（35%＋等级×0.5%），有 {pierce}%* 概率穿透目标（25%＋等级×0.5%）。最多命中 2 个目标。 | 通过；两项概率／穿甲公式与结果未互换，长提示待视觉验收。 |
| `egyptian.cone_arrow.full.2` | 穿甲超过 100% 的部分转化为额外伤害。 | 通过。 |
| `guandao.combo.simple.1` | 施展四次打击。前三次稍微抬起敌人，并使敌人的近战攻击距离降低 80%、持续 2 秒。冷却 12 秒。 | 通过；su alcance 的主语关系指前面的敌人；不是英灵或主人。 |
| `guandao.combo.full.1` | 施展四次打击。前三次稍微抬起敌人，并使敌人的近战攻击距离降低 80%、持续 2 秒。冷却 12 秒。 | 通过；同简版，敌我和 2 秒时长正确。 |
| `guandao.combo.full.2` | 每段伤害为 {hit1}*／{hit2}*／{hit1}*／{hit4}*（攻击伤害×70%／100%／70%／180%，受越战越勇加成）。 | 通过；{hit1} 重复两次及四段顺序完整。 |
| `guandao.combo.full.3` | 前三次命中各自刷新减益。 | 通过。 |
| `guandao.combo.full.4` | 受到近战或投射物伤害使冷却缩短 0.5 秒。 | 通过。 |

### 协助战斗

表中键省略共同前缀 `gui.echo_warrior.summoner.`。

| 键后缀 | 中文直译回译／自然中文释义 | 结论 |
|---|---|---|
| `section.assist` | 协助。 | 通过；召唤器分区上下文可明确为战斗协助。 |
| `assist.off.name` | 已关闭。 | 通过；阴性 Desactivada 与 Asistencia 一致。 |
| `assist.off.description` | 不加入主人的攻击。 | 通过；只关闭跟随主人攻击，没有承诺禁用全部独立战斗。 |
| `assist.safe.name` | 保护。 | 通过。 |
| `assist.safe.description` | 加入主人的攻击，但不攻击村民、铁傀儡或已驯服生物。 | 通过；不限定主人的驯服物，也不扩成所有中立／被动生物。 |
| `assist.all.name` | 所有目标。 | 通过；Todos 省略的名词是 objetivos（目标），不是修饰 Asistencia。 |
| `assist.all.description` | 加入主人针对任何允许目标的攻击。 | 通过；保留允许目标限制，没有承诺能攻击一切实体。 |

### 死亡消息

表中键省略共同前缀 `death.attack.`。

| 键后缀 | 中文直译回译／自然中文释义 | 结论 |
|---|---|---|
| `quetzalcoatl_curse` | %1$s 死于羽蛇神（Quetzalcóatl）的诅咒。 | 通过；沿用技能专名 Quetzalcóatl，占位符归因正确。 |
| `quetzalcoatl_curse.player` | %1$s 在与 %2$s 战斗时死于羽蛇神（Quetzalcóatl）的诅咒。 | 通过；受害者为 %1$s、对战者为 %2$s，没有倒置。 |

## 结构、数值与样式检查

对两份候选分别执行了本地确定性检查，结果均通过：

- JSON 可解析，恰好覆盖源包 58 个键，无缺键或额外键；两地区候选逐键值相同。
- `{armor}`、`{release}`、`{curse}`、`{bleed}`、`{half}`、`{threequarters}`、`{penetration}`、`{pierce}`、`{hit1}`、`{hit2}`、`{hit4}` 与 `%1$s`、`%2$s` 的名称及出现次数相同；`{hit1}` 在四段伤害中重复两次得到保留。
- `<gray>`、`<attack>`、`<level>` 开闭标签数量与源文一致，嵌套闭合；攻击项和等级项仍各在对应标签内。无新增整段灰色正文，灰色公式和短补充保留。
- 资源星号数量逐键一致。简版资源保留源文标记；是否按运行时规则隐藏星号属于渲染验收，本报告不据资源文件声称已通过。
- 数字经西语小数逗号归一化后与英文源文逐键一致；已人工核对百分比基数、加减号、乘除、主次目标、时长及条件。百分号前留空格、小数写作 `0,1`／`0,5`／`2,5`／`4,5`，未改变数值。
- 无空文本、异常控制字符或遗留完整翻译键；文化专名、动态占位符和原有 creeper 称呼不属于遗漏翻译。

本批仅审两份候选；没有检查或宣称正式主线与兼容线已同步，也没有检查或宣称五个拉美别名文件已同步。这些属于合并后的发布门禁。

## UI 长度及剩余验收

以下长度是移除样式标签后的资源字符数，仍包括动态占位符；并非像素宽度、实际换行行数或动态值替换后的长度。长度仅用于指出复测重点，不据此认定文字已溢出。

| 文本 | 字符数 | 建议验收内容 |
|---|---:|---|
| `roman.endures.full.1` | 226 | 两组公式、白色正文及整段高度 |
| `roman.endures.simple.1` | 185 | 默认悬浮提示宽高、计算值实际显示 |
| `egyptian.cone_arrow.full.1` | 179 | 两处概率及公式，百分号与数值断行 |
| `aztec.quetzalcoatls_curse.full.2` | 164 | 公式与“每种增益／减益”的换行连续性 |
| `egyptian.leaf_arrow.full.1` | 152 | “发射时攻击力”红色项与 4 秒／2 秒时长 |
| `guandao.combo.simple.1`／`full.1` | 147 | 敌人近战距离和 2 秒减益在同段中的可读性 |
| `Asistencia` | 10 | 分区标题 |
| `Desactivada` | 11 | 模式按钮最窄宽度 |
| `Proteger` | 8 | 模式按钮 |
| `Todos` | 5 | 模式按钮，与相邻说明对应关系 |

尚未启动 1.21.1 或 26.1.2 客户端。本批独立语言审校通过不等于已通过视觉验收，不构成 `--mark-current` 授权，也不代表作者接受了未完成的视觉风险。需要按既有流程验收召唤器、Alt 展开提示、饰品悬浮框和死亡消息；仅翻译资源变动无需另建世界。

## 作者裁决项

无需作者决定。没有需要作者判断含义、语气或文化边界的争议项，因此不生成虚构的待裁决审阅卡。语法、自然度、术语及删减由本次独立审校负责。

## 最终合并稿复审（2026-11-12）

已在主任务合并后独立读取 `final-es_es.json`、`final-es_mx.json`，逐键比较最终稿与本报告审过的候选，并核对主线及 1.21.1 正式语言资源。两种语言最终稿均恰好覆盖源包的 58 个键，键和值与各自已审候选完全一致；两条正式语言线中的对应 58 个键均与最终稿一致。两种语言各自的主线及 1.21.1 完整资源文件 SHA256 也相同。

最终复审结论：`es_es` 和 `es_mx` 本批语言审校通过，无新增修改项、无作者裁决项；此前逐条中文回译与术语结论继续适用于最终稿。上文“未检查正式主线与兼容线同步”的限定描述的是首次候选审校，本节补充实际完成的最终资源复核。未检查五个拉美别名，未执行客户端视觉验收或发布门禁，不据此登记语言基线。

下列 SHA256 对文件原始字节计算，供锁定实际复审版本：

| 文件 | SHA256 |
|---|---|
| `build/localization-update-2026-11-12/final-es_es.json` | `bdbb8ac059c37f43d8fae7410f78e9252f0e5712e46521678ea734e9ad110838` |
| `build/localization-update-2026-11-12/final-es_mx.json` | `bdbb8ac059c37f43d8fae7410f78e9252f0e5712e46521678ea734e9ad110838` |
| `common/src/main/resources/assets/echo_warrior/lang/es_es.json` | `adce3d840d5c44f77f8e6fe9df425cea90cbec5a28150c173194aec637917601` |
| `versions/1.21.1/common/src/main/resources/assets/echo_warrior/lang/es_es.json` | `adce3d840d5c44f77f8e6fe9df425cea90cbec5a28150c173194aec637917601` |
| `common/src/main/resources/assets/echo_warrior/lang/es_mx.json` | `5f74e92bd0f335e3cb2e606555e32434493ddaec49630938bc7e67dbc5ff1357` |
| `versions/1.21.1/common/src/main/resources/assets/echo_warrior/lang/es_mx.json` | `5f74e92bd0f335e3cb2e606555e32434493ddaec49630938bc7e67dbc5ff1357` |
