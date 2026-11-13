# 技能公式文案独立审校：繁中、日语与英文源文案（2026-11-13）

后续集成验收：本批已完成八加载器客户端及26.1.2／1.21.1代表性视觉复核，语言基线已更新，发布级门禁0错误、0待处理；见[实施与验收记录](../research/skill-formula-display-2026-11-13.md)。以下独立审校过程保留当时的职责边界。

审校范围：`candidate-cjk.json` 中 `zh_tw`、`ja_jp` 各13键，共26条；另独立核对本批13条 `en_us` 与 `zh_cn`。仅审阅提供的双源、既有译文上下文及术语文件，未读取译者解释或自评。

依据：`docs/localization/REVIEW_WORKFLOW.md`、`TERMINOLOGY.md`、`TARGET_TERMS_CORE.md`，以及 `PROJECT.md` 中现有技能机制边界。初始双源及上下文来自 `build/skill-formula-update-2026-11-13/translation-packet.json`；最终英文锥锋箭已另核对两条版本线的实际源文件。

## 独立结论

- 繁中：13/13通过；日语：13/13通过。无需修改CJK候选，未解决的“必须修改”为0。
- 英文源：发现1项必须澄清的问题，锥锋箭原句 `chance to pierce up to 2 targets` 未清楚区分“贯穿次数”和“总命中目标数”。已交由主任务修订为 `chance to pierce. Hits up to 2 targets.`，同时补足 `of armor`。已只读复核主线与1.21.1的英文源均落实；最终13/13通过。
- 两种CJK候选均沿用既有属性与状态术语；没有加入眩晕、定身、缴械、完全禁止行动或关刀越战越勇加成。
- 无需作者决定新的语义、语气或文化问题。本报告不作发布授权，也不标记语言基线为当前。

## 范围内自动检查

- 26条候选的JSON可解析，键集合分别与13条中文源完全一致。
- 每种语言12组公式结果，数字、百分号与加乘除运算符顺序均与中文源一致；公式结束的等号后立即出现对应结果，至多隔一个闭合灰色标记。
- 占位符出现顺序及 `<gray>`、`<attack>`、`<level>` 标记序列与中文源完全一致；全批无星号。
- 关刀第1/3、2、4段分别对应70%、100%、180%与 `{hit1}`、`{hit2}`、`{hit4}`；未残留越战越勇文案。
- 此次只运行候选范围的静态核对，没有执行游戏、构建或网络请求。全库发布门禁、版本线资源同步及别名字节同步由主任务合并后检验，不能由本候选检查替代。

候选文件SHA-256：`61533e298b294dafdd9c84b2d2849e5b2121a8fa5c8db8416ec1990d81aee14e`。

## 视觉验收边界

尚无本批在1.21.1与26.1.2客户端中的视觉证据。CJK字形、数字代入后的公式宽度、灰/红/绿标记渲染、关刀六条补充行及日语换行禁则均待实际检查。下方“通过”只表示语义、语言和候选结构通过，不表示UI已经验收；没有将此剩余风险视为作者已接受。

## 英文源逐键复核

