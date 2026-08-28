param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$atlasPath = Join-Path $projectRoot 'encyclopedia\content\atlas.zh_cn.json'
$recipeRoot = Join-Path $projectRoot 'src\main\resources\data\echo_warrior\recipe'
$atlas = Get-Content -LiteralPath $atlasPath -Raw | ConvertFrom-Json

$atlas.schemaVersion = 2
$atlas.meta.lastVerified = '2026-08-28'

foreach ($category in $atlas.categories) {
    if ($category.id -eq 'growth') {
        $category.description = '记录等级成长、经验规则，以及它们与英灵饰品的关系。'
    }
}

if (-not ($atlas.categories | Where-Object id -eq 'inheritance')) {
    $atlas.categories += [pscustomobject][ordered]@{
        id = 'inheritance'; label = '传承'; shortLabel = '传承'; symbol = '⬡'
        kicker = 'SHARED LEGACIES'; description = '跨越文明留下的共通精神，也是饰品合成的核心材料。'; accent = '#d6a45d'
    }
}
if (-not ($atlas.categories | Where-Object id -eq 'accessories')) {
    $atlas.categories += [pscustomobject][ordered]@{
        id = 'accessories'; label = '英灵饰品'; shortLabel = '饰品'; symbol = '✧'
        kicker = 'RELIC LOADOUT'; description = '六个开放槽中的固定构筑组件，只强化绑定英灵。'; accent = '#d47d75'
    }
}

$atlas.nodes = @($atlas.nodes | Where-Object {
    $_.id -notin @('modules', 'legacy_overview', 'accessories_overview') -and
    $_.categoryId -notin @('inheritance', 'accessories')
})
foreach ($node in $atlas.nodes) {
    foreach ($connection in $node.connections) {
        if ($connection.targetId -eq 'modules') { $connection.targetId = 'accessories_overview' }
    }
    $node.article.related = @($node.article.related | ForEach-Object { if ($_ -eq 'modules') { 'accessories_overview' } else { $_ } })
}

$legacyMetadata = @(
    [ordered]@{ id='courage_legacy'; name='勇气的传承'; subtitle='攻击与攻速方向'; effect='长按使用 32 tick，获得 60 秒力量 II；非创造模式消耗 1 个。'; art='已经采用现有像素素材。它应像从多个文明武器与战旗中凝结出的赤色传承碎片。' },
    [ordered]@{ id='fortitude_legacy'; name='坚毅的传承'; subtitle='防御方向'; effect='长按使用 32 tick，获得 60 秒生命恢复 II；非创造模式消耗 1 个。'; art='已经采用现有像素素材。形体强调沉重、稳定与石铁般的灰色核心。' },
    [ordered]@{ id='purity_legacy'; name='纯净的传承'; subtitle='生命与恢复方向'; effect='长按使用 32 tick，清除玩家身上全部负面状态；非创造模式消耗 1 个。'; art='已经采用原名“纯洁的传承”的现有像素素材，游戏内正式显示名为“纯净的传承”。' },
    [ordered]@{ id='wisdom_legacy'; name='智慧的传承'; subtitle='经验与视野方向'; effect='长按使用 32 tick，给予玩家 100 点原始经验值；非创造模式消耗 1 个。'; art='已经采用现有像素素材。蓝色知识核心应兼具书页、符文与观察之眼的联想。' },
    [ordered]@{ id='craft_legacy'; name='工艺的传承'; subtitle='特殊机制方向'; effect='不具备直接使用效果。与恰好一件已损伤的工具或护甲无序合成，恢复向上取整的 20% 最大耐久，并保留附魔、名称和其他组件。'; art='已经采用现有像素素材。铜色结构表现跨文明的工具、机关与精密技艺。' }
)

