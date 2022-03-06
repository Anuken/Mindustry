package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.Pathfinder.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.ammo.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static arc.graphics.g2d.Draw.*;
import static mindustry.Vars.*;

//TODO document
public class UnitType extends UnlockableContent{
    public static final float shadowTX = -12, shadowTY = -13;
    private static final Vec2 legOffset = new Vec2();

    /** If true, the unit is always at elevation 1. */
    public boolean flying;
    /** Creates a new instance of this unit class. */
    public Prov<? extends Unit> constructor;

    /** Environmental flags that are *all* required for this unit to function. 0 = any environment */
    public int envRequired = 0;
    /** The environment flags that this unit can function in. If the env matches any of these, it will be enabled. */
    public int envEnabled = Env.terrestrial;
    /** The environment flags that this unit *cannot* function in. If the env matches any of these, it will explode or be disabled. */
    public int envDisabled = Env.scorching;

    public float speed = 1.1f, boostMultiplier = 1f, rotateSpeed = 5f, baseRotateSpeed = 5f;
    public float drag = 0.3f, accel = 0.5f, landShake = 0f, rippleScale = 1f, riseSpeed = 0.08f, fallSpeed = 0.018f;
    public float health = 200f, range = -1, miningRange = 70f, armor = 0f, maxRange = -1f, buildRange = Vars.buildingRange;
    public float crashDamageMultiplier = 1f;
    public boolean targetAir = true, targetGround = true;
    public boolean faceTarget = true, rotateShooting = true, isCounted = true, lowAltitude = false, circleTarget = false;
    public boolean canBoost = false;
    public boolean logicControllable = true;
    public boolean playerControllable = true;
    public boolean allowedInPayloads = true;
    /** TODO If true, core units will re-appear on this unit when respawning. */
    public boolean coreUnitDock = false;
    public boolean createWreck = true;
    public boolean createScorch = true;
    public boolean useUnitCap = true;
    public boolean destructibleWreck = true;
    /** If true, this modded unit always has a -outline region generated for its base. Normally, outlines are ignored if there are no top = false weapons. */
    public boolean alwaysCreateOutline = false;
    /** If true, this unit has a square shadow. TODO physics? */
    public boolean squareShape = false;
    public float groundLayer = Layer.groundUnit;
    public float payloadCapacity = 8;
    public float aimDst = -1f;
    public float buildBeamOffset = 3.8f;
    /** WIP: Units of low priority will always be ignored in favor of those with higher priority, regardless of distance. */
    public float targetPriority = 0f;
    /** If false, this unit is not targeted by anything. */
    public boolean targetable = true;
    public boolean drawBuildBeam = true;
    public boolean rotateToBuilding = true;
    public float commandRadius = 150f;
    public float visualElevation = -1f;
    /** If true and this is a legged unit, this unit can walk over blocks. */
    public boolean allowLegStep = false;
    /** If true, this unit cannot drown, and will not be affected by the floor under it. */
    public boolean hovering = false;
    public boolean omniMovement = true;
    /** If true, the unit faces its moving direction before actually moving. */
    public boolean rotateMoveFirst = false;
    public boolean showHeal = true;
    public Color healColor = Pal.heal;
    public Effect fallEffect = Fx.fallSmoke;
    public Effect fallThrusterEffect = Fx.fallSmoke;
    public Effect deathExplosionEffect = Fx.dynamicExplosion;
    public @Nullable Effect treadEffect;
    /** Extra (usually animated) parts */
    public Seq<DrawPart> parts = new Seq<>(DrawPart.class);
    public Seq<Ability> abilities = new Seq<>();
    /** Flags to target based on priority. Null indicates that the closest target should be found. The closest enemy core is used as a fallback. */
    public BlockFlag[] targetFlags = {null};
    /** Target items to mine. Used in MinerAI */
    public Seq<Item> mineItems = Seq.with(Items.copper, Items.lead, Items.titanium, Items.thorium);

    //TODO different names for these fields.
    /** The default AI controller to assign on creation. */
    public Prov<? extends UnitController> aiController = () -> !flying ? new GroundAI() : new FlyingAI();
    /** Function that chooses AI controller based on unit entity. */
    public Func<Unit, ? extends UnitController> defaultController = u -> !playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? aiController.get() : new CommandAI();

    public Color outlineColor = Pal.darkerMetal;
    public int outlineRadius = 3;
    public boolean outlines = true;

    public int legCount = 4, legGroupSize = 2;
    public float legLength = 10f, legSpeed = 0.1f, legTrns = 1f, legBaseOffset = 0f, legMoveSpace = 1f, legExtension = 0, legPairOffset = 0, legLengthScl = 1f, kinematicScl = 1f, maxStretch = 1.75f, maxCompress = 0f;
    public float legSplashDamage = 0f, legSplashRange = 5;
    public float legStraightLength = 1f;
    /** If true, legs are locked to the base of the unit instead of being on an implicit rotating "mount". */
    public boolean lockLegBase = false, legContinuousMove;
    public float baseLegStraightness, legStraightness;
    /** TODO neither of these appear to do much */
    public boolean flipBackLegs = true, flipLegSide = false;

    public float mechSideSway = 0.54f, mechFrontSway = 0.1f;
    public float mechStride = -1f;
    public float mechStepShake = -1f;
    public boolean mechStepParticles = false;
    public Color mechLegColor = Pal.darkMetal;

    public Rect[] treadRects = {};
    public int treadFrames = 18;
    public int treadPullOffset = 0;

