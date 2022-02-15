package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class PayloadDeconstructor extends PayloadBlock{
    public float maxPayloadSize = 4;
    public float deconstructSpeed = 2.5f;
    public int dumpRate = 4;

    public PayloadDeconstructor(String name){
        super(name);

        outputsPayload = false;
        acceptsPayload = true;
        update = true;
        rotate = false;
        solid = true;
        size = 5;
        payloadSpeed = 1f;
        //make sure to display large units.
        clipSize = 120;
        hasItems = true;
        hasPower = true;
        itemCapacity = 100;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("progress", (PayloadDeconstructorBuild e) -> new Bar("bar.progress", Pal.ammo, () -> e.progress));
    }

    public class PayloadDeconstructorBuild extends PayloadBlockBuild<Payload>{
        public @Nullable Payload deconstructing;
        public @Nullable float[] accum;
        public float progress;
        public float time, speedScl;

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input
            for(int i = 0; i < 4; i++){
                if(blends(i)){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }

            Draw.z(Layer.blockOver);
            drawPayload();
            if(deconstructing != null){
                deconstructing.set(x + payVector.x, y + payVector.y, payRotation);

                Draw.z(Layer.blockOver);
                deconstructing.drawShadow(1f - progress);

                //TODO looks really bad
                Draw.draw(Layer.blockOver, () -> {
                    Drawf.construct(x, y, deconstructing.icon(), Pal.remove, deconstructing instanceof BuildPayload ? 0f : payRotation - 90f, 1f - progress, 1f - progress, time);
                    Draw.color(Pal.remove);
                    Draw.alpha(1f);

                    Lines.lineAngleCenter(x + Mathf.sin(time, 20f, tilesize / 2f * block.size - 3f), y, 90f, block.size * tilesize - 6f);

                    Draw.reset();
                });
            }

            Draw.rect(topRegion, x, y);
        }

        @Override
        public boolean acceptUnitPayload(Unit unit){
            return payload == null && deconstructing == null && unit.type.allowedInPayloads && !unit.spawnedByCore
                && unit.type.getTotalRequirements().length > 0 && unit.hitSize / tilesize <= maxPayloadSize;
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            super.handlePayload(source, payload);
            accum = null;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return deconstructing == null && this.payload == null && super.acceptPayload(source, payload) && payload.requirements().length > 0 && payload.fits(maxPayloadSize);
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(items.total() > 0){
                for(int i = 0; i < dumpRate; i++){
                    dumpAccumulate();
                }
            }

            if(deconstructing == null){
                progress = 0f;
            }

            payRotation = Angles.moveToward(payRotation, 90f, payloadRotateSpeed * edelta());

            if(deconstructing != null){
                var reqs = deconstructing.requirements();
                if(accum == null || reqs.length != accum.length){
                    accum = new float[reqs.length];
                }

                //check if there is enough space to get the items for deconstruction
                boolean canProgress = items.total() <= itemCapacity;
                if(canProgress){
                    for(var ac : accum){
                        if(ac >= 1f){
                            canProgress = false;
                            break;
                        }
                    }
                }

                //move progress forward if possible
                if(canProgress){
                    float shift = edelta() * deconstructSpeed / deconstructing.buildTime();
                    float realShift = Math.min(shift, 1f - progress);

                    progress += shift;
                    time += edelta();

                    for(int i = 0; i < reqs.length; i++){
                        accum[i] += reqs[i].amount * state.rules.deconstructRefundMultiplier * realShift;
                    }
                }

                speedScl = Mathf.lerpDelta(speedScl, canProgress ? 1f : 0f, 0.1f);

                //transfer items from accumulation buffer into block inventory when they reach integers
                for(int i = 0; i < reqs.length; i++){
                    int taken = Math.min((int)accum[i], itemCapacity - items.total());
                    if(taken > 0){
                        items.add(reqs[i].item, taken);
                        accum[i] -= taken;
                    }
                }

                //finish deconstruction, prepare for next payload.
                if(progress >= 1f){
                    canProgress = true;
                    //check for rounding errors
                    for(int i = 0; i < reqs.length; i++){
                        if(Mathf.equal(accum[i], 1f, 0.0001f)){
                            if(items.total() < itemCapacity){
                                items.add(reqs[i].item, 1);
                                accum[i] = 0f;
                            }else{
                                canProgress = false;
                                break;
                            }
                        }
                    }

                    if(canProgress){
                        Fx.breakBlock.at(x, y, deconstructing.size() / tilesize);

                        deconstructing = null;
                        accum = null;
                    }
                }
            }else if(moveInPayload(false) && payload != null){
                accum = new float[payload.requirements().length];
                deconstructing = payload;
                payload = null;
                progress = 0f;
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return progress;
            return super.sense(sensor);
        }

        @Override
        public boolean shouldConsume(){
            return deconstructing != null && enabled;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
            if(accum != null){
                write.s(accum.length);
                for(float v : accum){
                    write.f(v);
                }
            }else{
                write.s(0);
            }
            Payload.write(deconstructing, write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            progress = read.f();
            short accums = read.s();
            if(accums > 0){
                accum = new float[accums];
                for(int i = 0; i < accums; i++){
                    accum[i] = read.f();
                }
            }
            deconstructing = Payload.read(read);
        }
    }
}
