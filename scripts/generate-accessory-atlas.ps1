param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$atlasPath = Join-Path $projectRoot 'encyclopedia\content\atlas.zh_cn.json'
$recipeRoot = Join-Path $projectRoot 'src\main\resources\data\echo_warrior\recipe'
$atlas = Get-Content -LiteralPath $atlasPath -Raw | ConvertFrom-Json

$atlas.schemaVersion = 2
$atlas.meta.lastVerified = '2026-08-30'

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
    [ordered]@{id='plate_armor_accessory';name='罗马环片坠';effect='+2 防御';tier='常见';art='三片铜色弧形甲片叠成小坠，保留铆钉与皮绳结构。'},
    [ordered]@{id='chainmail_armor_accessory';name='锁帷重带';effect='+4 防御，陆地移动速度 -15%';tier='精良';art='深蓝织带包裹细密锁环，中央嵌一枚银色护片。'},
    [ordered]@{id='spiked_armor_accessory';name='黑曜荆棘符';effect='+1 防御；受到近战生命伤害时按实际损失 1:1 反伤';tier='精良';art='紫黑色黑曜石尖片交错成荆棘状护符。'},
    [ordered]@{id='battle_worn_whetstone_accessory';name='百战砺符';effect='+2 攻击力';tier='常见';art='浅灰色穿孔砺石佩片，边缘带磨痕并系红绳。'},
    [ordered]@{id='mountain_burden_blade_accessory';name='负山根付';effect='+4 攻击力，陆地移动速度 -20%';tier='精良';art='棕红色山形根付，体量厚重，顶部留有系绳结构。'},
    [ordered]@{id='fractured_crystal_blade_accessory';name='裂曜晶坠';effect='+4 攻击力，-6 防御';tier='精良';art='细长紫黑晶片，裂缝与刃缘呈现明亮紫色反光。'},
    [ordered]@{id='twin_oath_badge_accessory';name='双鱼玉徽';effect='+1 攻击力，+2 防御';tier='常见';art='米白色圆玉佩，以两条首尾相随的鱼形成中央纹样。'},
    [ordered]@{id='battle_blindfold_accessory';name='盲剑目隐';effect='+3 攻击力，主动索敌视野 -75%';tier='精良';art='青蓝色蒙眼布带卷叠成环，结扣处带冷白高光。'},
    [ordered]@{id='crack_ring_hammer_charm_accessory';name='裂响祭祀石';effect='攻击时 30% 暴击，造成 200% 伤害并播放一次暴击音效';tier='稀有';art='深绿色祭祀石吊坠，表面有银白色裂响纹。'},
    [ordered]@{id='victors_laurel_accessory';name='凯旋桂冠';effect='击杀任意有效生物后恢复 10% 饰品修正后的最大生命';tier='稀有';art='铜绿色月桂叶编成开口圆环，下方垂落金色系带。'},
    [ordered]@{id='blood_pact_fang_accessory';name='血契河马牙';effect='每个攻击窗口 30% 概率恢复 4 点生命';tier='稀有';art='弯曲象牙色河马牙，根部残留暗色束带与暖色阴影。'},
    [ordered]@{id='memory_ritual_knife_accessory';name='阿努比斯护符';effect='+2 攻击力；击杀时 0.5% 概率凝炼额外传承';tier='稀有';art=@('最终图标为青绿色阿努比斯小像护符，以胡狼首、竖耳与直立神像构成剪影。','来源参考古埃及晚期至托勒密时期的彩釉陶阿努比斯护符；其形象与防腐、墓地守护和亡者审判相关。')},
    [ordered]@{id='substitute_doll_accessory';name='身代木偶';effect='受到合格攻击时有 10% 概率完全闪避';tier='常见';art='棕色木制小人，头顶与身体缠有醒目红绳。'},
    [ordered]@{id='heart_sprout_amber_accessory';name='玉蝉';effect='+6 最大生命值';tier='常见';art='淡黄色玉蝉以浅色高光区分蝉翼与腹部。'},
    [ordered]@{id='feast_ham_accessory';name='盛宴胸针';effect='+12 最大生命值，陆地移动速度 -15%';tier='精良';art='棕金色圆形胸针，中央以盛宴食物和叶穗形成紧凑浮雕。'},
    [ordered]@{id='peacemaker_accessory';name='玉璧';effect='+20 最大生命值，-4 攻击力';tier='稀有';art='素白圆形玉璧，以中央圆孔和温润明暗构成主体。'},
    [ordered]@{id='sunwheel_garland_accessory';name='托纳蒂乌花冠';effect='白昼获得生命恢复 I';tier='精良';art='金黄色太阳花冠，中心圆盘与放射状花瓣形成强轮廓。'},
    [ordered]@{id='moondew_bottle_accessory';name='孔苏月露瓶';effect='夜间获得生命恢复 I';tier='精良';art='浅青色细颈小瓶，瓶身以月牙和冷色高光表现月露。'},
    [ordered]@{id='tomato_fish_accessory';name='尼罗河红鱼符';effect='水中移动速度 +50%';tier='常见';art='红色圆胖鱼形玩偶，外观像番茄，并保留悬挂珠串。'},
    [ordered]@{id='cat_bell_fish_charm_accessory';name='猫纹玉';effect='6 格内持续阻止苦力怕爆炸；主动锁敌状态下允许英灵主动锁定苦力怕';tier='稀有';art=@('最终图标为浅绿色猫形玉牌，以竖耳、圆润轮廓和中央猫纹表达主题。','来源参考国立故宫博物院所藏元至明玉猫形坠饰；游戏图标是适配 16×16 的再设计。')},
    [ordered]@{id='light_gathering_magnet_accessory';name='鹰纹宝石';effect='英灵击杀产生的世界经验球 +50%';tier='精良';art=@('最终图标为红色椭圆鹰纹宝石，外缘使用银白托座。','来源参考希腊—罗马鹰纹缠丝玛瑙浮雕与雕刻宝石传统；聚拢经验球仍属于游戏功能。')},
    [ordered]@{id='training_notes_accessory';name='墨西加鹰首唇饰';effect='只有英灵自身的成长经验获取 +50%';tier='精良';art=@('最终图标为金色墨西加鹰首唇饰，以弯曲鹰喙、镂空口部和金属高光构成。','来源参考 15～16 世纪墨西加金质鹰首唇饰；此类饰物佩戴于下唇下方的穿孔中，与权力、身份和精英战士相关。')},
    [ordered]@{id='hawkeye_lens_accessory';name='鹰纹远目镜';effect='主动索敌视野 +50%，最终不超过 32 格';tier='常见';art='金色短筒远目镜，前端嵌淡蓝镜片，筒身带鹰纹。'},
    [ordered]@{id='windchaser_feather_accessory';name='风神羽饰';effect='陆地移动速度 +10%';tier='常见';art='浅色羽毛束系在朱红漆扣上，辅以浅蓝飘带。'},
    [ordered]@{id='hollow_bird_bone_accessory';name='尼罗鹮骨';effect='陆地移动速度 +20%，-8 最大生命值';tier='精良';art='细长中空白色鸟骨，两端磨圆穿绳，表面绘有蓝色条纹。'}
)