$legacyNodes = @()
$legacyX = @(110, 280, 450, 620, 790)
for ($index = 0; $index -lt $legacyMetadata.Count; $index++) {
    $entry = $legacyMetadata[$index]
    $legacyNodes += [pscustomobject][ordered]@{
        id = $entry.id; categoryId = 'inheritance'; title = $entry.name; subtitle = $entry.subtitle
        status = 'implemented'; x = $legacyX[$index]; y = 350; symbol = '⬡'
        icon = "/assets/echo-warrior/item/$($entry.id).png"; connections = @()
        article = [pscustomobject][ordered]@{
            summary = $entry.effect
            tags = @('传承', '考古', $entry.subtitle, '已实装')
            sections = @(
                [pscustomobject][ordered]@{ type='prose'; title='用途'; paragraphs=@($entry.effect, '传承可以单独使用，但主要用途是作为对应方向饰品的合成核心。') },
                [pscustomobject][ordered]@{ type='prose'; title='绘制思路与参考'; paragraphs=@($entry.art) }
            )
            related = @('legacy_overview', 'accessories_overview')
        }
    }
}

$legacyOverview = [pscustomobject][ordered]@{
    id='legacy_overview'; categoryId='inheritance'; title='传承总览'; subtitle='跨文明的共同遗产'; status='implemented'
    x=450; y=105; symbol='⬡'; icon='/assets/echo-warrior/item/courage_legacy.png'
    connections=@($legacyMetadata | ForEach-Object { [pscustomobject][ordered]@{ targetId=$_.id; kind='primary' } })
    article=[pscustomobject][ordered]@{
        summary='五种传承可从普通战场考古中获得，也为未来的特殊敌人掉落预留了数据接口；它们既能独立使用，也是饰品配方的核心。'
        tags=@('传承','考古','合成核心','特殊敌人预留')
        sections=@(
            [pscustomobject][ordered]@{ type='cards'; title='五种方向'; items=@($legacyMetadata | ForEach-Object { [pscustomobject][ordered]@{ title=$_.name; meta=$_.subtitle; body=$_.effect; icon="/assets/echo-warrior/item/$($_.id).png" } }) },
            [pscustomobject][ordered]@{ type='callout'; tone='planned'; title='特殊敌人掉落'; body='本次比赛范围先完成考古获取与完整饰品体系。特殊敌人掉落尚未绑定到具体敌人，但传承物品标签与数据接口已经保留。' }
        )
        related=@('archaeology','accessories_overview')
    }
}

