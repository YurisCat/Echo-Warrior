# 技能公式与关刀说明独立审校：俄语／巴西葡语（2026-11-13）

后续集成验收：本批已完成八加载器客户端及26.1.2／1.21.1代表性视觉复核，语言基线已更新，发布级门禁0错误、0待处理；见[实施与验收记录](../research/skill-formula-display-2026-11-13.md)。以下独立审校过程保留当时的职责边界。

审校对象：`build/skill-formula-update-2026-11-13/candidate-ru-pt.json`，每种语言13键，共26条。双源及已有语言界面上下文来自同目录 `translation-packet.json`。审校者独立读取了双源、候选、`REVIEW_WORKFLOW.md`、`TERMINOLOGY.md` 及 `TARGET_TERMS_CORE.md`，未读取译者解释。锥锋箭英文另外核对了主线 `common/src/main/resources/assets/echo_warrior/lang/en_us.json` 的本轮修订（`Hits up to 2 targets`），两种候选均已用独立句明确总共最多命中2个目标。

**结论：俄语13条通过；巴西葡语13条通过（1处简洁性建议已落实）。必须修改问题：0；未解决必须修改：0；需要作者决定的中文语义问题：0。**

本报告仅证明本批文本与结构审校完成。未联网、未构建、未运行游戏，未取得视觉验收证据，也未标记任何语言为 current。两版本客户端的悬浮框宽高、换行、公式着色和结果可读性仍待后续验收。

## 审校修改

- `pt_br` / `gui.echo_warrior.summoner.skill.guandao.combo.simple.1`：将 `atrapalham suas ações por um breve período` 缩短为 `atrapalham brevemente suas ações`，含义仍为“短暂妨碍行动”。这是简洁性建议，非机制纠错；未引入眩晕、定身、缴械或新状态名称。
- `ru_ru`：无修改。

## 本地结构核对

- JSON可解析；两种候选语言各13键，与双源本批键集完全一致。
- 每键花括号／printf占位符及其出现次数与英文双源一致；语义标记序列完全一致。
- 两种语言各12项动态结果的公式都直接放在相应结果前；去除语言标签文字、空格并统一小数逗号后，公式运算符、常数和变量类型与英文双源完全一致。
- 全部展开文本无星号；未发现异常控制字符。关刀条目无“越战越勇”加成声明，无眩晕、定身或缴械承诺。
- 本检查针对候选文件，不代替合并后的全语言、兼容线、别名同步或发布级门禁。

## 逐键审阅卡

下面的“通过”均指语言与候选结构审校；所有条目的实际UI排版仍待验收。中文回译用于确认机制含义，不强求复制外语语序。

### 俄语（ru_ru）

#### 1. `gui.echo_warrior.summoner.skill.roman.endures.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`举盾嘲讽5秒，护甲增加<gray>（12＋<level>等级</level>×0.1）＝</gray>{armor}点。结束时对周围敌人造成<gray>（100%＋<level>等级</level>×1%）＝</gray>{release}%的累计伤害。冷却20秒。`

英文原文：`Raise the shield and taunt for 5s, gaining <gray>(12 + <level>level</level> × 0.1) = </gray>{armor} armor. At the end, deal <gray>(100% + <level>level</level> × 1%) = </gray>{release}% of accumulated damage to nearby enemies. Cooldown: 20s.`

目标语言：`Поднимает щит и провоцирует врагов на 5 с, получая <gray>(12 + <level>уровень</level> × 0,1) = </gray>{armor} брони. Затем наносит ближайшим врагам <gray>(100% + <level>уровень</level> × 1%) = </gray>{release}% накопленного урона. Перезарядка: 20 с.`

中文直译回译：举盾并嘲讽敌人5秒，获得（12＋等级×0.1）＝{armor}护甲。随后对附近敌人造成（100%＋等级×1%）＝{release}%的累计伤害。冷却20秒。

自然中文释义：举盾嘲讽期间增加护甲，结束后按等级计算的比例向附近敌人释放累计伤害；持续与冷却分别为5秒和20秒。