$ingredientNames = @{
    '#minecraft:wool'='任意颜色羊毛'; '#minecraft:planks'='任意木板'; '#minecraft:leaves'='任意树叶'; '#minecraft:fishes'='任意原版鱼类';
    'echo_warrior:courage_legacy'='勇气的传承'; 'echo_warrior:fortitude_legacy'='坚毅的传承';
    'echo_warrior:purity_legacy'='纯净的传承'; 'echo_warrior:wisdom_legacy'='智慧的传承'; 'echo_warrior:craft_legacy'='工艺的传承';
    'minecraft:amethyst_shard'='紫水晶碎片'; 'minecraft:anvil'='铁砧'; 'minecraft:bamboo'='竹子';
    'minecraft:bell'='钟'; 'minecraft:black_wool'='黑色羊毛'; 'minecraft:bone'='骨头'; 'minecraft:book'='书'; 'minecraft:cactus'='仙人掌';
    'minecraft:copper_ingot'='铜锭'; 'minecraft:cyan_wool'='青色羊毛';
    'minecraft:cooked_porkchop'='熟猪排'; 'minecraft:diamond'='钻石'; 'minecraft:emerald'='绿宝石'; 'minecraft:feather'='羽毛';
    'minecraft:flint'='燧石'; 'minecraft:ghast_tear'='恶魂之泪'; 'minecraft:glass'='玻璃'; 'minecraft:glass_bottle'='玻璃瓶'; 'minecraft:glow_ink_sac'='荧光墨囊';
    'minecraft:gold_ingot'='金锭'; 'minecraft:gold_nugget'='金粒'; 'minecraft:golden_apple'='金苹果'; 'minecraft:golden_sword'='金剑'; 'minecraft:honeycomb'='蜜脾';
    'minecraft:ink_sac'='墨囊'; 'minecraft:iron_axe'='铁斧'; 'minecraft:iron_chestplate'='铁胸甲'; 'minecraft:iron_helmet'='铁头盔';
    'minecraft:iron_ingot'='铁锭'; 'minecraft:iron_leggings'='铁护腿'; 'minecraft:iron_sword'='铁剑'; 'minecraft:lapis_lazuli'='青金石'; 'minecraft:leather'='皮革';
    'minecraft:lily_of_the_valley'='铃兰'; 'minecraft:oak_leaves'='橡树树叶'; 'minecraft:oak_planks'='橡木木板'; 'minecraft:oak_sapling'='橡树树苗';
    'minecraft:obsidian'='黑曜石'; 'minecraft:phantom_membrane'='幻翼膜'; 'minecraft:rabbit_foot'='兔子脚'; 'minecraft:red_wool'='红色羊毛';
    'minecraft:red_dye'='红色染料'; 'minecraft:redstone'='红石粉'; 'minecraft:shield'='盾牌'; 'minecraft:spider_eye'='蜘蛛眼'; 'minecraft:spyglass'='望远镜';
    'minecraft:stick'='木棍'; 'minecraft:stone'='石头'; 'minecraft:string'='线'; 'minecraft:sugar'='糖'; 'minecraft:sunflower'='向日葵'; 'minecraft:terracotta'='陶瓦'; 'minecraft:tuff'='凝灰岩';
    'minecraft:tropical_fish'='热带鱼'; 'minecraft:white_wool'='白色羊毛'; 'minecraft:wooden_sword'='木剑'
}
$blockIcons = @{
    'minecraft:anvil'='anvil'; 'minecraft:black_wool'='black_wool'; 'minecraft:cactus'='cactus_side';
    'minecraft:cyan_wool'='cyan_wool'; 'minecraft:glass'='glass';
    'minecraft:lily_of_the_valley'='lily_of_the_valley'; 'minecraft:oak_leaves'='oak_leaves'; 'minecraft:oak_planks'='oak_planks';
    'minecraft:oak_sapling'='oak_sapling'; 'minecraft:obsidian'='obsidian'; 'minecraft:red_wool'='red_wool'; 'minecraft:stone'='stone';
    'minecraft:sunflower'='sunflower_front'; 'minecraft:terracotta'='terracotta'; 'minecraft:tuff'='tuff'; 'minecraft:white_wool'='white_wool'
}
$tagIcons = @{
    '#minecraft:wool'='/assets/minecraft/block/white_wool.png'; '#minecraft:planks'='/assets/minecraft/block/oak_planks.png';
    '#minecraft:leaves'='/assets/minecraft/block/oak_leaves.png'; '#minecraft:fishes'='/assets/minecraft/item/cod.png'
}

