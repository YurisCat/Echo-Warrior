# 技能公式增量独立审校：西班牙西语与中性拉美西语

后续集成验收：本批已完成八加载器客户端及26.1.2／1.21.1代表性视觉复核，语言基线已更新，发布级门禁0错误、0待处理；见[实施与验收记录](../research/skill-formula-display-2026-11-13.md)。以下独立审校过程保留当时的职责边界。

日期：2026-11-13。角色：独立审校者。范围：`es_es`、`es_mx`各13键，共26条。

依据：`REVIEW_WORKFLOW.md`、`TERMINOLOGY.md`、`TARGET_TERMS_CORE.md`，以及 `build/skill-formula-update-2026-11-13/translation-packet.json` 中的中英双源、既有地区语言上下文和 `candidate-spanish.json`。本轮没有读取译者解释，没有访问网络、构建或启动游戏。

补充核对：锥锋箭英文引用同步本轮 `common/src/main/resources/assets/echo_warrior/lang/en_us.json` 的最终句式 `Hits up to 2 targets.`；两套西语原候选已用独立句 `Alcanza hasta 2 objetivos.` 表达总共最多命中2个，含义一致，无需改译。

## 独立结论

- 两种地区语言逐条审校通过；必须修改0项，建议修改0项，未解决作者语义问题0项。候选未作修改。
- 本批用词属于通用西语，两地区采用相同文本自然且准确；不是将两地区整体语言资源合并。`es_mx`的地区别名同步由主任务负责，本报告不把别名称作独立审校。
- 以本批最新双源为准：展开版公式直接位于对应结果之前，展开版结果无星号。术语表2026-11-12的旧星号描述不能覆盖此次明确格式变更。
- 关刀分段分别为第1、3段70%，第2段100%，第4段180%；没有旧版越战越勇或被动加成说明。简版只说短暂控制，展开版明确敌人近战攻击距离降低80%、持续2秒。
- 语言审校与本批结构检查通过不代表本轮视觉验收已完成；尚无本轮实机证据，未执行或授权 `--mark-current`。

## 本批静态核对

- 每个地区均为13键，与双源完全一致；共12组公式及结果，全部保持计算项、系数、运算符与结果绑定。
- 已用本地Python逐键核对命名占位符及重复次数、样式标签顺序与嵌套、公式紧邻结果、无星号；全部通过。对公式归一化时只移除排版空格、将小数逗号还原为小数点并统一标签内术语，未忽略任何系数或运算符。
- `0,1`、`0,5`和百分号前空格沿用既有西语排版，不改变值占位符。红色攻击项、绿色等级项、灰色公式及整行补充均保持与双源相同的标签结构。
- 本检查只覆盖26条候选；整包键集、所有兼容线与别名同步、发布门禁由主任务完成，不能由本报告代替。
- 候选文件SHA-256：`ab90b1fb31b52159a0351c83742a57450a0b178292e868185b783cefa9374741`。

## 逐项审阅卡

下列每张卡均以当前候选原文记录。中文回译保留公式和占位符，省略颜色标签以便作者检查含义；目标语言代码块完整保留标签。两地区逐项结论分别登记。

### 西班牙西语（es_es）

#### 1. `roman.endures.full.1`

键：`gui.echo_warrior.summoner.skill.roman.endures.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
举盾嘲讽5秒，护甲增加<gray>（12＋<level>等级</level>×0.1）＝</gray>{armor}点。结束时对周围敌人造成<gray>（100%＋<level>等级</level>×1%）＝</gray>{release}%的累计伤害。冷却20秒。
```

英文原文：

```text
Raise the shield and taunt for 5s, gaining <gray>(12 + <level>level</level> × 0.1) = </gray>{armor} armor. At the end, deal <gray>(100% + <level>level</level> × 1%) = </gray>{release}% of accumulated damage to nearby enemies. Cooldown: 20s.
```

目标语言（es_es）：

```text
Alza el escudo y provoca a los enemigos durante 5 s, ganando <gray>(12 + <level>nivel</level> × 0,1) = </gray>{armor} puntos de armadura. Al terminar, inflige a los enemigos cercanos un <gray>(100 % + <level>nivel</level> × 1 %) = </gray>{release} % del daño acumulado. Recarga: 20 s.
```

