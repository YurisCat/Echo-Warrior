# Echo Warrior 本地化术语表

> 状态：中文／英文核心语义已由作者确认  
> 建立日期：2026-09-18  
> 作者确认日期：2026-09-18  
> 当前源语言：`zh_cn` + `en_us`

## 使用规则

- 中文负责说明设计原意，英文负责国际正式命名与格式；其他语言翻译时必须同时参考二者，不得进行“中文逐字直译”或“只看英文二次转译”。
- `固定` 表示后续翻译必须复用同一概念；任何需要改变固定术语含义的修改，都应先由作者重新确认。
- 第一批目标语言核心术语与中文回译记录在 `docs/localization/TARGET_TERMS_CORE.md`；独立语言审校通过不等于语言基线已接受，仍须满足视觉验收等接受条件。
- 英灵不是亡灵、鬼魂或完整复活的人。它是召唤器根据遗物承载的信息与执念重建出的历史战士回声；其他语言不得擅自翻成 zombie、undead 或普通 ghost。
- 物品名、英雄名和技能名优先简洁；技能描述保持类似成熟动作／策略游戏的直接效果说明，不加入开发策划口吻或无效气氛描写。
- Minecraft 原版术语优先沿用该语言的原版官方译法，例如生物群系、护甲、击退、时运和亡灵；生物类别必须与实际标签范围一致，不能将袭击类生物缩成掠夺者或灾厄村民家族。
- 目标外语的作者审阅格式、中文回译和独立审校要求见 `docs/localization/REVIEW_WORKFLOW.md`。

## 核心世界观与系统术语

| ID | 中文正式名称 | 当前英文 | 含义与翻译约束 | 状态 |
|---|---|---|---|---|
| CORE-001 | 英灵回声 | Echo Warrior | 模组正式名称；英文品牌不改写为 Echo Warriors。 | 固定 |
| CORE-002 | 英灵 | Echo | 历史武士留下、并被召唤器重建出的“回声投影”，不是灵魂本人、亡灵或鬼魂。复数按目标语言语法处理。 | 固定 |
| CORE-003 | 回声召唤器 | Echo Summoner | 一件召唤装置，不是“负责召唤的人”。其他语言应优先表达装置含义。 | 固定 |
| CORE-004 | 回声罗盘 | Echo Compass | 定位战场遗迹与考古回声的罗盘。 | 固定 |
| CORE-005 | 战场遗迹 | Battlefield Ruins | 自然生成的古战场考古地点；不是普通建筑废墟。 | 固定 |
| CORE-006 | 英灵遗物／遗物 | Echo Relic / Relic | 决定召唤哪名英灵、保存等级与天赋的核心物品；不是普通饰品。英雄专名采用 Roman Legionary Relic 等格式。 | 固定 |
| CORE-007 | 英灵饰品／饰品 | Echo Accessory / Accessory | 安装在召唤器内、为英灵提供加成的装备；与遗物严格区分。 | 固定 |
| CORE-008 | 传承 | Legacy | 记载英雄事迹或高尚品质的可消耗典籍，也可作为饰品材料；不是血统继承。 | 固定 |
| CORE-009 | 天赋 | Talent | 遗物随机生成的被动特质。代码键使用 `trait`，玩家文本统一使用 Talent。 | 固定 |
| CORE-010 | 技能 | Skill | 英灵固有、可在召唤器中启用或禁用的能力；与随机天赋区分。 | 固定 |
| CORE-011 | 状态效果 | Status Effect | 显示在 Minecraft 状态栏中的效果。优先使用目标语言的原版术语。 | 固定 |
| CORE-012 | 英灵燃料／燃料 | Echo Fuel / Fuel | 召唤和自然恢复消耗的召唤器资源。 | 固定 |
| CORE-013 | 召唤 | Summon | 让装入遗物的英灵在世界中出现。 | 固定 |
| CORE-014 | 收回／遣散 | Dismiss | 让当前英灵退出世界但保留遗物数据；不是杀死或删除。 | 固定 |
| CORE-015 | 知识碎片 | Knowledge Fragment | 单页历史知识收藏物。 | 固定 |
| CORE-016 | 知识碎片集 | Knowledge Fragment Collection | 保存多个知识页面的合集物品，不直接称普通 Book。 | 固定 |
| CORE-017 | 英灵杂物回收箱 | Echo Salvage Chest | 回收重复或不需要的英灵物品，等待午夜后返还材料。英文名称、动词及可回收属性统一采用 Salvage / salvage / salvageable。 | 固定 |
| CORE-018 | 《回声、战士与你》 | Echoes, Warriors, and You | 教程手册正式书名；标点按目标语言书名习惯处理。 | 固定 |

