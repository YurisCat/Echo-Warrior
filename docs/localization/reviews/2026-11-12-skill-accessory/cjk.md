# 2026-11-12 繁体中文与日文独立审校

审校源提交：`7e50c583bb68bbe9491802bfcb80c216df85da21`。范围为本批 `source-packet.json` 的58键 × 2语言。独立审校者未参与初译，未读取 `translator-*.md` 或译者解释；只依据双源、两份候选、术语规范、审阅流程及正式旧locale的技能术语。未联网。

## 逐语言结论

| 语言 | 结论 | 必须修改 | 建议修改 | 作者裁决 |
|---|---|---:|---:|---|
| zh_tw | 58键语言审校通过；无需改文案 | 0 | 0 | 无需作者决定 |
| ja_jp | 58键语义通过；其中2键有同一项自然度建议，其余56键直接通过 | 0 | 2 | 无需作者决定 |

这里的“通过”仅指本批文本审校，不代表正式locale已合并、全量发布门禁通过、语言基线已接受或游戏内视觉验收通过。两份 `review-fixes-*.json` 只是建议补丁，未修改draft及正式资源，未登记基线。

## 核心机制复核

- 军团阵列：两种语言都将30%攻击力加成给予附近友军，将持盾时额外15%减伤明确限定给主人。繁中第二句主语是「主人」，日文为「所有者は」。补充行说明8格内自身、主人及同主人英灵；没有把持盾减伤扩成全队效果。
- 关刀连段：降低80%的对象是被命中敌人的近战攻击距离，持续2秒；不是英灵自身减距。前三段每次命中刷新减益，四段伤害顺序与70%／100%／70%／180%均保留。
- 羽蛇神诅咒：按自身增益种类数与目标减益种类数之和，每种追加固定的 `{curse}` 魔法伤害；不是两边配对、相乘或按效果等级计算。繁中「每種……各追加」与日文「合計し、1種類につき」表达正确。日照、群系强化、追猎各计一种。60%是虚弱总触发率，75%／25%是触发后的两档分配。
- 叶形箭：4秒流血、每2秒结算、减速2秒；使用发射时攻击力。来源独立计时、命中只刷新对应来源、仅最强未过期流血造成伤害，三点均齐全。
- 协助战斗：关闭只关闭跟随主人攻击，未宣称完全停止战斗；保护项涵盖村民、铁傀儡及所有已经驯服的生物，没有缩成主人的宠物，也没有扩大为所有中立或被动生物；全部仍受“允许攻击的目标”限定。
- 删除旧效果：猫神提示没有遗留“无法爆炸”，马夸威特没有遗留攻速下降，关刀没有遗留旧短时挡投射物描述。

## 重要术语

| 概念 | zh_tw | ja_jp | 审校结论 |
|---|---|---|---|
| 英灵 | 英靈 | エコー | 沿用核心术语；日文未改成灵魂本人的「英霊」 |
| 主人 | 主人 | 所有者 | 与正式旧技能文本一致 |
| 攻击力／最大生命值 | 攻擊力／最大生命值 | 攻撃力／最大体力 | 属性含义一致；没有把百分比改为固定加值 |
| 护甲 | 盔甲 | 防御力 | 沿用本项目正式旧技能用语 |
| 生物群系 | 生態域 | バイオーム | 符合本批语境 |
| 虚弱／缓慢 | 虛弱／緩速 | 弱体化／移動速度低下 | 与状态效果称谓一致；等级Ⅰ、Ⅱ正确 |
| 追猎 | 追獵 | 追撃 | 与现有技能名称完全相符 |
| 越战越勇 | 越戰越勇 | 戦うほど勇猛に | 关刀公式补充正确引用现有技能名 |
| 羽蛇神的诅咒 | 羽蛇神的詛咒 | ケツァルコアトルの呪い | 死亡信息与正式技能名称一致；没有引入额外神话事实 |
| 协助战斗 | 協助戰鬥 | 戦闘支援 | 独立辅助战斗选项，未与警戒或行动模式混淆 |
| 关闭／保护／全部 | 關閉／保護／全部 | オフ／保護／すべて | 三档语义清楚 |
| 铁傀儡 | 鐵魔像 | アイアンゴーレム | 保留目标语言对应生物名 |