术语对应：护甲 → броня（正文 брони）；等级 → уровень；冷却 → перезарядка；累计伤害 → накопленный урон。

审校结论：通过。

风险：无语义风险；同一段含两项公式，是本批最长文本之一，待检查长悬浮框换行。

需要作者决定：无需作者决定。

#### 2. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.2`

使用位置：召唤器技能Alt展开提示。

中文原文：`直接命中时，每种自身增益与目标减益追加<gray>（1＋<level>等级</level>÷100）＝</gray>{curse}点魔法伤害。`

英文原文：`Direct hits deal <gray>(1 + <level>level</level> ÷ 100) = </gray>{curse} extra magic damage per buff on the Echo and debuff on the target.`

目标语言：`Прямые попадания наносят <gray>(1 + <level>уровень</level> ÷ 100) = </gray>{curse} дополнительного магического урона за каждый положительный эффект на этом отголоске и каждый отрицательный эффект на цели.`

中文直译回译：直接命中时，英灵身上的每种正面效果和目标身上的每种负面效果，各追加（1＋等级÷100）＝{curse}魔法伤害。

自然中文释义：每种自身增益和每种目标减益分别计入直接命中的附加魔法伤害，没有把条件改成两者同时存在。

术语对应：英灵 → отголосок（正文 отголоске）；增益 → положительный эффект；减益 → отрицательный эффект；魔法伤害 → магический урон。

审校结论：通过。

风险：无语义风险；俄语的效果归属说明较长，待检查换行。

需要作者决定：无需作者决定。

#### 3. `gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`攻击有概率造成流血，每秒<gray>（1＋<attack>攻击力</attack>×10%）＝</gray>{bleed}点伤害，持续6秒。`

英文原文：`Attacks may cause bleeding for 6s, dealing <gray>(1 + <attack>attack damage</attack> × 10%) = </gray>{bleed} damage each second.`

目标语言：`Атаки могут вызвать кровотечение: <gray>(1 + <attack>урон при атаке</attack> × 10%) = </gray>{bleed} урона каждую секунду в течение 6 с.`

中文直译回译：攻击可能造成流血，每秒造成（1＋攻击力×10%）＝{bleed}伤害，持续6秒。

自然中文释义：攻击有概率触发6秒流血，每秒伤害按攻击力计算；未改为必定触发。

术语对应：攻击力 → урон при атаке；流血 → кровотечение；每秒 → каждую секунду。

审校结论：通过。

风险：无新增语义或文化风险；公式分段颜色与换行待视觉验收。

需要作者决定：无需作者决定。

#### 4. `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.3`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-次目标伤害（<attack>攻击力</attack>×50%）＝{half}点，强化后（<attack>攻击力</attack>×75%）＝{threequarters}点。</gray>`

英文原文：`<gray>-Secondary targets take (<attack>attack damage</attack> × 50%) = {half} damage, or (<attack>attack damage</attack> × 75%) = {threequarters} while empowered.</gray>`

目标语言：`<gray>-Урон второстепенным целям: (<attack>урон при атаке</attack> × 50%) = {half}; при усилении — (<attack>урон при атаке</attack> × 75%) = {threequarters}.</gray>`

中文直译回译：次要目标伤害：（攻击力×50%）＝{half}；强化期间为（攻击力×75%）＝{threequarters}。

自然中文释义：次要目标平时承受50%攻击力的伤害，强化时承受75%；每项公式直接对应自己的结果。

术语对应：次要目标 → второстепенные цели；攻击力 → урон при атаке；强化时 → при усилении。

审校结论：通过。

风险：无新增语义风险；整行灰色、攻击力项红色的嵌套渲染待视觉验收。

需要作者决定：无需作者决定。

#### 5. `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`命中后流血4秒，每2秒造成<gray>（1＋<attack>发射时攻击力</attack>×10%）＝</gray>{bleed}点伤害，并减速2秒。`