中文直译回译：举起盾牌并嘲讽敌人5秒，获得（12＋等级×0.1）＝{armor}点护甲。结束时，对附近敌人造成累计伤害的（100%＋等级×1%）＝{release}%。冷却20秒。

自然中文释义：嘲讽期间增加计算后的护甲，结束后按计算比例向周围敌人释放累计伤害；持续时间和冷却未变。

术语对应：护甲 → armadura；等级 → nivel；嘲讽 → provoca a los enemigos；累计伤害 → daño acumulado；冷却 → Recarga。

审校结论：通过。

风险：长正文包含两组公式，须实机确认换行；未发现语义、数值或占位符问题。

需要作者决定：无需作者决定。

#### 2. `aztec.quetzalcoatls_curse.full.2`

键：`gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.2`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
直接命中时，每种自身增益与目标减益追加<gray>（1＋<level>等级</level>÷100）＝</gray>{curse}点魔法伤害。
```

英文原文：

```text
Direct hits deal <gray>(1 + <level>level</level> ÷ 100) = </gray>{curse} extra magic damage per buff on the Echo and debuff on the target.
```

目标语言（es_es）：

```text
Los golpes directos infligen <gray>(1 + <level>nivel</level> ÷ 100) = </gray>{curse} de daño mágico adicional por cada efecto beneficioso de este Eco y por cada efecto perjudicial del objetivo.
```

中文直译回译：直接命中造成（1＋等级÷100）＝{curse}额外魔法伤害，按此英灵的每种有益效果和目标的每种有害效果分别计入。

自然中文释义：自身增益与敌方减益都参与计数，每种追加同一份魔法伤害；没有误写为只计算一侧或按攻击力相乘。

术语对应：英灵 → Eco；直接命中 → golpes directos；增益 → efecto beneficioso；减益 → efecto perjudicial；魔法伤害 → daño mágico；等级 → nivel。

审校结论：通过。

风险：句子较长，须实机确认后半句完整显示；双侧计数、除以100与值占位符均正确。

需要作者决定：无需作者决定。

#### 3. `aztec.obsidian_wound.full.1`

键：`gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
攻击有概率造成流血，每秒<gray>（1＋<attack>攻击力</attack>×10%）＝</gray>{bleed}点伤害，持续6秒。
```

英文原文：

```text
Attacks may cause bleeding for 6s, dealing <gray>(1 + <attack>attack damage</attack> × 10%) = </gray>{bleed} damage each second.
```

目标语言（es_es）：

```text
Los ataques pueden causar una hemorragia que inflige <gray>(1 + <attack>daño de ataque</attack> × 10 %) = </gray>{bleed} de daño por segundo durante 6 s.
```

中文直译回译：攻击可能引起流血，在6秒内每秒造成（1＋攻击力×10%）＝{bleed}伤害。

自然中文释义：流血有触发概率，持续6秒，每秒结算一次；伤害包含固定1点与攻击力的10%。

术语对应：流血 → hemorragia；攻击力 → daño de ataque；有概率 → pueden causar；每秒 → por segundo。

审校结论：通过。

风险：需实机确认公式与秒数换行；未把概率触发改成必定触发。

需要作者决定：无需作者决定。

#### 4. `aztec.macuahuitl.full.3`

键：`gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.3`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-次目标伤害（<attack>攻击力</attack>×50%）＝{half}点，强化后（<attack>攻击力</attack>×75%）＝{threequarters}点。</gray>
```

英文原文：

```text
<gray>-Secondary targets take (<attack>attack damage</attack> × 50%) = {half} damage, or (<attack>attack damage</attack> × 75%) = {threequarters} while empowered.</gray>
```

目标语言（es_es）：

```text
<gray>-Daño a objetivos secundarios: (<attack>daño de ataque</attack> × 50 %) = {half}; al potenciarse: (<attack>daño de ataque</attack> × 75 %) = {threequarters}.</gray>
```

中文直译回译：对次要目标的伤害：（攻击力×50%）＝{half}；强化时：（攻击力×75%）＝{threequarters}。

自然中文释义：次要目标普通情况下受到50%攻击力伤害，强化期间为75%；两种结果各自紧跟对应公式。

术语对应：次要目标 → objetivos secundarios；攻击力 → daño de ataque；强化时 → al potenciarse。

审校结论：通过。

风险：本行承接同技能正文中的强化状态，语义明确；整行灰色且攻击力项红色的显示仍待实机确认。