## 逐键中文回译

以下按源包顺序列出全部58键，每一行均独立核对两种语言；重复的简版与展开正文用“同本技能简版”避免冗余。表内中文回译用于确认玩法语义，不要求保留目标语言词序。`*`、公式样式标签和占位符的完整性另作自动检查，回译表省略灰色包裹标签。除第31、32行日文措辞建议外，逐项结论均为通过且无需作者决定。

### 饰品（14键）

| 序号／键 | 繁中回译 | 日文回译 | 结论 |
|---|---|---|---|
| 1. `item.echo_warrior.accessory.battle_worn_whetstone.effect.1` | 攻击力 +20%。 | 攻击力 +20%。 | 两者通过 |
| 2. `item.echo_warrior.accessory.mountain_burden_blade.effect.1` | 攻击力 +40%。 | 攻击力 +40%。 | 两者通过 |
| 3. `item.echo_warrior.accessory.fractured_crystal_blade.effect.1` | 攻击力 +40%。 | 攻击力 +40%。 | 两者通过 |
| 4. `item.echo_warrior.accessory.twin_oath_badge.effect.1` | 攻击力 +10%。 | 攻击力 +10%。 | 两者通过 |
| 5. `item.echo_warrior.accessory.battle_blindfold.effect.1` | 攻击力 +30%。 | 攻击力 +30%。 | 两者通过 |
| 6. `item.echo_warrior.accessory.blood_pact_fang.effect.1` | 攻击造成伤害时，30%概率恢复2点＋最大生命值的5%。 | 攻击造成伤害后，30%概率恢复2点＋最大体力的5%。 | 两者通过 |
| 7. `item.echo_warrior.accessory.memory_ritual_knife.effect.1` | 攻击力 +20%。 | 攻击力 +20%。 | 两者通过 |
| 8. `item.echo_warrior.accessory.heart_sprout_amber.effect.1` | 最大生命值 +20%。 | 最大体力 +20%。 | 两者通过 |
| 9. `item.echo_warrior.accessory.feast_ham.effect.1` | 最大生命值 +35%。 | 最大体力 +35%。 | 两者通过 |
| 10. `item.echo_warrior.accessory.peacemaker.effect.1` | 最大生命值 +60%。 | 最大体力 +60%。 | 两者通过 |
| 11. `item.echo_warrior.accessory.peacemaker.effect.2` | 攻击力 -40%。 | 攻击力 -40%。 | 两者通过 |
| 12. `item.echo_warrior.accessory.sunwheel_garland.effect.1` | 白天每2.5秒恢复1点＋最大生命值的1%。 | 白天每2.5秒恢复1点＋最大体力的1%。 | 两者通过 |
| 13. `item.echo_warrior.accessory.moondew_bottle.effect.1` | 夜晚每2.5秒恢复1点＋最大生命值的1%。 | 夜间每2.5秒恢复1点＋最大体力的1%。 | 两者通过 |
| 14. `item.echo_warrior.accessory.hollow_bird_bone.effect.2` | 最大生命值 -25%。 | 最大体力 -25%。 | 两者通过 |

### 罗马军团兵（7键）

| 序号／键 | 繁中回译 | 日文回译 | 结论 |
|---|---|---|---|
| 15. `gui.echo_warrior.summoner.skill.roman.formation.simple.1` | 附近友军攻击力提高30%；主人持盾时，其受到的伤害额外降低15%。 | 周围友方攻击力增加30%；所有者持盾期间，其受到的伤害额外减少15%。 | 两者通过 |
| 16. `gui.echo_warrior.summoner.skill.roman.formation.full.1` | 同本技能简版；持盾减伤的受益者明确是主人。 | 同本技能简版；主语「所有者は」明确限定持盾减伤受益者。 | 两者通过 |
| 17. `gui.echo_warrior.summoner.skill.roman.formation.full.2` | 影响8格内的自身、主人及同一主人所属英灵。 | 对8方块以内的自身、所有者、同一所有者的回声生效。 | 两者通过 |
| 18. `gui.echo_warrior.summoner.skill.roman.formation.full.3` | 同名光环不叠加。 | 同一光环的效果不重复叠加。 | 两者通过 |
| 19. `gui.echo_warrior.summoner.skill.roman.endures.simple.1` | 举盾嘲讽5秒，盔甲增加{armor}*点；结束时向周围敌人造成累计伤害的{release}%*；冷却20秒。 | 持盾挑衅5秒，防御力增加{armor}*；结束时对周围敌人造成累计伤害的{release}%*；冷却20秒。 | 两者通过 |
| 20. `gui.echo_warrior.summoner.skill.roman.endures.full.1` | 同简版；盔甲公式12＋等级×0.1，返还比例公式100%＋等级×1%。 | 同简版；防御力公式12＋等级×0.1，返还比例公式100%＋等级×1%。 | 两者通过 |
| 21. `gui.echo_warrior.summoner.skill.roman.endures.full.2` | 累计减伤前受到的伤害。 | 累计减免前的受到伤害。 | 两者通过 |