$accessoryMetadata = @(
    [ordered]@{id='plate_armor_accessory';name='板甲';effect='+2 防御';tier='常见';art='一块厚重、边缘铆接的铁制胸甲板碎片，轮廓简洁，强调可靠防护。'},
    [ordered]@{id='chainmail_armor_accessory';name='全身锁链甲';effect='+4 防御，陆地移动速度 -15%';tier='精良';art='折叠成一束的全身锁链甲，垂下几段清楚的铁环，显得沉重而完整。'},
    [ordered]@{id='spiked_armor_accessory';name='尖刺护甲';effect='+1 防御；受到近战生命伤害时按实际损失 1:1 反伤';tier='精良';art='嵌着仙人掌硬刺的胸甲残片，尖刺方向外翻，危险但仍像可以佩戴的遗物。'},
    [ordered]@{id='battle_worn_whetstone_accessory';name='战痕磨刀石';effect='+2 攻击力';tier='常见';art='布满刀痕的小磨刀石，以磨损红绳束住，边缘残留金属亮屑。'},
    [ordered]@{id='mountain_burden_blade_accessory';name='负山巨刃';effect='+4 攻击力，陆地移动速度 -20%';tier='精良';art='尺寸夸张的暗色巨刃，刀背像压着岩层，视觉重心明显下沉。'},
    [ordered]@{id='fractured_crystal_blade_accessory';name='裂曜晶刃';effect='+4 攻击力，-6 防御';tier='精良';art='黑曜石与紫晶构成的短剑或匕首，裂纹透亮，像随时会碎裂的玻璃大炮。'},
    [ordered]@{id='twin_oath_badge_accessory';name='双誓徽章';effect='+1 攻击力，+2 防御';tier='常见';art='剑与盾左右咬合成一枚徽章，两个方向权重均衡，没有一方压过另一方。'},
    [ordered]@{id='battle_blindfold_accessory';name='盲眼战带';effect='+3 攻击力，主动索敌视野 -75%';tier='精良';art='磨损的黑色蒙眼布，正中缝有暗红色眼痕，表达舍弃观察换取直觉。'},
    [ordered]@{id='crack_ring_hammer_charm_accessory';name='裂响锤坠';effect='攻击时 30% 暴击，造成 200% 伤害并播放一次暴击音效';tier='稀有';art='带裂纹的铁锤吊坠，四周带冲击波或碎裂铃环，形体紧凑而有爆发感。'},
    [ordered]@{id='victors_laurel_accessory';name='胜者桂冠';effect='击杀任意有效生物后恢复 10% 饰品修正后的最大生命';tier='稀有';art='金绿相间的旧桂冠，叶尖磨损，像从许多胜利者手中一路传下。'},
    [ordered]@{id='blood_pact_fang_accessory';name='血契獠牙';effect='每个攻击窗口 30% 概率恢复 4 点生命';tier='稀有';art='用暗红绳结缠住的苍白獠牙，根部有一滴凝固血珠。'},
    [ordered]@{id='memory_ritual_knife_accessory';name='拾忆祭刀';effect='+2 攻击力；击杀时 0.5% 概率凝炼额外传承';tier='稀有';art='短小仪式刀，刀身映出五色微光；掉落表现为从战利品记忆中凝出额外传承。'},
    [ordered]@{id='substitute_doll_accessory';name='替身木偶';effect='受到合格攻击时有 10% 概率完全闪避';tier='常见';art='小型木制或稻草玩偶，身上有替命用的白色刻痕与重新缝合的躯干。'},
    [ordered]@{id='heart_sprout_amber_accessory';name='心芽琥珀';effect='+6 最大生命值';tier='常见';art='琥珀内部封存心形嫩芽，温暖透明，像仍有生命在缓慢生长。'},
    [ordered]@{id='feast_ham_accessory';name='盛宴火腿';effect='+12 最大生命值，陆地移动速度 -15%';tier='精良';art='巨大而油亮的火腿，以绳索绑成可携带的肉鸽遗物，滑稽但非常有分量。'},
    [ordered]@{id='peacemaker_accessory';name='和平使者';effect='+20 最大生命值，-4 攻击力';tier='稀有';art='被白布封住的剑或断刃，外圈形成柔和心形，表达拒绝伤害换取生存。'},
    [ordered]@{id='sunwheel_garland_accessory';name='日轮花冠';effect='白昼获得生命恢复 I';tier='精良';art='向日葵编成的圆形花冠，花瓣排列如太阳齿轮。'},
    [ordered]@{id='moondew_bottle_accessory';name='月露瓶';effect='夜间获得生命恢复 I';tier='精良';art='小型蓝色玻璃瓶，瓶壁凝着月牙形露珠，内部有安静冷光。'},
    [ordered]@{id='tomato_fish_accessory';name='番茄鱼';effect='水中移动速度 +50%';tier='常见';art='一个红色鱼玩偶，浅色腹部，带明显缝线与纽扣眼；不要画成真实鱼或靴子。'},
    [ordered]@{id='cat_bell_fish_charm_accessory';name='猫铃鱼符';effect='6 格内持续阻止苦力怕爆炸；主动锁敌状态下允许英灵主动锁定苦力怕';tier='稀有';art='猫脸小铃铛与鱼尾护符结合，既像玩具又像昂贵的驱爆祭具。'},
    [ordered]@{id='light_gathering_magnet_accessory';name='拾光磁石';effect='英灵击杀产生的世界经验球 +50%';tier='精良';art='马蹄形磁石吸引数颗黄绿色经验光球，光球轨迹要清楚。'},
    [ordered]@{id='training_notes_accessory';name='练兵札记';effect='只有英灵自身的成长经验获取 +50%';tier='精良';art='用红绳束起的磨损训练札记或小册，边角有反复翻阅的卷曲。'},
    [ordered]@{id='hawkeye_lens_accessory';name='鹰眼透镜';effect='主动索敌视野 +50%，最终不超过 32 格';tier='常见';art='黄铜单片镜或短小窥镜，镜面中有锐利鹰眼轮廓。'},
    [ordered]@{id='windchaser_feather_accessory';name='逐风羽饰';effect='陆地移动速度 +10%';tier='常见';art='被气流向后拉直的浅色羽毛，末端绑有轻薄丝线。'},
    [ordered]@{id='hollow_bird_bone_accessory';name='空心鸟骨';effect='陆地移动速度 +20%，-8 最大生命值';tier='精良';art='由逐风羽饰进一步制成的空心鸟骨哨，骨壁薄而脆，带少量残羽。'}
)

