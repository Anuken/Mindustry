package mindustry.world.blocks.payloads;

import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.world.blocks.payloads.PayloadMassDriver.PayloadDriverState.*;

public class PayloadMassDriver extends PayloadBlock{
    public float range = 100f;
    public float rotateSpeed = 2f;
    public float length = 89 / 8f;
    public float knockback = 5f;
    public float reload = 30f;
    public float chargeTime = 100f;
    public float maxPayloadSize = 3;
    public float grabWidth = 8f, grabHeight = 11/4f;
    public Effect shootEffect = Fx.shootBig2;
    public Effect smokeEffect = Fx.shootPayloadDriver;
    public Effect receiveEffect = Fx.payloadReceive;
    public Sound shootSound = Sounds.shootBig;
    public float shake = 3f;

    public Effect transferEffect = new Effect(11f, 600f, e -> {
        if(!(e.data instanceof PayloadMassDriverData data)) return;
        Tmp.v1.set(data.x, data.y).lerp(data.ox, data.oy, Interp.sineIn.apply(e.fin()));
        data.payload.set(Tmp.v1.x, Tmp.v1.y, e.rotation);
        data.payload.draw();
    }).layer(Layer.flyingUnitLow - 1);

    public @Load("@-base") TextureRegion baseRegion;
    public @Load("@-cap") TextureRegion capRegion;
    public @Load("@-left") TextureRegion leftRegion;
    public @Load("@-right") TextureRegion rightRegion;
    public @Load("@-cap-outline") TextureRegion capOutlineRegion;
    public @Load("@-left-outline") TextureRegion leftOutlineRegion;
    public @Load("@-right-outline") TextureRegion rightOutlineRegion;
    public @Load("bridge-arrow") TextureRegion arrow;

    public PayloadMassDriver(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasPower = true;
        outlineIcon = true;
        sync = true;
        rotate = true;
        outputsPayload = true;
        group = BlockGroup.units;
        regionRotated1 = 1;

        //point2 is relative
        config(Point2.class, (PayloadDriverBuild tile, Point2 point) -> tile.link = Point2.pack(point.x + tile.tileX(), point.y + tile.tileY()));
        config(Integer.class, (PayloadDriverBuild tile, Integer point) -> tile.link = point);
    }