需要作者决定：无需作者决定。

#### 5. `egyptian.leaf_arrow.full.1`

键：`gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
命中后流血4秒，每2秒造成<gray>（1＋<attack>发射时攻击力</attack>×10%）＝</gray>{bleed}点伤害，并减速2秒。
```

英文原文：

```text
Hits cause bleeding for 4s, dealing <gray>(1 + <attack>attack damage at firing</attack> × 10%) = </gray>{bleed} damage every 2s, and slow the target for 2s.
```

目标语言（es_es）：

```text
Los impactos causan hemorragia durante 4 s, con <gray>(1 + <attack>daño de ataque al disparar</attack> × 10 %) = </gray>{bleed} de daño cada 2 s, y ralentizan al objetivo durante 2 s.
```

中文直译回译：命中造成4秒流血，每2秒造成（1＋发射时攻击力×10%）＝{bleed}伤害，并使目标减速2秒。

自然中文释义：流血使用箭矢发射时的攻击力快照，持续4秒、每2秒结算；减速独立持续2秒。

术语对应：流血 → hemorragia；发射时攻击力 → daño de ataque al disparar；每2秒 → cada 2 s；减速 → ralentizan。

审校结论：通过。

风险：长公式的攻击力名称较长，须实机确认；发射时点及两种持续时间准确。

需要作者决定：无需作者决定。

#### 6. `egyptian.cone_arrow.full.1`

键：`gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
忽略<gray>（35%＋<level>等级</level>×0.5%）＝</gray>{penetration}%护甲，以<gray>（25%＋<level>等级</level>×0.5%）＝</gray>{pierce}%的概率贯穿，最多命中2个目标。
```

英文原文：

```text
Ignore <gray>(35% + <level>level</level> × 0.5%) = </gray>{penetration}% of armor, with a <gray>(25% + <level>level</level> × 0.5%) = </gray>{pierce}% chance to pierce. Hits up to 2 targets.
```

目标语言（es_es）：

```text
Ignora un <gray>(35 % + <level>nivel</level> × 0,5 %) = </gray>{penetration} % de la armadura y tiene un <gray>(25 % + <level>nivel</level> × 0,5 %) = </gray>{pierce} % de probabilidad de atravesar al objetivo. Alcanza hasta 2 objetivos.
```

中文直译回译：忽略护甲的（35%＋等级×0.5%）＝{penetration}%，并有（25%＋等级×0.5%）＝{pierce}%的概率贯穿目标。最多命中2个目标。

自然中文释义：穿甲比例与贯穿概率是不同数值，各自先写公式再写结果；总命中上限仍是2个目标。

术语对应：护甲 → armadura；等级 → nivel；概率 → probabilidad；贯穿 → atravesar；最多2个目标 → hasta 2 objetivos。

审校结论：通过。

风险：双公式使正文较长，须实机确认；未把穿甲与贯穿概率混为一项。

需要作者决定：无需作者决定。

#### 7. `guandao.combo.simple.1`

键：`gui.echo_warrior.summoner.skill.guandao.combo.simple.1`\
使用位置：召唤器关刀连段技能简版提示。

中文原文：

```text
施展四段连击。前三段使敌人小幅浮空并施加短暂控制效果，冷却12秒。受到攻击时，该技能冷却缩短0.5秒。
```

英文原文：

```text
Perform a four-hit combo. The first three hits lift enemies slightly and briefly hinder them. Cooldown: 12s. Taking a hit reduces this skill's cooldown by 0.5s.
```

目标语言（es_es）：

```text
Ejecuta cuatro golpes. Los tres primeros elevan ligeramente a los enemigos y les aplican un breve efecto de control. Recarga: 12 s. Recibir un golpe reduce la recarga de esta habilidad en 0,5 s.
```

中文直译回译：施展四次打击。前三次将敌人轻微抬起，并施加短暂的控制效果。冷却12秒。受到一次打击会使此技能的冷却减少0.5秒。

自然中文释义：四段连击的前三段带轻微浮空和短暂控制；受击可减冷却。简版没有重复展开版的80%与2秒细节。

术语对应：四段连击 → cuatro golpes；小幅浮空 → elevan ligeramente；短暂控制效果 → un breve efecto de control；技能 → habilidad；冷却 → recarga。

审校结论：通过。

