# Changelog

All notable development changes to Echo Warrior are recorded here.

## Unreleased

### Planned

- Continue manual playtesting and tune the head-target radius, body-turn speed, and animation responsiveness if required.

### Added

- Added server-authoritative persistent Echo bindings, including container-safe summoner state, controller transfer, follow-mode cross-dimension reconstruction, duplicate UUID handling, configurable population limits, and operator diagnostics.
- Added the 44-page in-game tutorial manual "Echoes, Warriors, and You" with chapter tabs, recipes, skill and accessory references, persistent reading position, credits, and source attribution.
- Added the Roman Legionary's Legionary Bulwark passive, reducing direct attack and projectile damage from the front hemisphere by 50% after armor.
- Added terminal item-destruction hooks and nested vanilla-container inspection so confirmed summoner destruction terminates its binding without treating ordinary storage as destruction.
- Added the first complete Echo Compass and battlefield-archaeology gameplay loop: toggleable audio navigation, persistent renewable battlefield sites, guaranteed relic targets, biome-matched suspicious blocks, and exact weighted archaeology rewards.
- Added placeholder Small Knowledge and five Legacy materials, plus functional Plate Armor, Full Chainmail, and Spiked Armor Echo modules with live stat updates, duplicate-name limits, summoner display integration, recipes, localization, and encyclopedia documentation.
- Added a guaranteed journeyman-cartographer Echo Compass trade and the compass recipe using one compass, four amethyst shards, and four copper ingots.
- Added gamemaster-only battlefield generate, locate, and safe 32–50-block annulus teleport commands for archaeology playtesting; batch generation is spread across server ticks and reports progress instead of synchronously freezing the server.
- Added a two-stage Echo Compass: a 2048-block exterior needle, a 320-block optional travel reminder, 24/48-block hysteresis, and directional inner-site archaeology echoes.
- Added persistent per-player post-relic cleanup tracking with shared remaining-block counts and automatic handling for brushed, broken, exploded, replaced, or fallen suspicious blocks.
- Added the first playable Japanese Samurai Echo prototype, including its relic, model and split combat animations, summoner integration, localization, hand-drawn skill icons, encyclopedia entry, and repeatable BBModel export pipeline.
- Added Zanshin health-scaled dodge, attack-window dodge bonus, Fumikomi charges and invulnerable forward dash, branching two-slash normal attacks, and the automatic two-hit Stab skill.
- Added client-only frozen-pose Samurai afterimages with neutral/themed color modes, optional UV-anchored dissolve, performance limits, and temporary visual debug commands.
- Added the first playable implementation of the Chinese Guandao Warrior Echo, including its relic, model, five normalized animations, summoner preview, localization, and four hand-drawn skill icons.
- Added projectile resistance, Growing Valor stacks, low-health damage reduction, full-damage crescent-blade sweeps, and the four-stage advancing Guandao combo with launch and projectile deflection windows.
- Added Chinese Guandao Warrior documentation and interactive encyclopedia content, plus a repeatable BBModel-to-GeckoLib asset export script.
- Added the complete server-synchronised visual-life system to the Chinese Guandao Warrior: natural and double blinks, layered eye/head attention, player mutual gaze, caught-watching reactions and exits, locomotion focus, creeper awareness, approaching-entity reactions, and damage-source attention.
- Added command-toggleable Guandao animation diagnostics. Server logs cover action boundaries, hit windows, opening correction, and retaliation; client logs sample key-bone deltas around attack and combo release. Diagnostics now default to off after logs confirmed stable action release.
- Temporarily re-enabled Guandao animation diagnostics by default for the rear-retaliation regression pass, including alignment waits, queued/promoted retaliation targets, and locked attack/body yaw values.
- Added the modeler's updated Aztec and Egyptian relic icons to runtime, summoner, and encyclopedia assets; archived the future Japanese Samurai relic icon without registering that unreleased hero.
- Head-centred player-gaze acquisition with distance-scaled timing, two-tick mouse tolerance, line-of-sight validation, multiplayer owner priority, and close-range handling for invisible players.
- A mutual-gaze state with randomized hold and renewal durations, occasional glance-away breaks, last-seen-position persistence, combat suppression, and threat interruption.
- Independent pupil, head, and body attention layers with explicit threat priorities and minimum target-hold windows.
- Modeler-authored full-body attack and hurt animations, plus imported shield raise/lower previews.
- Gamemaster-only `/echo_warrior animation` commands for attack, hurt, shield raise, shield lower, and reset previews.
- A repeatable `.bbmodel` import path that normalizes animation and bone names, protects code-owned face bones, extracts the embedded texture, and validates geometry compatibility.
- An owner-only caught-watching reaction with delayed guaranteed activation during sustained mutual gaze, a restrained startle, rapid double blink, eyes-first glance-away, head follow-through, and a final covert look back at the owner.
- Three post-reaction exit behaviours: continued observation, a stationary patrol turn, or a short path-validated walk-away, each with an optional one-time profile glance back at the owner.

