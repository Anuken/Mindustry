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
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
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

import static mindustry.Vars.*;

//TODO document
public class UnitType extends UnlockableContent{
    public static final float shadowTX = -12, shadowTY = -13;
    private static final Vec2 legOffset = new Vec2();

    /** If true, the unit is always at elevation 1. */
    public boolean flying;
    /** If `flying` and this is true, the unit can appear on the title screen */
    public boolean onTitleScreen = true;
    /** Creates a new instance of this unit class. */
    public Prov<? extends Unit> constructor;
    /** The default AI controller to assign on creation. */
    public Prov<? extends UnitController> defaultController = () -> !flying ? new GroundAI() : new FlyingAI();

    /** Environmental flags that are *all* required for this unit to function. 0 = any environment */
    public int envRequired = 0;
    /** The environment flags that this unit can function in. If the env matches any of these, it will be enabled. */
    public int envEnabled = Env.terrestrial;
    /** The environment flags that this unit *cannot* function in. If the env matches any of these, it will be *disabled*. */
    public int envDisabled = 0;

    public float speed = 1.1f, boostMultiplier = 1f, rotateSpeed = 5f, baseRotateSpeed = 5f;
    public float drag = 0.3f, accel = 0.5f, landShake = 0f, rippleScale = 1f, riseSpeed = 0.08f, fallSpeed = 0.018f;
    public float health = 200f, range = -1, miningRange = 70f, armor = 0f, maxRange = -1f;
    public float crashDamageMultiplier = 1f;
    public boolean targetAir = true, targetGround = true;
    public boolean faceTarget = true, rotateShooting = true, isCounted = true, lowAltitude = false, circleTarget = false;
    public boolean canBoost = false;
    public boolean destructibleWreck = true;
    public float groundLayer = Layer.groundUnit;
    public float payloadCapacity = 8;
    public float aimDst = -1f;
    public float buildBeamOffset = 3.8f;
    public int commandLimit = 8;
    public float commandRadius = 150f;
    public float visualElevation = -1f;
    public boolean allowLegStep = false;
    public boolean hovering = false;
    public boolean omniMovement = true;
    public Effect fallEffect = Fx.fallSmoke;
    public Effect fallThrusterEffect = Fx.fallSmoke;
    public Effect deathExplosionEffect = Fx.dynamicExplosion;
    public Seq<Ability> abilities = new Seq<>();
    /** Flags to target based on priority. Null indicates that the closest target should be found. The closest enemy core is used as a fallback. */
    public BlockFlag[] targetFlags = {null};
    /** targetFlags, as an override for "non-AI" teams. By default, units of this type will rush the core. */
    public BlockFlag[] playerTargetFlags = {BlockFlag.core, null};

    public Color outlineColor = Pal.darkerMetal;
    public int outlineRadius = 3;
    public boolean outlines = true;

    public int legCount = 4, legGroupSize = 2;
    public float legLength = 10f, legSpeed = 0.1f, legTrns = 1f, legBaseOffset = 0f, legMoveSpace = 1f, legExtension = 0, legPairOffset = 0, legLengthScl = 1f, kinematicScl = 1f, maxStretch = 1.75f;
    public float legSplashDamage = 0f, legSplashRange = 5;
    public boolean flipBackLegs = true;

    public float mechSideSway = 0.54f, mechFrontSway = 0.1f;
    public float mechStride = -1f;
    public float mechStepShake = -1f;
    public boolean mechStepParticles = false;
    public Color mechLegColor = Pal.darkMetal;

    public int itemCapacity = -1;
    public int ammoCapacity = -1;
    public AmmoType ammoType = new ItemAmmoType(Items.copper);
    public int mineTier = -1;
    public float buildSpeed = -1f, mineSpeed = 1f;
    public Sound mineSound = Sounds.minebeam;
    public float mineSoundVolume = 0.6f;