### 阿兹特克勇士（13键）

| 序号／键 | 繁中回译 | 日文回译 | 结论 |
|---|---|---|---|
| 22. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.simple.1` | 自身或主人受击时，有概率使攻击者虚弱。 | 自身或所有者受到攻击时，可能使攻击者虚弱。 | 两者通过 |
| 23. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.simple.2` | 直接命中时，自身每种增益和目标每种减益各追加{curse}*点魔法伤害。 | 直击时将自身的强化效果和目标的弱化效果合计，每一种造成{curse}*额外魔法伤害。 | 两者通过 |
| 24. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.1` | 同本技能简版第1行：受击者为自身或主人，虚弱施加给攻击者。 | 同本技能简版第1行：自身或所有者受击时，可能弱化攻击者。 | 两者通过 |
| 25. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.2` | 同简版第2行；每种追加伤害公式为1＋等级÷100。 | 同简版第2行；每种追加魔法伤害公式为1＋等级÷100。 | 两者通过 |
| 26. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.3` | 60%概率触发虚弱；触发后75%为虚弱Ⅰ、5秒，25%为虚弱Ⅱ、3秒。 | 60%概率施加虚弱；其中75%为虚弱Ⅰ持续5秒，25%为虚弱Ⅱ持续3秒。 | 两者通过 |
| 27. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.4` | 日照、生物群系强化、追猎各计1种增益。 | 日光、擅长的生物群系、追击带来的强化，各按1种强化效果计数。 | 两者通过 |
| 28. `gui.echo_warrior.summoner.skill.aztec.obsidian_wound.simple.1` | 攻击有概率造成6秒流血，每秒{bleed}*点伤害。 | 攻击时可能施加6秒出血，每秒{bleed}*伤害。 | 两者通过 |
| 29. `gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.1` | 同简版；每秒伤害公式1＋攻击力×10%。 | 同简版；每秒伤害公式1＋攻击力×10%。 | 两者通过 |
| 30. `gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.2` | 触发概率：普攻主目标30%；横扫次要目标及追猎命中15%。 | 发生概率：普通攻击的主目标30%；横扫的非主目标及追击命中目标15%。 | 两者通过 |
| 31. `gui.echo_warrior.summoner.skill.aztec.macuahuitl.simple.1` | 普攻变为范围横扫；次要目标伤害减半；追猎后强化3秒。 | 普通攻击变成范围横扫；非主目标伤害减半；追击后强化3秒。 | 繁中通过；日文建议修改S1 |
| 32. `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.1` | 同本技能简版。 | 同本技能简版；同一处措辞建议同步修改。 | 繁中通过；日文建议修改S1 |
| 33. `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.2` | 横扫半径4.5格，强化后5格。 | 横扫半径4.5方块，强化期间5方块。 | 两者通过 |
| 34. `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.3` | 次要目标伤害{half}*＝攻击力×50%；强化后{threequarters}*＝攻击力×75%。 | 非主目标伤害{half}*＝攻击力×50%；强化期间{threequarters}*＝攻击力×75%。 | 两者通过 |

### 埃及弓箭手（10键）

