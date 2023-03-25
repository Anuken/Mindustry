package mindustry.world.blocks.payloads;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class PayloadLoader extends PayloadBlock{
    public final int timerLoad = timers++;

    public float loadTime = 2f;
    public int itemsLoaded = 8;
    public float liquidsLoaded = 40f;
    public int maxBlockSize = 3;
    public float maxPowerConsumption = 40f;
    public boolean loadPowerDynamic = true;

    public @Load("@-over") TextureRegion overRegion;

    //initialized in init(), do not touch
    protected float basePowerUse = 0f;

    public PayloadLoader(String name){
        super(name);
 
        hasItems = true;
        hasLiquids = true;
        hasPower = true;
        itemCapacity = 100;
        liquidCapacity = 100f;
        update = true;
        outputsPayload = true;
        size = 3;
        rotate = true;
        canOverdrive = false;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, inRegion, outRegion, topRegion};
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("progress", (PayloadLoaderBuild build) -> new Bar(() ->
            Core.bundle.format(build.payload != null && build.payload.block().hasItems ? "bar.items" : "bar.loadprogress",
                build.payload == null || !build.payload.block().hasItems ? 0 : build.payload.build.items.total()), () -> Pal.items, build::fraction));
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(inRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public void init(){
        if(loadPowerDynamic){
            basePowerUse = consPower != null ? consPower.usage : 0f;
            consumePowerDynamic((PayloadLoaderBuild loader) -> loader.hasBattery() && !loader.exporting ? maxPowerConsumption + basePowerUse : basePowerUse);
        }

        super.init();
    }

    public class PayloadLoaderBuild extends PayloadBlockBuild<BuildPayload>{
        public boolean exporting = false;

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return super.acceptPayload(source, payload) &&
                payload.fits(maxBlockSize) &&
                payload instanceof BuildPayload build && (
                //item container
                (build.build.block.hasItems && build.block().unloadable && build.block().itemCapacity >= 10 && build.block().size <= maxBlockSize) ||
                //liquid container
                (build.build.block().hasLiquids && build.block().liquidCapacity >= 10f) ||
                //battery
                (build.build.block.consPower != null && build.build.block.consPower.buffered)
            );
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            super.handlePayload(source, payload);
            exporting = false;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.total() < itemCapacity;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return liquids.current() == liquid || liquids.currentAmount() < 0.2f;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input
            boolean fallback = true;
            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                    fallback = false;
                }
            }
            if(fallback) Draw.rect(inRegion, x, y, rotation * 90);

            Draw.rect(outRegion, x, y, rotdeg());

            //drawn below payload so 3x3 blocks don't look even even weirder
            Draw.rect(topRegion, x, y);

            Draw.z(Layer.blockOver);
            drawPayload();

            if(overRegion.found()){
                Draw.z(Layer.blockOver + 0.1f);
                Draw.rect(overRegion, x, y);
            }
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(shouldExport()){
                moveOutPayload();
            }else if(moveInPayload()){

                //load up items
                if(payload.block().hasItems && items.any()){
                    if(efficiency > 0.01f && timer(timerLoad, loadTime / efficiency)){
                        //load up items a set amount of times
                        for(int j = 0; j < itemsLoaded && items.any(); j++){

                            for(int i = 0; i < items.length(); i++){
                                if(items.get(i) > 0){
                                    Item item = content.item(i);
                                    if(payload.build.acceptItem(payload.build, item)){
                                        payload.build.handleItem(payload.build, item);
                                        items.remove(item, 1);
                                        break;
                                    }else if(payload.block().separateItemCapacity || payload.block().consumesItem(item)){
                                        exporting = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                //load up liquids
                if(payload.block().hasLiquids && liquids.currentAmount() >= 0.001f){
                    Liquid liq = liquids.current();
                    float total = liquids.currentAmount();
                    float flow = Math.min(Math.min(liquidsLoaded * edelta(), payload.block().liquidCapacity - payload.build.liquids.get(liq)), total);
                    //TODO potential crash here
                    if(payload.build.acceptLiquid(payload.build, liq)){
                        payload.build.liquids.add(liq, flow);
                        liquids.remove(liq, flow);
                    }
                }

                //load up power
                if(hasBattery()){
                    //base power input that in raw units
                    float powerInput = power.status * (basePowerUse + maxPowerConsumption);
                    //how much is actually usable
                    float availableInput = Math.max(powerInput - basePowerUse, 0f);

                    //charge the battery
                    float cap = payload.block().consPower.capacity;
                    payload.build.power.status += availableInput / cap * edelta();

                    //export if full
                    if(payload.build.power.status >= 1f){
                        exporting = true;
                        payload.build.power.status = Mathf.clamp(payload.build.power.status);
                    }
                }
            }
        }

        public float fraction(){
            return payload == null ? 0f :
                payload.build.items != null ? payload.build.items.total() / (float)payload.build.block.itemCapacity :
                payload.build.liquids != null ? payload.build.liquids.currentAmount() / payload.block().liquidCapacity :
                hasBattery() ? payload.build.power.status :
                0f;
        }

        public boolean shouldExport(){
            return payload != null && (
                exporting ||
                (payload.block().hasLiquids && liquids.currentAmount() >= 0.1f && payload.build.liquids.currentAmount() >= payload.block().liquidCapacity - 0.001f) ||
                (payload.block().hasItems && items.any() && payload.block().separateItemCapacity && content.items().contains(i -> payload.build.items.get(i) >= payload.block().itemCapacity)) ||
                (hasBattery() && payload.build.power.status >= 0.999999999f));
        }

        public boolean hasBattery(){
            return payload != null && payload.block().consPower != null && payload.block().consPower.buffered;
        }

        @Override
        public boolean shouldConsume(){
            return payload != null;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(exporting);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                exporting = read.bool();
            }
        }
    }
}