英文原文：`Hits cause bleeding for 4s, dealing <gray>(1 + <attack>attack damage at firing</attack> × 10%) = </gray>{bleed} damage every 2s, and slow the target for 2s.`

目标语言：`Попадания вызывают кровотечение на 4 с, наносящее <gray>(1 + <attack>урон при атаке в момент выстрела</attack> × 10%) = </gray>{bleed} урона каждые 2 с, и замедляют цель на 2 с.`

中文直译回译：命中造成4秒流血，每2秒造成（1＋发射时攻击力×10%）＝{bleed}伤害，并使目标减速2秒。

自然中文释义：流血采用箭矢发射时的攻击力快照，4秒内每2秒结算；减速另持续2秒。

术语对应：发射时攻击力 → урон при атаке в момент выстрела；流血 → кровотечение；减速 → замедлять。

审校结论：通过。

风险：无语义风险；俄语攻击力快照词组较长，待检查公式跨行与着色。

需要作者决定：无需作者决定。

#### 6. `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`忽略<gray>（35%＋<level>等级</level>×0.5%）＝</gray>{penetration}%护甲，以<gray>（25%＋<level>等级</level>×0.5%）＝</gray>{pierce}%的概率贯穿，最多命中2个目标。`

英文原文（已核对本轮修订源）：`Ignore <gray>(35% + <level>level</level> × 0.5%) = </gray>{penetration}% of armor, with a <gray>(25% + <level>level</level> × 0.5%) = </gray>{pierce}% chance to pierce. Hits up to 2 targets.`

目标语言：`Игнорирует <gray>(35% + <level>уровень</level> × 0,5%) = </gray>{penetration}% брони, с шансом <gray>(25% + <level>уровень</level> × 0,5%) = </gray>{pierce}% пробивает цель насквозь. Поражает до 2 целей.`

中文直译回译：忽略（35%＋等级×0.5%）＝{penetration}%护甲，以（25%＋等级×0.5%）＝{pierce}%的概率贯穿目标。最多命中2个目标。

自然中文释义：穿甲比例和贯穿概率分别计算，最多命中2个目标；不是必定贯穿。

术语对应：护甲 → броня；等级 → уровень；概率 → шанс；贯穿 → пробивать насквозь。

审校结论：通过。

风险：无语义风险；含两项百分比公式，待检查相邻结果的视觉对应。

需要作者决定：无需作者决定。

#### 7. `gui.echo_warrior.summoner.skill.guandao.combo.simple.1`

使用位置：召唤器技能简版提示。

中文原文：`施展四段连击。前三段使敌人小幅浮空并施加短暂控制效果，冷却12秒。受到攻击时，该技能冷却缩短0.5秒。`

英文原文：`Perform a four-hit combo. The first three hits lift enemies slightly and briefly hinder them. Cooldown: 12s. Taking a hit reduces this skill's cooldown by 0.5s.`

目标语言：`Наносит четыре удара. Первые три слегка подбрасывают врагов и ненадолго затрудняют их действия. Перезарядка: 12 с. Получение удара сокращает перезарядку этого навыка на 0,5 с.`

中文直译回译：施展四次打击。前三次轻微抛起敌人，并短暂妨碍其行动。冷却12秒。受到打击使本技能冷却缩短0.5秒。

自然中文释义：四段连击的前三段有轻微浮空与短暂控制；受击缩短冷却。简版不展开80%近战距离缩减及其2秒时长。

术语对应：短暂妨碍行动 → ненадолго затруднять действия；技能 → навык；冷却 → перезарядка；受击 → получение удара。

审校结论：通过。

风险：未承诺眩晕、定身或缴械；简版字数仍较多，待检查悬浮框高度。

需要作者决定：无需作者决定。

#### 8. `gui.echo_warrior.summoner.skill.guandao.combo.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`施展四段连击。前三段使敌人小幅浮空，命中单位近战攻击距离降低80%，持续2秒。冷却12秒。`

英文原文：`Perform a four-hit combo. The first three hits lift enemies slightly and reduce their melee reach by 80% for 2s. Cooldown: 12s.`