| 键后缀（统一前缀见下） | 中文语义复核 | 最终结论 |
|---|---|---|
| `roman.endures.full.1` | 持续5秒的举盾嘲讽增加护甲；结束时按累计伤害及等级比例反击周围敌人，冷却20秒。 | 通过；无必须修改 |
| `aztec.quetzalcoatls_curse.full.2` | 只在直接命中时按自身增益种数与目标减益种数之和追加魔法伤害；每种效果的附伤受等级影响。 | 通过；无必须修改 |
| `aztec.obsidian_wound.full.1` | 攻击可能触发6秒流血，每秒伤害由固定值1加攻击力的10%计算。 | 通过；无必须修改 |
| `aztec.macuahuitl.full.3` | 横扫次要目标承受50%攻击力伤害，强化期间提高为75%；两档结果各紧跟自己的公式。 | 通过；无必须修改 |
| `egyptian.leaf_arrow.full.1` | 使用发射时的攻击力计算流血；流血4秒、每2秒伤害一次，另有2秒减速。 | 通过；无必须修改 |
| `egyptian.cone_arrow.full.1` | 穿甲比例与贯穿概率分别受等级影响；总命中数最多为两个目标。 | 通过；原总命中上限歧义已修正并复核两条源文件 |
| `guandao.combo.simple.1` | 简版保留四段连击、小幅浮空、泛化的短暂控制、12秒冷却以及受击减冷却0.5秒。 | 通过；无必须修改 |
| `guandao.combo.full.1` | 完整版明确控制为2秒内降低80%近战攻击距离，不涉及远程攻击距离。 | 通过；无必须修改 |
| `guandao.combo.full.2` | 第1与第3段分别为70%攻击力，单个结果{hit1}对应两段各自的伤害。 | 通过；无必须修改 |
| `guandao.combo.full.3` | 第2段为100%攻击力，公式后直接显示{hit2}结果。 | 通过；无必须修改 |
| `guandao.combo.full.4` | 第4段为180%攻击力，公式后直接显示{hit4}结果。 | 通过；无必须修改 |
| `guandao.combo.full.5` | 前三段成功命中时刷新已有减益；第四段未包含在刷新范围中。 | 通过；无必须修改 |
| `guandao.combo.full.6` | 完整说明限定减冷却触发来源为近战或投射物伤害，每次缩短0.5秒。 | 通过；无必须修改 |

统一键前缀：`gui.echo_warrior.summoner.skill.`。英文沿用 `attack damage`、`level`、`armor`、`buff`/`debuff`、`melee reach`、`cooldown`；没有以晕眩/定身/缴械替换简版 `briefly hinder`。

英文锥锋箭最终文本：

```text
Ignore <gray>(35% + <level>level</level> × 0.5%) = </gray>{penetration}% of armor, with a <gray>(25% + <level>level</level> × 0.5%) = </gray>{pierce}% chance to pierce. Hits up to 2 targets.
```

## 繁體中文（zh_tw）：13条审阅卡

共同使用位置：回声召唤器技能悬浮说明；`simple` 为默认简版，`full` 为Alt展开说明。下列目标语言保留候选原样，源文本采用本批双源（英文锥锋箭列最终已修订源）。

### `gui.echo_warrior.summoner.skill.roman.endures.full.1`

中文原文：举盾嘲讽5秒，护甲增加<gray>（12＋<level>等级</level>×0.1）＝</gray>{armor}点。结束时对周围敌人造成<gray>（100%＋<level>等级</level>×1%）＝</gray>{release}%的累计伤害。冷却20秒。

英文原文：Raise the shield and taunt for 5s, gaining <gray>(12 + <level>level</level> × 0.1) = </gray>{armor} armor. At the end, deal <gray>(100% + <level>level</level> × 1%) = </gray>{release}% of accumulated damage to nearby enemies. Cooldown: 20s.

目标语言：

```text
舉盾嘲諷5秒，盔甲增加<gray>（12＋<level>等級</level>×0.1）＝</gray>{armor}點。結束時對周圍敵人造成<gray>（100%＋<level>等級</level>×1%）＝</gray>{release}%的累計傷害。冷卻20秒。
```

中文直译回译：举盾嘲讽5秒，盔甲增加（12＋等级×0.1）＝{armor}点。结束时对周围敌人造成（100%＋等级×1%）＝{release}%的累计伤害。冷却20秒。

自然中文释义：持续5秒的举盾嘲讽增加护甲；结束时按累计伤害及等级比例反击周围敌人，冷却20秒。

术语对应：护甲→盔甲；等级→等級；嘲讽→嘲諷；累计伤害→累計傷害。

审校结论：通过；候选无需修改。

风险：无语义问题。包含两条公式，展开后的总宽度和换行尚未实机验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.2`

中文原文：直接命中时，每种自身增益与目标减益追加<gray>（1＋<level>等级</level>÷100）＝</gray>{curse}点魔法伤害。

英文原文：Direct hits deal <gray>(1 + <level>level</level> ÷ 100) = </gray>{curse} extra magic damage per buff on the Echo and debuff on the target.

目标语言：

```text
直接命中時，自身每種增益與目標每種減益各追加<gray>（1＋<level>等級</level>÷100）＝</gray>{curse}點魔法傷害。
```

中文直译回译：直接命中时，自身的每种增益及目标的每种减益分别追加（1＋等级÷100）＝{curse}点魔法伤害。

自然中文释义：只在直接命中时按自身增益种数与目标减益种数之和追加魔法伤害；每种效果的附伤受等级影响。

术语对应：直接命中→直接命中；增益→增益；减益→減益；魔法伤害→魔法傷害。

审校结论：通过；候选无需修改。

风险：无。日语「1種類につき」明确按种类计数；没有把附伤改成攻击力倍率。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.1`

