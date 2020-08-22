package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Reconstructor extends UnitBlock{
    public UpgradePlan[] upgrades = {};
    public int[] capacities;

    public Reconstructor(String name){
        super(name);
        consumes.add(new ConsumeItemDynamic((ReconstructorBuild e) -> e.currentPlan != UpgradePlan.empty ? e.currentPlan.requirements : ItemStack.empty));
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(outRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy());
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("progress", (ReconstructorBuild entity) -> new Bar("bar.progress", Pal.ammo, entity::fraction));
        bars.add("units", (ReconstructorBuild e) ->
        new Bar(
            () -> e.unit() == null ? "[lightgray]" + Iconc.cancel :
                Core.bundle.format("bar.unitcap",
                Fonts.getUnicodeStr(e.unit().name),
                teamIndex.countType(e.team, e.unit()),
                Units.getCap(e.team)
            ),
            () -> Pal.power,
            () -> e.unit() == null ? 0f : (float)teamIndex.countType(e.team, e.unit()) / Units.getCap(e.team)
        ));
    }

    @Override
    public void setStats(){
        super.setStats();
    }

    @Override
    public void init(){
        capacities = new int[Vars.content.items().size];
        if(consumes.has(ConsumeType.item) && consumes.get(ConsumeType.item) instanceof ConsumeItems){
            for(ItemStack stack : consumes.<ConsumeItems>get(ConsumeType.item).items){
                capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
                itemCapacity = Math.max(itemCapacity, stack.amount * 2);
            }
        }

        super.init();
    }

    public static class UpgradePlan{
        public static final UpgradePlan empty = new UpgradePlan();

        /** the "base" unit or the one that is being upgraded */
        public UnitType base;
        public UnitType unit;
        public ItemStack[] requirements = ItemStack.empty;
        public float time;

        public UpgradePlan(UnitType base, UnitType unit, ItemStack[] requirements){

            this.base = base;
            this.unit = unit;
            this.time = 0f;
            for(ItemStack stack : requirements){
                this.time += stack.amount * stack.item.cost * 60f;
            }
            this.requirements = requirements;
        }

        UpgradePlan(){}
    }

    public class ReconstructorBuild extends UnitBuild{
        public UpgradePlan currentPlan = UpgradePlan.empty;
        public float constructTime = 1f;
        public float fraction(){
            return progress / constructTime;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return currentPlan != UpgradePlan.empty && items.get(item) < getMaximumAccepted(item) &&
                Structs.contains(currentPlan.requirements, stack -> stack.item == item);
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return this.payload == null
                && relativeTo(source) != rotation
                && payload instanceof UnitPayload
                && hasUpgrade(((UnitPayload)payload).unit.type());
        }

        @Override
        public int getMaximumAccepted(Item item){
            for(ItemStack stack : currentPlan.requirements){
                capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
                itemCapacity = Math.max(itemCapacity, stack.amount * 2);
            }
            return capacities[item.id];
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input
            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.rect(inRegion, x, y, i * 90);
                }
            }

            Draw.rect(outRegion, x, y, rotdeg());

            if(constructing() && hasArrived()){
                Draw.draw(Layer.blockOver, () -> {
                    Draw.alpha(1f - progress/ constructTime);
                    Draw.rect(payload.unit.type().icon(Cicon.full), x, y, rotdeg() - 90);
                    Draw.reset();
                    Drawf.construct(this, currentPlan.unit, rotdeg() - 90f, progress / constructTime, speedScl, time);
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
            //update plan (this was the only way I could get it to work help help help help)
            try {
                currentPlan = upgrade(payload.unit.type());
            } catch(NullPointerException npe) {
                currentPlan = UpgradePlan.empty;
            }

            if(payload != null){
                //check if offloading
                if(!hasUpgrade(payload.unit.type())){
                    moveOutPayload();
                }else{ //update progress
                    constructTime = currentPlan.time;
                    if(moveInPayload()){
                        if(consValid()){
                            valid = true;
                            progress += edelta();
                        }

                        //upgrade the unit
                        if(progress >= constructTime){
                            payload.unit = currentPlan.unit.create(payload.unit.team());
                            progress = 0;
                            Effect.shake(2f, 3f, this);
                            Fx.producesmoke.at(this);
                            consume();
                        }
                    }
                }
            }

            speedScl = Mathf.lerpDelta(speedScl, Mathf.num(valid), 0.05f);
            time += edelta() * speedScl * state.rules.unitBuildSpeedMultiplier;
        }

        @Override
        public boolean shouldConsume(){
            //do not consume when cap reached
            if(constructing()){
                return Units.canCreate(team, currentPlan.unit);
            }
            return false;
        }

        public UnitType unit(){
            if(payload == null) return null;

            UnitType t = currentPlan.unit;
            return t != null && t.unlockedNow() ? t : null;
        }

        public boolean constructing(){
            return payload != null && hasUpgrade(payload.unit.type());
        }

        public boolean hasUpgrade(UnitType type){
            UnitType t = upgrade(type).unit;
            return t != null && t.unlockedNow();
        }

        public UpgradePlan upgrade(UnitType type){
            if(type == null) return UpgradePlan.empty;
            for(UpgradePlan plan : upgrades) {
                if(plan.base == type) return plan;
            }
            return UpgradePlan.empty;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision == 1){
                progress = read.f();
            }

        }

    }
}