风险：efecto de control 是一般控制效果，不声明眩晕、定身、缴械或精神操控；未引入原文没有的具体控制类型。长简版仍待视觉验收。

需要作者决定：无需作者决定。

#### 8. `guandao.combo.full.1`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
施展四段连击。前三段使敌人小幅浮空，命中单位近战攻击距离降低80%，持续2秒。冷却12秒。
```

英文原文：

```text
Perform a four-hit combo. The first three hits lift enemies slightly and reduce their melee reach by 80% for 2s. Cooldown: 12s.
```

目标语言（es_es）：

```text
Ejecuta cuatro golpes. Los tres primeros elevan ligeramente a los enemigos y reducen su alcance cuerpo a cuerpo un 80 % durante 2 s. Recarga: 12 s.
```

中文直译回译：施展四次打击。前三次将敌人轻微抬起，并使其近战攻击距离降低80%，持续2秒。冷却12秒。

自然中文释义：降距对象是被命中的敌人；减少80%而非减少到80%，持续2秒。

术语对应：近战攻击距离 → alcance cuerpo a cuerpo；降低80% → reducen … un 80 %；轻微浮空 → elevan ligeramente；冷却 → Recarga。

审校结论：通过。

风险：代词 su 指向前句的敌人，未写成英灵自身降距；须实机确认全文换行。

需要作者决定：无需作者决定。

#### 9. `guandao.combo.full.2`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.2`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-第1、3段各造成（<attack>攻击力</attack>×70%）＝{hit1}点伤害。</gray>
```

英文原文：

```text
<gray>-Hits 1 and 3 each deal (<attack>attack damage</attack> × 70%) = {hit1} damage.</gray>
```

目标语言（es_es）：

```text
<gray>-Los golpes 1 y 3 infligen cada uno (<attack>daño de ataque</attack> × 70 %) = {hit1} de daño.</gray>
```

中文直译回译：第1段和第3段各造成（攻击力×70%）＝{hit1}伤害。

自然中文释义：第1、3段分别结算同一70%攻击力公式，不是两段合计70%。

术语对应：各 → cada uno；第1、3段 → golpes 1 y 3；攻击力 → daño de ataque。

审校结论：通过。

风险：无未解决语言风险；各段数值、标签与占位符正确。整行灰色及红色攻击力仍待实机验证。

需要作者决定：无需作者决定。

#### 10. `guandao.combo.full.3`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.3`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-第2段造成（<attack>攻击力</attack>×100%）＝{hit2}点伤害。</gray>
```

英文原文：

```text
<gray>-Hit 2 deals (<attack>attack damage</attack> × 100%) = {hit2} damage.</gray>
```

目标语言（es_es）：

```text
<gray>-El golpe 2 inflige (<attack>daño de ataque</attack> × 100 %) = {hit2} de daño.</gray>
```

中文直译回译：第2段造成（攻击力×100%）＝{hit2}伤害。

自然中文释义：第二段单独列出100%攻击力及其当前计算值。

术语对应：第2段 → golpe 2；造成伤害 → inflige … de daño；攻击力 → daño de ataque。

审校结论：通过。

风险：无未解决语言风险；未夹带越战越勇或被动加成说明。显示仍待实机验证。

需要作者决定：无需作者决定。

#### 11. `guandao.combo.full.4`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.4`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-第4段造成（<attack>攻击力</attack>×180%）＝{hit4}点伤害。</gray>
```

英文原文：

```text
<gray>-Hit 4 deals (<attack>attack damage</attack> × 180%) = {hit4} damage.</gray>
```

目标语言（es_es）：

```text
<gray>-El golpe 4 inflige (<attack>daño de ataque</attack> × 180 %) = {hit4} de daño.</gray>
```

中文直译回译：第4段造成（攻击力×180%）＝{hit4}伤害。

自然中文释义：第四段单独列出180%攻击力及其当前计算值。

术语对应：第4段 → golpe 4；造成伤害 → inflige … de daño；攻击力 → daño de ataque。

审校结论：通过。

风险：无未解决语言风险；未夹带越战越勇或被动加成说明。显示仍待实机验证。

需要作者决定：无需作者决定。

#### 12. `guandao.combo.full.5`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.5`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-前三段命中刷新减益。</gray>
```

英文原文：

```text
<gray>-The first three hits refresh the debuff.</gray>
```

目标语言（es_es）：

```text
<gray>-Cada uno de los tres primeros golpes renueva el efecto perjudicial.</gray>
```

中文直译回译：前三次打击中的每一次都刷新有害效果。

自然中文释义：前三段命中分别刷新同一个减益效果，不是叠加新效果或只在第三段刷新。

术语对应：每次 → cada uno；前三段命中 → tres primeros golpes；刷新 → renueva；减益 → efecto perjudicial。

审校结论：通过。

风险：无未解决语言风险；简短补充，仍未取得本轮实机视觉证据。

需要作者决定：无需作者决定。

#### 13. `guandao.combo.full.6`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.6`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-受近战或投射物伤害时，冷却缩短0.5秒。</gray>
```

英文原文：

```text
<gray>-Taking melee or projectile damage reduces the cooldown by 0.5s.</gray>
```

目标语言（es_es）：

```text
<gray>-Recibir daño cuerpo a cuerpo o de proyectiles reduce la recarga en 0,5 s.</gray>
```

中文直译回译：受到近战或投射物伤害，会使冷却减少0.5秒。

自然中文释义：受伤来源限定为近战或投射物；每次触发减去0.5秒，不是把冷却设置成0.5秒。

术语对应：近战伤害 → daño cuerpo a cuerpo；投射物 → proyectiles；冷却缩短 → reduce la recarga；0.5秒 → 0,5 s。

审校结论：通过。

风险：无未解决语言风险；来源限制及减少量正确，仍未取得本轮实机视觉证据。

需要作者决定：无需作者决定。

### 中性拉美西语（es_mx）

#### 1. `roman.endures.full.1`

键：`gui.echo_warrior.summoner.skill.roman.endures.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
举盾嘲讽5秒，护甲增加<gray>（12＋<level>等级</level>×0.1）＝</gray>{armor}点。结束时对周围敌人造成<gray>（100%＋<level>等级</level>×1%）＝</gray>{release}%的累计伤害。冷却20秒。
```

英文原文：

```text
Raise the shield and taunt for 5s, gaining <gray>(12 + <level>level</level> × 0.1) = </gray>{armor} armor. At the end, deal <gray>(100% + <level>level</level> × 1%) = </gray>{release}% of accumulated damage to nearby enemies. Cooldown: 20s.
```

目标语言（es_mx）：

```text
Alza el escudo y provoca a los enemigos durante 5 s, ganando <gray>(12 + <level>nivel</level> × 0,1) = </gray>{armor} puntos de armadura. Al terminar, inflige a los enemigos cercanos un <gray>(100 % + <level>nivel</level> × 1 %) = </gray>{release} % del daño acumulado. Recarga: 20 s.
```

中文直译回译：举起盾牌并嘲讽敌人5秒，获得（12＋等级×0.1）＝{armor}点护甲。结束时，对附近敌人造成累计伤害的（100%＋等级×1%）＝{release}%。冷却20秒。

自然中文释义：嘲讽期间增加计算后的护甲，结束后按计算比例向周围敌人释放累计伤害；持续时间和冷却未变。

术语对应：护甲 → armadura；等级 → nivel；嘲讽 → provoca a los enemigos；累计伤害 → daño acumulado；冷却 → Recarga。

审校结论：通过。

风险：长正文包含两组公式，须实机确认换行；未发现语义、数值或占位符问题。

需要作者决定：无需作者决定。

#### 2. `aztec.quetzalcoatls_curse.full.2`

键：`gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.2`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
直接命中时，每种自身增益与目标减益追加<gray>（1＋<level>等级</level>÷100）＝</gray>{curse}点魔法伤害。
```