目标语言：`Наносит четыре удара. Первые три слегка подбрасывают врагов и снижают дальность их атак в ближнем бою на 80% на 2 с. Перезарядка: 12 с.`

中文直译回译：施展四次打击。前三次轻微抛起敌人，并将他们的近战攻击距离降低80%，持续2秒。冷却12秒。

自然中文释义：展开版明确控制效果是降低近战攻击距离80%、持续2秒，同时保留轻微浮空与12秒冷却。

术语对应：近战攻击距离 → дальность атак в ближнем бою；轻微抛起 → слегка подбрасывать；冷却 → перезарядка。

审校结论：通过。

风险：无语义风险；降低的是近战攻击距离，不是攻击力、移动速度或完全禁止攻击。

需要作者决定：无需作者决定。

#### 9. `gui.echo_warrior.summoner.skill.guandao.combo.full.2`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-第1、3段各造成（<attack>攻击力</attack>×70%）＝{hit1}点伤害。</gray>`

英文原文：`<gray>-Hits 1 and 3 each deal (<attack>attack damage</attack> × 70%) = {hit1} damage.</gray>`

目标语言：`<gray>-Удары 1 и 3 наносят по (<attack>урон при атаке</attack> × 70%) = {hit1} урона.</gray>`

中文直译回译：第1和第3段打击各造成（攻击力×70%）＝{hit1}伤害。

自然中文释义：第1、3段分别造成相同的70%攻击力伤害，不把两段合并为一次总伤害。

术语对应：第1和第3段 → удары 1 и 3；各 → по；攻击力 → урон при атаке。

审校结论：通过。

风险：无新增语义风险；无“越战越勇”加成声明，{hit1}仅保留一次，符合双源。

需要作者决定：无需作者决定。

#### 10. `gui.echo_warrior.summoner.skill.guandao.combo.full.3`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-第2段造成（<attack>攻击力</attack>×100%）＝{hit2}点伤害。</gray>`

英文原文：`<gray>-Hit 2 deals (<attack>attack damage</attack> × 100%) = {hit2} damage.</gray>`

目标语言：`<gray>-Удар 2 наносит (<attack>урон при атаке</attack> × 100%) = {hit2} урона.</gray>`

中文直译回译：第2段打击造成（攻击力×100%）＝{hit2}伤害。

自然中文释义：第2段伤害是100%攻击力，使用独立的{hit2}结果。

术语对应：第2段 → удар 2；攻击力 → урон при атаке；伤害 → урон。

审校结论：通过。

风险：无新增语义风险；未混入其他段数、占位符或“越战越勇”声明。

需要作者决定：无需作者决定。

#### 11. `gui.echo_warrior.summoner.skill.guandao.combo.full.4`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-第4段造成（<attack>攻击力</attack>×180%）＝{hit4}点伤害。</gray>`

英文原文：`<gray>-Hit 4 deals (<attack>attack damage</attack> × 180%) = {hit4} damage.</gray>`

目标语言：`<gray>-Удар 4 наносит (<attack>урон при атаке</attack> × 180%) = {hit4} урона.</gray>`

中文直译回译：第4段打击造成（攻击力×180%）＝{hit4}伤害。

自然中文释义：第4段伤害是180%攻击力，使用独立的{hit4}结果。

术语对应：第4段 → удар 4；攻击力 → урон при атаке；伤害 → урон。

审校结论：通过。

风险：无新增语义风险；未混入其他段数、占位符或“越战越勇”声明。

需要作者决定：无需作者决定。

#### 12. `gui.echo_warrior.summoner.skill.guandao.combo.full.5`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-前三段命中刷新减益。</gray>`

英文原文：`<gray>-The first three hits refresh the debuff.</gray>`

目标语言：`<gray>-Каждое из первых трёх попаданий обновляет отрицательный эффект.</gray>`

中文直译回译：前三段每次命中都会刷新负面效果。

自然中文释义：前三段命中均刷新既有减益，不暗示叠加，也未将第四段加入刷新范围。