$ingredientNames = @{
    '#minecraft:fishes'='任意原版鱼类'; 'echo_warrior:courage_legacy'='勇气的传承'; 'echo_warrior:fortitude_legacy'='坚毅的传承';
    'echo_warrior:purity_legacy'='纯净的传承'; 'echo_warrior:wisdom_legacy'='智慧的传承'; 'echo_warrior:craft_legacy'='工艺的传承';
    'echo_warrior:windchaser_feather_accessory'='逐风羽饰'; 'minecraft:amethyst_shard'='紫水晶碎片'; 'minecraft:anvil'='铁砧';
    'minecraft:bell'='钟'; 'minecraft:black_wool'='黑色羊毛'; 'minecraft:bone'='骨头'; 'minecraft:book'='书'; 'minecraft:cactus'='仙人掌';
    'minecraft:cooked_porkchop'='熟猪排'; 'minecraft:diamond'='钻石'; 'minecraft:emerald'='绿宝石'; 'minecraft:feather'='羽毛';
    'minecraft:flint'='燧石'; 'minecraft:ghast_tear'='恶魂之泪'; 'minecraft:glass_bottle'='玻璃瓶'; 'minecraft:glow_ink_sac'='荧光墨囊';
    'minecraft:gold_nugget'='金粒'; 'minecraft:golden_apple'='金苹果'; 'minecraft:golden_sword'='金剑'; 'minecraft:honeycomb'='蜜脾';
    'minecraft:ink_sac'='墨囊'; 'minecraft:iron_axe'='铁斧'; 'minecraft:iron_chestplate'='铁胸甲'; 'minecraft:iron_helmet'='铁头盔';
    'minecraft:iron_ingot'='铁锭'; 'minecraft:iron_leggings'='铁护腿'; 'minecraft:iron_sword'='铁剑'; 'minecraft:leather'='皮革';
    'minecraft:lily_of_the_valley'='铃兰'; 'minecraft:oak_leaves'='橡树树叶'; 'minecraft:oak_planks'='橡木木板'; 'minecraft:oak_sapling'='橡树树苗';
    'minecraft:obsidian'='黑曜石'; 'minecraft:phantom_membrane'='幻翼膜'; 'minecraft:rabbit_foot'='兔子脚'; 'minecraft:red_wool'='红色羊毛';
    'minecraft:redstone'='红石粉'; 'minecraft:shield'='盾牌'; 'minecraft:spider_eye'='蜘蛛眼'; 'minecraft:spyglass'='望远镜';
    'minecraft:stick'='木棍'; 'minecraft:stone'='石头'; 'minecraft:string'='线'; 'minecraft:sugar'='糖'; 'minecraft:sunflower'='向日葵';
    'minecraft:tropical_fish'='热带鱼'; 'minecraft:white_wool'='白色羊毛'; 'minecraft:wooden_sword'='木剑'
}
$blockIcons = @{
    'minecraft:anvil'='anvil'; 'minecraft:black_wool'='black_wool'; 'minecraft:cactus'='cactus_side';
    'minecraft:lily_of_the_valley'='lily_of_the_valley'; 'minecraft:oak_leaves'='oak_leaves'; 'minecraft:oak_planks'='oak_planks';
    'minecraft:oak_sapling'='oak_sapling'; 'minecraft:obsidian'='obsidian'; 'minecraft:red_wool'='red_wool'; 'minecraft:stone'='stone';
    'minecraft:sunflower'='sunflower_front'; 'minecraft:white_wool'='white_wool'
}