    @Override
    public void init(){
        super.init();
        updateClipRadius(range);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.payloadCapacity, StatValues.squared(maxPayloadSize, StatUnit.blocksSquared));
        stats.add(Stat.reload, 60f / (chargeTime + reload), StatUnit.seconds);
        stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, outRegion, region};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(baseRegion, plan.drawx(), plan.drawy());
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(region, plan.drawx(), plan.drawy());
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize, y * tilesize, range, Pal.accent);

        //check if a mass driver is selected while placing this driver
        if(!control.input.config.isShown()) return;
        Building selected = control.input.config.getSelected();
        if(selected == null || selected.block != this || !selected.within(x * tilesize, y * tilesize, range)) return;

        //if so, draw a dotted line towards it while it is in range
        float sin = Mathf.absin(Time.time, 6f, 1f);
        Tmp.v1.set(x * tilesize + offset, y * tilesize + offset).sub(selected.x, selected.y).limit((size / 2f + 1) * tilesize + sin + 0.5f);
        float x2 = x * tilesize - Tmp.v1.x, y2 = y * tilesize - Tmp.v1.y,
            x1 = selected.x + Tmp.v1.x, y1 = selected.y + Tmp.v1.y;
        int segs = (int)(selected.dst(x * tilesize, y * tilesize) / tilesize);

        Lines.stroke(4f, Pal.gray);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Lines.stroke(2f, Pal.placing);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Draw.reset();
    }

    @Override
    public TextureRegion[] makeIconRegions(){
        return new TextureRegion[]{leftRegion, rightRegion, capRegion};
    }

    public class PayloadDriverBuild extends PayloadBlockBuild<Payload>{
        public int link = -1;
        public float turretRotation = 90;
        public float reloadCounter = 0f, charge = 0f;
        public float targetSize = grabWidth*2f, curSize = targetSize;
        public float payLength = 0f, effectDelayTimer = -1f;
        public PayloadDriverBuild lastOther;
        public boolean loaded;
        public boolean charging;
        public PayloadDriverState state = idle;
        public Queue<Building> waitingShooters = new Queue<>();
        public Payload recPayload;

        public Building currentShooter(){
            return waitingShooters.isEmpty() ? null : waitingShooters.first();
        }

        @Override
        public void updateTile(){
            super.updateTile();
            Building link = world.build(this.link);
            boolean hasLink = linkValid();

            //discharge when charging isn't happening
            if(!charging){
                charge -= Time.delta * 10f;
                if(charge < 0) charge = 0f;
            }

            curSize = Mathf.lerpDelta(curSize, targetSize, 0.05f);
            targetSize = grabWidth*2f;

            if(payload != null){
                targetSize = payload.size();
            }

            boolean pos = effectDelayTimer > 0;
            effectDelayTimer -= Time.delta;
            if(effectDelayTimer <= 0 && pos && lastOther != null){
                var other = lastOther;
                float cx = Angles.trnsx(other.turretRotation, length), cy = Angles.trnsy(other.turretRotation, length);
                receiveEffect.at(x - cx/2f, y - cy/2f, turretRotation);
                reloadCounter = 1f;
                Effect.shake(shake, shake, this);
            }

            charging = false;

            if(hasLink){
                this.link = link.pos();
            }

            //reload regardless of state
            reloadCounter -= edelta() / reload;
            if(reloadCounter < 0) reloadCounter = 0f;

            var current = currentShooter();

            //cleanup waiting shooters that are not valid
            if(current != null &&
                !(
                    current instanceof PayloadDriverBuild entity &&
                    current.isValid() &&
                    entity.efficiency > 0 && entity.block == block &&
                    entity.link == pos() && within(current, range)
                )){
                waitingShooters.removeFirst();
            }

            //switch states
            if(state == idle){
                //start accepting when idle and there's space
                if(!waitingShooters.isEmpty() && payload == null){
                    state = accepting;
                }else if(hasLink){ //switch to shooting if there's a valid link.
                    state = shooting;
                }
            }

            //dump when idle or accepting
            if((state == idle || state == accepting) && payload != null){
                if(loaded){
                    payLength -= payloadSpeed * delta();
                    if(payLength <= 0f){
                        loaded = false;
                        payVector.setZero();
                        payRotation = Angles.moveToward(payRotation, turretRotation + 180f, payloadRotateSpeed * delta());
                    }
                }else if(effectDelayTimer <= 0){
                    moveOutPayload();
                }
            }

            //skip when there's no power
            if(efficiency <= 0f){
                return;
            }

            if(state == accepting){
                //if there's nothing shooting at this or items are full, bail out
                if(currentShooter() == null || payload != null){
                    state = idle;
                    return;
                }

                if(currentShooter().getPayload() != null){
                    targetSize = recPayload == null ? currentShooter().getPayload().size() : recPayload.size();
                }

                //align to shooter rotation
                turretRotation = Angles.moveToward(turretRotation, angleTo(currentShooter()), rotateSpeed * efficiency);
            }else if(state == shooting){
                //if there's nothing to shoot at OR someone wants to shoot at this thing, bail
                if(!hasLink || (!waitingShooters.isEmpty() && payload == null)){
                    state = idle;
                    return;
                }

                float targetRotation = angleTo(link);
                boolean movedOut = false;

                payRotation = Angles.moveToward(payRotation, turretRotation, payloadRotateSpeed * delta());
                if(loaded){
                    float loadLength = length - reloadCounter *knockback;
                    payLength += payloadSpeed * delta();
                    if(payLength >= loadLength){
                        payLength = loadLength;
                        movedOut = true;
                    }
                }else if(moveInPayload()){
                    payLength = 0f;
                    loaded = true;
                }

                //make sure payload firing can happen
                if(movedOut && payload != null && link.getPayload() == null){
                    var other = (PayloadDriverBuild)link;

                    if(!other.waitingShooters.contains(this)){
                        other.waitingShooters.addLast(this);
                    }

                    if(reloadCounter <= 0){
                        //align to target location
                        turretRotation = Angles.moveToward(turretRotation, targetRotation, rotateSpeed * efficiency);

                        //fire when it's the first in the queue and angles are ready.
                        if(other.currentShooter() == this &&
                        other.state == accepting &&
                        other.reloadCounter <= 0f &&
                        Angles.within(turretRotation, targetRotation, 1f) && Angles.within(other.turretRotation, targetRotation + 180f, 1f)){
                            charge += edelta();
                            charging = true;

                            if(charge >= chargeTime){
                                float cx = Angles.trnsx(turretRotation, length), cy = Angles.trnsy(turretRotation, length);

                                //effects
                                shootEffect.at(x + cx, y + cy, turretRotation);
                                smokeEffect.at(x, y, turretRotation);

                                Effect.shake(shake, shake, this);
                                shootSound.at(this, Mathf.random(0.9f, 1.1f));
                                transferEffect.at(x + cx, y + cy, turretRotation, new PayloadMassDriverData(x + cx, y + cy, other.x - cx, other.y - cy, payload));
                                Payload pay = payload;
                                other.recPayload = payload;
                                other.effectDelayTimer = transferEffect.lifetime;

                                //transfer payload
                                other.handlePayload(this, pay);
                                other.lastOther = this;
                                other.payVector.set(-cx, -cy);
                                other.payRotation = turretRotation;
                                other.payLength = length;
                                other.loaded = true;
                                other.updatePayload();
                                other.recPayload = null;

                                if(other.waitingShooters.size != 0 && other.waitingShooters.first() == this){
                                    other.waitingShooters.removeFirst();
                                }
                                other.state = idle;

                                //reset state after shooting immediately
                                payload = null;
                                payLength = 0f;
                                loaded = false;
                                state = idle;
                                reloadCounter = 1f;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return Mathf.clamp(1f - reloadCounter / reload);
            return super.sense(sensor);
        }

        @Override
        public void updatePayload(){
            if(payload != null){
                if(loaded){
                    payload.set(x + Angles.trnsx(turretRotation, payLength), y + Angles.trnsy(turretRotation, payLength), payRotation);
                }else{
                    payload.set(x + payVector.x, y + payVector.y, payRotation);
                }
            }
        }

        @Override
        public void draw(){
            float
            tx = x + Angles.trnsx(turretRotation + 180f, reloadCounter * knockback),
            ty = y + Angles.trnsy(turretRotation + 180f, reloadCounter * knockback), r = turretRotation - 90;

            Draw.rect(baseRegion, x, y);

            //draw input
            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }

            Draw.rect(outRegion, x, y, rotdeg());

            if(payload != null){
                updatePayload();

                if(effectDelayTimer <= 0){
                    Draw.z(loaded ? Layer.blockOver + 0.2f : Layer.blockOver);
                    payload.draw();
                }
            }

            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);

            Draw.z(Layer.turret);
            //TODO
            Drawf.shadow(region, tx - (size / 2f), ty - (size / 2f), r);

            Tmp.v1.trns(turretRotation, 0, -(curSize/2f - grabWidth));
            Tmp.v2.trns(rotation, -Math.max(curSize/2f - grabHeight - length, 0f), 0f);
            float rx = tx + Tmp.v1.x + Tmp.v2.x, ry = ty + Tmp.v1.y + Tmp.v2.y;
            float lx = tx - Tmp.v1.x + Tmp.v2.x, ly = ty - Tmp.v1.y + Tmp.v2.y;

            Draw.rect(capOutlineRegion, tx, ty, r);
            Draw.rect(leftOutlineRegion, lx, ly, r);
            Draw.rect(rightOutlineRegion, rx, ry, r);

            Draw.rect(leftRegion, lx, ly, r);
            Draw.rect(rightRegion, rx, ry, r);
            Draw.rect(capRegion, tx, ty, r);

            Draw.z(Layer.effect);

            if(charge > 0 && linkValid()){
                Building link = world.build(this.link);

                float fin = Interp.pow2Out.apply(charge / chargeTime), fout = 1f-fin, len = length*1.8f, w = curSize/2f + 7f*fout;
                Vec2 right = Tmp.v1.trns(turretRotation, len, w);
                Vec2 left = Tmp.v2.trns(turretRotation, len, -w);

                Lines.stroke(fin * 1.2f, Pal.accent);
                Lines.line(x + left.x, y + left.y, link.x - right.x, link.y - right.y);
                Lines.line(x + right.x, y + right.y, link.x - left.x, link.y - left.y);

                for(int i = 0; i < 4; i++){
                    Tmp.v3.set(x, y).lerp(link.x, link.y, 0.5f + (i - 2) * 0.1f);
                    Draw.scl(fin * 1.1f);
                    Draw.rect(arrow, Tmp.v3.x, Tmp.v3.y, turretRotation);
                    Draw.scl();
                }

                Draw.reset();
            }
        }

        @Override
        public void drawConfigure(){
            float sin = Mathf.absin(Time.time, 6f, 1f);

            Draw.color(Pal.accent);
            Lines.stroke(1f);
            Drawf.circles(x, y, (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.accent);

            for(var shooter : waitingShooters){
                Drawf.circles(shooter.x, shooter.y, (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(shooter.x, shooter.y, x, y, size * tilesize + sin, 4f + sin, Pal.place);
            }

            if(linkValid()){
                Building target = world.build(link);
                Drawf.circles(target.x, target.y, (target.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
                Drawf.arrow(x, y, target.x, target.y, size * tilesize + sin, 4f + sin);
            }

            Drawf.dashCircle(x, y, range, Pal.accent);
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other){
                if(link == -1) deselect();
                configure(-1);
                return false;
            }

            if(link == other.pos()){
                configure(-1);
                return false;
            }else if(other.block instanceof PayloadMassDriver && other.dst(tile) <= range && other.team == team){
                configure(other.pos());
                return false;
            }

            return true;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return super.acceptPayload(source, payload) && payload.size() <= maxPayloadSize * tilesize;
        }

        protected boolean linkValid(){
            return link != -1 && world.build(this.link) instanceof PayloadDriverBuild other && other.block == block && other.team == team && within(other, range);
        }

        @Override
        public Point2 config(){
            if(tile == null) return null;
            return Point2.unpack(link).sub(tile.x, tile.y);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(link);
            write.f(turretRotation);
            write.b((byte)state.ordinal());

            write.f(reloadCounter);
            write.f(charge);
            write.bool(loaded);
            write.bool(charging);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            link = read.i();
            turretRotation = read.f();
            state = PayloadDriverState.all[read.b()];

            if(revision >= 1){
                reloadCounter = read.f();
                charge = read.f();
                loaded = read.bool();
                charging = read.bool();
            }
        }
    }

    public static class PayloadMassDriverData{
        public float x, y, ox, oy;
        public Payload payload;

        public PayloadMassDriverData(float x, float y, float ox, float oy, Payload payload){
            this.x = x;
            this.y = y;
            this.ox = ox;
            this.oy = oy;
            this.payload = payload;
        }
    }

    public enum PayloadDriverState{
        idle, accepting, shooting;

        public static final PayloadDriverState[] all = values();
    }
}