术语对应：前三段命中 → первые три попадания；刷新 → обновлять；减益 → отрицательный эффект。

审校结论：通过。

风险：无新增语义或文化风险；本行没有数值公式和结果占位符。

需要作者决定：无需作者决定。

#### 13. `gui.echo_warrior.summoner.skill.guandao.combo.full.6`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-受近战或投射物伤害时，冷却缩短0.5秒。</gray>`

英文原文：`<gray>-Taking melee or projectile damage reduces the cooldown by 0.5s.</gray>`

目标语言：`<gray>-Получение урона в ближнем бою или от снарядов сокращает перезарядку на 0,5 с.</gray>`

中文直译回译：受到近战或投射物伤害会使冷却缩短0.5秒。

自然中文释义：受近战或投射物伤害时减少0.5秒冷却；没有扩展到所有环境伤害。

术语对应：近战伤害 → урон в ближнем бою；投射物 → снаряды；冷却 → перезарядка。

审校结论：通过。

风险：无新增语义或文化风险；保留本地小数逗号0,5，数值仍为0.5秒。

需要作者决定：无需作者决定。

### 巴西葡语（pt_br）

#### 1. `gui.echo_warrior.summoner.skill.roman.endures.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`举盾嘲讽5秒，护甲增加<gray>（12＋<level>等级</level>×0.1）＝</gray>{armor}点。结束时对周围敌人造成<gray>（100%＋<level>等级</level>×1%）＝</gray>{release}%的累计伤害。冷却20秒。`

英文原文：`Raise the shield and taunt for 5s, gaining <gray>(12 + <level>level</level> × 0.1) = </gray>{armor} armor. At the end, deal <gray>(100% + <level>level</level> × 1%) = </gray>{release}% of accumulated damage to nearby enemies. Cooldown: 20s.`

目标语言：`Ergue o escudo e provoca os inimigos por 5 s, ganhando <gray>(12 + <level>nível</level> × 0,1) = </gray>{armor} de armadura. Depois, causa <gray>(100% + <level>nível</level> × 1%) = </gray>{release}% do dano acumulado aos inimigos próximos. Recarga: 20 s.`

中文直译回译：举盾并嘲讽敌人5秒，获得（12＋等级×0.1）＝{armor}护甲。随后对附近敌人造成（100%＋等级×1%）＝{release}%的累计伤害。冷却20秒。

自然中文释义：举盾嘲讽期间增加护甲，结束后按等级计算的比例向附近敌人释放累计伤害；持续与冷却分别为5秒和20秒。

术语对应：护甲 → armadura；等级 → nível；冷却 → recarga；累计伤害 → dano acumulado。

审校结论：通过。

风险：无语义风险；同一段含两项公式，是本批最长文本之一，待检查长悬浮框换行。

需要作者决定：无需作者决定。

#### 2. `gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse.full.2`

使用位置：召唤器技能Alt展开提示。

中文原文：`直接命中时，每种自身增益与目标减益追加<gray>（1＋<level>等级</level>÷100）＝</gray>{curse}点魔法伤害。`

英文原文：`Direct hits deal <gray>(1 + <level>level</level> ÷ 100) = </gray>{curse} extra magic damage per buff on the Echo and debuff on the target.`

目标语言：`Acertos diretos causam <gray>(1 + <level>nível</level> ÷ 100) = </gray>{curse} de dano mágico adicional por cada efeito benéfico neste Eco e cada efeito nocivo no alvo.`

中文直译回译：直接命中时，英灵身上的每种正面效果和目标身上的每种负面效果，各追加（1＋等级÷100）＝{curse}魔法伤害。

自然中文释义：每种自身增益和每种目标减益分别计入直接命中的附加魔法伤害，没有把条件改成两者同时存在。

术语对应：英灵 → Eco；增益 → efeito benéfico；减益 → efeito nocivo；魔法伤害 → dano mágico。

审校结论：通过。

风险：无语义风险；俄语的效果归属说明较长，待检查换行。

需要作者决定：无需作者决定。