中文原文：攻击有概率造成流血，每秒<gray>（1＋<attack>攻击力</attack>×10%）＝</gray>{bleed}点伤害，持续6秒。

英文原文：Attacks may cause bleeding for 6s, dealing <gray>(1 + <attack>attack damage</attack> × 10%) = </gray>{bleed} damage each second.

目标语言：

```text
攻擊有機率造成流血，每秒<gray>（1＋<attack>攻擊力</attack>×10%）＝</gray>{bleed}點傷害，持續6秒。
```

中文直译回译：攻击有概率造成流血，每秒（1＋攻击力×10%）＝{bleed}点伤害，持续6秒。

自然中文释义：攻击可能触发6秒流血，每秒伤害由固定值1加攻击力的10%计算。

术语对应：流血→流血；攻击力→攻擊力；有概率→有機率。

审校结论：通过；候选无需修改。

风险：无。日语先写持续时间，保留了概率与每秒伤害，未改为必定触发。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.3`

中文原文：<gray>-次目标伤害（<attack>攻击力</attack>×50%）＝{half}点，强化后（<attack>攻击力</attack>×75%）＝{threequarters}点。</gray>

英文原文：<gray>-Secondary targets take (<attack>attack damage</attack> × 50%) = {half} damage, or (<attack>attack damage</attack> × 75%) = {threequarters} while empowered.</gray>

目标语言：

```text
<gray>-次要目標傷害（<attack>攻擊力</attack>×50%）＝{half}點，強化後（<attack>攻擊力</attack>×75%）＝{threequarters}點。</gray>
```

中文直译回译：次要目标伤害为（攻击力×50%）＝{half}点，强化后为（攻击力×75%）＝{threequarters}点。

自然中文释义：横扫次要目标承受50%攻击力伤害，强化期间提高为75%；两档结果各紧跟自己的公式。

术语对应：次目标→次要目標；攻击力→攻擊力；强化后→強化後。

审校结论：通过；候选无需修改。

风险：无。日语用「強化中」沿用既有语境，明确强化有效期间，不暗示永久增伤。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.1`

中文原文：命中后流血4秒，每2秒造成<gray>（1＋<attack>发射时攻击力</attack>×10%）＝</gray>{bleed}点伤害，并减速2秒。

英文原文：Hits cause bleeding for 4s, dealing <gray>(1 + <attack>attack damage at firing</attack> × 10%) = </gray>{bleed} damage every 2s, and slow the target for 2s.

目标语言：

```text
命中後流血4秒，每2秒造成<gray>（1＋<attack>發射時攻擊力</attack>×10%）＝</gray>{bleed}點傷害，並減速2秒。
```

中文直译回译：命中后流血4秒，每2秒造成（1＋发射时攻击力×10%）＝{bleed}点伤害，并减速2秒。

自然中文释义：使用发射时的攻击力计算流血；流血4秒、每2秒伤害一次，另有2秒减速。

术语对应：发射时攻击力→發射時攻擊力；流血→流血；减速→減速。

审校结论：通过；候选无需修改。

