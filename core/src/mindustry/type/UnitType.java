package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.*;

import static mindustry.Vars.*;

public class UnitType extends UnlockableContent{
    public static final float shadowTX = -12, shadowTY = -13, shadowColor = Color.toFloatBits(0, 0, 0, 0.22f), outlineSpace = 0.01f;
    private static final Vec2 legOffset = new Vec2();

    /** If true, the unit is always at elevation 1. */
    public boolean flying;
    public @NonNull Prov<? extends Unit> constructor;
    public @NonNull Prov<? extends UnitController> defaultController = () -> !flying ? new GroundAI() : new FlyingAI();
    public float speed = 1.1f, boostMultiplier = 1f, rotateSpeed = 5f, baseRotateSpeed = 5f;
    public float drag = 0.3f, accel = 0.5f, landShake = 0f, rippleScale = 1f, fallSpeed = 0.018f;
    public float health = 200f, range = -1, armor = 0f;
    public float crashDamageMultiplier = 3f;
    public boolean targetAir = true, targetGround = true;
    public boolean faceTarget = true, rotateShooting = true, isCounted = true, lowAltitude = false;
    public boolean canBoost = false;
    public boolean destructibleWreck = true;
    public float groundLayer = Layer.groundUnit;
    public float payloadCapacity = 8;
    public int commandLimit = 24;
    public float visualElevation = -1f;
    public boolean allowLegStep = false;
    public boolean hovering = false;
    public Effect fallEffect = Fx.fallSmoke;
    public Effect fallThrusterEffect = Fx.fallSmoke;
    public Seq<Ability> abilities = new Seq<>();

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
    public int ammoCapacity = 220;
    public int mineTier = -1;
    public float buildSpeed = 1f, mineSpeed = 1f;

    public boolean canDrown = true;
    public float engineOffset = 5f, engineSize = 2.5f;
    public float strafePenalty = 0.5f;
    public float hitsize = 6f;
    public float itemOffsetY = 3f;
    public float lightRadius = 60f, lightOpacity = 0.6f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true, drawShields = true;
    public int trailLength = 3;
    public float trailX = 4f, trailY = -3f, trailScl = 1f;
    /** Whether the unit can heal blocks. Initialized in init() */
    public boolean canHeal = false;
    public boolean singleTarget = false;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Seq<Weapon> weapons = new Seq<>();
    public TextureRegion baseRegion, legRegion, region, shadowRegion, cellRegion,
        occlusionRegion, jointRegion, footRegion, legBaseRegion, baseJointRegion, outlineRegion;
    public TextureRegion[] wreckRegions;

    public UnitType(String name){
        super(name);

        constructor = EntityMapping.map(name);
    }

    public UnitController createController(){
        return defaultController.get();
    }

    public Unit create(Team team){
        Unit unit = constructor.get();
        unit.team = team;
        unit.type(this);
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

    public boolean hasWeapons(){
        return weapons.size > 0;
    }

    public void update(Unit unit){

        if(unit instanceof Mechc){
            updateMechEffects(unit);
        }
    }

    public void updateMechEffects(Unit unit){
        Mechc mech = (Mechc)unit;

        float extend = walkExtend((Mechc)unit, false);
        float base = walkExtend((Mechc)unit, true);
        float extendScl = base % 1f;

        float lastExtend = mech.walkExtension();

        if(extendScl < lastExtend && base % 2f > 1f){
            int side = -Mathf.sign(extend);
            float width = hitsize / 2f * side, length = mechStride * 1.35f;

            float cx = unit.x + Angles.trnsx(mech.baseRotation(), length, width),
            cy = unit.y + Angles.trnsy(mech.baseRotation(), length, width);

            if(mechStepShake > 0){
                Effect.shake(mechStepShake, mechStepShake, cx, cy);
            }

            if(mechStepParticles){
                Tile tile = world.tileWorld(cx, cy);
                if(tile != null){
                    Color color = tile.floor().mapColor;
                    Fx.unitLand.at(cx, cy, hitsize/8f, color);
                }
            }
        }

        mech.walkExtension(extendScl);
    }

    public void landed(Unit unit){}

    public void display(Unit unit, Table table){
        table.table(t -> {
            t.left();
            t.add(new Image(icon(Cicon.medium))).size(8 * 4).scaling(Scaling.fit);
            t.labelWrap(localizedName).left().width(190f).padLeft(5);
        }).growX().left();
        table.row();

        table.table(bars -> {
            bars.defaults().growX().height(18f).pad(4);

            bars.add(new Bar("blocks.health", Pal.health, unit::healthf).blink(Color.white));
            bars.row();

            if(state.rules.unitAmmo){
                bars.add(new Bar("blocks.ammo", Pal.ammo, () -> unit.ammo / ammoCapacity));
                bars.row();
            }
        }).growX();
        
        table.row();
        if(unit.deactivated){
            table.table(d -> {
                d.left();
                d.label(() -> Core.bundle.format("bar.limitreached", unit.count(), unit.cap(), Fonts.getUnicodeStr(name)));
            }).left().visible(() -> unit.deactivated);
        }
        
    }

    @Override
    public void getDependencies(Cons<UnlockableContent> cons){
        //units require reconstructors being researched
        for(Block block : content.blocks()){
            if(block instanceof Reconstructor){
                Reconstructor r = (Reconstructor)block;
                for(UnitType[] recipe : r.upgrades){
                    //result of reconstruction is this, so it must be a dependency
                    if(recipe[1] == this){
                        cons.get(block);
                    }
                }
            }
        }
    }


    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayUnit(table, this);
    }