#### 3. `gui.echo_warrior.summoner.skill.aztec.obsidian_wound.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`攻击有概率造成流血，每秒<gray>（1＋<attack>攻击力</attack>×10%）＝</gray>{bleed}点伤害，持续6秒。`

英文原文：`Attacks may cause bleeding for 6s, dealing <gray>(1 + <attack>attack damage</attack> × 10%) = </gray>{bleed} damage each second.`

目标语言：`Ataques podem causar sangramento, com <gray>(1 + <attack>dano de ataque</attack> × 10%) = </gray>{bleed} de dano a cada segundo por 6 s.`

中文直译回译：攻击可能造成流血，每秒造成（1＋攻击力×10%）＝{bleed}伤害，持续6秒。

自然中文释义：攻击有概率触发6秒流血，每秒伤害按攻击力计算；未改为必定触发。

术语对应：攻击力 → dano de ataque；流血 → sangramento；每秒 → a cada segundo。

审校结论：通过。

风险：无新增语义或文化风险；公式分段颜色与换行待视觉验收。

需要作者决定：无需作者决定。

#### 4. `gui.echo_warrior.summoner.skill.aztec.macuahuitl.full.3`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-次目标伤害（<attack>攻击力</attack>×50%）＝{half}点，强化后（<attack>攻击力</attack>×75%）＝{threequarters}点。</gray>`

英文原文：`<gray>-Secondary targets take (<attack>attack damage</attack> × 50%) = {half} damage, or (<attack>attack damage</attack> × 75%) = {threequarters} while empowered.</gray>`

目标语言：`<gray>-Dano aos alvos secundários: (<attack>dano de ataque</attack> × 50%) = {half}, ou (<attack>dano de ataque</attack> × 75%) = {threequarters} enquanto fortalecido.</gray>`

中文直译回译：次要目标伤害：（攻击力×50%）＝{half}；强化期间为（攻击力×75%）＝{threequarters}。

自然中文释义：次要目标平时承受50%攻击力的伤害，强化时承受75%；每项公式直接对应自己的结果。

术语对应：次要目标 → alvos secundários；攻击力 → dano de ataque；强化期间 → enquanto fortalecido。

审校结论：通过。

风险：无新增语义风险；整行灰色、攻击力项红色的嵌套渲染待视觉验收。

需要作者决定：无需作者决定。

#### 5. `gui.echo_warrior.summoner.skill.egyptian.leaf_arrow.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`命中后流血4秒，每2秒造成<gray>（1＋<attack>发射时攻击力</attack>×10%）＝</gray>{bleed}点伤害，并减速2秒。`

英文原文：`Hits cause bleeding for 4s, dealing <gray>(1 + <attack>attack damage at firing</attack> × 10%) = </gray>{bleed} damage every 2s, and slow the target for 2s.`

目标语言：`Acertos causam sangramento por 4 s, com <gray>(1 + <attack>dano de ataque no disparo</attack> × 10%) = </gray>{bleed} de dano a cada 2 s, e reduzem a velocidade do alvo por 2 s.`

中文直译回译：命中造成4秒流血，每2秒造成（1＋发射时攻击力×10%）＝{bleed}伤害，并使目标减速2秒。

自然中文释义：流血采用箭矢发射时的攻击力快照，4秒内每2秒结算；减速另持续2秒。

术语对应：发射时攻击力 → dano de ataque no disparo；流血 → sangramento；减速 → reduzir a velocidade。

审校结论：通过。

风险：无语义风险；俄语攻击力快照词组较长，待检查公式跨行与着色。

需要作者决定：无需作者决定。

#### 6. `gui.echo_warrior.summoner.skill.egyptian.cone_arrow.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`忽略<gray>（35%＋<level>等级</level>×0.5%）＝</gray>{penetration}%护甲，以<gray>（25%＋<level>等级</level>×0.5%）＝</gray>{pierce}%的概率贯穿，最多命中2个目标。`