风险：无语义问题。保留发射时取值与不同计时单位；日语较长，公式与后句换行待视觉验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1`

中文原文：忽略<gray>（35%＋<level>等级</level>×0.5%）＝</gray>{penetration}%护甲，以<gray>（25%＋<level>等级</level>×0.5%）＝</gray>{pierce}%的概率贯穿，最多命中2个目标。

英文原文：Ignore <gray>(35% + <level>level</level> × 0.5%) = </gray>{penetration}% of armor, with a <gray>(25% + <level>level</level> × 0.5%) = </gray>{pierce}% chance to pierce. Hits up to 2 targets.

目标语言：

```text
忽略<gray>（35%＋<level>等級</level>×0.5%）＝</gray>{penetration}%盔甲，以<gray>（25%＋<level>等級</level>×0.5%）＝</gray>{pierce}%的機率貫穿，最多命中2個目標。
```

中文直译回译：忽略（35%＋等级×0.5%）＝{penetration}%盔甲，以（25%＋等级×0.5%）＝{pierce}%的概率贯穿，最多命中2个目标。

自然中文释义：穿甲比例与贯穿概率分别受等级影响；总命中数最多为两个目标。

术语对应：护甲→盔甲；贯穿→貫穿；概率→機率；最多命中→最多命中。

审校结论：通过；候选无需修改。

风险：无语义问题。两种语言均明确总命中上限，没有“贯穿两次后再命中第三个”的歧义；双公式宽度待视觉验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.simple.1`

中文原文：施展四段连击。前三段使敌人小幅浮空并施加短暂控制效果，冷却12秒。受到攻击时，该技能冷却缩短0.5秒。

英文原文：Perform a four-hit combo. The first three hits lift enemies slightly and briefly hinder them. Cooldown: 12s. Taking a hit reduces this skill's cooldown by 0.5s.

目标语言：

```text
施展四段連擊。前三段使敵人小幅浮空並施加短暫控制效果，冷卻12秒。受到攻擊時，此技能冷卻縮短0.5秒。
```

中文直译回译：施展四段连击。前三段使敌人小幅浮空并施加短暂控制效果，冷却12秒。受到攻击时，此技能冷却缩短0.5秒。

自然中文释义：简版保留四段连击、小幅浮空、泛化的短暂控制、12秒冷却以及受击减冷却0.5秒。

术语对应：四段连击→四段連擊；小幅浮空→小幅浮空；短暂控制→短暫控制效果；冷却→冷卻。

审校结论：通过；候选无需修改。

风险：无。泛化控制表述没有宣称眩晕、定身、缴械或完全不能行动；不把完整说明的80%及2秒重新塞回简版。日语总宽度待视觉验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.1`

中文原文：施展四段连击。前三段使敌人小幅浮空，命中单位近战攻击距离降低80%，持续2秒。冷却12秒。

英文原文：Perform a four-hit combo. The first three hits lift enemies slightly and reduce their melee reach by 80% for 2s. Cooldown: 12s.

目标语言：

```text
施展四段連擊。前三段使敵人小幅浮空，並使其近戰攻擊距離降低80%，持續2秒。冷卻12秒。
```

中文直译回译：施展四段连击。前三段使敌人小幅浮空，并使其近战攻击距离降低80%，持续2秒。冷却12秒。

自然中文释义：完整版明确控制为2秒内降低80%近战攻击距离，不涉及远程攻击距离。

术语对应：近战攻击距离→近戰攻擊距離；小幅浮空→小幅浮空；冷却→冷卻。

审校结论：通过；候选无需修改。

风险：无。缩距对象明确为敌人的近战攻击；未承诺击晕、缴械或完全禁止近战还击。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.2`

中文原文：<gray>-第1、3段各造成（<attack>攻击力</attack>×70%）＝{hit1}点伤害。</gray>

英文原文：<gray>-Hits 1 and 3 each deal (<attack>attack damage</attack> × 70%) = {hit1} damage.</gray>

目标语言：

```text
<gray>-第1、3段各造成（<attack>攻擊力</attack>×70%）＝{hit1}點傷害。</gray>
```

中文直译回译：第1、3段各造成（攻击力×70%）＝{hit1}点伤害。

自然中文释义：第1与第3段分别为70%攻击力，单个结果{hit1}对应两段各自的伤害。

术语对应：第1、3段各→第1、3段各；攻击力→攻擊力；伤害→傷害。

审校结论：通过；候选无需修改。

风险：无。明确是每段各70%，没有误成两段合计70%；未保留越战越勇加成。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.3`

中文原文：<gray>-第2段造成（<attack>攻击力</attack>×100%）＝{hit2}点伤害。</gray>

英文原文：<gray>-Hit 2 deals (<attack>attack damage</attack> × 100%) = {hit2} damage.</gray>

目标语言：

```text
<gray>-第2段造成（<attack>攻擊力</attack>×100%）＝{hit2}點傷害。</gray>
```

中文直译回译：第2段造成（攻击力×100%）＝{hit2}点伤害。

自然中文释义：第2段为100%攻击力，公式后直接显示{hit2}结果。

术语对应：第2段→第2段；攻击力→攻擊力；伤害→傷害。

审校结论：通过；候选无需修改。

风险：无。段次、倍率与占位符正确；未保留越战越勇加成。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.4`