## 召唤器模式

| 中文 | 当前英文 | 约束 |
|---|---|---|
| 跟随 | Follow | 跟随主人，过远时安全传送。 |
| 等待 | Wait | 留在指定地点附近。 |
| 闲逛 | Wander | 在当前位置附近自由活动。 |
| 主动出击 | Aggressive | 主动攻击附近敌人并响应主人目标。 |
| 被动防御 | Defensive | 只为自身、主人或主人的攻击进行反击。 |
| 和平模式 | Peaceful | 不主动索敌，但受到攻击时仍可自卫；不是 Minecraft 难度。 |

## Minecraft 生物类别

| 中文正式名称 | 英文 | 含义与翻译约束 |
|---|---|---|
| 袭击类生物 | raider mobs | 对应 `EntityTypeTags.RAIDERS` 整个标签，不要求当前正在参加袭击；不能缩成 Pillager 单一实体或仅 Illager 家族。相关天赋名为“袭击者杀手／Raider Slayer”。 |

## 英雄与文明

| 中文正式名称 | 当前英文 | 文化／命名规则 | 状态 |
|---|---|---|---|
| 罗马军团兵 | Roman Legionary | `legionary`，不简化成普通 Roman Soldier。实体名为 Roman Legionary Echo。 | 固定 |
| 阿兹特克勇士 | Aztec Warrior | 玩家英雄名保留大众熟悉的 Aztec；具体历史条目使用更准确的 Mexica。 | 固定 |
| 埃及弓箭手 | Egyptian Archer | 实体名为 Egyptian Archer Echo。 | 固定 |
| 中国关刀战士 | Chinese Guandao Warrior | Guandao 保留专名，不泛化为普通 spear 或 sword。 | 固定 |
| 日本武士 | Japanese Samurai | 英雄名使用 Samurai。 | 固定 |
| 古罗马 | Ancient Rome | 知识条目的文明分类。 | 固定 |
| 墨西加 | Mexica | 特诺奇蒂特兰相关历史文化的精确称呼；与英雄展示名 Aztec Warrior 并存。 | 固定 |
| 古埃及 | Ancient Egypt | 知识条目的文明分类。 | 固定 |
| 古代中国 | Ancient China | 知识条目的文明分类。 | 固定 |
| 日本武家 | Samurai-Era Japan | 知识条目的文明分类；不再使用不自然的 Warrior Japan。 | 固定 |

## 文化专名

| 中文／音译 | 拉丁字母基准 | 说明 |
|---|---|---|
| 羽蛇神／克察尔科亚特尔 | Quetzalcoatl | 技能名当前使用 Quetzalcoatl's Curse。 |
| 维齐洛波奇特利 | Huitzilopochtli | 技能名当前使用 Huitzilopochtli's Blessing。 |
| 马夸威特 | Macuahuitl | 墨西加黑曜石刃武器；不翻成普通 sword。 |
| 关刀 | Guandao | 中国长柄刀专名。 |
| 踏込 | Fumikomi | 日本武术用语；其他语言可音译并在描述中解释效果。 |
| 残心 | Zanshin | 日本武术概念；优先保留文化专名。 |
| 斩 | Zan | 当前技能英文保留日语读音。 |
| 狸奴 | linu | 古人对猫的爱称；外语保留音译或文化专名，不追加“浣熊仆人”的字面解释。作者于 2026-09-19 确认独立审校问题 A1。 |