function New-IngredientSlot([string]$ingredient) {
    if ($ingredient -eq '#minecraft:fishes') {
        return [pscustomobject][ordered]@{ name=$ingredientNames[$ingredient]; icon='/assets/minecraft/item/cod.png' }
    }
    if ($ingredient.StartsWith('echo_warrior:')) {
        $path = $ingredient.Split(':')[1]
        return [pscustomobject][ordered]@{ name=$ingredientNames[$ingredient]; icon="/assets/echo-warrior/item/$path.png" }
    }
    $path = $ingredient.Split(':')[1]
    $icon = if ($blockIcons.ContainsKey($ingredient)) { "/assets/minecraft/block/$($blockIcons[$ingredient]).png" } else { "/assets/minecraft/item/$path.png" }
    return [pscustomobject][ordered]@{ name=$ingredientNames[$ingredient]; icon=$icon }
}

function New-RecipeSection($entry) {
    $recipePath = Join-Path $recipeRoot ($entry.id + '.json')
    $recipe = Get-Content -LiteralPath $recipePath -Raw | ConvertFrom-Json
    $grid = [System.Collections.ArrayList]::new()
    1..9 | ForEach-Object { [void]$grid.Add($null) }
    $shapeless = $recipe.type -eq 'minecraft:crafting_shapeless'
    if ($shapeless) {
        for ($index = 0; $index -lt $recipe.ingredients.Count; $index++) {
            $grid[$index] = New-IngredientSlot ([string]$recipe.ingredients[$index])
        }
    } else {
        for ($row = 0; $row -lt $recipe.pattern.Count; $row++) {
            $line = [string]$recipe.pattern[$row]
            for ($column = 0; $column -lt $line.Length; $column++) {
                $symbol = [string]$line[$column]
                if ($symbol -ne ' ') {
                    $ingredient = [string]$recipe.key.PSObject.Properties[$symbol].Value
                    $grid[$row * 3 + $column] = New-IngredientSlot $ingredient
                }
            }
        }
    }
    return [pscustomobject][ordered]@{
        type='recipe'; title='合成表'; grid=@($grid); shapeless=$shapeless
        output=[pscustomobject][ordered]@{ name=$entry.name; icon="/assets/echo-warrior/item/$($entry.id).png" }
    }
}

$accessoryNodes = @()
$gridX = @(100, 270, 440, 610, 780)
$gridY = @(145, 245, 345, 445, 545)
for ($index = 0; $index -lt $accessoryMetadata.Count; $index++) {
    $entry = $accessoryMetadata[$index]
    $accessoryNodes += [pscustomobject][ordered]@{
        id=$entry.id; categoryId='accessories'; title=$entry.name; subtitle=$entry.effect; status='development'
        x=$gridX[$index % 5]; y=$gridY[[math]::Floor($index / 5)]; symbol='✧'; icon="/assets/echo-warrior/item/$($entry.id).png"; connections=@()
        article=[pscustomobject][ordered]@{
            summary=$entry.effect
            tags=@('饰品', $entry.tier, '六槽构筑', '固定数值', $entry.name)
            sections=@(
                [pscustomobject][ordered]@{ type='stats'; title='固定效果'; items=@([pscustomobject][ordered]@{ label='效果'; value=$entry.effect; note='只影响绑定英灵；与等级无关' }, [pscustomobject][ordered]@{ label='考古稀有度'; value=$entry.tier; note='饰品总掉率为 0.2%' }) },
                (New-RecipeSection $entry),
                [pscustomobject][ordered]@{ type='prose'; title='绘制思路与参考'; paragraphs=@($entry.art) },
                [pscustomobject][ordered]@{ type='callout'; tone='planned'; title='美术状态'; body="当前游戏与百科暂用原版图标占位，但资源已经固定为 echo_warrior:item/$($entry.id)。正式像素素材完成后可直接覆盖同名 PNG，无需改代码、模型或百科数据。" }
            )
            related=@('accessories_overview','legacy_overview')
        }
    }
}

