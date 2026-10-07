# 0.2.4 新增及变化短文本独立审校

日期：2026-10-07。范围仅为下列 4 个键；其余已接受译文不重新标记为新翻译。

译者：独立子代理 `release_translation`。审校者：主代理，未参与本批目标译文写作；仅按双源、术语表、目标文本与界面位置审查。依照 `REVIEW_WORKFLOW.md` 分离角色，不以译者自查代替独立审校。

结论：6 个基准语言的语义、语法、术语和占位符通过；无待作者裁决的文化或玩法问题。繁中、日语与俄语保持既有 Echo 用语；TBF 作为第三方缩写保留，召唤器简称沿用各语言已有用词。香港繁中及 5 个拉美别名与基准逐字节相同。

## 本次验收范围

沿用 `GPT6_INDEPENDENT_REVIEW.md` 的 0.2.2 已接受字体及代表性界面验收。本次没有更改字体、按钮、书页或布局；4 个变化键均已逐语言独立审校。两个等级文本使用既有悬浮提示，动态参数从硬编码 30 改为当前等级与配置上限；两条 TBF 反馈使用原版动作栏。源代码调用点、参数数目、别名及两条资源线一致性均已核对。

本次以局部文本更新签收，接受尚未逐语言重做游戏内排版的剩余风险：极小窗口或很大的 GUI 缩放可能截断较长的 TBF 动作栏提示。没有漏译、过期译文或未决的语义问题；不宣称 24 条新文本均已游戏内截图验收，也不沿用旧版本对未来缺失翻译的豁免。本次发布不使用 `--allow-pending`。登记基线后仍执行完整发布门禁及六份最终 JAR 的语言内容审计。

## zh_tw

### `gui.echo_warrior.summoner.attribute.level`

- 使用位置：召唤器属性悬浮提示
- 中文原文：等级：%s/%s
- 英文原文：Level: %s/%s
- 目标语言：等級：%s/%s
- 中文直译回译／自然释义：等级：当前值/上限。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.experience.max`

- 使用位置：召唤器经验条满级悬浮提示
- 中文原文：等级%s · 已达到最高等级
- 英文原文：Level %s · Max level
- 目标语言：等級%s · 已達到最高等級
- 中文直译回译／自然释义：当前等级为指定数值，已达到最高等级。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.healing_disabled`

- 使用位置：TBF 治疗操作的动作栏反馈
- 中文原文：英灵使用自身的恢复机制，永恒伙伴治疗不适用。
- 英文原文：Echoes use their own recovery. TBF healing is unavailable.
- 目标语言：英靈使用自身的恢復機制，TBF 治療不適用。
- 中文直译回译／自然释义：英灵使用自身恢复机制，TBF 治疗不可用。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.unavailable`

- 使用位置：TBF 无法操作的动作栏反馈
- 中文原文：无法操作这名英灵：请检查遗物、控制权和召唤器状态。
- 英文原文：This Echo is unavailable. Check its relic, controller, and summoner.
- 目标语言：無法操作這名英靈：請檢查遺物、控制權和召喚器狀態。
- 中文直译回译／自然释义：这名英灵无法操作，请检查遗物、控制权限和召唤器。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

文件 SHA-256：`4440270fe8af27667e06dac6ca7a99c25b8d6a4f282caa6d088db99f7c0e7005`。

## ja_jp

### `gui.echo_warrior.summoner.attribute.level`

- 使用位置：召唤器属性悬浮提示
- 中文原文：等级：%s/%s
- 英文原文：Level: %s/%s
- 目标语言：レベル：%s/%s
- 中文直译回译／自然释义：等级：当前值/上限。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.experience.max`