| 序号／键 | 繁中回译 | 日文回译 | 结论 |
|---|---|---|---|
| 35. `gui.echo_warrior.summoner.skill.egyptian.cat_god.simple.1` | 附近苦力怕惊慌逃跑；埃及弓箭手对亡灵伤害提高20%。 | 附近苦力怕因害怕而逃离；埃及弓兵对不死生物的伤害增加20%。 | 两者通过 |
| 36. `gui.echo_warrior.summoner.skill.egyptian.cat_god.full.1` | 同本技能简版；没有保留旧文案的“无法爆炸”。 | 同本技能简版；没有保留旧文案的“无法爆炸”。 | 两者通过 |
| 37. `gui.echo_warrior.summoner.skill.egyptian.cat_god.full.2` | 影响30格内的苦力怕。 | 对30方块以内的苦力怕生效。 | 两者通过 |
| 38. `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.simple.1` | 命中后流血4秒，每2秒{bleed}*点伤害；同时减速2秒。 | 命中时造成4秒出血，每2秒{bleed}*伤害；另使移动速度下降2秒。 | 两者通过 |
| 39. `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.1` | 同简版；每次流血伤害公式1＋发射时攻击力×10%。 | 同简版；每次出血伤害公式1＋发射时的攻击力×10%。 | 两者通过 |
| 40. `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.2` | 减速施加缓速Ⅰ状态。 | 施加移动速度降低Ⅰ状态。 | 两者通过 |
| 41. `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.3` | 命中刷新对应来源；每个来源分别计时，只有未到期的最强流血造成伤害。 | 命中更新同一来源的出血；持续时间按来源分别管理，只有仍有效的最强出血造成伤害。 | 两者通过 |
| 42. `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.simple.1` | 忽略{penetration}%*盔甲，以{pierce}%*概率贯穿，最多命中2个目标。 | 无视{penetration}%*防御力，以{pierce}%*概率贯通，最多命中2个实体。 | 两者通过 |
| 43. `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1` | 同简版；忽略盔甲比例35%＋等级×0.5%，贯穿概率25%＋等级×0.5%。 | 同简版；防御力无视率35%＋等级×0.5%，贯通概率25%＋等级×0.5%。 | 两者通过 |
| 44. `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.2` | 穿甲超过100%的部分转为额外伤害。 | 防御力无视率超过100%的部分变成追加伤害。 | 两者通过 |

### 关刀战士（5键）

| 序号／键 | 繁中回译 | 日文回译 | 结论 |
|---|---|---|---|
| 45. `gui.echo_warrior.summoner.skill.guandao.combo.simple.1` | 四段连击；前三段略微挑起敌人，并使该敌人的近战攻击距离降低80%、持续2秒；冷却12秒。 | 打出4连击；前3击稍稍击飞敌人，将敌人的近战攻击距离缩短80%、持续2秒；冷却12秒。 | 两者通过 |
| 46. `gui.echo_warrior.summoner.skill.guandao.combo.full.1` | 同本技能简版；「其」指前句被命中的敌人。 | 同本技能简版；明确写出「敵の近接攻撃のリーチ」。 | 两者通过 |
| 47. `gui.echo_warrior.summoner.skill.guandao.combo.full.2` | 四段伤害{hit1}*／{hit2}*／{hit1}*／{hit4}*；攻击力×70%／100%／70%／180%，受越战越勇加成。 | 各击伤害{hit1}*／{hit2}*／{hit1}*／{hit4}*；攻击力×70%／100%／70%／180%，应用“越战越勇”的强化。 | 两者通过 |
| 48. `gui.echo_warrior.summoner.skill.guandao.combo.full.3` | 前三段每次命中都刷新减益。 | 前3击每次命中都会更新弱化效果。 | 两者通过 |
| 49. `gui.echo_warrior.summoner.skill.guandao.combo.full.4` | 受到近战或投射物伤害时，冷却缩短0.5秒。 | 受到近战攻击或投射物伤害后，冷却缩短0.5秒。 | 两者通过 |

### 协助战斗（7键）

