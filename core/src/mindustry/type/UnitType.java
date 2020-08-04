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
    public static final float shadowTX = -12, shadowTY = -13, shadowColor = Color.toFloatBits(0, 0, 0, 0.22f);
    private static final Vec2 legOffset = new Vec2();

    /** If true, the unit is always at elevation 1. */
    public boolean flying;
    public @NonNull Prov<? extends Unit> constructor;
    public @NonNull Prov<? extends UnitController> defaultController = () -> !flying ? new GroundAI() : new FlyingAI();
    public float speed = 1.1f, boostMultiplier = 1f, rotateSpeed = 5f, baseRotateSpeed = 5f;
    public float drag = 0.3f, accel = 0.5f, landShake = 0f, rippleScale = 1f, fallSpeed = 0.018f;
    public float health = 200f, range = -1, armor = 0f;
    public float crashDamageMultiplier = 4f;
    public boolean targetAir = true, targetGround = true;
    public boolean faceTarget = true, rotateShooting = true, isCounted = true, lowAltitude = false;
    public boolean canBoost = false;
    public boolean destructibleWreck = true;
    public float groundLayer = Layer.groundUnit;
    public float sway = 1f;
    public int payloadCapacity = 1;
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

    public int itemCapacity = 30;
    public int ammoCapacity = 220;
    public int mineTier = -1;
    public float buildSpeed = 1f, mineSpeed = 1f;

    public float engineOffset = 5f, engineSize = 2.5f;
    public float strafePenalty = 0.5f;
    public float hitsize = 6f;
    public float itemOffsetY = 3f;
    public float lightRadius = 60f, lightOpacity = 0.6f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true, drawShields = true;
    public int parts = 0;
    public int trailLength = 3;
    public float trailX = 4f, trailY = -3f, trailScl = 1f;
    /** Whether the unit can heal blocks. Initialized in init() */
    public boolean canHeal = false;
    public boolean singleTarget = false;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Seq<Weapon> weapons = new Seq<>();
    public TextureRegion baseRegion, legRegion, region, shadowRegion, cellRegion,
        occlusionRegion, jointRegion, footRegion, legBaseRegion, baseJointRegion;
    public TextureRegion[] partRegions, partCellRegions, wreckRegions;

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

    public boolean hasWeapons(){
        return weapons.size > 0;
    }

    public void update(Unit unit){
        if(abilities.size > 0){
            for(Ability a : abilities){
                a.update(unit);
            }
        }
    }

    public void landed(Unit unit){}

    public void display(Unit unit, Table table){
        table.table(t -> {
            t.left();
            t.add(new Image(icon(Cicon.medium))).size(8 * 4);
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

        //set up default range
        if(range < 0){
            range = Float.MAX_VALUE;
            for(Weapon weapon : weapons){
                range = Math.min(range, weapon.bullet.range() + hitsize/2f);
            }
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
        shadowRegion = icon(Cicon.full);

        partRegions = new TextureRegion[parts];
        partCellRegions = new TextureRegion[parts];

        for(int i = 0; i < parts; i++){
            partRegions[i] = Core.atlas.find(name + "-part" + i);
            partCellRegions[i] = Core.atlas.find(name + "-cell" + i);
        }

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
        Mechc legs = unit instanceof Mechc ? (Mechc)unit : null;
        float z = unit.elevation > 0.5f ? (lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit) : groundLayer;

        if(unit.controller().isBeingControlled(player.unit())){
            drawControl(unit);
        }

        if(unit.isFlying()){
            Draw.z(Math.min(Layer.darkness, z - 1f));
            drawShadow(unit);
        }

        Draw.z(z - 0.02f);

        if(legs != null){
            drawMech((Unit & Mechc)legs);

            float ft = Mathf.sin(legs.walkTime(), 3f, 3f);
            legOffset.trns(legs.baseRotation(), 0f, Mathf.lerp(ft * 0.18f * sway, 0f, unit.elevation));
            unit.trns(legOffset.x, legOffset.y);
        }

        if(unit instanceof Legsc){
            drawLegs((Unit & Legsc)unit);
        }

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        if(unit instanceof Payloadc){
            drawPayload((Unit & Payloadc)unit);
        }

        drawOcclusion(unit);

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

        if(legs != null){
            unit.trns(-legOffset.x, -legOffset.y);
        }

        if(unit.deactivated){
            drawDeactive(unit);
        }

        if(abilities.size > 0){
            for(Ability a : abilities){
                a.draw(unit);
                Draw.reset();
            }
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
        float size = Math.max(region.getWidth(), region.getHeight()) * Draw.scl;
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
            float width = weapon.region.getWidth();
            float recoil = -((mount.reload) / weapon.reload * weapon.recoil);
            float wx = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y) + Angles.trnsx(weaponRotation, 0, recoil),
                wy = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y) + Angles.trnsy(weaponRotation, 0, recoil);

            if(weapon.occlusion > 0){
                Drawf.shadow(wx, wy, weapon.occlusion);
            }

            Draw.rect(weapon.region, wx, wy,
            width * Draw.scl * -Mathf.sign(weapon.flipSprite),
            weapon.region.getHeight() * Draw.scl,
            weaponRotation);
        }

        Draw.reset();
    }

    public void drawBody(Unit unit){
        applyColor(unit);

        Draw.rect(region, unit, unit.rotation - 90);

        Draw.reset();
    }

    public void drawCell(Unit unit){
        applyColor(unit);

        Draw.color(cellColor(unit));
        Draw.rect(cellRegion, unit, unit.rotation - 90);
        Draw.reset();
    }

    public Color cellColor(Unit unit){
        return Tmp.c1.set(Color.black).lerp(unit.team.color, unit.healthf() + Mathf.absin(Time.time(), Math.max(unit.healthf() * 5f, 1f), 1f - unit.healthf()));
    }

    public void drawLight(Unit unit){
        if(lightRadius > 0){
            Drawf.light(unit.team, unit, lightRadius, lightColor, lightOpacity);
        }
    }

    public <T extends Unit & Legsc> void drawLegs(T unit){
        //Draw.z(Layer.groundUnit - 0.02f);

        Leg[] legs = unit.legs();

        float ssize = footRegion.getWidth() * Draw.scl * 1.5f;
        float rotation = unit.baseRotation();

        for(Leg leg : legs){
            Drawf.shadow(leg.base.x, leg.base.y, ssize);
        }

        //TODO should be below/above legs
        if(baseRegion.found()){
            Draw.rect(baseRegion, unit.x, unit.y, rotation);
        }

        //TODO figure out layering
        for(int i = 0; i < legs.length; i++){
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

            Lines.stroke(legRegion.getHeight() * Draw.scl * flips);
            Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, CapStyle.none, 0);

            Lines.stroke(legBaseRegion.getHeight() * Draw.scl * flips);
            Lines.line(legBaseRegion, leg.joint.x + Tmp.v1.x, leg.joint.y + Tmp.v1.y, leg.base.x, leg.base.y, CapStyle.none, 0);

            if(jointRegion.found()){
                Draw.rect(jointRegion, leg.joint.x, leg.joint.y);
            }

            if(baseJointRegion.found()){
                Draw.rect(baseJointRegion, position.x, position.y, rotation);
            }
        }

        Draw.reset();
    }

    public <T extends Unit & Mechc> void drawMech(T unit){
        Draw.reset();

        Draw.mixcol(Color.white, unit.hitTime);

        float e = unit.elevation;
        float sin = Mathf.lerp(Mathf.sin(unit.walkTime(), 3f, 1f), 0f, e);
        float ft = sin*(2.5f + (unit.hitSize-8f)/2f);
        float boostTrns = e * 2f;

        Floor floor = unit.isFlying() ? Blocks.air.asFloor() : unit.floorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.rect(legRegion,
            unit.x + Angles.trnsx(unit.baseRotation(), ft * i - boostTrns, -boostTrns*i),
            unit.y + Angles.trnsy(unit.baseRotation(), ft * i - boostTrns, -boostTrns*i),
            legRegion.getWidth() * i * Draw.scl,
            legRegion.getHeight() * Draw.scl - Math.max(-sin * i, 0) * legRegion.getHeight() * 0.5f * Draw.scl,
            unit.baseRotation() - 90 + 35f*i*e);
        }

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, unit.drownTime() * 0.4f);
        }else{
            Draw.color(Color.white);
        }

        Draw.rect(baseRegion, unit, unit.baseRotation() - 90);

        Draw.mixcol();
    }

    public void applyColor(Unit unit){
        Draw.mixcol(Color.white, unit.hitTime);
        if(unit.drownTime > 0 && unit.floorOn().isDeep()){
            Draw.mixcol(unit.floorOn().mapColor, unit.drownTime * 0.8f);
        }
    }

    //endregion
}
