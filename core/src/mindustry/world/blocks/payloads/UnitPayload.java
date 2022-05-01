package mindustry.world.blocks.payloads;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.EntityCollisions.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class UnitPayload implements Payload{
    public static final float overlayDuration = 40f;

    public Unit unit;
    public float overlayTime = 0f;
    public @Nullable TextureRegion overlayRegion;

    public UnitPayload(Unit unit){
        this.unit = unit;
    }

    /** Flashes a red overlay region. */
    public void showOverlay(TextureRegion icon){
        overlayRegion = icon;
        overlayTime = 1f;
    }

    /** Flashes a red overlay region. */
    public void showOverlay(TextureRegionDrawable icon){
        if(icon == null || headless) return;
        showOverlay(icon.getRegion());
    }

    @Override
    public UnlockableContent content(){
        return unit.type;
    }

    @Override
    public ItemStack[] requirements(){
        return unit.type.getTotalRequirements();
    }

    @Override
    public float buildTime(){
        return unit.type.getBuildTime();
    }

    @Override
    public void write(Writes write){
        write.b(payloadUnit);
        write.b(unit.classId());
        unit.write(write);
    }

    @Override
    public void set(float x, float y, float rotation){
        unit.set(x, y);
        unit.rotation = rotation;
    }

    @Override
    public float x(){
        return unit.x;
    }

    @Override
    public float y(){
        return unit.y;
    }

    @Override
    public float rotation(){
        return unit.rotation;
    }

    @Override
    public float size(){
        return unit.hitSize;
    }

    @Override
    public boolean dump(){
        //TODO should not happen
        if(unit.type == null) return true;

        if(!Units.canCreate(unit.team, unit.type)){
            overlayTime = 1f;
            overlayRegion = null;
            return false;
        }

        //check if unit can be dumped here
        SolidPred solid = unit.solidity();
        if(solid != null){
            Tmp.v1.trns(unit.rotation, 1f);

            int tx = World.toTile(unit.x + Tmp.v1.x), ty = World.toTile(unit.y + Tmp.v1.y);

            //cannot dump on solid blocks
            if(solid.solid(tx, ty)) return false;
        }

        //cannot dump when there's a lot of overlap going on
        if(!unit.type.flying && Units.count(unit.x, unit.y, unit.physicSize(), o -> o.isGrounded() && (o.type.allowLegStep == unit.type.allowLegStep)) > 0){
            return false;
        }

        //no client dumping
        if(Vars.net.client()) return true;

        //prevents stacking
        unit.vel.add(Mathf.range(0.5f), Mathf.range(0.5f));
        unit.add();
        unit.unloaded();
        Events.fire(new UnitUnloadEvent(unit));

        return true;
    }

    @Override
    public void drawShadow(float alpha){
        //TODO should not happen
        if(unit.type == null) return;

        unit.type.drawSoftShadow(unit, alpha);
    }

    @Override
    public void draw(){
        //TODO should not happen
        if(unit.type == null) return;

        //TODO this would be more accurate but has all sorts of associated problems (?)
        if(false){
            float e = unit.elevation;
            unit.elevation = 0f;
            //avoids drawing mining or building
            unit.type.draw(unit);
            unit.elevation = e;
            return;
        }

        unit.type.drawSoftShadow(unit);
        Draw.rect(unit.type.fullIcon, unit.x, unit.y, unit.rotation - 90);
        unit.type.drawCell(unit);

        //draw warning
        if(overlayTime > 0){
            var region = overlayRegion == null ? Icon.warning.getRegion() : overlayRegion;
            Draw.color(Color.scarlet);
            Draw.alpha(0.8f * Interp.exp5Out.apply(overlayTime));

            float size = 8f;
            Draw.rect(region, unit.x, unit.y, size, size);

            Draw.reset();

            overlayTime = Math.max(overlayTime - Time.delta/overlayDuration, 0f);
        }
    }

    @Override
    public TextureRegion icon(){
        return unit.type.fullIcon;
    }
}