英文原文（已核对本轮修订源）：`Ignore <gray>(35% + <level>level</level> × 0.5%) = </gray>{penetration}% of armor, with a <gray>(25% + <level>level</level> × 0.5%) = </gray>{pierce}% chance to pierce. Hits up to 2 targets.`

目标语言：`Ignora <gray>(35% + <level>nível</level> × 0,5%) = </gray>{penetration}% da armadura, com <gray>(25% + <level>nível</level> × 0,5%) = </gray>{pierce}% de chance de atravessar o alvo. Atinge até 2 alvos.`

中文直译回译：忽略（35%＋等级×0.5%）＝{penetration}%护甲，以（25%＋等级×0.5%）＝{pierce}%的概率贯穿目标。最多命中2个目标。

自然中文释义：穿甲比例和贯穿概率分别计算，最多命中2个目标；不是必定贯穿。

术语对应：护甲 → armadura；等级 → nível；概率 → chance；贯穿 → atravessar。

审校结论：通过。

风险：无语义风险；含两项百分比公式，待检查相邻结果的视觉对应。

需要作者决定：无需作者决定。

#### 7. `gui.echo_warrior.summoner.skill.guandao.combo.simple.1`

使用位置：召唤器技能简版提示。

中文原文：`施展四段连击。前三段使敌人小幅浮空并施加短暂控制效果，冷却12秒。受到攻击时，该技能冷却缩短0.5秒。`

英文原文：`Perform a four-hit combo. The first three hits lift enemies slightly and briefly hinder them. Cooldown: 12s. Taking a hit reduces this skill's cooldown by 0.5s.`

目标语言：`Executa quatro golpes. Os três primeiros erguem levemente os inimigos e atrapalham brevemente suas ações. Recarga: 12 s. Receber um golpe reduz a recarga desta habilidade em 0,5 s.`

中文直译回译：施展四次打击。前三次轻微抛起敌人，并短暂妨碍其行动。冷却12秒。受到打击使本技能冷却缩短0.5秒。

自然中文释义：四段连击的前三段有轻微浮空与短暂控制；受击缩短冷却。简版不展开80%近战距离缩减及其2秒时长。

术语对应：短暂妨碍行动 → atrapalhar brevemente as ações；技能 → habilidade；冷却 → recarga；受击 → receber um golpe。

审校结论：通过（简洁性建议已落实）。

风险：未承诺眩晕、定身或缴械；简版字数仍较多，待检查悬浮框高度。

需要作者决定：无需作者决定。

#### 8. `gui.echo_warrior.summoner.skill.guandao.combo.full.1`

使用位置：召唤器技能Alt展开提示。

中文原文：`施展四段连击。前三段使敌人小幅浮空，命中单位近战攻击距离降低80%，持续2秒。冷却12秒。`

英文原文：`Perform a four-hit combo. The first three hits lift enemies slightly and reduce their melee reach by 80% for 2s. Cooldown: 12s.`

目标语言：`Executa quatro golpes. Os três primeiros erguem levemente os inimigos e reduzem o alcance dos ataques corpo a corpo deles em 80% por 2 s. Recarga: 12 s.`

中文直译回译：施展四次打击。前三次轻微抛起敌人，并将他们的近战攻击距离降低80%，持续2秒。冷却12秒。

自然中文释义：展开版明确控制效果是降低近战攻击距离80%、持续2秒，同时保留轻微浮空与12秒冷却。

术语对应：近战攻击距离 → alcance dos ataques corpo a corpo；轻微抛起 → erguer levemente；冷却 → recarga。

审校结论：通过。

风险：无语义风险；降低的是近战攻击距离，不是攻击力、移动速度或完全禁止攻击。

需要作者决定：无需作者决定。

#### 9. `gui.echo_warrior.summoner.skill.guandao.combo.full.2`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-第1、3段各造成（<attack>攻击力</attack>×70%）＝{hit1}点伤害。</gray>`

英文原文：`<gray>-Hits 1 and 3 each deal (<attack>attack damage</attack> × 70%) = {hit1} damage.</gray>`

