package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class UnitType extends UnlockableContent{
    static final float shadowTX = -12, shadowTY = -13, shadowColor = Color.toFloatBits(0, 0, 0, 0.22f);

    public boolean flying;
    public @NonNull Prov<? extends Unitc> constructor;
    public @NonNull Prov<? extends UnitController> defaultController = () -> !flying ? new GroundAI() : new FlyingAI();
    public float speed = 1.1f, boostSpeed = 0.75f, rotateSpeed = 5f, baseRotateSpeed = 5f;
    public float drag = 0.3f, mass = 1f, accel = 0.5f;
    public float health = 200f, range = -1;
    public boolean targetAir = true, targetGround = true;
    public boolean faceTarget = true, isCounted = true;

    public int itemCapacity = 30;
    public int drillTier = -1;
    public float buildSpeed = 1f, mineSpeed = 1f;

    public Color engineColor = Pal.engine;
    public float engineOffset = 5f, engineSize = 2.5f;

    public float hitsize = 6f;
    public float itemOffsetY = 3f;
    public float lightRadius = 60f, lightOpacity = 0.6f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Array<Weapon> weapons = new Array<>();
    public TextureRegion baseRegion, legRegion, region, cellRegion, occlusionRegion;

    public UnitType(String name){
        super(name);

        if(EntityMapping.map(name) != null){
            constructor = EntityMapping.map(name);
        }else{
            //TODO fix for mods
            throw new RuntimeException("Unit has no type: " + name);
            //constructor = () -> Nulls.unit;
        }
    }

    public UnitController createController(){
        return defaultController.get();
    }

    public Unitc create(Team team){
        Unitc unit = constructor.get();
        unit.team(team);
        unit.type(this);
        return unit;
    }

    public boolean hasWeapons(){
        return weapons.size > 0;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayUnit(table, this);
    }

    @CallSuper
    @Override
    public void init(){
        //set up default range
        if(range < 0){
            for(Weapon weapon : weapons){
                range = Math.max(range, weapon.bullet.range());
            }
        }
    }

    @CallSuper
    @Override
    public void load(){
        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        legRegion = Core.atlas.find(name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));
        occlusionRegion = Core.atlas.find("circle-shadow");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    //region drawing

    public void draw(Unitc unit){
        float opacity = Core.settings.getInt("unitopacity") / 100f;
        if(Mathf.zero(opacity)) return;

        if(unit.controller().isBeingControlled(player.unit())){
            Draw.alpha(opacity);
            drawControl(unit);
        }

        if(unit.isFlying()){
            Draw.z(Layer.darkness);
            drawShadow(unit, opacity);
        }

        float z = Mathf.lerp(Layer.groundUnit, Layer.flyingUnit, unit.elevation());

        Draw.z(z - 0.02f);
        if(unit instanceof Legsc){
            drawLegs((Legsc)unit, opacity);
        }

        Draw.z(z - 0.01f);
        drawOcclusion(unit, opacity);

        Draw.z(z);
        drawEngine(unit, opacity);
        drawBody(unit,opacity);
        drawWeapons(unit, opacity);
        if(drawCell) drawCell(unit, opacity);
        if(drawItems) drawItems(unit);
        drawLight(unit);
    }

    public void drawControl(Unitc unit, float opacity){
        Draw.z(Layer.groundUnit - 2);

        Draw.color(Pal.accent, Color.white, Mathf.absin(4f, 0.3f));
        Draw.alpha(Draw.getColor().a * opacity);
        Lines.poly(unit.x(), unit.y(), 4, unit.hitSize() + 1.5f);

        Draw.reset();
    }

    public void drawControl(Unitc unit){
        drawControl(unit, 1f);
    }

    public void drawShadow(Unitc unit, float opacity){
        Draw.color(shadowColor);
        Draw.alpha(Draw.getColor().a * opacity);
        Draw.rect(region, unit.x() + shadowTX * unit.elevation(), unit.y() + shadowTY * unit.elevation(), unit.rotation() - 90);
        Draw.color();
    }

    public void drawShadow(Unitc unit){
        drawShadow(unit, 1f);
    }

    public void drawOcclusion(Unitc unit, float opacity){
        Draw.color(0, 0, 0, 0.4f);
        Draw.alpha(Draw.getColor().a * opacity);
        float rad = 1.6f;
        float size = Math.max(region.getWidth(), region.getHeight()) * Draw.scl;
        Draw.rect(occlusionRegion, unit, size * rad, size * rad);
        Draw.color();
    }

    public void drawOcclusion(Unitc unit){
        drawOcclusion(unit, 1f);
    }


    public void drawItems(Unitc unit){
        applyColor(unit);

        //draw back items
        if(unit.hasItem() && unit.itemTime() > 0.01f){
            float size = (itemSize + Mathf.absin(Time.time(), 5f, 1f)) * unit.itemTime();

            Draw.mixcol(Pal.accent, Mathf.absin(Time.time(), 5f, 0.5f));
            Draw.rect(unit.item().icon(Cicon.medium),
            unit.x() + Angles.trnsx(unit.rotation() + 180f, itemOffsetY),
            unit.y() + Angles.trnsy(unit.rotation() + 180f, itemOffsetY),
            size, size, unit.rotation());

            Draw.mixcol();

            Lines.stroke(1f, Pal.accent);
            Lines.circle(
            unit.x() + Angles.trnsx(unit.rotation() + 180f, itemOffsetY),
            unit.y() + Angles.trnsy(unit.rotation() + 180f, itemOffsetY),
            (3f + Mathf.absin(Time.time(), 5f, 1f)) * unit.itemTime());

            if(unit.isLocal()){
                Fonts.outline.draw(unit.stack().amount + "",
                unit.x() + Angles.trnsx(unit.rotation() + 180f, itemOffsetY),
                unit.y() + Angles.trnsy(unit.rotation() + 180f, itemOffsetY) - 3,
                Pal.accent, 0.25f * unit.itemTime() / Scl.scl(1f), false, Align.center
                );
            }

            Draw.reset();
        }
    }

    public void drawEngine(Unitc unit, float opacity){
        if(!unit.isFlying()) return;

        if(unit instanceof Trailc){
            Trail trail = ((Trailc)unit).trail();

            float cx = unit.x() + Angles.trnsx(unit.rotation() + 180, engineOffset),
            cy = unit.y() + Angles.trnsy(unit.rotation() + 180, engineOffset);
            trail.update(cx, cy);
            trail.draw(unit.team().color, (engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f) * unit.elevation()));
        }

        Draw.color(unit.team().color);
        Draw.alpha(Draw.getColor().a * opacity);
        Fill.circle(
            unit.x() + Angles.trnsx(unit.rotation() + 180, engineOffset),
            unit.y() + Angles.trnsy(unit.rotation() + 180, engineOffset),
            (engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f) * unit.elevation())
        );
        Draw.color(Color.white);
        Draw.alpha(opacity);
        Fill.circle(
            unit.x() + Angles.trnsx(unit.rotation() + 180, engineOffset - 1f),
            unit.y() + Angles.trnsy(unit.rotation() + 180, engineOffset - 1f),
            (engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f)) / 2f  * unit.elevation()
        );
        Draw.color();
    }

    public void drawEngine(Unitc unit){
        drawEngine(unit, 1f);
    }

    public void drawWeapons(Unitc unit, float opacity){
        applyColor(unit);
        Draw.alpha(Draw.getColor().a * opacity);

        for(WeaponMount mount : unit.mounts()){
            Weapon weapon = mount.weapon;

            for(int i : (weapon.mirror ? Mathf.signs : Mathf.one)){
                i *= Mathf.sign(weapon.flipped);

                float rotation = unit.rotation() - 90;
                float weaponRotation  = rotation + (weapon.rotate ? mount.rotation : 0);
                float width = i > 0 ? -weapon.region.getWidth() : weapon.region.getWidth();
                float recoil = -(mount.reload / weapon.reload * weapon.recoil) * (weapon.alternate ? Mathf.num(i == Mathf.sign(mount.side)) : 1);

                if(weapon.mirror) rotation = weaponRotation;

                Draw.rect(weapon.region,
                unit.x() + Angles.trnsx(rotation, weapon.x * i, weapon.y) + Angles.trnsx(weaponRotation, 0, recoil),
                unit.y() + Angles.trnsy(rotation, weapon.x * i, weapon.y) + Angles.trnsy(weaponRotation, 0, recoil),
                width * Draw.scl,
                weapon.region.getHeight() * Draw.scl,
                weaponRotation);
            }
        }

        Draw.reset();
    }

    public void drawWeapons(Unitc unit){
        drawWeapons(unit, 1f);
    }

    public void drawBody(Unitc unit, float opacity){
        applyColor(unit);

        Draw.alpha(Draw.getColor().a * opacity);
        Draw.rect(region, unit, unit.rotation() - 90);

        Draw.reset();
    }

    public void drawBody(Unitc unit){
        drawBody(unit, 1f);
    }

    public void drawCell(Unitc unit, float opacity){
        applyColor(unit);

        Draw.color(Color.black, unit.team().color, unit.healthf() + Mathf.absin(Time.time(), Math.max(unit.healthf() * 5f, 1f), 1f - unit.healthf()));
        Draw.alpha(Draw.getColor().a * opacity);
        Draw.rect(cellRegion, unit, unit.rotation() - 90);
        Draw.reset();
    }

    public void drawCell(Unitc unit){
        drawCell(unit, 1f);
    }

    public void drawLight(Unitc unit){
        if(lightRadius > 0){
            Drawf.light(unit, lightRadius, lightColor, lightOpacity);
        }
    }

    public void drawLegs(Legsc unit, float opacity){
        Draw.reset();

        Draw.mixcol(Color.white, unit.hitTime());

        float ft = Mathf.sin(unit.walkTime(), 6f, 2f + unit.hitSize() / 15f);

        Floor floor = unit.floorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, 0.5f);
        }

        Draw.alpha(Draw.getColor().a * opacity);
        for(int i : Mathf.signs){
            Draw.rect(legRegion,
            unit.x() + Angles.trnsx(unit.baseRotation(), ft * i),
            unit.y() + Angles.trnsy(unit.baseRotation(), ft * i),
            legRegion.getWidth() * i * Draw.scl, legRegion.getHeight() * Draw.scl - Mathf.clamp(ft * i, 0, 2), unit.baseRotation() - 90);
        }

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, unit.drownTime() * 0.4f);
        }else{
            Draw.color(Color.white);
        }

        Draw.alpha(Draw.getColor().a * opacity);
        Draw.rect(baseRegion, unit, unit.baseRotation() - 90);

        Draw.mixcol();
    }

    public void drawLegs(Legsc unit){
        drawLegs(unit, 1f);
    }

    public void applyColor(Unitc unit){
        Draw.mixcol(Color.white, unit.hitTime());
        if(unit.drownTime() > 0 && unit.floorOn().isDeep()){
            Draw.mixcol(unit.floorOn().mapColor, unit.drownTime() * 0.8f);
        }
    }

    //endregion
}