    public int itemCapacity = -1;
    public int ammoCapacity = -1;
    public AmmoType ammoType = new ItemAmmoType(Items.copper);
    public int mineTier = -1;
    public boolean mineWalls = false, mineFloor = true;
    public boolean mineHardnessScaling = true;
    public float buildSpeed = -1f, mineSpeed = 1f;
    public Sound mineSound = Sounds.minebeam;
    public float mineSoundVolume = 0.6f;

    //missiles only!
    public float lifetime = 60f * 5f;
    public float homingDelay = 10f;

    /** This is a VERY ROUGH estimate of unit DPS. */
    public float dpsEstimate = -1;
    public float clipSize = -1;
    public boolean canDrown = true, naval = false;
    public float drownTimeMultiplier = 1f;
    public float engineOffset = 5f, engineSize = 2.5f;
    public @Nullable Color engineColor = null;
    public Color engineColorInner = Color.white;
    public Seq<UnitEngine> engines = new Seq<>();
    public float strafePenalty = 0.5f;
    /** If false, this unit does not physically collide with others. */
    public boolean physics = true;
    public float hitSize = 6f;
    public float itemOffsetY = 3f;
    public float lightRadius = -1f, lightOpacity = 0.6f;
    /** Fog view radius in tiles. <0 for automatic radius. */
    public float fogRadius = -1f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true, drawShields = true, drawBody = true;
    public int trailLength = 0;
    public float researchCostMultiplier = 50;
    public float trailX = 4f, trailY = -3f, trailScl = 1f;
    /** Whether the unit can heal blocks. Initialized in init() */
    public boolean canHeal = false;
    /** If true, all weapons will attack the same target. */
    public boolean singleTarget = false;
    public boolean forceMultiTarget = false;
    /** If false, this unit has no weapons that can attack. */
    public boolean canAttack = true;
    public boolean hidden = false;
    public boolean internal = false;
    /** Function used for calculating cost of moving with ControlPathfinder. Does not affect "normal" flow field pathfinding. */
    public @Nullable PathCost pathCost;
    /** A sample of the unit that this type creates. Do not modify! */
    public @Nullable Unit sample;

    //for crawlers
    public int segments = 0;
    public float segmentSpacing = 2f;
    public float segmentScl = 4f, segmentPhase = 5f;
    public float segmentRotSpeed = 1f, segmentMaxRot = 30f;
    public float crawlSlowdown = 0.5f;
    /** Damage dealt to blocks under this tank/crawler every frame. */
    public float crushDamage = 0f;
    public float crawlSlowdownFrac = 0.55f;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Seq<Weapon> weapons = new Seq<>();
    public TextureRegion baseRegion, legRegion, region, shadowRegion, cellRegion,
        softShadowRegion, jointRegion, footRegion, legBaseRegion, baseJointRegion, outlineRegion, treadRegion;
    public TextureRegion[] wreckRegions, segmentRegions, segmentOutlineRegions;
    public TextureRegion[][] treadRegions;

    protected float buildTime = -1f;
    protected @Nullable ItemStack[] totalRequirements, cachedRequirements, firstRequirements;

    public UnitType(String name){
        super(name);

        constructor = EntityMapping.map(this.name);
    }

    public UnitController createController(Unit unit){
        return defaultController.get(unit);
    }

    public Unit create(Team team){
        Unit unit = constructor.get();
        unit.team = team;
        unit.setType(this);
        unit.ammo = ammoCapacity; //fill up on ammo upon creation
        unit.elevation = flying ? 1f : 0;
        unit.heal();
        if(unit instanceof TimedKillc u){
            u.lifetime(lifetime);
        }
        return unit;
    }

    public Unit spawn(Team team, float x, float y){
        Unit out = create(team);
        out.set(x, y);
        out.add();
        return out;
    }

    public Unit spawn(float x, float y){
        return spawn(state.rules.defaultTeam, x, y);
    }

    public Unit spawn(Team team, Position pos){
        return spawn(team, pos.getX(), pos.getY());
    }

    public Unit spawn(Position pos){
        return spawn(state.rules.defaultTeam, pos);
    }

    public boolean hasWeapons(){
        return weapons.size > 0;
    }

    public void update(Unit unit){

    }

    public void landed(Unit unit){}

    public void display(Unit unit, Table table){
        table.table(t -> {
            t.left();
            t.add(new Image(uiIcon)).size(iconMed).scaling(Scaling.fit);
            t.labelWrap(localizedName).left().width(190f).padLeft(5);
        }).growX().left();
        table.row();

        table.table(bars -> {
            bars.defaults().growX().height(20f).pad(4);

            //TODO overlay shields
            bars.add(new Bar("stat.health", Pal.health, unit::healthf).blink(Color.white));
            bars.row();

            if(state.rules.unitAmmo){
                bars.add(new Bar(ammoType.icon() + " " + Core.bundle.get("stat.ammo"), ammoType.barColor(), () -> unit.ammo / ammoCapacity));
                bars.row();
            }

            for(Ability ability : unit.abilities){
                ability.displayBars(unit, bars);
            }

            if(payloadCapacity > 0 && unit instanceof Payloadc payload){
                bars.add(new Bar("stat.payloadcapacity", Pal.items, () -> payload.payloadUsed() / unit.type().payloadCapacity));
                bars.row();

                var count = new float[]{-1};
                bars.table().update(t -> {
                    if(count[0] != payload.payloadUsed()){
                        payload.contentInfo(t, 8 * 2, 270);
                        count[0] = payload.payloadUsed();
                    }
                }).growX().left().height(0f).pad(0f);
            }
        }).growX();

        if(unit.controller() instanceof LogicAI){
            table.row();
            table.add(Blocks.microProcessor.emoji() + " " + Core.bundle.get("units.processorcontrol")).growX().wrap().left();
            table.row();
            table.label(() -> Iconc.settings + " " + (long)unit.flag + "").color(Color.lightGray).growX().wrap().left();
        }
        
        table.row();
    }