目标语言：`<gray>-Os golpes 1 e 3 causam (<attack>dano de ataque</attack> × 70%) = {hit1} de dano cada.</gray>`

中文直译回译：第1和第3段打击各造成（攻击力×70%）＝{hit1}伤害。

自然中文释义：第1、3段分别造成相同的70%攻击力伤害，不把两段合并为一次总伤害。

术语对应：第1和第3段 → golpes 1 e 3；各 → cada；攻击力 → dano de ataque。

审校结论：通过。

风险：无新增语义风险；无“越战越勇”加成声明，{hit1}仅保留一次，符合双源。

需要作者决定：无需作者决定。

#### 10. `gui.echo_warrior.summoner.skill.guandao.combo.full.3`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-第2段造成（<attack>攻击力</attack>×100%）＝{hit2}点伤害。</gray>`

英文原文：`<gray>-Hit 2 deals (<attack>attack damage</attack> × 100%) = {hit2} damage.</gray>`

目标语言：`<gray>-O golpe 2 causa (<attack>dano de ataque</attack> × 100%) = {hit2} de dano.</gray>`

中文直译回译：第2段打击造成（攻击力×100%）＝{hit2}伤害。

自然中文释义：第2段伤害是100%攻击力，使用独立的{hit2}结果。

术语对应：第2段 → golpe 2；攻击力 → dano de ataque；伤害 → dano。

审校结论：通过。

风险：无新增语义风险；未混入其他段数、占位符或“越战越勇”声明。

需要作者决定：无需作者决定。

#### 11. `gui.echo_warrior.summoner.skill.guandao.combo.full.4`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-第4段造成（<attack>攻击力</attack>×180%）＝{hit4}点伤害。</gray>`

英文原文：`<gray>-Hit 4 deals (<attack>attack damage</attack> × 180%) = {hit4} damage.</gray>`

目标语言：`<gray>-O golpe 4 causa (<attack>dano de ataque</attack> × 180%) = {hit4} de dano.</gray>`

中文直译回译：第4段打击造成（攻击力×180%）＝{hit4}伤害。

自然中文释义：第4段伤害是180%攻击力，使用独立的{hit4}结果。

术语对应：第4段 → golpe 4；攻击力 → dano de ataque；伤害 → dano。

审校结论：通过。

风险：无新增语义风险；未混入其他段数、占位符或“越战越勇”声明。

需要作者决定：无需作者决定。

#### 12. `gui.echo_warrior.summoner.skill.guandao.combo.full.5`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-前三段命中刷新减益。</gray>`

英文原文：`<gray>-The first three hits refresh the debuff.</gray>`

目标语言：`<gray>-Cada um dos três primeiros acertos renova o efeito nocivo.</gray>`

中文直译回译：前三段每次命中都会刷新负面效果。

自然中文释义：前三段命中均刷新既有减益，不暗示叠加，也未将第四段加入刷新范围。

术语对应：前三段命中 → três primeiros acertos；刷新 → renovar；减益 → efeito nocivo。

审校结论：通过。

风险：无新增语义或文化风险；本行没有数值公式和结果占位符。

需要作者决定：无需作者决定。

#### 13. `gui.echo_warrior.summoner.skill.guandao.combo.full.6`

使用位置：召唤器技能Alt展开提示。

中文原文：`<gray>-受近战或投射物伤害时，冷却缩短0.5秒。</gray>`

英文原文：`<gray>-Taking melee or projectile damage reduces the cooldown by 0.5s.</gray>`

目标语言：`<gray>-Receber dano corpo a corpo ou de projéteis reduz a recarga em 0,5 s.</gray>`

中文直译回译：受到近战或投射物伤害会使冷却缩短0.5秒。

自然中文释义：受近战或投射物伤害时减少0.5秒冷却；没有扩展到所有环境伤害。

术语对应：近战伤害 → dano corpo a corpo；投射物 → projéteis；冷却 → recarga。

审校结论：通过。

风险：无新增语义或文化风险；保留本地小数逗号0,5，数值仍为0.5秒。

需要作者决定：无需作者决定。