英文原文：

```text
Direct hits deal <gray>(1 + <level>level</level> ÷ 100) = </gray>{curse} extra magic damage per buff on the Echo and debuff on the target.
```

目标语言（es_mx）：

```text
Los golpes directos infligen <gray>(1 + <level>nivel</level> ÷ 100) = </gray>{curse} de daño mágico adicional por cada efecto beneficioso de este Eco y por cada efecto perjudicial del objetivo.
```

中文直译回译：直接命中造成（1＋等级÷100）＝{curse}额外魔法伤害，按此英灵的每种有益效果和目标的每种有害效果分别计入。

自然中文释义：自身增益与敌方减益都参与计数，每种追加同一份魔法伤害；没有误写为只计算一侧或按攻击力相乘。

术语对应：英灵 → Eco；直接命中 → golpes directos；增益 → efecto beneficioso；减益 → efecto perjudicial；魔法伤害 → daño mágico；等级 → nivel。

审校结论：通过。

风险：句子较长，须实机确认后半句完整显示；双侧计数、除以100与值占位符均正确。

需要作者决定：无需作者决定。

#### 3. `aztec.obsidian_wound.full.1`

键：`gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
攻击有概率造成流血，每秒<gray>（1＋<attack>攻击力</attack>×10%）＝</gray>{bleed}点伤害，持续6秒。
```