    /** @return whether this block supports a specific environment. */
    public boolean supportsEnv(int env){
        return (envEnabled & env) != 0 && (envDisabled & env) == 0 && (envRequired == 0 || (envRequired & env) == envRequired);
    }

    public boolean isBanned(){
        return state.rules.bannedUnits.contains(this);
    }

    @Override
    public void getDependencies(Cons<UnlockableContent> cons){
        //units require reconstructors being researched
        for(Block block : content.blocks()){
            if(block instanceof Reconstructor r){
                for(UnitType[] recipe : r.upgrades){
                    //result of reconstruction is this, so it must be a dependency
                    if(recipe[1] == this){
                        cons.get(block);
                    }
                }
            }
        }

        for(ItemStack stack : researchRequirements()){
            cons.get(stack.item);
        }
    }

    @Override
    public boolean isHidden(){
        return hidden;
    }

    @Override
    public void setStats(){
        stats.add(Stat.health, health);
        stats.add(Stat.armor, armor);
        stats.add(Stat.speed, speed * 60f / tilesize, StatUnit.tilesSecond);
        stats.add(Stat.size, StatValues.squared(hitSize / tilesize, StatUnit.blocksSquared));
        stats.add(Stat.itemCapacity, itemCapacity);
        stats.add(Stat.range, (int)(maxRange / tilesize), StatUnit.blocks);

        if(abilities.any()){
            var unique = new ObjectSet<String>();

            for(Ability a : abilities){
                if(unique.add(a.localized())){
                    stats.add(Stat.abilities, a.localized());
                }
            }
        }

        stats.add(Stat.flying, flying);

        if(!flying){
            stats.add(Stat.canBoost, canBoost);
        }

        if(mineTier >= 1){
            stats.addPercent(Stat.mineSpeed, mineSpeed);
            stats.add(Stat.mineTier, StatValues.blocks(b -> b instanceof Floor f && f.itemDrop != null && f.itemDrop.hardness <= mineTier && (!f.playerUnmineable || Core.settings.getBool("doubletapmine"))));
        }
        if(buildSpeed > 0){
            stats.addPercent(Stat.buildSpeed, buildSpeed);
        }
        if(sample instanceof Payloadc){
            stats.add(Stat.payloadCapacity, StatValues.squared(Mathf.sqrt(payloadCapacity / (tilesize * tilesize)), StatUnit.blocksSquared));
        }

        var reqs = getFirstRequirements();

        if(reqs != null){
            stats.add(Stat.buildCost, StatValues.items(reqs));
        }

        if(weapons.any()){
            stats.add(Stat.weapons, StatValues.weapons(this, weapons));
        }

        if(immunities.size > 0){
            var imm = immunities.toSeq().sort();
            //it's redundant to list wet for naval units
            if(naval){
                imm.remove(StatusEffects.wet);
            }
            for(var i : imm){
                stats.add(Stat.immunities, i.emoji() + " " + i.localizedName);
            }
        }
    }