| 序号／键 | 繁中回译 | 日文回译 | 结论 |
|---|---|---|---|
| 50. `gui.echo_warrior.summoner.section.assist` | 协助战斗。 | 战斗支援。 | 两者通过 |
| 51. `gui.echo_warrior.summoner.assist.off.name` | 关闭。 | 关闭／Off。 | 两者通过 |
| 52. `gui.echo_warrior.summoner.assist.off.description` | 不协助攻击主人的目标。 | 不配合所有者的攻击发起攻击。 | 两者通过 |
| 53. `gui.echo_warrior.summoner.assist.safe.name` | 保护。 | 保护。 | 两者通过 |
| 54. `gui.echo_warrior.summoner.assist.safe.description` | 跟随主人攻击，但避开村民、铁傀儡及已经驯服的生物。 | 配合所有者攻击，但不攻击村民、铁傀儡及已经驯服的生物。 | 两者通过 |
| 55. `gui.echo_warrior.summoner.assist.all.name` | 全部。 | 全部。 | 两者通过 |
| 56. `gui.echo_warrior.summoner.assist.all.description` | 跟随主人攻击所有允许攻击的目标。 | 配合所有者，攻击所有可以攻击的目标。 | 两者通过 |

### 死亡信息（2键）

| 序号／键 | 繁中回译 | 日文回译 | 结论 |
|---|---|---|---|
| 57. `death.attack.quetzalcoatl_curse` | %1$s被羽蛇神的诅咒吞噬。 | %1$s因克察尔科亚特尔的诅咒而倒下／死亡。 | 两者通过 |
| 58. `death.attack.quetzalcoatl_curse.player` | %1$s在与%2$s战斗时被羽蛇神的诅咒吞噬。 | %1$s在与%2$s交战期间，因克察尔科亚特尔的诅咒而倒下／死亡。 | 两者通过 |

## 建议修改S1：日文马夸威特的名词串

键：`gui.echo_warrior.summoner.skill.aztec.macuahuitl.simple.1` 与 `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.1`\
使用位置：召唤器马夸威特技能的简版／展开主说明。

中文原文：普攻变为范围横扫，次目标伤害减半。追猎后强化3秒。\
英文原文：Basic attacks sweep an area, dealing half damage to secondary targets. Empowered for 3s after Pursuit.\
目标语言原文：通常攻撃が範囲薙ぎ払いになり、主対象以外へのダメージは半減。追撃後は3秒間強化される。

中文直译回译：普通攻击变为“范围横扫”，对主目标以外的伤害减半。追击后强化3秒。\
自然中文释义：普攻横扫周围，对次要目标只造成一半伤害；追猎后强化3秒。

术语对应：普通攻击 → 通常攻撃；横扫 → 薙ぎ払い；次目标 → 主対象以外；追猎 → 追撃。

审校结论：建议修改，非必须修改。原句语义正确，但「範囲薙ぎ払い」是省去连接成分的生硬名词串。改成动词句更自然，并缩短正文。建议文案：

> 通常攻撃で周囲を薙ぎ払い、主対象以外へのダメージは半減。追撃後は3秒間強化される。

建议文案中文回译：普通攻击横扫周围，非主目标伤害减半；追击后强化3秒。\
风险：仅文风／自然度；没有数值、占位符、术语或敌我变化。\
需要作者决定：无需作者决定。无需让作者判断日文语法；由集成者采用建议即可。

本建议的两键已写入 `review-fixes-ja_jp.json`。`review-fixes-zh_tw.json` 为 `{}`。

## 自动核对与长度预警

- 两份JSON均可解析，各58键，键集合与源包完全一致，没有缺项、多项、空文本或控制字符。
- 逐键核对命名占位符和printf占位符的多重集合，无差异；包括关刀重复出现两次的 `{hit1}`，以及死亡信息中的 `%1$s`／`%2$s`，数量与归属均正确。
- `<gray>`、`<attack>`、`<level>` 开闭标签数量、嵌套及语义位置正确，星号数量一致；所有公式系数、百分比、时长、间隔与冷却均经人工核对。没有把攻击力标为等级或把等级标为攻击力。
- 机械数字比较的提示均为书写方式差异：英文“one／per／four／three”在目标语言中写为1／4／3。已按双源人工消歧，不属于数值错误。负百分比、加号及Ⅰ／Ⅱ等级亦已核对。
- 以上星号检查只证明候选与源文本一致，不验证运行时简版的星号隐藏、Alt展开或颜色渲染行为。
- 短选项最长为4个字符（「協助戰鬥」「戦闘支援」），未见明显长度风险；该字符计数仍不是像素宽度测试。
- 长说明需实机核对：去样式标签、保留尚未展开的占位符时，繁中军团永存展开主说明76字符、锥锋箭70、关刀伤害行68；日文对应为96、78、81。日文保护项说明、叶形箭来源行、军团永存长公式也应覆盖。没有依据这些静态长度宣称必然溢出或视觉通过。
- 本子任务没有检查主线／兼容线同步、地区别名字节同步或全量发布门禁；这些必须在合并后的完整资源上由集成步骤完成，不能拿候选包的58键检查替代。