英文原文：

```text
Attacks may cause bleeding for 6s, dealing <gray>(1 + <attack>attack damage</attack> × 10%) = </gray>{bleed} damage each second.
```

目标语言（es_mx）：

```text
Los ataques pueden causar una hemorragia que inflige <gray>(1 + <attack>daño de ataque</attack> × 10 %) = </gray>{bleed} de daño por segundo durante 6 s.
```

中文直译回译：攻击可能引起流血，在6秒内每秒造成（1＋攻击力×10%）＝{bleed}伤害。

自然中文释义：流血有触发概率，持续6秒，每秒结算一次；伤害包含固定1点与攻击力的10%。

术语对应：流血 → hemorragia；攻击力 → daño de ataque；有概率 → pueden causar；每秒 → por segundo。

审校结论：通过。

风险：需实机确认公式与秒数换行；未把概率触发改成必定触发。

需要作者决定：无需作者决定。

#### 4. `aztec.macuahuitl.full.3`

键：`gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.3`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-次目标伤害（<attack>攻击力</attack>×50%）＝{half}点，强化后（<attack>攻击力</attack>×75%）＝{threequarters}点。</gray>
```

英文原文：

```text
<gray>-Secondary targets take (<attack>attack damage</attack> × 50%) = {half} damage, or (<attack>attack damage</attack> × 75%) = {threequarters} while empowered.</gray>
```

目标语言（es_mx）：

```text
<gray>-Daño a objetivos secundarios: (<attack>daño de ataque</attack> × 50 %) = {half}; al potenciarse: (<attack>daño de ataque</attack> × 75 %) = {threequarters}.</gray>
```

中文直译回译：对次要目标的伤害：（攻击力×50%）＝{half}；强化时：（攻击力×75%）＝{threequarters}。

自然中文释义：次要目标普通情况下受到50%攻击力伤害，强化期间为75%；两种结果各自紧跟对应公式。

术语对应：次要目标 → objetivos secundarios；攻击力 → daño de ataque；强化时 → al potenciarse。

审校结论：通过。

风险：本行承接同技能正文中的强化状态，语义明确；整行灰色且攻击力项红色的显示仍待实机确认。

需要作者决定：无需作者决定。

#### 5. `egyptian.leaf_arrow.full.1`

键：`gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
命中后流血4秒，每2秒造成<gray>（1＋<attack>发射时攻击力</attack>×10%）＝</gray>{bleed}点伤害，并减速2秒。
```

英文原文：

```text
Hits cause bleeding for 4s, dealing <gray>(1 + <attack>attack damage at firing</attack> × 10%) = </gray>{bleed} damage every 2s, and slow the target for 2s.
```

目标语言（es_mx）：

```text
Los impactos causan hemorragia durante 4 s, con <gray>(1 + <attack>daño de ataque al disparar</attack> × 10 %) = </gray>{bleed} de daño cada 2 s, y ralentizan al objetivo durante 2 s.
```

中文直译回译：命中造成4秒流血，每2秒造成（1＋发射时攻击力×10%）＝{bleed}伤害，并使目标减速2秒。

自然中文释义：流血使用箭矢发射时的攻击力快照，持续4秒、每2秒结算；减速独立持续2秒。

术语对应：流血 → hemorragia；发射时攻击力 → daño de ataque al disparar；每2秒 → cada 2 s；减速 → ralentizan。

审校结论：通过。

风险：长公式的攻击力名称较长，须实机确认；发射时点及两种持续时间准确。

需要作者决定：无需作者决定。

#### 6. `egyptian.cone_arrow.full.1`

键：`gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
忽略<gray>（35%＋<level>等级</level>×0.5%）＝</gray>{penetration}%护甲，以<gray>（25%＋<level>等级</level>×0.5%）＝</gray>{pierce}%的概率贯穿，最多命中2个目标。
```

英文原文：

```text
Ignore <gray>(35% + <level>level</level> × 0.5%) = </gray>{penetration}% of armor, with a <gray>(25% + <level>level</level> × 0.5%) = </gray>{pierce}% chance to pierce. Hits up to 2 targets.
```

目标语言（es_mx）：

```text
Ignora un <gray>(35 % + <level>nivel</level> × 0,5 %) = </gray>{penetration} % de la armadura y tiene un <gray>(25 % + <level>nivel</level> × 0,5 %) = </gray>{pierce} % de probabilidad de atravesar al objetivo. Alcanza hasta 2 objetivos.
```

中文直译回译：忽略护甲的（35%＋等级×0.5%）＝{penetration}%，并有（25%＋等级×0.5%）＝{pierce}%的概率贯穿目标。最多命中2个目标。

自然中文释义：穿甲比例与贯穿概率是不同数值，各自先写公式再写结果；总命中上限仍是2个目标。

术语对应：护甲 → armadura；等级 → nivel；概率 → probabilidad；贯穿 → atravesar；最多2个目标 → hasta 2 objetivos。

审校结论：通过。

风险：双公式使正文较长，须实机确认；未把穿甲与贯穿概率混为一项。

需要作者决定：无需作者决定。

#### 7. `guandao.combo.simple.1`

键：`gui.echo_warrior.summoner.skill.guandao.combo.simple.1`\
使用位置：召唤器关刀连段技能简版提示。

中文原文：

```text
施展四段连击。前三段使敌人小幅浮空并施加短暂控制效果，冷却12秒。受到攻击时，该技能冷却缩短0.5秒。
```

英文原文：

```text
Perform a four-hit combo. The first three hits lift enemies slightly and briefly hinder them. Cooldown: 12s. Taking a hit reduces this skill's cooldown by 0.5s.
```

目标语言（es_mx）：

```text
Ejecuta cuatro golpes. Los tres primeros elevan ligeramente a los enemigos y les aplican un breve efecto de control. Recarga: 12 s. Recibir un golpe reduce la recarga de esta habilidad en 0,5 s.
```

中文直译回译：施展四次打击。前三次将敌人轻微抬起，并施加短暂的控制效果。冷却12秒。受到一次打击会使此技能的冷却减少0.5秒。

自然中文释义：四段连击的前三段带轻微浮空和短暂控制；受击可减冷却。简版没有重复展开版的80%与2秒细节。

术语对应：四段连击 → cuatro golpes；小幅浮空 → elevan ligeramente；短暂控制效果 → un breve efecto de control；技能 → habilidad；冷却 → recarga。

审校结论：通过。

风险：efecto de control 是一般控制效果，不声明眩晕、定身、缴械或精神操控；未引入原文没有的具体控制类型。长简版仍待视觉验收。

需要作者决定：无需作者决定。

#### 8. `guandao.combo.full.1`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.1`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
施展四段连击。前三段使敌人小幅浮空，命中单位近战攻击距离降低80%，持续2秒。冷却12秒。
```

英文原文：

```text
Perform a four-hit combo. The first three hits lift enemies slightly and reduce their melee reach by 80% for 2s. Cooldown: 12s.
```

目标语言（es_mx）：

```text
Ejecuta cuatro golpes. Los tres primeros elevan ligeramente a los enemigos y reducen su alcance cuerpo a cuerpo un 80 % durante 2 s. Recarga: 12 s.
```

中文直译回译：施展四次打击。前三次将敌人轻微抬起，并使其近战攻击距离降低80%，持续2秒。冷却12秒。

自然中文释义：降距对象是被命中的敌人；减少80%而非减少到80%，持续2秒。

术语对应：近战攻击距离 → alcance cuerpo a cuerpo；降低80% → reducen … un 80 %；轻微浮空 → elevan ligeramente；冷却 → Recarga。

审校结论：通过。

风险：代词 su 指向前句的敌人，未写成英灵自身降距；须实机确认全文换行。

需要作者决定：无需作者决定。

#### 9. `guandao.combo.full.2`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.2`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-第1、3段各造成（<attack>攻击力</attack>×70%）＝{hit1}点伤害。</gray>
```

英文原文：

```text
<gray>-Hits 1 and 3 each deal (<attack>attack damage</attack> × 70%) = {hit1} damage.</gray>
```

目标语言（es_mx）：

```text
<gray>-Los golpes 1 y 3 infligen cada uno (<attack>daño de ataque</attack> × 70 %) = {hit1} de daño.</gray>
```

中文直译回译：第1段和第3段各造成（攻击力×70%）＝{hit1}伤害。

自然中文释义：第1、3段分别结算同一70%攻击力公式，不是两段合计70%。

术语对应：各 → cada uno；第1、3段 → golpes 1 y 3；攻击力 → daño de ataque。

审校结论：通过。

风险：无未解决语言风险；各段数值、标签与占位符正确。整行灰色及红色攻击力仍待实机验证。

需要作者决定：无需作者决定。

#### 10. `guandao.combo.full.3`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.3`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-第2段造成（<attack>攻击力</attack>×100%）＝{hit2}点伤害。</gray>
```

英文原文：

```text
<gray>-Hit 2 deals (<attack>attack damage</attack> × 100%) = {hit2} damage.</gray>
```

目标语言（es_mx）：

```text
<gray>-El golpe 2 inflige (<attack>daño de ataque</attack> × 100 %) = {hit2} de daño.</gray>
```

中文直译回译：第2段造成（攻击力×100%）＝{hit2}伤害。

自然中文释义：第二段单独列出100%攻击力及其当前计算值。

术语对应：第2段 → golpe 2；造成伤害 → inflige … de daño；攻击力 → daño de ataque。

审校结论：通过。

风险：无未解决语言风险；未夹带越战越勇或被动加成说明。显示仍待实机验证。

需要作者决定：无需作者决定。

#### 11. `guandao.combo.full.4`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.4`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-第4段造成（<attack>攻击力</attack>×180%）＝{hit4}点伤害。</gray>
```

英文原文：

```text
<gray>-Hit 4 deals (<attack>attack damage</attack> × 180%) = {hit4} damage.</gray>
```

目标语言（es_mx）：

```text
<gray>-El golpe 4 inflige (<attack>daño de ataque</attack> × 180 %) = {hit4} de daño.</gray>
```

中文直译回译：第4段造成（攻击力×180%）＝{hit4}伤害。

自然中文释义：第四段单独列出180%攻击力及其当前计算值。

术语对应：第4段 → golpe 4；造成伤害 → inflige … de daño；攻击力 → daño de ataque。

审校结论：通过。

风险：无未解决语言风险；未夹带越战越勇或被动加成说明。显示仍待实机验证。

需要作者决定：无需作者决定。

#### 12. `guandao.combo.full.5`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.5`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-前三段命中刷新减益。</gray>
```

英文原文：

```text
<gray>-The first three hits refresh the debuff.</gray>
```

目标语言（es_mx）：

```text
<gray>-Cada uno de los tres primeros golpes renueva el efecto perjudicial.</gray>
```

中文直译回译：前三次打击中的每一次都刷新有害效果。

自然中文释义：前三段命中分别刷新同一个减益效果，不是叠加新效果或只在第三段刷新。

术语对应：每次 → cada uno；前三段命中 → tres primeros golpes；刷新 → renueva；减益 → efecto perjudicial。

审校结论：通过。

风险：无未解决语言风险；简短补充，仍未取得本轮实机视觉证据。

需要作者决定：无需作者决定。

#### 13. `guandao.combo.full.6`

键：`gui.echo_warrior.summoner.skill.guandao.combo.full.6`\
使用位置：召唤器技能展开提示（按住Alt）。

中文原文：

```text
<gray>-受近战或投射物伤害时，冷却缩短0.5秒。</gray>
```

英文原文：

```text
<gray>-Taking melee or projectile damage reduces the cooldown by 0.5s.</gray>
```

目标语言（es_mx）：

```text
<gray>-Recibir daño cuerpo a cuerpo o de proyectiles reduce la recarga en 0,5 s.</gray>
```

中文直译回译：受到近战或投射物伤害，会使冷却减少0.5秒。

自然中文释义：受伤来源限定为近战或投射物；每次触发减去0.5秒，不是把冷却设置成0.5秒。

术语对应：近战伤害 → daño cuerpo a cuerpo；投射物 → proyectiles；冷却缩短 → reduce la recarga；0.5秒 → 0,5 s。

审校结论：通过。

风险：无未解决语言风险；来源限制及减少量正确，仍未取得本轮实机视觉证据。

需要作者决定：无需作者决定。