    @CallSuper
    @Override
    public void init(){
        if(constructor == null) throw new IllegalArgumentException("no constructor set up for unit '" + name + "'");

        Unit example = constructor.get();

        allowLegStep = example instanceof Legsc;

        //water preset
        if(example instanceof WaterMovec){
            naval = true;
            canDrown = false;
            omniMovement = false;
            immunities.add(StatusEffects.wet);
            if(visualElevation < 0f){
                visualElevation = 0.11f;
            }
        }

        if(pathCost == null){
            pathCost =
                example instanceof WaterMovec ? ControlPathfinder.costNaval :
                allowLegStep ? ControlPathfinder.costLegs :
                ControlPathfinder.costGround;
        }

        if(flying){
            envEnabled |= Env.space;
        }

        if(lightRadius == -1){
            lightRadius = Math.max(60f, hitSize * 2.3f);
        }

        clipSize = Math.max(clipSize, lightRadius * 1.1f);
        singleTarget = weapons.size <= 1 && !forceMultiTarget;

        if(itemCapacity < 0){
            itemCapacity = Math.max(Mathf.round((int)(hitSize * 4f), 10), 10);
        }

        //assume slight range margin
        float margin = 4f;

        //set up default range
        if(range < 0){
            range = Float.MAX_VALUE;
            for(Weapon weapon : weapons){
                range = Math.min(range, weapon.range() - margin);
                maxRange = Math.max(maxRange, weapon.range() - margin);
            }
        }

        if(maxRange < 0){
            maxRange = Math.max(0f, range);

            for(Weapon weapon : weapons){
                maxRange = Math.max(maxRange, weapon.range() - margin);
            }
        }

        if(fogRadius < 0){
            fogRadius = Math.max(11f * 2.3f * 3f, hitSize * 2f) / 8f;
        }

        if(weapons.isEmpty()){
            range = maxRange = miningRange;
        }

        if(mechStride < 0){
            mechStride = 4f + (hitSize -8f)/2.1f;
        }

        if(aimDst < 0){
            aimDst = weapons.contains(w -> !w.rotate) ? hitSize * 2f : hitSize / 2f;
        }

        if(mechStepShake < 0){
            mechStepShake = Mathf.round((hitSize - 11f) / 9f);
            mechStepParticles = hitSize > 15f;
        }

        if(treadEffect == null){
            treadEffect = new Effect(50, e -> {
                color(Tmp.c1.set(e.color).mul(1.5f));
                Fx.rand.setSeed(e.id);
                for(int i = 0; i < 3; i++){
                    Fx.v.trns(e.rotation + Fx.rand.range(40f), Fx.rand.random(6f * e.finpow()));
                    Fill.circle(e.x + Fx.v.x + Fx.rand.range(4f), e.y + Fx.v.y + Fx.rand.range(4f), Math.min(e.fout(), e.fin() * e.lifetime / 8f) * hitSize / 28f * 3f * Fx.rand.random(0.8f, 1.1f) + 0.3f);
                }
            }).layer(Layer.debris);
        }

        for(Ability ab : abilities){
            ab.init(this);
        }

        canHeal = weapons.contains(w -> w.bullet.heals());

        //add mirrored weapon variants
        Seq<Weapon> mapped = new Seq<>();
        for(Weapon w : weapons){
            if(w.recoilTime < 0) w.recoilTime = w.reload;
            mapped.add(w);

            //mirrors are copies with X values negated
            if(w.mirror){
                Weapon copy = w.copy();
                copy.flip();
                mapped.add(copy);

                //since there are now two weapons, the reload and recoil time must be doubled
                w.recoilTime *= 2f;
                copy.recoilTime *= 2f;
                w.reload *= 2f;
                copy.reload *= 2f;

                w.otherSide = mapped.size - 1;
                copy.otherSide = mapped.size - 2;
            }
        }
        this.weapons = mapped;

        weapons.each(Weapon::init);

        canAttack = weapons.contains(w -> !w.noAttack);

        //dynamically create ammo capacity based on firing rate
        if(ammoCapacity < 0){
            float shotsPerSecond = weapons.sumf(w -> w.useAmmo ? 60f / w.reload : 0f);
            //duration of continuous fire without reload
            float targetSeconds = 35;

            ammoCapacity = Math.max(1, (int)(shotsPerSecond * targetSeconds));
        }

        //calculate estimated DPS for one target based on weapons
        if(dpsEstimate < 0){
            dpsEstimate = weapons.sumf(Weapon::dps);

            //suicide enemy
            if(weapons.contains(w -> w.bullet.killShooter)){
                //scale down DPS to be insignificant
                dpsEstimate /= 25f;
            }
        }

        //only do this after everything else was initialized
        sample = constructor.get();
    }

    @CallSuper
    @Override
    public void load(){
        super.load();

        for(var part : parts){
            part.load(name);
        }
        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        legRegion = Core.atlas.find(name + "-leg");
        jointRegion = Core.atlas.find(name + "-joint");
        baseJointRegion = Core.atlas.find(name + "-joint-base");
        footRegion = Core.atlas.find(name + "-foot");
        treadRegion = Core.atlas.find(name + "-treads");
        if(treadRegion.found()){
            treadRegions = new TextureRegion[treadRects.length][treadFrames];
            for(int r = 0; r < treadRects.length; r++){
                for(int i = 0; i < treadFrames; i++){
                    treadRegions[r][i] = Core.atlas.find(name + "-treads" + r + "-" + i);
                }
            }
        }
        legBaseRegion = Core.atlas.find(name + "-leg-base", name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));
        //when linear filtering is on, it's acceptable to use the relatively low-res 'particle' region
        softShadowRegion =
            squareShape ? Core.atlas.find("square-shadow") :
            hitSize <= 10f || (Core.settings != null && Core.settings.getBool("linear", true)) ?
                Core.atlas.find("particle") :
                Core.atlas.find("circle-shadow");

        outlineRegion = Core.atlas.find(name + "-outline");
        shadowRegion = fullIcon;

        wreckRegions = new TextureRegion[3];
        for(int i = 0; i < wreckRegions.length; i++){
            wreckRegions[i] = Core.atlas.find(name + "-wreck" + i);
        }

        segmentRegions = new TextureRegion[segments];
        segmentOutlineRegions = new TextureRegion[segments];
        for(int i = 0; i < segments; i++){
            segmentRegions[i] = Core.atlas.find(name + "-segment" + i);
            segmentOutlineRegions[i] = Core.atlas.find(name + "-segment-outline" + i);
        }