    @CallSuper
    @Override
    public void init(){
        if(constructor == null) throw new IllegalArgumentException("no constructor set up for unit '" + name + "'");

        singleTarget = weapons.size <= 1;

        if(itemCapacity < 0){
            itemCapacity = Math.max(Mathf.round(hitsize * 7, 20), 20);
        }

        //set up default range
        if(range < 0){
            range = Float.MAX_VALUE;
            for(Weapon weapon : weapons){
                range = Math.min(range, weapon.bullet.range() + hitsize/2f);
            }
        }

        if(mechStride < 0){
            mechStride = 4f + (hitsize-8f)/2.1f;
        }

        if(mechStepShake < 0){
            mechStepShake = Mathf.round((hitsize - 11f) / 9f);
            mechStepParticles = hitsize > 15f;
        }

        canHeal = weapons.contains(w -> w.bullet instanceof HealBulletType);

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
    }

    @CallSuper
    @Override
    public void load(){
        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        legRegion = Core.atlas.find(name + "-leg");
        jointRegion = Core.atlas.find(name + "-joint");
        baseJointRegion = Core.atlas.find(name + "-joint-base");
        footRegion = Core.atlas.find(name + "-foot");
        legBaseRegion = Core.atlas.find(name + "-leg-base", name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));
        occlusionRegion = Core.atlas.find("circle-shadow");
        outlineRegion = Core.atlas.find(name + "-outline");
        shadowRegion = icon(Cicon.full);

        wreckRegions = new TextureRegion[3];
        for(int i = 0; i < wreckRegions.length; i++){
            wreckRegions[i] = Core.atlas.find(name + "-wreck" + i);
        }
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    //region drawing

    public void draw(Unit unit){
        Mechc mech = unit instanceof Mechc ? (Mechc)unit : null;
        float z = unit.elevation > 0.5f ? (lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : groundLayer + Mathf.clamp(hitsize/4000f, 0, 0.01f);

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
            legOffset.trns(mech.baseRotation(), 0f, Mathf.lerp(Mathf.sin(walkExtend(mech, true), 2f/Mathf.PI, 1) * mechSideSway, 0f, unit.elevation));

            //front
            legOffset.add(Tmp.v1.trns(mech.baseRotation() + 90, 0f, Mathf.lerp(Mathf.sin(walkExtend(mech, true), 1f/Mathf.PI, 1) * mechFrontSway, 0f, unit.elevation)));

            unit.trns(legOffset.x, legOffset.y);
        }

        if(unit instanceof Legsc){
            drawLegs((Unit & Legsc)unit);
        }

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        if(unit instanceof Payloadc){
            drawPayload((Unit & Payloadc)unit);
        }

        //TODO
        drawOcclusion(unit);

        Draw.z(z - outlineSpace);

        drawOutline(unit);

        Draw.z(z);
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

        if(unit.deactivated){
            drawDeactive(unit);
        }

        if(unit.abilities.size > 0){
            for(Ability a : unit.abilities){
                Draw.reset();
                a.draw(unit);
            }

            Draw.reset();
        }
    }

    public void drawDeactive(Unit unit){
        Draw.color(Color.scarlet);
        Draw.alpha(0.8f);

        float size = 8f;
        Draw.rect(Icon.warning.getRegion(), unit.x, unit.y, size, size);

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
        Fill.light(unit.x, unit.y, Lines.circleVertices(radius), radius, Tmp.c1.set(Pal.shieldIn), Tmp.c2.set(Pal.shield).lerp(Color.white, Mathf.clamp(unit.hitTime() / 2f)).a(Pal.shield.a * alpha));
    }

    public void drawControl(Unit unit){
        Draw.z(Layer.groundUnit - 2);

        Draw.color(Pal.accent, Color.white, Mathf.absin(4f, 0.3f));
        Lines.poly(unit.x, unit.y, 4, unit.hitSize + 1.5f);

        Draw.reset();
    }

    public void drawShadow(Unit unit){
        Draw.color(shadowColor);
        float e = Math.max(unit.elevation, visualElevation);
        Draw.rect(shadowRegion, unit.x + shadowTX * e, unit.y + shadowTY * e, unit.rotation - 90);
        Draw.color();
    }