中文原文：<gray>-第4段造成（<attack>攻击力</attack>×180%）＝{hit4}点伤害。</gray>

英文原文：<gray>-Hit 4 deals (<attack>attack damage</attack> × 180%) = {hit4} damage.</gray>

目标语言：

```text
<gray>-第4段造成（<attack>攻擊力</attack>×180%）＝{hit4}點傷害。</gray>
```

中文直译回译：第4段造成（攻击力×180%）＝{hit4}点伤害。

自然中文释义：第4段为180%攻击力，公式后直接显示{hit4}结果。

术语对应：第4段→第4段；攻击力→攻擊力；伤害→傷害。

审校结论：通过；候选无需修改。

风险：无。段次、倍率与占位符正确；未保留越战越勇加成。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.5`

中文原文：<gray>-前三段命中刷新减益。</gray>

英文原文：<gray>-The first three hits refresh the debuff.</gray>

目标语言：

```text
<gray>-前三段每次命中均會刷新減益。</gray>
```

中文直译回译：前三段每次命中都会刷新减益。

自然中文释义：前三段成功命中时刷新已有减益；第四段未包含在刷新范围中。

术语对应：前三段→前三段；刷新→刷新；减益→減益。

审校结论：通过；候选无需修改。

风险：无。保留“每次命中”，没有改成叠加减益强度。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.6`

中文原文：<gray>-受近战或投射物伤害时，冷却缩短0.5秒。</gray>

英文原文：<gray>-Taking melee or projectile damage reduces the cooldown by 0.5s.</gray>

目标语言：

```text
<gray>-受到近戰或投射物傷害時，冷卻縮短0.5秒。</gray>
```

中文直译回译：受到近战或投射物伤害时，冷却缩短0.5秒。

自然中文释义：完整说明限定减冷却触发来源为近战或投射物伤害，每次缩短0.5秒。

术语对应：近战伤害→近戰傷害；投射物伤害→投射物傷害；冷却→冷卻。

审校结论：通过；候选无需修改。

风险：无。「飛び道具」沿用既有投射物用词，不限缩成箭矢，也不扩展为所有环境伤害。 共同视觉边界见上。

需要作者决定：无需作者决定。

## 日本語（ja_jp）：13条审阅卡

共同使用位置：回声召唤器技能悬浮说明；`simple` 为默认简版，`full` 为Alt展开说明。下列目标语言保留候选原样，源文本采用本批双源（英文锥锋箭列最终已修订源）。

### `gui.echo_warrior.summoner.skill.roman.endures.full.1`

中文原文：举盾嘲讽5秒，护甲增加<gray>（12＋<level>等级</level>×0.1）＝</gray>{armor}点。结束时对周围敌人造成<gray>（100%＋<level>等级</level>×1%）＝</gray>{release}%的累计伤害。冷却20秒。

英文原文：Raise the shield and taunt for 5s, gaining <gray>(12 + <level>level</level> × 0.1) = </gray>{armor} armor. At the end, deal <gray>(100% + <level>level</level> × 1%) = </gray>{release}% of accumulated damage to nearby enemies. Cooldown: 20s.

目标语言：

```text
5秒間盾を構えて挑発し、防御力が<gray>（12＋<level>レベル</level>×0.1）＝</gray>{armor}増加。終了時、蓄積したダメージの<gray>（100%＋<level>レベル</level>×1%）＝</gray>{release}%を周囲の敵に与える。クールダウン20秒。
```

中文直译回译：举盾并挑衅5秒，防御力增加（12＋等级×0.1）＝{armor}。结束时，将累计伤害的（100%＋等级×1%）＝{release}%施加给周围敌人。冷却20秒。

自然中文释义：持续5秒的举盾嘲讽增加护甲；结束时按累计伤害及等级比例反击周围敌人，冷却20秒。