$accessoryOverview = [pscustomobject][ordered]@{
    id='accessories_overview'; categoryId='accessories'; title='饰品总览'; subtitle='25 件固定构筑组件'; status='development'
    x=440; y=45; symbol='✧'; icon='/assets/echo-warrior/item/twin_oath_badge_accessory.png'
    connections=@(0,5,10,15,20 | ForEach-Object { [pscustomobject][ordered]@{targetId=$accessoryMetadata[$_].id;kind='primary'} })
    article=[pscustomobject][ordered]@{
        summary='召唤器提供六个饰品槽。同名饰品最多安装一个，不同饰品可以共同生效；所有属性都是等级成长完成后再叠加的固定值。'
        tags=@('饰品','六槽','同名唯一','固定值','正式比赛范围')
        sections=@(
            [pscustomobject][ordered]@{ type='prose'; title='装卸规则'; paragraphs=@('饰品只能放进英灵召唤器的六个饰品槽，不会强化玩家。同名饰品最多一个，不同饰品允许组合。', '攻击、护甲和最大生命采用固定加减值；移速相对基础值相加，最终不低于基础值的 25%。取出最大生命饰品时保留当前生命的绝对数值，只有超过新上限时才截断。') },
            [pscustomobject][ordered]@{ type='stats'; title='当前目录'; items=@([pscustomobject][ordered]@{label='饰品总数';value='25';note='3 件旧内容更名 + 22 件新内容'}, [pscustomobject][ordered]@{label='槽位';value='6';note='同名最多 1 件'}, [pscustomobject][ordered]@{label='普通考古总掉率';value='0.2%';note='常见/精良/稀有 = 60/30/10'}) },
            [pscustomobject][ordered]@{ type='callout'; tone='planned'; title='正式美术待替换'; body='25 件饰品已经全部采用独立资源 ID 与百科绘制说明。当前原版图标只用于可玩占位和直观查表，正式美术素材计划在 2026 年 8 月 30 日前补齐。' }
        )
        related=@('summoner','legacy_overview','leveling')
    }
}

$atlas.nodes += @($legacyOverview) + $legacyNodes + @($accessoryOverview) + $accessoryNodes

$archaeology = $atlas.nodes | Where-Object id -eq 'archaeology'
if ($archaeology) {
    $archaeology.article.sections += [pscustomobject][ordered]@{
        type='callout'; tone='info'; title='普通考古的饰品池'
        body='饰品成品总概率为 0.2%。命中饰品池后：常见占 60%（8 件等权）、精良占 30%（11 件等权）、稀有占 10%（6 件等权）。五种传承合计 20%，彼此等权。'
    }
    $archaeology.article.related += @('legacy_overview','accessories_overview')
}

$json = $atlas | ConvertTo-Json -Depth 30
$json = $json.Replace('模块', '饰品')
Set-Content -LiteralPath $atlasPath -Encoding utf8 -Value $json
Write-Output "Updated $atlasPath with $($legacyNodes.Count) legacy nodes and $($accessoryNodes.Count) accessory nodes."