    /** This is a VERY ROUGH estimate of unit DPS. */
    public float dpsEstimate = -1;
    public float clipSize = -1;
    public boolean canDrown = true;
    public float engineOffset = 5f, engineSize = 2.5f;
    public float strafePenalty = 0.5f;
    public float hitSize = 6f;
    public float itemOffsetY = 3f;
    public float lightRadius = -1f, lightOpacity = 0.6f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true, drawShields = true;
    public int trailLength = 3;
    public float trailX = 4f, trailY = -3f, trailScl = 1f;
    /** Whether the unit can heal blocks. Initialized in init() */
    public boolean canHeal = false;
    /** If true, all weapons will attack the same target. */
    public boolean singleTarget = false;
    public boolean forceMultiTarget = false;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Seq<Weapon> weapons = new Seq<>();
    public TextureRegion baseRegion, legRegion, region, shadowRegion, cellRegion,
        softShadowRegion, jointRegion, footRegion, legBaseRegion, baseJointRegion, outlineRegion;
    public TextureRegion[] wreckRegions;

    protected @Nullable ItemStack[] cachedRequirements;

    public UnitType(String name){
        super(name);

        constructor = EntityMapping.map(this.name);
    }

    public UnitController createController(){
        return defaultController.get();
    }

    public Unit create(Team team){
        Unit unit = constructor.get();
        unit.team = team;
        unit.setType(this);
        unit.ammo = ammoCapacity; //fill up on ammo upon creation
        unit.elevation = flying ? 1f : 0;
        unit.heal();
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

            bars.add(new Bar("stat.health", Pal.health, unit::healthf).blink(Color.white));
            bars.row();

            if(state.rules.unitAmmo){
                bars.add(new Bar(ammoType.icon() + " " + Core.bundle.get("stat.ammo"), ammoType.barColor(), () -> unit.ammo / ammoCapacity));
                bars.row();
            }

            for(Ability ability : unit.abilities){
                ability.displayBars(unit, bars);
            }

            if(unit instanceof Payloadc payload){
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
    public void setStats(){
        Unit inst = constructor.get();

        stats.add(Stat.health, health);
        stats.add(Stat.armor, armor);
        stats.add(Stat.speed, speed * 60f / tilesize, StatUnit.tilesSecond);
        stats.add(Stat.size, hitSize / tilesize, StatUnit.blocksSquared);
        stats.add(Stat.itemCapacity, itemCapacity);
        stats.add(Stat.range, (int)(maxRange / tilesize), StatUnit.blocks);
        stats.add(Stat.commandLimit, commandLimit);

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
        if(inst instanceof Payloadc){
            stats.add(Stat.payloadCapacity, (payloadCapacity / (tilesize * tilesize)), StatUnit.blocksSquared);
        }

        if(weapons.any()){
            stats.add(Stat.weapons, StatValues.weapons(this, weapons));
        }
    }

    @CallSuper
    @Override
    public void init(){
        if(constructor == null) throw new IllegalArgumentException("no constructor set up for unit '" + name + "'");

        Unit example = constructor.get();

        //water preset
        if(example instanceof WaterMovec){
            canDrown = false;
            omniMovement = false;
            immunities.add(StatusEffects.wet);
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
                range = Math.min(range, weapon.bullet.range() - margin);
                maxRange = Math.max(maxRange, weapon.bullet.range() - margin);
            }
        }

        if(maxRange < 0){
            maxRange = Math.max(0f, range);

            for(Weapon weapon : weapons){
                maxRange = Math.max(maxRange, weapon.bullet.range() - margin);
            }
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

        canHeal = weapons.contains(w -> w.bullet.healPercent > 0f);

        //add mirrored weapon variants
        Seq<Weapon> mapped = new Seq<>();
        for(Weapon w : weapons){
            mapped.add(w);

            //mirrors are copies with X values negated
            if(w.mirror){
                Weapon copy = w.copy();
                copy.x *= -1;
                copy.shootX *= -1;
                copy.flipSprite = !copy.flipSprite;
                mapped.add(copy);

                //since there are now two weapons, the reload time must be doubled
                w.reload *= 2f;
                copy.reload *= 2f;

                w.otherSide = mapped.size - 1;
                copy.otherSide = mapped.size - 2;
            }
        }
        this.weapons = mapped;

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
    }

    @CallSuper
    @Override
    public void load(){
        super.load();

        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        legRegion = Core.atlas.find(name + "-leg");
        jointRegion = Core.atlas.find(name + "-joint");
        baseJointRegion = Core.atlas.find(name + "-joint-base");
        footRegion = Core.atlas.find(name + "-foot");
        legBaseRegion = Core.atlas.find(name + "-leg-base", name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));
        softShadowRegion = Core.atlas.find("circle-shadow");
        outlineRegion = Core.atlas.find(name + "-outline");
        shadowRegion = fullIcon;

        wreckRegions = new TextureRegion[3];
        for(int i = 0; i < wreckRegions.length; i++){
            wreckRegions[i] = Core.atlas.find(name + "-wreck" + i);
        }

        clipSize = Math.max(region.width * 2f, clipSize);
    }

    private void makeOutline(MultiPacker packer, TextureRegion region){
        if(region instanceof AtlasRegion at && region.found()){
            String name = at.name;
            if(!packer.has(name + "-outline")){
                PixmapRegion base = Core.atlas.getPixmap(region);
                var result = Pixmaps.outline(base, outlineColor, outlineRadius);
                if(Core.settings.getBool("linear")){
                    Pixmaps.bleed(result);
                }
                packer.add(PageType.main, name + "-outline", result);
            }
        }
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        //currently does not create outlines for legs or base regions due to older mods having them outlined by default
        if(outlines){
            makeOutline(packer, region);
            for(Weapon weapon : weapons){
                if(!weapon.name.isEmpty()){
                    makeOutline(packer, weapon.region);
                }
            }
        }
    }

    @Override
    public ItemStack[] researchRequirements(){
        if(cachedRequirements != null){
            return cachedRequirements;
        }

        ItemStack[] stacks = null;

        //calculate costs based on reconstructors or factories found
        Block rec = content.blocks().find(b -> b instanceof Reconstructor re && re.upgrades.contains(u -> u[1] == this));

        if(rec != null && rec.consumes.has(ConsumeType.item) && rec.consumes.get(ConsumeType.item) instanceof ConsumeItems ci){
            stacks = ci.items;
        }else{
            UnitFactory factory = (UnitFactory)content.blocks().find(u -> u instanceof UnitFactory uf && uf.plans.contains(p -> p.unit == this));
            if(factory != null){
                stacks = factory.plans.find(p -> p.unit == this).requirements;
            }
        }

        if(stacks != null){
            ItemStack[] out = new ItemStack[stacks.length];
            for(int i = 0; i < out.length; i++){
                out[i] = new ItemStack(stacks[i].item, UI.roundAmount((int)(Math.pow(stacks[i].amount, 1.1) * 50)));
            }

            cachedRequirements = out;

            return out;
        }

        return super.researchRequirements();
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    //region drawing

    public void draw(Unit unit){
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

        if(unit instanceof Legsc){
            drawLegs((Unit & Legsc)unit);
        }

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        if(unit instanceof Payloadc){
            drawPayload((Unit & Payloadc)unit);
        }

        drawSoftShadow(unit);

        Draw.z(z);

        drawOutline(unit);
        drawWeaponOutlines(unit);
        if(engineSize > 0) drawEngine(unit);
        drawBody(unit);
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

        if(unit.abilities.size > 0){
            for(Ability a : unit.abilities){
                Draw.reset();
                a.draw(unit);
            }
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
        Draw.z(Layer.groundUnit - 2);

        Draw.color(Pal.accent, Color.white, Mathf.absin(4f, 0.3f));
        Lines.poly(unit.x, unit.y, 4, unit.hitSize + 1.5f);

        Draw.reset();
    }

    public void drawShadow(Unit unit){
        Draw.color(Pal.shadow);
        float e = Math.max(unit.elevation, visualElevation);
        Draw.rect(shadowRegion, unit.x + shadowTX * e, unit.y + shadowTY * e, unit.rotation - 90);
        Draw.color();
    }

    public void drawSoftShadow(Unit unit){
        Draw.color(0, 0, 0, 0.4f);
        float rad = 1.6f;
        float size = Math.max(region.width, region.height) * Draw.scl;
        Draw.rect(softShadowRegion, unit, size * rad * Draw.xscl, size * rad * Draw.yscl);
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

        if(unit instanceof Trailc){
            Trail trail = ((Trailc)unit).trail();
            trail.draw(unit.team.color, (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f) * scale) * trailScl);
        }

        Draw.color(unit.team.color);
        Fill.circle(
            unit.x + Angles.trnsx(unit.rotation + 180, offset),
            unit.y + Angles.trnsy(unit.rotation + 180, offset),
            (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f)) * scale
        );
        Draw.color(Color.white);
        Fill.circle(
            unit.x + Angles.trnsx(unit.rotation + 180, offset - 1f),
            unit.y + Angles.trnsy(unit.rotation + 180, offset - 1f),
            (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f)) / 2f  * scale
        );
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
                mount.weapon.drawOutline(unit, mount);
            }
        }

        Draw.reset();
    }

    public void applyOutlineColor(Unit unit){
        if(unit.isBoss()){
            Draw.mixcol(unit.team.color, Mathf.absin(7f, 1f));
        }
    }

    public void drawOutline(Unit unit){
        Draw.reset();

        if(Core.atlas.isFound(outlineRegion)){
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

    public <T extends Unit & Legsc> void drawLegs(T unit){
        applyColor(unit);

        Leg[] legs = unit.legs();

        float ssize = footRegion.width * Draw.scl * 1.5f;
        float rotation = unit.baseRotation();

        for(Leg leg : legs){
            Drawf.shadow(leg.base.x, leg.base.y, ssize);
        }

        //legs are drawn front first
        for(int j = legs.length - 1; j >= 0; j--){
            int i = (j % 2 == 0 ? j/2 : legs.length - 1 - j/2);
            Leg leg = legs[i];
            float angle = unit.legAngle(rotation, i);
            boolean flip = i >= legs.length/2f;
            int flips = Mathf.sign(flip);

            Vec2 position = legOffset.trns(angle, legBaseOffset).add(unit);

            Tmp.v1.set(leg.base).sub(leg.joint).inv().setLength(legExtension);

            if(leg.moving && visualElevation > 0){
                float scl = visualElevation;
                float elev = Mathf.slope(1f - leg.stage) * scl;
                Draw.color(Pal.shadow);
                Draw.rect(footRegion, leg.base.x + shadowTX * elev, leg.base.y + shadowTY * elev, position.angleTo(leg.base));
                Draw.color();
            }

            Draw.rect(footRegion, leg.base.x, leg.base.y, position.angleTo(leg.base));

            Lines.stroke(legRegion.height * Draw.scl * flips);
            Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, false);

            Lines.stroke(legBaseRegion.height * Draw.scl * flips);
            Lines.line(legBaseRegion, leg.joint.x + Tmp.v1.x, leg.joint.y + Tmp.v1.y, leg.base.x, leg.base.y, false);

            if(jointRegion.found()){
                Draw.rect(jointRegion, leg.joint.x, leg.joint.y);
            }

            if(baseJointRegion.found()){
                Draw.rect(baseJointRegion, position.x, position.y, rotation);
            }
        }

        if(baseRegion.found()){
            Draw.rect(baseRegion, unit.x, unit.y, rotation - 90);
        }

        Draw.reset();
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

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, unit.drownTime() * 0.4f);
        }else{
            Draw.color(Color.white);
        }

        Draw.rect(baseRegion, unit, mech.baseRotation() - 90);

        Draw.mixcol();
    }

    public void applyColor(Unit unit){
        Draw.color();
        Tmp.c1.set(Color.white).lerp(Pal.heal, Mathf.clamp(unit.healTime - unit.hitTime));
        Draw.mixcol(Tmp.c1, Math.max(unit.hitTime, Mathf.clamp(unit.healTime)));

        if(unit.drownTime > 0 && unit.floorOn().isDeep()){
            Draw.mixcol(unit.floorOn().mapColor, unit.drownTime * 0.8f);
        }
    }

    //endregion
}