- 使用位置：召唤器经验条满级悬浮提示
- 中文原文：等级%s · 已达到最高等级
- 英文原文：Level %s · Max level
- 目标语言：レベル%s · 最大レベル
- 中文直译回译／自然释义：当前等级为指定数值，已达到最高等级。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.healing_disabled`

- 使用位置：TBF 治疗操作的动作栏反馈
- 中文原文：英灵使用自身的恢复机制，永恒伙伴治疗不适用。
- 英文原文：Echoes use their own recovery. TBF healing is unavailable.
- 目标语言：エコーは独自の仕組みで回復するため、TBFの治療は利用できません。
- 中文直译回译／自然释义：英灵使用自身恢复机制，TBF 治疗不可用。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.unavailable`

- 使用位置：TBF 无法操作的动作栏反馈
- 中文原文：无法操作这名英灵：请检查遗物、控制权和召唤器状态。
- 英文原文：This Echo is unavailable. Check its relic, controller, and summoner.
- 目标语言：このエコーは操作できません。遺物、操作権限、召喚器の状態を確認してください。
- 中文直译回译／自然释义：这名英灵无法操作，请检查遗物、控制权限和召唤器。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

文件 SHA-256：`2c2da1ae2d9ec9f27c1dbf891de5d826a834bf94649531120e0bfb23527b35f9`。

## ru_ru

### `gui.echo_warrior.summoner.attribute.level`

- 使用位置：召唤器属性悬浮提示
- 中文原文：等级：%s/%s
- 英文原文：Level: %s/%s
- 目标语言：Уровень: %s/%s
- 中文直译回译／自然释义：等级：当前值/上限。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.experience.max`

- 使用位置：召唤器经验条满级悬浮提示
- 中文原文：等级%s · 已达到最高等级
- 英文原文：Level %s · Max level
- 目标语言：Уровень %s · Максимум
- 中文直译回译／自然释义：当前等级为指定数值，已达到最高等级。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.healing_disabled`

- 使用位置：TBF 治疗操作的动作栏反馈
- 中文原文：英灵使用自身的恢复机制，永恒伙伴治疗不适用。
- 英文原文：Echoes use their own recovery. TBF healing is unavailable.
- 目标语言：У отголосков своя система восстановления. Лечение через TBF недоступно.
- 中文直译回译／自然释义：英灵使用自身恢复机制，TBF 治疗不可用。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.unavailable`

- 使用位置：TBF 无法操作的动作栏反馈
- 中文原文：无法操作这名英灵：请检查遗物、控制权和召唤器状态。
- 英文原文：This Echo is unavailable. Check its relic, controller, and summoner.
- 目标语言：Нельзя управлять этим отголоском. Проверьте реликвию, права управления и устройство призыва.
- 中文直译回译／自然释义：这名英灵无法操作，请检查遗物、控制权限和召唤器。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

文件 SHA-256：`5dba44d72a02fb8c5d7d43b0bea6568c747749e92acbe5d1f1788a92c7827ce8`。

## pt_br

### `gui.echo_warrior.summoner.attribute.level`

- 使用位置：召唤器属性悬浮提示
- 中文原文：等级：%s/%s
- 英文原文：Level: %s/%s
- 目标语言：Nível: %s/%s
- 中文直译回译／自然释义：等级：当前值/上限。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.experience.max`

- 使用位置：召唤器经验条满级悬浮提示
- 中文原文：等级%s · 已达到最高等级
- 英文原文：Level %s · Max level
- 目标语言：Nível %s · Nível máximo
- 中文直译回译／自然释义：当前等级为指定数值，已达到最高等级。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.healing_disabled`

- 使用位置：TBF 治疗操作的动作栏反馈
- 中文原文：英灵使用自身的恢复机制，永恒伙伴治疗不适用。
- 英文原文：Echoes use their own recovery. TBF healing is unavailable.
- 目标语言：Ecos usam seu próprio sistema de recuperação. A cura do TBF não está disponível.
- 中文直译回译／自然释义：英灵使用自身恢复机制，TBF 治疗不可用。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.unavailable`

- 使用位置：TBF 无法操作的动作栏反馈
- 中文原文：无法操作这名英灵：请检查遗物、控制权和召唤器状态。
- 英文原文：This Echo is unavailable. Check its relic, controller, and summoner.
- 目标语言：Não é possível controlar este Eco. Verifique a relíquia, a permissão de controle e o Invocador.
- 中文直译回译／自然释义：这名英灵无法操作，请检查遗物、控制权限和召唤器。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