## 技能正式名称

| 英雄 | 中文 | 当前英文 |
|---|---|---|
| 罗马军团兵 | 军团阵列 | Legion Formation |
| 罗马军团兵 | 军团壁垒 | Legionary Bulwark |
| 罗马军团兵 | 举盾冲锋 | Shield Charge |
| 罗马军团兵 | 军团永存 | The Legion Endures |
| 阿兹特克勇士 | 羽蛇神的诅咒 | Quetzalcoatl's Curse |
| 阿兹特克勇士 | 维齐洛波奇特利的祝福 | Huitzilopochtli's Blessing |
| 阿兹特克勇士 | 黑曜石创口 | Obsidian Wound |
| 阿兹特克勇士 | 追猎 | Pursuit |
| 阿兹特克勇士 | 马夸威特 | Macuahuitl |
| 埃及弓箭手 | 赞美猫神 | Praise the Cat God |
| 埃及弓箭手 | 战车与齐射之魂 | Spirit of Chariot and Volley |
| 埃及弓箭手 | 后撤步 | Backstep |
| 埃及弓箭手 | 普通箭 | Normal Arrow |
| 埃及弓箭手 | 叶形箭 | Leaf-shaped Arrow |
| 埃及弓箭手 | 锥锋箭 | Cone-point Arrow |
| 中国关刀战士 | 甲胄傍身 | Armor-Clad |
| 中国关刀战士 | 关刀连段招式 | Guandao Combo |
| 中国关刀战士 | 偃月刀 | Crescent Blade |
| 中国关刀战士 | 越战越勇 | Growing Valor |
| 日本武士 | 踏込 | Fumikomi |
| 日本武士 | 刺 | Stab |
| 日本武士 | 斩 | Zan |
| 日本武士 | 残心 | Zanshin |

## 作者确认记录

### D1：其他语言如何理解“英灵”——已确认

英文 `Echo` 是概念基准，解释为“历史武士的回声投影”，不改成 Heroic Spirit、Soul、Ghost 或 Undead。其他语言优先寻找“回声／残响”词根，同时保证玩家能看出它是可召唤伙伴。

### D2：`Echo Summoner` 是否继续作为装置名——已确认

正式英文名保留 `Echo Summoner`，正式中文名为“回声召唤器”。教程和其他语言应明确它是装置，不在翻译时擅自改成职业名称。

### D3：Aztec 与 Mexica 的边界——已确认

英雄名保留 `Aztec Warrior`，因为辨识度高；历史知识、文明分类和具体文物继续使用 `Mexica`。采用“玩家名称通俗、百科内容精确”的双层规则。

### D4：日本文明分类英文——已确认

英文统一使用 `Samurai-Era Japan`，中文仍为“日本武家”。不再使用 `Warrior Japan`。

### D5：回收系统统一使用 Salvage 还是 Recycle——已确认

英文统一使用 `Salvage`：方块为 `Echo Salvage Chest`，动词为 `salvage`，可回收属性为 `salvageable`。技术 ID 和数据标签路径继续保留既有 `recycler`／`recyclable`，避免破坏兼容性。

### A1：狸奴的文化释义——已确认（2026-09-19）

作者同意将“狸奴”统一解释为古人对猫的爱称；外语保留音译或文化专名，删除英文、俄语、葡语及西语中的“浣熊仆人”字面解释。画作“传统归属”的史实限定维持不变。

## 后续维护

- D1～D5 与 A1 已确认；生成目标语言术语时必须遵守上述固定含义。
- 新增英雄、技能、物品系统或反复出现的机制词时，先更新本文件，再进行批量翻译。
- 普通一次性句子无需全部进入术语表；只有会反复出现、容易产生文化或机制歧义的词才收录。