        clipSize = Math.max(region.width * 2f, clipSize);
    }

    public void getRegionsToOutline(Seq<TextureRegion> out){
        for(Weapon weapon : weapons){
            for(var part : weapon.parts){
                part.getOutlines(out);
            }
        }
        for(var part : parts){
            part.getOutlines(out);
        }
    }

    public boolean needsBodyOutline(){
        return alwaysCreateOutline;
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        var toOutline = new Seq<TextureRegion>();
        getRegionsToOutline(toOutline);

        for(var region : toOutline){
            if(region instanceof AtlasRegion atlas){
                String regionName = atlas.name;
                Pixmap outlined = Pixmaps.outline(Core.atlas.getPixmap(region), outlineColor, outlineRadius);

                if(Core.settings.getBool("linear", true)) Pixmaps.bleed(outlined);

                packer.add(PageType.main, regionName + "-outline", outlined);
            }
        }

        //currently does not create outlines for legs or base regions due to older mods having them outlined by default
        if(outlines){

            //outlines only created when forced at the moment
            makeOutline(PageType.main, packer, region, alwaysCreateOutline, outlineColor, outlineRadius);

            for(Weapon weapon : weapons){
                if(!weapon.name.isEmpty()){
                    makeOutline(PageType.main, packer, weapon.region, true, outlineColor, outlineRadius);
                }
            }
        }
    }

    /** @return the time required to build this unit, as a value that takes into account reconstructors */
    public float getBuildTime(){
        getTotalRequirements();
        return buildTime;
    }

    /** @return all items needed to build this unit, including reconstructor steps. */
    public ItemStack[] getTotalRequirements(){
        if(totalRequirements == null){
            UnitType[] ret = {null};
            float[] timeret = {0f};
            ItemStack[] result = getRequirements(ret, timeret);

            //prevents stack overflow if requirements are circular and result != null
            totalRequirements = ItemStack.empty;

            if(result != null){
                ItemSeq total = new ItemSeq();

                total.add(result);
                if(ret[0] != null){
                    total.add(ret[0].getTotalRequirements());
                }
                totalRequirements = total.toArray();
            }

            for(var stack : totalRequirements){
                buildTime += stack.item.cost * stack.amount;
            }
        }
        return totalRequirements;
    }

    /** @return item requirements based on reconstructors or factories found; returns previous unit in array if provided */
    public @Nullable ItemStack[] getRequirements(@Nullable UnitType[] prevReturn, @Nullable float[] timeReturn){
        //find reconstructor
        var rec = (Reconstructor)content.blocks().find(b -> b instanceof Reconstructor re && re.upgrades.contains(u -> u[1] == this));

        if(rec != null && Structs.find(rec.consumers, i -> i instanceof ConsumeItems) instanceof ConsumeItems ci){
            if(prevReturn != null){
                prevReturn[0] = rec.upgrades.find(u -> u[1] == this)[0];
            }
            if(timeReturn != null){
                timeReturn[0] = rec.constructTime;
            }
            return ci.items;
        }else{
            //find a factory
            var factory = (UnitFactory)content.blocks().find(u -> u instanceof UnitFactory uf && uf.plans.contains(p -> p.unit == this));
            if(factory != null){

                var plan = factory.plans.find(p -> p.unit == this);
                if(timeReturn != null){
                    timeReturn[0] = plan.time;
                }
                return plan.requirements;
            }else{
                //find an assembler
                var assembler = (UnitAssembler)content.blocks().find(u -> u instanceof UnitAssembler a && a.plans.contains(p -> p.unit == this));
                if(assembler != null){
                    var plan = assembler.plans.find(p -> p.unit == this);

                    if(timeReturn != null){
                        timeReturn[0] = plan.time;
                    }
                    ItemSeq reqs = new ItemSeq();
                    for(var bstack : plan.requirements){
                        if(bstack.item instanceof Block block){
                            for(var stack : block.requirements){
                                reqs.add(stack.item, stack.amount * bstack.amount);
                            }
                        }else if(bstack.item instanceof UnitType unit){
                            for(var stack : unit.getTotalRequirements()){
                                reqs.add(stack.item, stack.amount * bstack.amount);
                            }
                        }
                    }
                    return reqs.toArray();
                }
            }
        }
        return null;
    }

    public @Nullable ItemStack[] getFirstRequirements(){
        if(firstRequirements == null){
            firstRequirements = getRequirements(null, null);
        }
        return firstRequirements;
    }

    @Override
    public ItemStack[] researchRequirements(){
        if(cachedRequirements != null){
            return cachedRequirements;
        }

        ItemStack[] stacks = getRequirements(null, null);

        if(stacks != null){
            ItemStack[] out = new ItemStack[stacks.length];
            for(int i = 0; i < out.length; i++){
                out[i] = new ItemStack(stacks[i].item, UI.roundAmount((int)(Math.pow(stacks[i].amount, 1.1) * researchCostMultiplier)));
            }

            //remove zero-requirements for automatic unlocks
            out = Structs.filter(ItemStack.class, out, stack -> stack.amount > 0);

            cachedRequirements = out;

            return out;
        }

        return super.researchRequirements();
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    /** Sets up engines, mirroring the contents of the specified array. */
    public void setEnginesMirror(UnitEngine... array){
        for(var base : array){
            engines.add(base);

            var engine = base.copy();
            engine.x *= -1;
            engine.rotation = 180f - engine.rotation;
            if(engine.rotation < 0) engine.rotation += 360f;
            engines.add(engine);
        }
    }

    //region drawing

    public void draw(Unit unit){
        if(unit.inFogTo(Vars.player.team())) return;

        Mechc mech = unit instanceof Mechc ? (Mechc)unit : null;
        float z = unit.elevation > 0.5f ? (lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);

        if(unit.controller().isBeingControlled(player.unit())){
            drawControl(unit);
        }

        if(unit.isFlying() || visualElevation > 0){
            Draw.z(Math.min(Layer.darkness, z - 1f));
            drawShadow(unit);
        }

        Draw.z(z - 0.02f);

        if(mech != null){
            drawMech(mech);

            //side
            legOffset.trns(mech.baseRotation(), 0f, Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f/Mathf.PI, 1) * mechSideSway, 0f, unit.elevation));

            //front
            legOffset.add(Tmp.v1.trns(mech.baseRotation() + 90, 0f, Mathf.lerp(Mathf.sin(mech.walkExtend(true), 1f/Mathf.PI, 1) * mechFrontSway, 0f, unit.elevation)));

            unit.trns(legOffset.x, legOffset.y);
        }

        if(unit instanceof Tankc){
            drawTank((Unit & Tankc)unit);
        }

        if(unit instanceof Legsc){
            drawLegs((Unit & Legsc)unit);
        }

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        if(unit instanceof Payloadc){
            drawPayload((Unit & Payloadc)unit);
        }

        drawSoftShadow(unit);

        Draw.z(z);

        if(unit instanceof Crawlc c){
            drawCrawl(c);
        }

        if(drawBody) drawOutline(unit);
        drawWeaponOutlines(unit);
        if(engineSize > 0) drawEngine(unit);
        if(engines.size > 0) drawEngines(unit);
        if(drawBody) drawBody(unit);
        if(drawCell) drawCell(unit);
        drawWeapons(unit);
        if(drawItems) drawItems(unit);
        drawLight(unit);

        if(unit.shieldAlpha > 0 && drawShields){
            drawShield(unit);
        }

        if(mech != null){
            unit.trns(-legOffset.x, -legOffset.y);
        }

        //TODO how/where do I draw under?
        if(parts.size > 0){
            //TODO does it need an outline?
            WeaponMount first = unit.mounts.length > 0 ? unit.mounts[0] : null;
            if(unit.mounts.length > 0){
                DrawPart.params.set(first.warmup, first.reload / weapons.first().reload, first.smoothReload, first.heat, unit.x, unit.y, unit.rotation);
            }else{
                DrawPart.params.set(0f, 0f, 0f, 0f, unit.x, unit.y, unit.rotation);
            }
            if(unit instanceof Scaled s){
                DrawPart.params.life = s.fin();
            }
            for(int i = 0; i < parts.size; i++){
                var part = parts.items[i];
                part.draw(DrawPart.params);
            }
        }

        for(Ability a : unit.abilities){
            Draw.reset();
            a.draw(unit);
        }

        Draw.reset();
    }

    public <T extends Unit & Payloadc> void drawPayload(T unit){
        if(unit.hasPayload()){
            Payload pay = unit.payloads().first();
            pay.set(unit.x, unit.y, unit.rotation);
            pay.draw();
        }
    }

    public void drawShield(Unit unit){
        float alpha = unit.shieldAlpha();
        float radius = unit.hitSize() * 1.3f;
        Fill.light(unit.x, unit.y, Lines.circleVertices(radius), radius,
            Color.clear,
            Tmp.c2.set(unit.team.color).lerp(Color.white, Mathf.clamp(unit.hitTime() / 2f)).a(0.7f * alpha)
        );
    }

    public void drawControl(Unit unit){
        Draw.z(unit.isFlying() ? Layer.flyingUnitLow : Layer.groundUnit - 2);

        Draw.color(Pal.accent, Color.white, Mathf.absin(4f, 0.3f));
        Lines.poly(unit.x, unit.y, 4, unit.hitSize + 1.5f);

        Draw.reset();
    }

    public void drawShadow(Unit unit){
        float e = Math.max(unit.elevation, visualElevation) * (1f - unit.drownTime);
        float x = unit.x + shadowTX * e, y = unit.y + shadowTY * e;
        Floor floor = world.floorWorld(x, y);

        float dest = floor.canShadow ? 1f : 0f;
        //yes, this updates state in draw()... which isn't a problem, because I don't want it to be obvious anyway
        unit.shadowAlpha = unit.shadowAlpha < 0 ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);

        Draw.rect(shadowRegion, unit.x + shadowTX * e, unit.y + shadowTY * e, unit.rotation - 90);
        Draw.color();
    }

    public void drawSoftShadow(Unit unit){
        drawSoftShadow(unit, 1f);
    }

    public void drawSoftShadow(Unit unit, float alpha){
        drawSoftShadow(unit.x, unit.y, unit.rotation, alpha);
    }

    public void drawSoftShadow(float x, float y, float rotation, float alpha){
        Draw.color(0, 0, 0, 0.4f * alpha);
        float rad = 1.6f;
        float size = Math.max(region.width, region.height) * Draw.scl;
        Draw.rect(softShadowRegion, x, y, size * rad * Draw.xscl, size * rad * Draw.yscl, rotation - 90);
        Draw.color();
    }

    public void drawItems(Unit unit){
        applyColor(unit);

        //draw back items
        if(unit.item() != null && unit.itemTime > 0.01f){
            float size = (itemSize + Mathf.absin(Time.time, 5f, 1f)) * unit.itemTime;

            Draw.mixcol(Pal.accent, Mathf.absin(Time.time, 5f, 0.1f));
            Draw.rect(unit.item().fullIcon,
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY),
            size, size, unit.rotation);

            Draw.mixcol();

            Lines.stroke(1f, Pal.accent);
            Lines.circle(
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY),
            (3f + Mathf.absin(Time.time, 5f, 1f)) * unit.itemTime);

            if(unit.isLocal() && !renderer.pixelator.enabled()){
                Fonts.outline.draw(unit.stack.amount + "",
                unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
                unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY) - 3,
                Pal.accent, 0.25f * unit.itemTime / Scl.scl(1f), false, Align.center
                );
            }

            Draw.reset();
        }
    }

    public void drawEngine(Unit unit){
        if(!unit.isFlying()) return;

        float scale = unit.elevation;
        float offset = engineOffset/2f + engineOffset/2f*scale;

        if(trailLength > 0 && !naval){
            drawTrail(unit);
        }

        Draw.color(engineColor == null ? unit.team.color : engineColor);
        Fill.circle(
            unit.x + Angles.trnsx(unit.rotation + 180, offset),
            unit.y + Angles.trnsy(unit.rotation + 180, offset),
            (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f)) * scale
        );
        Draw.color(engineColorInner);
        Fill.circle(
            unit.x + Angles.trnsx(unit.rotation + 180, offset - 1f),
            unit.y + Angles.trnsy(unit.rotation + 180, offset - 1f),
            (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f)) / 2f  * scale
        );
        Draw.color();
    }

    public void drawTrail(Unit unit){
        if(unit.trail == null){
            unit.trail = new Trail(trailLength);
        }
        Trail trail = unit.trail;
        trail.draw(unit.team.color, (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f) * unit.elevation) * trailScl);
    }

    public void drawEngines(Unit unit){
        if(!unit.isFlying()) return;

        float scale = unit.elevation;
        float rot = unit.rotation - 90;

        for(var engine : engines){
            Tmp.v1.set(engine.x, engine.y).rotate(rot);
            float ex = Tmp.v1.x, ey = Tmp.v1.y;

            Draw.color(unit.team.color);
            Fill.circle(
                unit.x + ex,
                unit.y + ey,
                (engine.radius + Mathf.absin(Time.time, 2f, engine.radius / 4f)) * scale
            );
            Draw.color(Color.white);
            Fill.circle(
                unit.x + ex - Angles.trnsx(rot + engine.rotation, 1f),
                unit.y + ey - Angles.trnsy(rot + engine.rotation, 1f),
                (engine.radius + Mathf.absin(Time.time, 2f, engine.radius / 4f)) / 2f  * scale
            );
        }

        Draw.color();
    }

    public void drawWeapons(Unit unit){
        applyColor(unit);

        for(WeaponMount mount : unit.mounts){
            mount.weapon.draw(unit, mount);
        }

        Draw.reset();
    }

    public void drawWeaponOutlines(Unit unit){
        applyColor(unit);
        applyOutlineColor(unit);

        for(WeaponMount mount : unit.mounts){
            if(!mount.weapon.top){
                //apply layer offset, roll it back at the end
                float z = Draw.z();
                Draw.z(z + mount.weapon.layerOffset);

                mount.weapon.drawOutline(unit, mount);

                Draw.z(z);
            }
        }

        Draw.reset();
    }

    public void drawOutline(Unit unit){
        Draw.reset();

        if(Core.atlas.isFound(outlineRegion)){
            applyColor(unit);
            applyOutlineColor(unit);
            Draw.rect(outlineRegion, unit.x, unit.y, unit.rotation - 90);
            Draw.reset();
        }
    }

    public void drawBody(Unit unit){
        applyColor(unit);

        Draw.rect(region, unit.x, unit.y, unit.rotation - 90);

        Draw.reset();
    }

    public void drawCell(Unit unit){
        applyColor(unit);

        Draw.color(cellColor(unit));
        Draw.rect(cellRegion, unit.x, unit.y, unit.rotation - 90);
        Draw.reset();
    }

    public Color cellColor(Unit unit){
        float f = Mathf.clamp(unit.healthf());
        return Tmp.c1.set(Color.black).lerp(unit.team.color, f + Mathf.absin(Time.time, Math.max(f * 5f, 1f), 1f - f));
    }

    public void drawLight(Unit unit){
        if(lightRadius > 0){
            Drawf.light(unit.team, unit.x, unit.y, lightRadius, lightColor, lightOpacity);
        }
    }

    public <T extends Unit & Tankc> void drawTank(T unit){
        Draw.rect(treadRegion, unit.x, unit.y, unit.rotation - 90);

        if(treadRegion.found()){
            int frame = (int)(unit.treadTime()) % treadFrames;
            for(int i = 0; i < treadRects.length; i ++){
                var region = treadRegions[i][frame];
                var treadRect = treadRects[i];
                float xOffset = treadRegion.width/2f - (treadRect.x + treadRect.width/2f);
                float yOffset = treadRegion.height/2f - (treadRect.y + treadRect.height/2f);

                for(int side : Mathf.signs){
                    Tmp.v1.set(xOffset * side, yOffset).rotate(unit.rotation - 90);
                    Draw.rect(region, unit.x + Tmp.v1.x / 4f, unit.y + Tmp.v1.y / 4f, treadRect.width / 4f, region.height / 4f, unit.rotation - 90);
                }
            }
        }
    }

    public <T extends Unit & Legsc> void drawLegs(T unit){
        applyColor(unit);
        Tmp.c3.set(Draw.getMixColor());

        Leg[] legs = unit.legs();

        float ssize = footRegion.width * Draw.scl * 1.5f;
        float rotation = unit.baseRotation();
        float invDrown = 1f - unit.drownTime;

        if(footRegion.found()){
            for(Leg leg : legs){
                Drawf.shadow(leg.base.x, leg.base.y, ssize, invDrown);
            }
        }

        //legs are drawn front first
        for(int j = legs.length - 1; j >= 0; j--){
            int i = (j % 2 == 0 ? j/2 : legs.length - 1 - j/2);
            Leg leg = legs[i];
            boolean flip = i >= legs.length/2f;
            int flips = Mathf.sign(flip);

            Vec2 position = unit.legOffset(legOffset, i).add(unit);

            Tmp.v1.set(leg.base).sub(leg.joint).inv().setLength(legExtension);

            if(footRegion.found() && leg.moving && visualElevation > 0){
                float scl = visualElevation * invDrown;
                float elev = Mathf.slope(1f - leg.stage) * scl;
                Draw.color(Pal.shadow);
                Draw.rect(footRegion, leg.base.x + shadowTX * elev, leg.base.y + shadowTY * elev, position.angleTo(leg.base));
                Draw.color();
            }

            Draw.mixcol(Tmp.c3, Tmp.c3.a);

            if(footRegion.found()){
                Draw.rect(footRegion, leg.base.x, leg.base.y, position.angleTo(leg.base));
            }

            Lines.stroke(legRegion.height * Draw.scl * flips);
            Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, false);

            Lines.stroke(legBaseRegion.height * Draw.scl * flips);
            Lines.line(legBaseRegion, leg.joint.x + Tmp.v1.x, leg.joint.y + Tmp.v1.y, leg.base.x, leg.base.y, false);

            if(jointRegion.found()){
                Draw.rect(jointRegion, leg.joint.x, leg.joint.y);
            }
        }

        //base joints are drawn after everything else
        if(baseJointRegion.found()){
            for(int j = legs.length - 1; j >= 0; j--){
                //TODO does the index / draw order really matter?
                Vec2 position = unit.legOffset(legOffset, (j % 2 == 0 ? j/2 : legs.length - 1 - j/2)).add(unit);
                Draw.rect(baseJointRegion, position.x, position.y, rotation);
            }
        }

        if(baseRegion.found()){
            Draw.rect(baseRegion, unit.x, unit.y, rotation - 90);
        }

        Draw.reset();
    }

    public void drawCrawl(Crawlc crawl){
        Unit unit = (Unit)crawl;
        applyColor(unit);

        //change to 2 TODO
        for(int p = 0; p < 2; p++){
            TextureRegion[] regions = p == 0 ? segmentOutlineRegions : segmentRegions;

            for(int i = 0; i < segments; i++){
                float trns = Mathf.sin(crawl.crawlTime() + i * segmentPhase, segmentScl, segmentSpacing);

                //at segment 0, rotation = segmentRot, but at the last segment it is rotation
                float rot = Mathf.slerp(crawl.segmentRot(), unit.rotation, i / (float)(segments - 1));
                float tx = Angles.trnsx(rot, trns), ty = Angles.trnsy(rot, trns);


                //shadow
                Draw.color(0f, 0f, 0f, 0.2f);
                //Draw.rect(regions[i], unit.x + tx + 2f, unit.y + ty - 2f, rot - 90);

                applyColor(unit);


                //TODO merge outlines?
                Draw.rect(regions[i], unit.x + tx, unit.y + ty, rot - 90);
            }
        }
    }

    public void drawMech(Mechc mech){
        Unit unit = (Unit)mech;

        Draw.reset();

        float e = unit.elevation;

        float sin = Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f / Mathf.PI, 1f), 0f, e);
        float extension = Mathf.lerp(mech.walkExtend(false), 0, e);
        float boostTrns = e * 2f;

        Floor floor = unit.isFlying() ? Blocks.air.asFloor() : unit.floorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.mixcol(Tmp.c1.set(mechLegColor).lerp(Color.white, Mathf.clamp(unit.hitTime)), Math.max(Math.max(0, i * extension / mechStride), unit.hitTime));

            Draw.rect(legRegion,
            unit.x + Angles.trnsx(mech.baseRotation(), extension * i - boostTrns, -boostTrns*i),
            unit.y + Angles.trnsy(mech.baseRotation(), extension * i - boostTrns, -boostTrns*i),
            legRegion.width * i * Draw.scl,
            legRegion.height * Draw.scl - Math.max(-sin * i, 0) * legRegion.height * 0.5f * Draw.scl,
            mech.baseRotation() - 90 + 35f*i*e);
        }

        Draw.mixcol(Color.white, unit.hitTime);

        if(unit.lastDrownFloor != null){
            Draw.color(Color.white, Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.83f), unit.drownTime * 0.9f);
        }else{
            Draw.color(Color.white);
        }

        Draw.rect(baseRegion, unit, mech.baseRotation() - 90);

        Draw.mixcol();
    }

    public void applyOutlineColor(Unit unit){
        if(unit.drownTime > 0 && unit.lastDrownFloor != null){
            Draw.color(Color.white, Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.8f), unit.drownTime * 0.9f);
        }
    }

    public void applyColor(Unit unit){
        Draw.color();
        if(showHeal){
            Tmp.c1.set(Color.white).lerp(healColor, Mathf.clamp(unit.healTime - unit.hitTime));
        }
        Draw.mixcol(Tmp.c1, Math.max(unit.hitTime, Mathf.clamp(unit.healTime)));

        if(unit.drownTime > 0 && unit.lastDrownFloor != null){
            Draw.mixcol(Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.83f), unit.drownTime * 0.9f);
        }
    }

    //endregion

    public static class UnitEngine implements Cloneable{
        public float x, y, radius, rotation;

        public UnitEngine(float x, float y, float radius, float rotation){
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.rotation = rotation;
        }

        public UnitEngine(){
        }

        public UnitEngine copy(){
            try{
                return (UnitEngine)clone();
            }catch(CloneNotSupportedException awful){
                throw new RuntimeException("fantastic", awful);
            }
        }
    }

}