function New-IngredientSlot([string]$ingredient) {
    if ($ingredient.StartsWith('#minecraft:')) {
        return [pscustomobject][ordered]@{ name=$ingredientNames[$ingredient]; icon=$tagIcons[$ingredient] }
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
                [pscustomobject][ordered]@{ type='stats'; title='固定效果'; items=@([pscustomobject][ordered]@{ label='效果'; value=$entry.effect; note='只影响绑定英灵；与等级无关' }, [pscustomobject][ordered]@{ label='考古稀有度'; value=$entry.tier; note='同文明饰品池总概率为 14.8148%' }) },
                (New-RecipeSection $entry),
                [pscustomobject][ordered]@{ type='prose'; title='美术造型与来源'; paragraphs=@($entry.art) },
                [pscustomobject][ordered]@{ type='callout'; tone='info'; title='正式美术'; body="模型师制作的 16×16 正式像素图标已于 2026 年 8 月 30 日接入游戏与百科；运行时资源保持为 echo_warrior:item/$($entry.id)。" }
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
            [pscustomobject][ordered]@{ type='stats'; title='当前目录'; items=@([pscustomobject][ordered]@{label='饰品总数';value='25';note='全部已确定最终名称与正式美术'}, [pscustomobject][ordered]@{label='槽位';value='6';note='同名最多 1 件'}, [pscustomobject][ordered]@{label='普通考古总掉率';value='14.8148%';note='常见/精良/稀有 = 60/30/10'}) },
            [pscustomobject][ordered]@{ type='callout'; tone='info'; title='稀有度名称颜色'; body='饰品使用原版物品稀有度颜色：常见为白色，精良为黄色，稀有为青色。颜色会显示在物品名称、悬浮提示及其他采用原版名称样式的界面中。' },
            [pscustomobject][ordered]@{ type='callout'; tone='info'; title='正式美术与创造标签页'; body='25 件饰品的 16×16 RGBA 正式图标已于 2026 年 8 月 30 日接入游戏与百科。创造模式中可在独立的“英灵饰品”标签页查看全部 25 件饰品，标签图标为阿努比斯护符。' }
        )
        related=@('summoner','legacy_overview','leveling')
    }
}

$atlas.nodes += @($legacyOverview) + $legacyNodes + @($accessoryOverview) + $accessoryNodes

$archaeology = $atlas.nodes | Where-Object id -eq 'archaeology'
if ($archaeology) {
    $archaeology.article.sections += [pscustomobject][ordered]@{
        type='callout'; tone='info'; title='普通考古的饰品池'
        body='同文明饰品成品总概率为 14.8148%。命中饰品池后仍按常见 60%、精良 30%、稀有 10% 分配；某文明缺少一档时，按现有档位原比例归一化。同文明知识碎片与五种传承分别在各自类别内等权。'
    }
    $archaeology.article.related += @('legacy_overview','accessories_overview')
}

$json = $atlas | ConvertTo-Json -Depth 30
$json = $json.Replace('模块', '饰品')
Set-Content -LiteralPath $atlasPath -Encoding utf8 -Value $json
Write-Output "Updated $atlasPath with $($legacyNodes.Count) legacy nodes and $($accessoryNodes.Count) accessory nodes."
