![Logo](core/assets-raw/sprites/ui/logo.png)

(I'll clean official read.me and put here my changelog (W.I.P). Copied message from pull (#11445).

# Disclaimer!
Documentation isn't fully finished. Some changes here in post might be slightly outdated or I could forget to write here something. I highly recommend to check minimal.diff in release (with ALL github changes which I made (compared my fork and upstream (anuke BE)). My rebalance works kinda fine in real playtests, so spend few hours on playtesting before placing final vote (if you placing negative voting, give me a list with all changes which you dont like or where I did balance issue. I need all player opinion and rebalance experience/feedback so I can make it better). My rebalance is the only one which exist rn as a stuff which fixes all game issues and pretending to be really added in game. ALSO please playtest Anuke BE version/v153 before playtesting rebalance (optional, but v7 experience feels very different from v8 experience and has a lot changes which might looks like made by me, but can be misunderstood as anuke changes one). Also I has small problems with English (its not my native language). Dropping //todo list below documentation what else I am planning to finish. If I forgot something, or you got any questions, feel free to ask me here or in discord (faster response): blackberry2093

## This is release page. Here you can install diff files (to see all changes), mindustry.jar (For PC players), mindustry.apk (for mobile users) and server.jar (if you wanna make a server with new balance). Later I'll also drop here a file with data-patch (as alternative for testing new changes) -> https://github.com/StalkerBaran/Mindustry/releases/tag/mindustry

## What my rebalance changes:
- Changing most stats (like health, damage, speed and etc) in UnitTypes, Blocks, StatusEffect files (and BuilderAI (2 lines))
- idk what else it could change

## What my rebalance NOT changes:
- New game sounds
- Campaign maps, difficulty, ...
- Unloader placement in conveyor section (in building menu) and etc

# Changelog. I gonna slowly turn this changelog in documentation 

---------------------
# Global changes
This changes appliees to most stuff in game and/or strongly affect in game (important to keep in head this changes before looking of separated changelog)

## Cores

**Core Armor Additions:**
- Shard: Health 1100 → 2000, Armor 0 → 3
- Foundation: Armor 0 → 5
- Nucleus: Health 6000 → 8000, Armor 0 → 7

Cores got armor and health buff! It might looks like questionable change, but it suppose to make most T1 attacks (and any units with low damage and/or fast firerate) more designed for eco harass. Armor suppose to makes T1 less dangerous for killing cores, making T2 in game more useful. Also upgrading core will make a more sense in PvP (and even PvE probably) to make it more defendable (against units like zenith, spiroct and etc (all units with low damage but good DPS on non-armor stuff). Also it make flares (together with other changes, yoy gonna read them later) less broken unit. Also cores got health buff as counterbuff to a percentage healing nerf (like poly, mega, ...), so outheal your core would be much harder, but increased health and armor gonna give you more time for defending 


## Global mobility (speed + rotarespeed) buff/debuff

Most turrets got small nerfs in rotarespeed (and huge nerfs for foreshadow, spectre and meltdown). Most of this changes are cosmetic and making turrets looks less insane (imagine swarmer/meltdown with cryo and overdrive shizo crazy fast rotarespeed, it looks funny, but probably need a small fixes). 4x4 turrets (spectre, foreshadow and meltdown) got very huge rotarespeed nerf to make them looks more scareful and powerful (and atleast seriosly being used with cryo + overdrive). This changes can easily be discussed in community if peoples wanna see crazy fast rotarespeed as meme part of game (like navanax cannon which can boost buildings like overdrive, nobody uses it, but looks like evil joke)

Most units got huge mobility buffs (almost all units got rotarespeed buff (except, uh... corvus) and most units got speed buff (except corvus, flare, zenith, (recheck changes to not lie, //todo). Some naval units got speed buff, some weren't changed and some got even slighty slower (check individually for every units below (unit changes)). Naval units got most huge rotarespeed buff to make them much easier and comfortable to control (very huge and annoying rotare got changed into small and fast, which also makes easier to move in small rivers (they wont stuck in 3x3 river rn, trust fr). Other units also got faster and more interesting for control (mobility buffs for units are one of important global changes for units which makes them stronger in PvP and PvE (player side) without making PvE (crux side) harder) for avoiding new difficulty gaps in campaign. I dont thinking that someone will against mobility buffs (???)

## Huge unit healing ability rework

This change more touches competitive gamemodes (1 vs 1 PvP, FFA (Hexed, OpenPvP, ...), less affect PvE. Most units (Nova, Pulsar, Quasar, Poly, Mega, Corvus) got percentage repair nerf. Nerfs were made because very OP for outhealing core (check funny picture. Keep in head that poly still shooting at enemy, not "repair" unit command (which is much stronger for outhealing!). 8-9 poly under "repair" command also fully outhealing core against 30+ zeniths. As counterchanges, cores got buffs (small hp buff for Shard and huge buff for Nucleus (still harder for outhealing, but balanced), also some units (Mega, Nova, Quasar) got "direct healing" (healing blocks in direct health amount (for example, unit with "10 direct repair" gonna heal 10 HP to any blocks with any max health, while percentage healing depends from max health amount (for example, unit with 2% healing, will heal 2/100 of total possible health block (50 shots from healing block from 0.000...1 to max block health) //todo 
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/ec63af86-b59b-4a6e-a1be-884d7d8e0c17" /> 
<img width="522" height="189" alt="image" src="https://github.com/user-attachments/assets/4559078d-9cd0-4eb8-a084-f387c500ca76" /> //todo add direct healing for poly and reduce percentage to 2%)

## Minor unit factory changes
Ye, I thought its also important to remember (this changes are the only which touches ratios (can be important for scheme builders and curated designes). I'll copy here a shortly changelog for changes which I made

**Ground Factory:**

Unit builcost changes:
- dagger (10 silicon, 10 lead -> 15 silicom, 10 lead)
(It was made to make daggers harder to spam on first minuts (since it request even lower silicon amount than flare), also keep in head that T1 change nerf compensated by T2 cost buffs (read below))
- nova (30 silicon, 20 lead, 20 titanium -> 25 silicon, 10 lead, 15 titanium)
(Huge nova cost buff, now it feels more usable in PvE and PvP. Important change for nova to make it moee usable (and easier to produce in bigger amount))

**Air Factory:**
- Requirements (block buildcost): Copper: 60 -> 70, Lead: 70 -> 120, Silicon: 60 -> 0, Titanium: 0 → 35 (added)
(Now air factory request titanium in build cost. Necessary evil change which designed to delay first flare production without making them very weak. Also it delays mono production, which increase time window where ground T2 rush (atrax, mace) can be useful and not spammed with poly arc/wall spam (PvP), delays mono production in campaign, which suppose to learn novice how to optimise drills (give more time for novice to learn input water in drills (and atleast to not connect 999 drills to single conveyor (real))

**Reconstructors:**
- Additive Reconstructor: Reduced consumption (Silicon: 40 → 30, Graphite: 40 → 30)
(Decreased consumption for T2 factory. It was made because T2 feels to expensive for mass production and combining poly + other T2 productionm. Increases window when players could play with T1 and T2 (and combine T1 + T2 (like nova + mace/nova + dagger/...) before players get enough resourses for making first T3 units (and might even build orders for combining T2 + T3 (like atrax + spiroct in pvp). As counternerf, most T2 got small nerfs in stats (because decreased T2 cost and also mobility buff exist)
--------------------

## Walls
**Plastanium Wall:**
- health: 400 (2000) -> 620 (2560)
(Health was buffed due of making vela stronger and more mobility, also corvus attack was changed to burst, but with increased damage per attack)

**Phase Wall:**
- health: 600 (2400) → 720 (2880)
(Buffed because was accepted in community as very expensive wall which feels weaker than thorium (it has specific niche, being pretty useful against swarmer BvB, but still not enough good to be used as lategame wall)

**Surge Wall:**
- health: 920 (3680) -> 980 (3920)
(Slighty better than thorium wall, but comparing thorium (minable resourse) and surge (request massive amount of raw resourses and silicone) very rare used in real gameplay. Almost never used in pvp, good in pve only in lategame (where resourse amount doesn't matter and cost to much). I hope that this change gonna make it more useful and more often to be used. Keep in that menders healing walls in percentage, so I tried to avoid huge buffs (and making rush units (dagger tree) almost unable to get throught enemy defense))

**Scrap Wall:**
- health: 240 (960, 2160, 3840) -> 200 (800, 1800, 3200) 
(Being very cheap wall (request scrap which rarely used for eco production) has insanely big health amount. It wasn't my idea to nerf it, thans to artically (idk7 aaa //todo find nickname) for noticing

## Other non-turret blocks (real)
**Repair Turret**
- repairSpeed: 180/sec -> 90/sec
(insanely powerful turret which outclasses any possible unit healing ability. Most players just using this turret without water/coolant supply, just turret spam + overdrive. I hope that halving gonna make peoples use atleast water/cryo for making it better, also this change should make BvB tactics with repair turret spam weaker (as counterchange, units with healing ability got huge buffs, same with shield ability)

**Overdrive Projector**
- requirements (block buildcost): lead: 100 -> 210, titanium: 75 (no changes), silicon: 75 (no changes), plastanium: 30 -> 35
(Gonna make it slighty more expensive, increased lead cost in two times. This change can be discussed and probably removed (it doesn't make a big impact, just small useful change because why not))
             
**Overdrive Dome**
- requirements (block buildcost): lead: 200 -> 720, titanium: 130 -> 210, silicon: 130 -> 170, plastanium: 80 -> 175, surgeAlloy: 120 -> 275
- consumePower: 600 pu/sec -> 1500 pu/sec
(Ahhh, ODD spam, very controversible block in game. Some players wanna remove this block from game, some half its percentage boost and etc. Being very questionable block (and being ODD speedrun very powerful build order in competitve gamemodes. Nerfing percentage could one of most controversible change in any rebalance, so I did necessary evil. Increased its cost and energy consuption to delay ita placement and much harder to speedrun when (also keep in head that ODD doing turret in x2.5 times faster (and stronger), so its increase window for T1-T3 agressive micro/macro gameplay (before turrets get insanely powerful ODD/POD buffs)
            
**ShockMine**
- health: 50 -> 80 
- tendrils: 4 -> 7
- damage: 25 -> 15
(Small nerfs for landmines against armored units (T3+), it touches more competitive gameplay, in v7 was very abused to kill in few seconds huge T3 army (like fortress), in v8 new hitbox sizes makes landmine slighty weaker (but still powerful). Landmine kinda good (and even better than vanilla) against T1 and T2, but weaker to use on T3+ units which has big armor value)

## Effects
**Burning:**
- Added reload multiplier: 0.92
- Added speed multiplier: 0.92
- Transition damage: 8 → 12
(Burning effect was made for buffing mace to counter against atrax enjoiners (which using melting effect with even more insane multiplies). Also it makes turrets on pyratite ammo (hail, swarmer, ripple, spectre)

**Freezing:**
- Transition damage: 18 → 14
(Transition nerf to avoid new blast meta, btw its still good (because freezing gives very powerful multiplies)

**Wet:**
- Transition damage: 14 → 13
(Surge + water nerf)

## Turrets
### Duo
- copper: damage: 9 -> 12
- graphite: damage: 18 -> 21, reloadMultiplier = 0.7x
- silicon: damage: 12 -> 15

(Duo feels like a meme in community. Being weakest turret in game, people kinda like this turret (like "in duo we trust" (gwapo jumpscare)). v8 changes from anuke already buffing duo enough, so I fixed graphite ammo (was to strong), and small buffs because other units also got more buffs (and tried to make duo more useful in pvp)

### Scatter
- scrap: splashDamage: 33 -> 25, splashDamageRadius: 3.0 -> 4.1
- lead: splashDamage: 40 -> 22, splashDamageRadius: 1.8 -> 3.3
- metaglass: splashDamage: 45 -> 15*1.5, splashDamageRadius: 2.5 -> 3.3, fragBullets: 6 -> 4, reloadMultiplier: 0.8x -> 0.7x
  
- increased coolant buff (water: 140% -> 160%; cryofluid: 190% -> 235%)

(Nearly halved damage, but increased coolant boost. Scatter, being very cheap and easy to use, could easily kill T1-T3 in big amount, and even possible to kill or deal massive damage to few T4 in vanilla. My changes are designed to be useful against T1-T2, but against zenith spam it would request to use alteast a coolant (also flare hp was halfed, reduced hp for horizon and zenith)

### Scorch
- pyratite: damage: 60 -> 32; rangeChange: 0 -> +3 tiles 

- range: 7.5 -> 9

(Nerfed scorch, its not meta rn I guess. I hope that range increase could make it slighty more useful in PvP (coal scorch more designed against T1-T2 (up to mace), while pyra scorch can stop up to spiroct (can be extremely useful against early spiroct rush)

### Hail
- silicone: splashDamage: 33 -> 27
- pyratite: splashDamageRadius: 2.3 -> 2.6

(Slighty nerfed silicone ammo (it feels stronger than graphite and even pyratite (never misses). Did small buffs for pyra to make it more useful)

### Wave
- slag: rangeChange (+3 range)
- oil: rangeChange (+3 range)

(idk who gonna use it in real gameplay, but makes possible for map makera to do small trollings (set slag + oil as start defrnse or use it in enemy defenses). I thinking nobody would against it)

### Lancer
- damage: 140 -> 120
(Very strong turret (for consuming energy only). Reduced damage makes harder to spam enemy units with lancer spam (mostly in pvp)). Also damage was designed with new unit stats. Lancer can oneshot crawler and nova, but cant do that do daggers (so player must use pulsar shield as optional (to avoid getting oneshot))

### Arc
- damage: 20 -> 13
- reload = 1.71 -> 1.87
(Arc spam very powerful in PvP (and even PvE) which allows to spam T1-T3 ground with arc spam, now damage redcued to 13 to make armor more useful in game (low damage against T3+)

### Parallax
No changes /shrug

### Swarmer
- blastCompound: damage: 10 -> 13; splashdamage: 45 -> 27; splashDamageRadius: 3.7 -> 5.2
- pyratite: damage: (no changes); splashdamage: 45 -> 32; splashDamageRadius: 2.5 -> 5.5
- surgeAlloy: damage: 18 -> 15; splashDamageRadius: 3.1 -> 3.2

(Nerfing swarmer to not be a meta. Swarmer feels very strong turret which can be useful even against T5 (and insane for reactive damage spam). I thinking I could overbuff splashDamageRadius for pyratite and blast, so probably it can be nerfed (should be discussed). Also decreased blast splashDamage (becuase cryo + swarmer feels overpowered))


### Salvo
- copper: damage: 11 -> 13
- graphite: damage: 20 -> 25, reloadMultiplier = 0.7x
- pyratite: damage: 18 -> 22, splashDamage = 12 -> 15, splashDamageRadius = 2.7 -> 2.8, pierceCap = 2, knockback = 0.7
- silicon: damage: 15 -> 18, reloadMultiplier = 1.5 -> 1.3, knockback = 0.3
- thorium: damage: 29 -> 27, reloadMultiplier = 0.7x, knockback = 1.5, pierceCap = 2

- reload: 31f -> 32f
- requirements (block buildcost): copper = 100, graphite = 80 -> copper = 125, graphite = 70 //todo recheck change
- added inaccuracy (real) 

(Salvo feels kinda useless turret (out of PvE, and even in PvE can easily be changed to swarmer). Game has kinda funny pattern when non AoE turrets (duo, salvo, spectre) are much worse than turrets with splash. I did slightly reduced cost buff for graphite to make it easier to place, also added knockback for all ammo and double pierce to pyratite and thorium (as ammo which hardly to get (comparing with 3 other ammo types) to keep it slightly more useful against flying units (knockback more sensitive to flying units one) and pierce for motivation to use better ammo. To avoid overbuff (most ammo already got damage buff in couple with reduced buildcost), thorium ammo got slightly reduced damage (as counternerf because double pierce) and reduced firerate for thorium and graphite one. Adding inaccuracy feels a cosmetic change and can be removed (but shots now looks more beatiful)


### Segment
No changes (idk what to change)

## Tsunami
- slag: rangeChange (+4 range);
- cryofluid: rangeChange (+2.5 range);
- oil: rangeChange (+4 range);
  
(Small buffs for rarely used tsunamy ammo. Same idea as wave, except cryo buff (to keep blast cyclone/swarmer weaker than surge versions one, but with synergy to be much better (blast swarmer/cyclone < surge swarmer/cyclone, but cryo tsunamy + blast >> water tsunamy + surge)

### Fuse
No changes (idk what to change, turret feels already fine (atleast gonna strongly outclass scorch (because it got nerfed))


## Ripple
- graphite: splashDamage: 70 -> 75, splashRadius: 0.8 -> 1.2;
- silicone: splashDamage: 70 -> 72;
- plastanium: splashDamage: 45 -> 70, fragBullets: 10 -> 7, fragBulletDamage (idk how it names): 14 -> 15;
- blastCompound: splashDamage: 55 -> 115, splashDamageRadius = 4.2 -> 5.2, reloadMultiplier: 0.7x;
- pyratite: splashDamage: 45 -> 90, splashDamageRadius = 2.3 -> 4.7;
- requirements (block buildcost): copper = 150, graphite = 135, titanium = 60 -> copper = 175, graphite = 90, titanium = 70;

(Ripple also feels kinda weird. It has splashdamage, but rarely used in actual gameplay (because kinda expensive and ineffectice for pvp (except plast bvb and defending against naval (still sucks but its better than foreshadow against T2 fr)(and kinda questionable for pve becsuse cyclone/swarmer exist). Anuke halfed its firerate with doubled damage, so I did something like this too. Might need some revieves because can be actually very strong even against T5 (I dont thinking thats bad, but need more tests). Nerfed plast frags because very unfairly strong and annoying in bvb)

### Cyclone
- metaglass: damage: 6 -> 13, ammoMultiplier: 2x -> 5x, splashDamage: 45 -> 32, fragBullets: 4 -> 10
- blastCompound: damage: 8 -> 12, splashDamage: 45 -> 55
- plastanium: damage: 8 -> 15, splashDamage: 37 -> 35
- surgeAlloy: splashDamageRadius = 4.7 -> 4.3

- coolant: 0.3f -> 0.2f ((decreased coolant buff (water: 160% -> 140%; cryofluid: 235% -> 190%))

(Decrease coolant buff (To keep turret more designed for ammo-save and compensate decreased firerate by being more anti-armor turret). Did huge metaglass ammo buff (metaglass scatter DPS unironically was bigger than metaglass cyclone). Now metaglass can be used as best ammo for shredding annoying T1 unit spam/units with low armor (up to zenith), ye (cheap ammo with 5x ammo multiple). Minor balance changes for plastanium and surge ammo. Buffed blast ammo to be more useful (while surge cyclone by itself) 

### Foreshadow
- reload: 0.3 -> 0.22 /todo
- damage: 1350 -> 1250
- increased coolant buff (water: 116% -> 140%; cryofluid: 136% -> 190%)

(Minor foreshadow nerf. Reduced firerate, but increased coolant boost (so if player wanna keep foreshadow shoot at full power, he must to do something with stable liquid supply too (vanilla coolant power making it useless or make a very low sense))


### Spectre
- graphite: damage: 50 -> 75, ammoMultiplier: 4 -> 3, knockback: 0.3 -> 1
- thorium: damage: 80 -> 120, knockback: 0.7 -> 1.3, ammoMultiplier: 4 -> 2
- pyratite: damage: 70 -> 110, splashDamage: 20 -> 32, knockback: 0.6 -> 0.7

- reload: 8.57 -> 7.5
- increased coolant buff (water: 116% -> 140%; cryofluid: 136% -> 190%)
- range: 32.5 -> 34.1

(Kinda fine turret for PvE but still worse than cyclone/swarmer + tsunamy combo. Huge damage buff for all ammo. Spcetre got slighty reduced firerate which compensated by increased coolant power. Decreased ammoMultiplier to keep it more cost for shooting (I thinking its pretty fair for a lategame. ~~Unlimited~~ Huge power but big difficulty for ammo and water supply). Also did slighty increased range (also to keep this turret way more useful. Doesn't break any maps)

### Meltdown
- damage: 936/sec -> 1560/sec
- reload: 90f -> 170f,
- firerate: 0.666 -> 0.352

(Since meltdown can be boostable (with overdrive) in v8, it gets extremely powerful and overpowered. Also meltdown with cryo and ODD (and even POD) shooting and reloading extremely fast, so it looks very comical (extremely fast rotarespeed and almost non-stop fire))

//todo finish documentation tomorrow

---

## Units (headpain)
### Dagger
- Speed: 3.75 → 4.8
- Health: 150 → 120
- Armor: 0 -> 2
- Firerate: 2.3 -> 1.5
- Damage: 9 → 12

### Mace
- Speed: 3.75 → 4.575
- Health: 550 → 510
- Armor: 4 → 5
- Damage: 74 → 54

### Fortress
- Speed: 3.225 → 3.75
- Health: 900 → 910
- Armor: 9 → 10
- Spldashdamage: 80 → 70
- Increased fortress speed projectile. Decreased range from 29.5 -> 28.8
  
### Scepter
- Speed: 2.7 → 3.9
- Health: 9000 → 9100
- Armor: 10 → 14
- Small bullet damage: 10 → 23
- Main bullet damage: 80 → 70
- Range: 26.5 -> 21.5

### Reign
- Speed: 3 → 3.6
- Health: 24000 → 25000
- Armor: 18 → 27
- Range: 23.8 -> 27.2

---------------------------

### Nova
- Speed: 4.125 → 5.1
- Health: 120 → 90
- Armor: 1 → 3 
- Damage: 13 → 15

- Build speed: 30% -> 50%
- Strongly buffed repair field (2.5/sec -> 6.66/sec)

### Pulsar
- Speed: 5.25 → 5.49
- Health: 320 → 290
- Mine speed: 300% -> 350%
- Heal weapon bullet:
  - Heal percent: 2% → 1%
- Lightning bullet:
  - Heal percent: 1.6% → 0.75%

### Quasar
- Health: 640 → 750
- Armor: 9 → 8
- Speed: 3.75 → 4.01
- Range: 18.2 -> 16.3
  
- Force Field Ability:
  - Regen: 24/sec → 18/sec
  - Max: 400 → 512
  - Cooldown: 6 seconds → 3 seconds
- Beam weapon laser bullet:
  - Damage: 45 → 52
  - Heal percent: 10% → 5%
  - Added heal amount: 35

### Vela
- Speed: 3.3 → 4.2 (slighty decreased boost multiplier)
- Health: 8200 → 7800
- Armor: 9 → 13
- Repair beam speed: 84/sec (*2) → 116/sec (*2)

### Corvus
- Health: 18000 → 12000
- Armor: 9 → 12

- Weapon now fires 3 shots with short delay between shots (instead single beam)
  
- Laser damage (per shot): 560 → 74 (or 74 * 3 * 3 = 666 damage per burst (damage to buildings))
- Added building damage multiplier: 3x
- Heal percentage: 25% → 18%

---------------------------

### Crawler
- Health: 150 → 120
- Speed: 7.5 -> 6

### Atrax**
- Speed: 4.5 → 6.3
- Health: 600 → 370

### Spiroct
- Speed: 4.05 → 6.21
- Health: 1000 → 1100
- Armor: 5 → 7

### Arkyid
- Speed: 4.65 → 6.9
- Health: 8000 → 7800
- Armor: 6 → 12
- (Artillery weapon) Splashdamage: 65 → 75

### Toxopid
- Speed: 3.75 → 5.1
- Health: 22000 → 21000
- Armor: 13 → 18
- Shrapnel (fuse-like) weapon damage: 110 → 180
- Artillery (long cannon) weapon damage: 50 → 110

---------------------------

### Flare
- Health: 70 → 35
- Firerate: 1.5 -> 2
- Bullet damage: 9 → 13 //todo finish changelog for flare (new ai and new changes ahh jumpscare. Spend much more time on testing flares tomorrow to be sure that all my changes are make a sense)

### Horizon
- Health: 340 → 240
- Speed: 12.375 → 16.5
- Damage (splashdamage): 27 → 23

### Zenith
- Health: 700 → 510
- Range: 19.6 -> 21.8

### Antumbra
- Speed: 6 → 5.4
- Health: 7200 → 8100
- Armor: 9 → 8
- Range: 21.3 -> 28.2
- Missile damage: 18 → 23
- Missile splash radius: 2.5 → 4.1

### Eclipse
- Speed: 4.05 → 4.95
- Armor: 13 → 17
- Range: 28.2 -> 29
  
**Flak bullet**:
- Damage: 15 → 35

**Laser weapon**:
- Damage: 115 → 130

---------------------------

### Mono
no changes

### Poly
- Health: 400 → 170
- Armor: 0 → 1
- Build speed: 50% -> 30%
- buffed repair field (0.62/sec -> 2.67/sec)
- Heal percent: 5.5% → 3%
- healAmount: added 7.5

### Mega
- Health: 460 → 380
- Armor: 3 → 2
- Speed: 18.75 → 19.2
- HealPercent 3% → 2%
- HealAmount: added 5.5

### Quad
- Build speed: 250% → 350%

### Oct
- Armor: 16 → 15
- Health: 24000 → 18000
- Force field ability: regen 240/sec → 600/sec, max 7000 → 12000
- Repair field ability: regen 65/sec -> 350/sec

---------------------------

### Risso
- Speed: 8.25 -> 7.72
- Health: 280 -> 220

### Minke
- Health: 600 -> 370
- Speed: 6.75 -> 6.15
- Range: 31 -> 24.6

### Bryde
- Health: 910 -> 730
- Speed: 6.37 -> 5.85
- Range: 31 -> 29.5

### Sei
- Armor: 12 -> 15
- Speed: 5.47 -> 5.7
- Range: 35.2 -> 31.4

### Omura
- Health: 22000 -> 21000
- Range: 62 -> 50.7
- Speed: 4.65 -> 3.97

---------------------------

### Retusa
Returned metaglass in its cost

### Oxynoe
Decreased segment-like attack firerate

### Cyerce
- Health: 870 -> 720
- Armor: 6 -> 8
- RepairSpeed: 42 (*2) -> 34 (*2)
- Speed: 6.45 -> 5.81
  
### Aegires
- Health: 12000 -> 9000
- Speed: 5.25 -> 5.62
  
### Navanax
- Health: 20000 -> 23000
- Armor: 16 -> 23
- Speed: 4.87 -> 5.55

- Changed main cannon firerate (0.46/sec -> 0.81/sec), damage (60 -> 30), splashdamage (70 -> 210), range (37 -> 34.5)
- Minor range and firerate buff for scorch-like cannons

---------------------------

# TODO list
- Finish documentation 
- Slowly increase servers with new gamemodes to playtest new changes
- Finish data-patcher (and open servers with data-patcher so player wont need to install custom jar)
- Create a list with all playtestes/those who help me/etc
- Create a guide for map makers with all possible tactics which player can use in attack maps and how design maps to be beated in different ways (like fortress + quasar, or use scepter + antumbra and etc)
- ...

---------------------------