    public void drawOcclusion(Unit unit){
        Draw.color(0, 0, 0, 0.4f);
        float rad = 1.6f;
        float size = Math.max(region.width, region.height) * Draw.scl;
        Draw.rect(occlusionRegion, unit, size * rad, size * rad);
        Draw.color();
    }

    public void drawItems(Unit unit){
        applyColor(unit);

        //draw back items
        if(unit.hasItem() && unit.itemTime > 0.01f){
            float size = (itemSize + Mathf.absin(Time.time(), 5f, 1f)) * unit.itemTime;

            Draw.mixcol(Pal.accent, Mathf.absin(Time.time(), 5f, 0.5f));
            Draw.rect(unit.item().icon(Cicon.medium),
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY),
            size, size, unit.rotation);

            Draw.mixcol();

            Lines.stroke(1f, Pal.accent);
            Lines.circle(
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY),
            (3f + Mathf.absin(Time.time(), 5f, 1f)) * unit.itemTime);

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
            trail.draw(unit.team.color, (engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f) * scale) * trailScl);
        }

        Draw.color(unit.team.color);
        Fill.circle(
            unit.x + Angles.trnsx(unit.rotation + 180, offset),
            unit.y + Angles.trnsy(unit.rotation + 180, offset),
            (engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f)) * scale
        );
        Draw.color(Color.white);
        Fill.circle(
            unit.x + Angles.trnsx(unit.rotation + 180, offset - 1f),
            unit.y + Angles.trnsy(unit.rotation + 180, offset - 1f),
            (engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f)) / 2f  * scale
        );
        Draw.color();
    }

    public void drawWeapons(Unit unit){
        applyColor(unit);

        for(WeaponMount mount : unit.mounts){
            Weapon weapon = mount.weapon;

            float rotation = unit.rotation - 90;
            float weaponRotation  = rotation + (weapon.rotate ? mount.rotation : 0);
            float width = weapon.region.width;
            float recoil = -((mount.reload) / weapon.reload * weapon.recoil);
            float wx = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y) + Angles.trnsx(weaponRotation, 0, recoil),
                wy = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y) + Angles.trnsy(weaponRotation, 0, recoil);

            if(weapon.occlusion > 0){
                Drawf.shadow(wx, wy, weapon.occlusion);
            }

            if(weapon.outlineRegion.found()){
                float z = Draw.z();
                if(!weapon.top) Draw.z(z - outlineSpace);

                Draw.rect(weapon.outlineRegion,
                wx, wy,
                width * Draw.scl * -Mathf.sign(weapon.flipSprite),
                weapon.region.height * Draw.scl,
                weaponRotation);


                Draw.z(z);
            }

            Draw.rect(weapon.region,
            wx, wy,
            width * Draw.scl * -Mathf.sign(weapon.flipSprite),
            weapon.region.height * Draw.scl,
            weaponRotation);

            if(weapon.heatRegion.found() && mount.heat > 0){
                Draw.color(weapon.heatColor, mount.heat);
                Draw.blend(Blending.additive);
                Draw.rect(weapon.heatRegion,
                wx, wy,
                width * Draw.scl * -Mathf.sign(weapon.flipSprite),
                weapon.region.height * Draw.scl,
                weaponRotation);
                Draw.blend();
                Draw.color();
            }
        }

        Draw.reset();
    }

    public void drawOutline(Unit unit){
        if(Core.atlas.isFound(outlineRegion)){
            Draw.rect(outlineRegion, unit.x, unit.y, unit.rotation - 90);
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
        return Tmp.c1.set(Color.black).lerp(unit.team.color, unit.healthf() + Mathf.absin(Time.time(), Math.max(unit.healthf() * 5f, 1f), 1f - unit.healthf()));
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

        //TODO should be below/above legs
        if(baseRegion.found()){
            Draw.rect(baseRegion, unit.x, unit.y, rotation);
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
                Draw.color(shadowColor);
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

        Draw.reset();
    }

    public void drawMech(Mechc mech){
        Unit unit = (Unit)mech;

        Draw.reset();

        float e = unit.elevation;

        float sin = Mathf.lerp(Mathf.sin(walkExtend(mech, true), 2f / Mathf.PI, 1f), 0f, e);
        float extension = Mathf.lerp(walkExtend(mech, false), 0, e);
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

    public float walkExtend(Mechc mech, boolean scaled){

        //now ranges from -maxExtension to maxExtension*3
        float raw = mech.walkTime() % (mechStride * 4);

        if(scaled) return raw / mechStride;

        if(raw > mechStride*3) raw = raw - mechStride * 4;
        else if(raw > mechStride*2) raw = mechStride * 2 - raw;
        else if(raw > mechStride) raw = mechStride * 2 - raw;

        return raw;
    }

    public void applyColor(Unit unit){
        Draw.color();
        Draw.mixcol(Color.white, unit.hitTime);
        if(unit.drownTime > 0 && unit.floorOn().isDeep()){
            Draw.mixcol(unit.floorOn().mapColor, unit.drownTime * 0.8f);
        }
    }

    //endregion
}