### Fixed

- Fixed long-lived aura effects and permanent attribute modifiers surviving without a valid source; periodic server audits now remove stale state, restore missing modifiers, and avoid multi-source stacking.
- Fixed fast projectiles crossing the Legionary's shield-charge interception path between ticks; interception now uses continuous relative trajectories and updates homing projectile targets after reflection.
- Fixed active Echo growth and summoner state depending on the physical item being loaded in a player's inventory; binding SavedData now remains authoritative while the item is stored or unloaded.
- Fixed the Chinese Guandao Warrior committing a normal attack against a newly selected rear target before its body had turned, causing the animation and sector damage to continue along the previous forward direction. Rear retaliation now queues across committed actions, turns at up to 30 degrees per tick, waits for a 25-degree alignment threshold, and locks one target yaw for the full swing.
- Fixed lethal damage leaving the Chinese Guandao Warrior's committed attack or combo windows active long enough to produce a post-death strike.
- Fixed the Guandao combo opener stacking its custom launch on top of vanilla hurt knockback and pushing targets out of the second strike; it now replaces the result with a restrained, resistance-scaled short float.
- Fixed the Echo Compass using vanilla random no-target wobble inside battlefield sites; it now spins continuously clockwise, accelerates from roughly 1.6 seconds to 0.4 seconds per revolution near its archaeology target, and uses a fixed fast spin when no site is available.
- Fixed suspicious grass and dirt being visually identical to ordinary blocks in every brushing stage; both now have visible echo markings and four progressive brushing textures while suspicious grass retains biome tinting.
- Fixed custom suspicious grass and dirt crashing the client when placed because the vanilla brushable block entity type did not yet recognize the two mod blocks.
- Fixed ready battlefield regions stalling behind a single unloaded candidate chunk; generation now rotates through currently loaded chunks without force-loading any of them.
- Fixed module-adjusted summoner values becoming invisible because their green and red text colors were missing an opaque alpha channel.
- Fixed suspicious grass rendering with a fixed color instead of using the surrounding biome's grass tint.

### Changed