术语对应：护甲→防御力；等级→レベル；嘲讽→挑発；累计伤害→蓄積したダメージ。

审校结论：通过；候选无需修改。

风险：无语义问题。包含两条公式，展开后的总宽度和换行尚未实机验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.2`

中文原文：直接命中时，每种自身增益与目标减益追加<gray>（1＋<level>等级</level>÷100）＝</gray>{curse}点魔法伤害。

英文原文：Direct hits deal <gray>(1 + <level>level</level> ÷ 100) = </gray>{curse} extra magic damage per buff on the Echo and debuff on the target.

目标语言：

```text
直撃時、自身の強化効果と対象の弱体効果を合計し、1種類につき<gray>（1＋<level>レベル</level>÷100）＝</gray>{curse}の追加魔法ダメージを与える。
```

中文直译回译：直击时，合计自身的强化效果和目标的弱化效果，每一种追加（1＋等级÷100）＝{curse}的魔法伤害。

自然中文释义：只在直接命中时按自身增益种数与目标减益种数之和追加魔法伤害；每种效果的附伤受等级影响。

术语对应：直接命中→直撃；增益→強化効果；减益→弱体効果；魔法伤害→魔法ダメージ。

审校结论：通过；候选无需修改。

风险：无。日语「1種類につき」明确按种类计数；没有把附伤改成攻击力倍率。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.1`

中文原文：攻击有概率造成流血，每秒<gray>（1＋<attack>攻击力</attack>×10%）＝</gray>{bleed}点伤害，持续6秒。

英文原文：Attacks may cause bleeding for 6s, dealing <gray>(1 + <attack>attack damage</attack> × 10%) = </gray>{bleed} damage each second.

目标语言：

```text
攻撃時、6秒間の出血を与えることがある。毎秒<gray>（1＋<attack>攻撃力</attack>×10%）＝</gray>{bleed}のダメージ。
```

中文直译回译：攻击时可能施加持续6秒的出血，每秒造成（1＋攻击力×10%）＝{bleed}的伤害。

自然中文释义：攻击可能触发6秒流血，每秒伤害由固定值1加攻击力的10%计算。

术语对应：流血→出血；攻击力→攻撃力；有概率→ことがある。

审校结论：通过；候选无需修改。

风险：无。日语先写持续时间，保留了概率与每秒伤害，未改为必定触发。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.3`

中文原文：<gray>-次目标伤害（<attack>攻击力</attack>×50%）＝{half}点，强化后（<attack>攻击力</attack>×75%）＝{threequarters}点。</gray>

英文原文：<gray>-Secondary targets take (<attack>attack damage</attack> × 50%) = {half} damage, or (<attack>attack damage</attack> × 75%) = {threequarters} while empowered.</gray>

目标语言：

```text
<gray>-主対象以外へのダメージは（<attack>攻撃力</attack>×50%）＝{half}、強化中は（<attack>攻撃力</attack>×75%）＝{threequarters}。</gray>
```

中文直译回译：主目标以外的伤害为（攻击力×50%）＝{half}，强化期间为（攻击力×75%）＝{threequarters}。

自然中文释义：横扫次要目标承受50%攻击力伤害，强化期间提高为75%；两档结果各紧跟自己的公式。

术语对应：次目标→主対象以外；攻击力→攻撃力；强化后→強化中。

审校结论：通过；候选无需修改。

风险：无。日语用「強化中」沿用既有语境，明确强化有效期间，不暗示永久增伤。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.1`

中文原文：命中后流血4秒，每2秒造成<gray>（1＋<attack>发射时攻击力</attack>×10%）＝</gray>{bleed}点伤害，并减速2秒。

英文原文：Hits cause bleeding for 4s, dealing <gray>(1 + <attack>attack damage at firing</attack> × 10%) = </gray>{bleed} damage every 2s, and slow the target for 2s.

目标语言：

```text
命中時、4秒間の出血を与え、2秒ごとに<gray>（1＋<attack>発射時の攻撃力</attack>×10%）＝</gray>{bleed}のダメージ。さらに2秒間、移動速度を低下させる。
```

中文直译回译：命中时施加4秒出血，每2秒造成（1＋发射时攻击力×10%）＝{bleed}的伤害。此外使移动速度降低2秒。

