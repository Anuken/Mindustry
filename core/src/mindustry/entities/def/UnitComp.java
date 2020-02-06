package mindustry.entities.def;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

@Component
abstract class UnitComp implements Healthc, Velc, Statusc, Teamc, Itemsc, Hitboxc, Rotc, Massc, Unitc, Weaponsc, DrawShadowc{
    private UnitController controller;
    private UnitDef type;

    @Override
    public TextureRegion getShadowRegion(){
        return type.region;
    }

    @Override
    public float clipSize(){
        return type.region.getWidth() * 2f;
    }

    @Override
    public int itemCapacity(){
        return type.itemCapacity;
    }

    @Override
    public float bounds(){
        return hitSize() *  2f;
    }

    @Override
    public void controller(UnitController controller){
        this.controller = controller;
        controller.unit(this);
    }

    @Override
    public UnitController controller(){
        return controller;
    }

    @Override
    public void set(UnitDef def, UnitController controller){
        type(type);
        controller(controller);
    }

    @Override
    public void type(UnitDef type){
        this.type = type;
        controller(type.createController());
        setupWeapons(type);
    }

    @Override
    public UnitDef type(){
        return type;
    }

    @Override
    public void update(){
        //apply knockback based on spawns
        //TODO move elsewhere
        if(team() != state.rules.waveTeam){
            float relativeSize = state.rules.dropZoneRadius + bounds()/2f + 1f;
            for(Tile spawn : spawner.getGroundSpawns()){
                if(withinDst(spawn.worldx(), spawn.worldy(), relativeSize)){
                    vel().add(Tmp.v1.set(this).sub(spawn.worldx(), spawn.worldy()).setLength(0.1f + 1f - dst(spawn) / relativeSize).scl(0.45f * Time.delta()));
                }
            }
        }

        Tile tile = tileOn();
        Floor floor = floorOn();

        if(tile != null){
            //unit block update
            tile.block().unitOn(tile, this);

            //apply damage
            if(floor.damageTaken > 0f){
                damageContinuous(floor.damageTaken);
            }
        }
    }

    @Override
    public void drawLight(){
        //TODO move
        if(type.lightRadius > 0){
            renderer.lights.add(getX(), getY(), type.lightRadius, type.lightColor, 0.6f);
        }
    }

    @Override
    public void draw(){
        //draw power cell - TODO move
        Draw.color(Color.black, team().color, healthf() + Mathf.absin(Time.time(), Math.max(healthf() * 5f, 1f), 1f - healthf()));
        Draw.rect(type.cellRegion, getX(), getY(), rotation() - 90);
        Draw.color();
    }

    @Override
    public void killed(){
        float explosiveness = 2f + item().explosiveness * stack().amount;
        float flammability = item().flammability * stack().amount;
        Damage.dynamicExplosion(getX(), getY(), flammability, explosiveness, 0f, bounds() / 2f, Pal.darkFlame);

        //TODO cleanup
        //ScorchDecal.create(getX(), getY());
        Fx.explosion.at(this);
        Effects.shake(2f, 2f, this);

        Sounds.bang.at(this);
        Events.fire(new UnitDestroyEvent(this));

        //TODO implement suicide bomb trigger
        //if(explosiveness > 7f && this == player){
        //    Events.fire(Trigger.suicideBomb);
        //}
    }
}