- Echo Warriors no longer expire because of a lifetime timer or disappear when their controller logs out or dies; explicit dismissal, death, relic removal, confirmed summoner destruction, or operator action now ends the binding.
- Follow-mode Echoes can reconstruct near their controller across dimensions, while wait and wander modes remain in their original dimension without force-loading chunks.
- Raised the Roman Legionary conditional combo's second hit from 50% to 75% damage and expanded shield charge to redirect projectiles and punish primed creepers without granting ongoing Creeper pursuit.
- English knowledge and tutorial pages now scale text only when needed to remain inside their safe layout bounds, while Chinese pages retain the default size.
- Growing Valor now keeps red-orange weapon flames at every stack. Five stacks are marked by denser flame and a periodic red-gold spark pulse instead of blue soul fire.
- Reduced suspicious grass and dirt markings to low-contrast dark-only details; removed bright highlights and color shifting so untouched pixels remain identical to the vanilla terrain texture.
- Echo Compass right-click now mutes only that individual compass's exterior reminder; it never disables navigation or inner-site echoes, and multiple enabled compasses resolve to one sound emitter by inventory priority.
- Echo summoner combat values now display their actual module-adjusted values, with increases shown in green and decreases shown in red.
- Japanese Samurai normal attacks now use limited safe target tracking before each hit so small target movement does not routinely make the committed slash miss. The first slash can travel up to 1.5 blocks and the follow-up up to 0.75 blocks without gaining Fumikomi invulnerability or afterimages.
- Samurai afterimages now default to original-texture dissolve with no extra tint; saturated cyan/gold coloring is opt-in through the neutral visual command. Removed forced full-bright lighting and the separate full-model white edge pass because GeckoLib rendered it over the detailed body; the proposed outline mode remains disabled for the same filled-silhouette limitation.
- Samurai advanced afterimages now combine UV dissolve with genuine age-based opacity decay, and dash snapshots are emitted only from positions the body has already traversed so they cannot visually overtake the Samurai during Fumikomi.
- Chinese Guandao Warrior passive skills are permanently enabled; only the automatic combo can be toggled. Its cooldown now lasts twelve seconds from combo start, retains uncapped 0.5-second reductions from real melee or projectile hits during the combo, and uses a radial GUI mask without a charge number.
- Guandao combo swings now play keyframe-bound, progressively weighted sweep sounds even when they miss. The fourth strike layers a strong attack with a wind-charge burst and creates a small directional gust in front without adding damage or knockback.
- Reinforced the Guandao combo finisher with the larger Breeze wind-charge burst and a restrained heavy-mace impact layer while preserving the existing directional gust and gameplay values.
- Guandao normal attacks and combos now hold their final GeckoLib pose until the server explicitly releases the trigger, preventing stale cached clips from flashing during recovery. Full-body hurt animation is suppressed whenever a target or attack is active, while damage, pain blinking, facial reaction, and attacker attention remain intact.
- Guandao action release now keeps an idle movement layer beneath full-body clips and uses a zero-tick action-controller transition to avoid stale walk phases and long-path Euler blending at attack/combo boundaries.
- The Guandao combo keeps its 4.5-block trigger range but can add up to 0.75 blocks of safe, target-tracking opening correction so the first strike prioritizes the committed primary target.
- Idle Chinese Guandao warriors now acquire, face, and counter legal melee attackers immediately within normal attack reach; ranged or distant attackers are acquired for pursuit while the current hurt animation is allowed to finish without repeated restarts.
- Mutual gaze now moves pupils first, then the head, and gently aligns the body only when the player lies well outside the forward cone.
- Removed SmartBrainLib's generic always-running look behaviour so it no longer competes with the code-owned visual-attention layer.
- Moved mutual-gaze body correction to the end of the entity tick so vanilla body-rotation control cannot overwrite it.
- Player-to-head visibility now uses a direct block ray to the model's measured head centre instead of the living-entity line-of-sight helper.
- Added `/echo_warrior visual status` to expose gaze acquisition and suppression state during playtesting.
- Mutual-gaze hold time now begins after the echo finishes facing the player, so a rear-facing turn cannot consume most of the visible eye-contact duration.
- Owner-follow navigation pauses during mutual gaze, and body alignment now converges within five degrees instead of relying on a visibly offset head correction.
- Pupil tracking now recalculates the target in head-local space every rendered frame, with larger safe travel, faster eye-leading response, and roll compensation during curious head tilts.
- Corrected the Blockbench head-bone yaw and pitch mapping so left/right and up/down tracking match the target while preserving the intentional roll-based curious tilt.
- Primed creepers and recent damage sources now override visual attention immediately; nearby unprimed creepers lead with the pupils, delay the head slightly, and only turn the non-combat body under close or rapidly approaching conditions.
- Nearby unprimed creepers now pause mutual gaze for up to one second instead of causing alternating-frame target flicker; a cleared distraction resumes player eye contact, while a persistent one ends it.
- `/echo_warrior visual status` now reports distraction state and the active eye, head, and body attention categories.
- Corrected horizontal pupil translation so the eyes lead toward the target instead of briefly moving in the mirrored direction.
- Updated the Roman legionary idle, walk, texture, and corrected bone pivots from the August 8 modeler delivery.
- SBL melee attacks now trigger the one-second attack animation while retaining the existing sixth-tick damage timing.
- Non-attacking echoes play the half-second full-body hurt animation; attacks retain body priority while code-driven pupil contraction, source tracking, and a short pain blink still communicate damage.
- Stabilized code-driven gaze against subtle modeler-authored idle torso rotation, while preserving larger full-body action motion; idle/walk changes now use a short stop delay and transition to filter navigation micro-movement.
- Ordinary attention now holds a living target or random world-space glance for its full intended duration instead of restarting every awareness scan or treating every anonymous point as the same target.
- Owner following now begins beyond fifteen blocks, settles within five, and reserves safe teleport recovery for distances beyond thirty-two blocks; visual-interaction navigation temporarily owns movement without competing with SBL follow paths.
- Rebalanced caught-watching exits to favour a staged short walk-away, require exit targets to differ clearly from the owner's direction, raise the one-time covert-glance chance, and suppress immediate owner reacquisition for 4-7 seconds after completion.

## 2026-08-07 - Legionary visual prototype baseline

### Added

- Code-driven shared visual attention for the Roman legionary echo.
- Head tracking, pupil tracking and contraction, randomized blinking using the existing eyebrow bone, curious head tilts, hurt and startled reactions, and an entity shadow.
- Visual debug commands for blink, double blink, curious, startled, and reset states.
- Dedicated SmartBrainLib owner-follow behaviour with walking, stopping, and safe teleport recovery.
- One-step `CATTEST` playtest launchers and model-artist preview documentation.
- Runtime GeckoLib model and animation assets plus an authoritative Blockbench source and modeler handoff copy.

### Changed

- Summoned and recalled legionaries now initially face their owner.
- The model source was restored to its original geometry and bone hierarchy. The old idle eyebrow blink track was removed because blinking is now code-owned; no eyelid geometry is present.
- Runtime worlds and temporary delivery packages remain excluded from Git.

### Known issue

- The initial mutual-gaze implementation reacts immediately and can visually oppose the player because normal gaze does not align the body while other AI systems can still change its base facing. The approved replacement is listed under Unreleased.