自然中文释义：使用发射时的攻击力计算流血；流血4秒、每2秒伤害一次，另有2秒减速。

术语对应：发射时攻击力→発射時の攻撃力；流血→出血；减速→移動速度を低下させる。

审校结论：通过；候选无需修改。

风险：无语义问题。保留发射时取值与不同计时单位；日语较长，公式与后句换行待视觉验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1`

中文原文：忽略<gray>（35%＋<level>等级</level>×0.5%）＝</gray>{penetration}%护甲，以<gray>（25%＋<level>等级</level>×0.5%）＝</gray>{pierce}%的概率贯穿，最多命中2个目标。

英文原文：Ignore <gray>(35% + <level>level</level> × 0.5%) = </gray>{penetration}% of armor, with a <gray>(25% + <level>level</level> × 0.5%) = </gray>{pierce}% chance to pierce. Hits up to 2 targets.

目标语言：

```text
防御力を<gray>（35%＋<level>レベル</level>×0.5%）＝</gray>{penetration}%無視し、<gray>（25%＋<level>レベル</level>×0.5%）＝</gray>{pierce}%の確率で貫通。最大2体に命中する。
```

中文直译回译：无视（35%＋等级×0.5%）＝{penetration}%防御力，以（25%＋等级×0.5%）＝{pierce}%的概率贯通。最多命中2个目标。

自然中文释义：穿甲比例与贯穿概率分别受等级影响；总命中数最多为两个目标。

术语对应：护甲→防御力；贯穿→貫通；概率→確率；最多命中→最大2体に命中。

审校结论：通过；候选无需修改。

风险：无语义问题。两种语言均明确总命中上限，没有“贯穿两次后再命中第三个”的歧义；双公式宽度待视觉验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.simple.1`

中文原文：施展四段连击。前三段使敌人小幅浮空并施加短暂控制效果，冷却12秒。受到攻击时，该技能冷却缩短0.5秒。

英文原文：Perform a four-hit combo. The first three hits lift enemies slightly and briefly hinder them. Cooldown: 12s. Taking a hit reduces this skill's cooldown by 0.5s.

目标语言：

```text
4連撃を繰り出す。最初の3撃は敵をわずかに打ち上げ、行動を短時間妨げる。クールダウン12秒。攻撃を受けると、このスキルのクールダウンが0.5秒短縮される。
```

中文直译回译：使出四连击。前三击稍微将敌人挑起，并短时间妨碍其行动。冷却12秒。受到攻击时，本技能冷却缩短0.5秒。

自然中文释义：简版保留四段连击、小幅浮空、泛化的短暂控制、12秒冷却以及受击减冷却0.5秒。

术语对应：四段连击→4連撃；小幅浮空→わずかに打ち上げる；短暂控制→行動を短時間妨げる；冷却→クールダウン。

审校结论：通过；候选无需修改。

风险：无。泛化控制表述没有宣称眩晕、定身、缴械或完全不能行动；不把完整说明的80%及2秒重新塞回简版。日语总宽度待视觉验证。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.1`

中文原文：施展四段连击。前三段使敌人小幅浮空，命中单位近战攻击距离降低80%，持续2秒。冷却12秒。

英文原文：Perform a four-hit combo. The first three hits lift enemies slightly and reduce their melee reach by 80% for 2s. Cooldown: 12s.

目标语言：

```text
4連撃を繰り出す。最初の3撃は敵をわずかに打ち上げ、敵の近接攻撃のリーチを2秒間80%短縮する。クールダウン12秒。
```

中文直译回译：使出四连击。前三击稍微将敌人挑起，并使敌人的近接攻击触及距离在2秒内缩短80%。冷却12秒。

自然中文释义：完整版明确控制为2秒内降低80%近战攻击距离，不涉及远程攻击距离。

术语对应：近战攻击距离→近接攻撃のリーチ；小幅浮空→わずかに打ち上げる；冷却→クールダウン。

审校结论：通过；候选无需修改。

风险：无。缩距对象明确为敌人的近战攻击；未承诺击晕、缴械或完全禁止近战还击。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.2`

中文原文：<gray>-第1、3段各造成（<attack>攻击力</attack>×70%）＝{hit1}点伤害。</gray>

