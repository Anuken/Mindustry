package mindustry.world.blocks.units;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.state;

public class Reconstructor extends UnitBlock{
    public @Load(value = "@-top", fallback = "factory-top") TextureRegion topRegion;
    public @Load(value = "@-out", fallback = "factory-out") TextureRegion outRegion;
    public @Load(value = "@-in", fallback = "factory-in") TextureRegion inRegion;
    public int tier = 1;
    public float constructTime = 60 * 2;

    public Reconstructor(String name){
        super(name);
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(outRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy());
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("progress", entity -> new Bar("bar.progress", Pal.ammo, ((ReconstructorEntity)entity)::fraction));
    }

    public class ReconstructorEntity extends UnitBlockEntity{

        public float fraction(){
            return progress / constructTime;
        }

        @Override
        public boolean acceptPayload(Tilec source, Payload payload){
            return this.payload == null
                && relativeTo(source) != rotation()
                && payload instanceof UnitPayload
                && ((UnitPayload)payload).unit.type().upgrade != null
                && ((UnitPayload)payload).unit.type().tier == tier;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input
            for(int i = 0; i < 4; i++){
                if(blends(this, i) && i != rotation()){
                    Draw.rect(inRegion, x, y, i * 90);
                }
            }

            Draw.rect(outRegion, x, y, rotdeg());

            if(constructing() && hasArrived()){
                Draw.draw(Layer.blockOver, () -> {
                    Draw.alpha(1f - progress/ constructTime);
                    Draw.rect(payload.unit.type().icon(Cicon.full), x, y, rotdeg() - 90);
                    Draw.reset();
                    Drawf.construct(this, payload.unit.type().upgrade, rotdeg() - 90f, progress / constructTime, speedScl, time);
                });
            }else{
                Draw.z(Layer.blockOver);
                payRotation = rotdeg();

                drawPayload();
            }

            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            boolean valid = false;

            if(payload != null){
                //check if offloading
                if(payload.unit.type().upgrade == null || payload.unit.type().tier != tier){
                    outputPayload();
                }else{ //update progress
                    if(moveInPayload()){
                        if(consValid()){
                            valid = true;
                            progress += edelta();
                        }

                        //upgrade the unit
                        if(progress >= constructTime){
                            payload.unit = payload.unit.type().upgrade.create(payload.unit.team());
                            progress = 0;
                            Effects.shake(2f, 3f, this);
                            Fx.producesmoke.at(this);
                            consume();
                        }
                    }
                }
            }

            speedScl = Mathf.lerpDelta(speedScl, Mathf.num(valid), 0.05f);
            time += edelta() * speedScl * state.rules.unitBuildSpeedMultiplier;
        }

        public boolean constructing(){
            return payload != null && payload.unit.type().upgrade != null && payload.unit.type().tier == tier;
        }
    }
}