文件 SHA-256：`f797d6c1227196351b9b7d1b1e0f81bb9c7b74fce48d4b97f921160073eefebf`。

## es_es

### `gui.echo_warrior.summoner.attribute.level`

- 使用位置：召唤器属性悬浮提示
- 中文原文：等级：%s/%s
- 英文原文：Level: %s/%s
- 目标语言：Nivel: %s/%s
- 中文直译回译／自然释义：等级：当前值/上限。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.experience.max`

- 使用位置：召唤器经验条满级悬浮提示
- 中文原文：等级%s · 已达到最高等级
- 英文原文：Level %s · Max level
- 目标语言：Nivel %s · Nivel máximo
- 中文直译回译／自然释义：当前等级为指定数值，已达到最高等级。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.healing_disabled`

- 使用位置：TBF 治疗操作的动作栏反馈
- 中文原文：英灵使用自身的恢复机制，永恒伙伴治疗不适用。
- 英文原文：Echoes use their own recovery. TBF healing is unavailable.
- 目标语言：Los Ecos usan su propio sistema de recuperación. La curación de TBF no está disponible.
- 中文直译回译／自然释义：英灵使用自身恢复机制，TBF 治疗不可用。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.unavailable`

- 使用位置：TBF 无法操作的动作栏反馈
- 中文原文：无法操作这名英灵：请检查遗物、控制权和召唤器状态。
- 英文原文：This Echo is unavailable. Check its relic, controller, and summoner.
- 目标语言：No se puede controlar este Eco. Comprueba la reliquia, los permisos de control y el invocador.
- 中文直译回译／自然释义：这名英灵无法操作，请检查遗物、控制权限和召唤器。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

文件 SHA-256：`c1fd2cf2e21b6f0329a93ac14e852328f63b224bf692b9f52854d722b745e537`。

## es_mx

### `gui.echo_warrior.summoner.attribute.level`

- 使用位置：召唤器属性悬浮提示
- 中文原文：等级：%s/%s
- 英文原文：Level: %s/%s
- 目标语言：Nivel: %s/%s
- 中文直译回译／自然释义：等级：当前值/上限。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `gui.echo_warrior.summoner.experience.max`

- 使用位置：召唤器经验条满级悬浮提示
- 中文原文：等级%s · 已达到最高等级
- 英文原文：Level %s · Max level
- 目标语言：Nivel %s · Nivel máximo
- 中文直译回译／自然释义：当前等级为指定数值，已达到最高等级。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.healing_disabled`

- 使用位置：TBF 治疗操作的动作栏反馈
- 中文原文：英灵使用自身的恢复机制，永恒伙伴治疗不适用。
- 英文原文：Echoes use their own recovery. TBF healing is unavailable.
- 目标语言：Los Ecos usan su propio sistema de recuperación. La curación de TBF no está disponible.
- 中文直译回译／自然释义：英灵使用自身恢复机制，TBF 治疗不可用。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

### `message.echo_warrior.tbf.unavailable`

- 使用位置：TBF 无法操作的动作栏反馈
- 中文原文：无法操作这名英灵：请检查遗物、控制权和召唤器状态。
- 英文原文：This Echo is unavailable. Check its relic, controller, and summoner.
- 目标语言：No se puede controlar este Eco. Revisa la reliquia, los permisos de control y el invocador.
- 中文直译回译／自然释义：这名英灵无法操作，请检查遗物、控制权限和召唤器。
- 术语对应：等级沿用既有 Level 对应词；英灵沿用各语言 Echo 对应词；TBF 指 Truly Best Friends Forever。
- 审校结论：通过。风险：无文化歧义；动态等级占位符与动作栏宽度由界面抽查另行确认。
- 需要作者决定：无需作者决定。

文件 SHA-256：`edf144ecaa057e7017a0d0a003abc7d4c6ff4225e2ca7749a14165a09161d761`。