英文原文：<gray>-Hits 1 and 3 each deal (<attack>attack damage</attack> × 70%) = {hit1} damage.</gray>

目标语言：

```text
<gray>-1撃目と3撃目はそれぞれ（<attack>攻撃力</attack>×70%）＝{hit1}のダメージを与える。</gray>
```

中文直译回译：第1击和第3击分别造成（攻击力×70%）＝{hit1}的伤害。

自然中文释义：第1与第3段分别为70%攻击力，单个结果{hit1}对应两段各自的伤害。

术语对应：第1、3段各→1撃目と3撃目はそれぞれ；攻击力→攻撃力；伤害→ダメージ。

审校结论：通过；候选无需修改。

风险：无。明确是每段各70%，没有误成两段合计70%；未保留越战越勇加成。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.3`

中文原文：<gray>-第2段造成（<attack>攻击力</attack>×100%）＝{hit2}点伤害。</gray>

英文原文：<gray>-Hit 2 deals (<attack>attack damage</attack> × 100%) = {hit2} damage.</gray>

目标语言：

```text
<gray>-2撃目は（<attack>攻撃力</attack>×100%）＝{hit2}のダメージを与える。</gray>
```

中文直译回译：第2击造成（攻击力×100%）＝{hit2}的伤害。

自然中文释义：第2段为100%攻击力，公式后直接显示{hit2}结果。

术语对应：第2段→2撃目；攻击力→攻撃力；伤害→ダメージ。

审校结论：通过；候选无需修改。

风险：无。段次、倍率与占位符正确；未保留越战越勇加成。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.4`

中文原文：<gray>-第4段造成（<attack>攻击力</attack>×180%）＝{hit4}点伤害。</gray>

英文原文：<gray>-Hit 4 deals (<attack>attack damage</attack> × 180%) = {hit4} damage.</gray>

目标语言：

```text
<gray>-4撃目は（<attack>攻撃力</attack>×180%）＝{hit4}のダメージを与える。</gray>
```

中文直译回译：第4击造成（攻击力×180%）＝{hit4}的伤害。

自然中文释义：第4段为180%攻击力，公式后直接显示{hit4}结果。

术语对应：第4段→4撃目；攻击力→攻撃力；伤害→ダメージ。

审校结论：通过；候选无需修改。

风险：无。段次、倍率与占位符正确；未保留越战越勇加成。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.5`

中文原文：<gray>-前三段命中刷新减益。</gray>

英文原文：<gray>-The first three hits refresh the debuff.</gray>

目标语言：

```text
<gray>-最初の3撃は命中するたびに弱体効果を更新する。</gray>
```

中文直译回译：最初三击每次命中时都会更新弱化效果。

自然中文释义：前三段成功命中时刷新已有减益；第四段未包含在刷新范围中。

术语对应：前三段→最初の3撃；刷新→更新する；减益→弱体効果。

审校结论：通过；候选无需修改。

风险：无。保留“每次命中”，没有改成叠加减益强度。 共同视觉边界见上。

需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.skill.guandao.combo.full.6`

中文原文：<gray>-受近战或投射物伤害时，冷却缩短0.5秒。</gray>

英文原文：<gray>-Taking melee or projectile damage reduces the cooldown by 0.5s.</gray>

目标语言：

```text
<gray>-近接攻撃か飛び道具でダメージを受けると、クールダウンが0.5秒短縮。</gray>
```

中文直译回译：受到近接攻击或飞行武器造成的伤害时，冷却缩短0.5秒。

自然中文释义：完整说明限定减冷却触发来源为近战或投射物伤害，每次缩短0.5秒。

术语对应：近战→近接攻撃；投射物→飛び道具；冷却→クールダウン。

审校结论：通过；候选无需修改。

风险：无。「飛び道具」沿用既有投射物用词，不限缩成箭矢，也不扩展为所有环境伤害。 共同视觉边界见上。

需要作者决定：无需作者决定。

## 本审校实际改动

- `candidate-cjk.json`：无改动，全部26条可直接采用。
- 英文源文件：审校者未修改；主任务按反馈修正 `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1`，审校者已复核。
- 新增本报告，记录26条CJK审阅卡、13条英文源复核及自动检查和视觉边界。