## 删减编辑与视觉边界

正文围绕对象、触发条件、效果、数值及公式展开，没有额外宣传句、气氛总结或重复解释需要删去。繁中沿用台湾用词「機率」「生態域」「鐵魔像」，本批未见简繁机械转换陷阱。日文总体符合游戏提示语体，S1改善一处名词堆叠，其他句子没有必要为求变化而改写。

没有发现需要作者重新确认的玩法含义、文化边界或语气选择；作者裁决项：无需。

实机视觉验收未执行。仍需分别在1.21.1与26.1.2检查CJK字形、日文标点禁则、最长数值代入、灰色公式及红绿字段、简版／展开切换、保护项换行与死亡消息实际拼接。是否将这些未验收项目作为本次发布接受的剩余风险，由主任务另行明确记录，不能据本报告自动登记语言基线。

## 最终文件复审（2026-11-12）

主任务合并后，独立审校者重新读取两份final、原draft、审校建议补丁及主线／1.21.1正式资源，未读取译者解释。原draft的SHA256与首次审校记录一致，保证本次差异核对仍以当时已审阅的候选为基准。

实际核对结果：

- `final-zh_tw.json` 恰好58键，逐键内容等于初稿；繁中建议补丁为空。
- `final-ja_jp.json` 恰好58键，逐键内容等于初稿加本报告S1的2键建议；两处文案均被逐字采纳，未引入额外变化。
- 每种语言在主线和1.21.1正式资源中的这58键均与对应final一致。两条正式语言线的同语言完整文件SHA256也相同。
- S1已落实。最终结论：繁中58键通过，日文58键通过；必须修改0项，未解决建议0项，作者裁决项为无需。

以下SHA256针对磁盘文件原始字节；final只包含本批58键，正式资源是完整语言文件，因此两者的文件哈希不应相同。

| 文件 | SHA256 |
|---|---|
| `build/localization-update-2026-11-12/draft-zh_tw.json` | `a7ac1b3f57412c5dbefdd79df8a4905cd0e06c7ad5eae4740f90fede48067d47` |
| `build/localization-update-2026-11-12/draft-ja_jp.json` | `e69e97c972e16c7dde3d8e1bd14b9d28c5d4a9aa3576f476e92df0f8fec4531a` |
| `build/localization-update-2026-11-12/final-zh_tw.json` | `f82e0b9fdff277bb343362522a40d09ef039fb0d76724bf0ca138abed515d4f9` |
| `build/localization-update-2026-11-12/final-ja_jp.json` | `8a023147969fd59a77c7e0231e9b27bfffd26e0eb790334fdedde53e0fcaddb9` |
| `common/src/main/resources/assets/echo_warrior/lang/zh_tw.json` | `2b6b6b055f8a1a8ba491a6a2e0b14f32b5ad28b83786a876bddc0d1c622d93d2` |
| `versions/1.21.1/common/src/main/resources/assets/echo_warrior/lang/zh_tw.json` | `2b6b6b055f8a1a8ba491a6a2e0b14f32b5ad28b83786a876bddc0d1c622d93d2` |
| `common/src/main/resources/assets/echo_warrior/lang/ja_jp.json` | `c0e7551dff5f47e5e005164f2873da9953eb29fc7b71ed813e988f83034dd279` |
| `versions/1.21.1/common/src/main/resources/assets/echo_warrior/lang/ja_jp.json` | `c0e7551dff5f47e5e005164f2873da9953eb29fc7b71ed813e988f83034dd279` |

本次复审仅确认最终文本与已审阅内容一致及上述正式资源对应关系，没有执行实机视觉验收或完整发布门禁，不据此接受语言基线。
