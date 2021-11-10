package mindustry.world.blocks.payloads;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import arc.util.io.*;
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
    public int maxBlockSize = 2;

    public PayloadLoader(String name){
        super(name);
 
        hasItems = true;
        hasLiquids = true;
        itemCapacity = 100;
        liquidCapacity = 100f;
        update = true;
        outputsPayload = true;
        size = 3;
        rotate = true;
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

        bars.add("progress", (PayloadLoaderBuild entity) -> new Bar(() -> Core.bundle.format("bar.items", entity.payload == null ? 0 : entity.payload.build.items.total()), () -> Pal.items, entity::fraction));
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(inRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(outRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy());
    }

    public class PayloadLoaderBuild extends PayloadBlockBuild<BuildPayload>{
        public boolean exporting = false;

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return super.acceptPayload(source, payload) &&
                payload.fits(maxBlockSize) &&
                payload instanceof BuildPayload build &&
                ((build.build.block.hasItems && build.block().unloadable && build.block().itemCapacity >= 10 && build.block().size <= maxBlockSize) ||
                build.build.block().hasLiquids && build.block().liquidCapacity >= 10f);
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

            Draw.z(Layer.blockOver);
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(shouldExport()){
                moveOutPayload();
            }else if(moveInPayload()){

                //load up items
                if(payload.block().hasItems && items.any()){
                    if(efficiency() > 0.01f && timer(timerLoad, loadTime / efficiency())){
                        //load up items a set amount of times
                        for(int j = 0; j < itemsLoaded && items.any(); j++){

                            for(int i = 0; i < items.length(); i++){
                                if(items.get(i) > 0){
                                    Item item = content.item(i);
                                    if(payload.build.acceptItem(payload.build, item)){
                                        payload.build.handleItem(payload.build, item);
                                        items.remove(item, 1);
                                        break;
                                    }else if(payload.block().separateItemCapacity || payload.block().consumes.consumesItem(item)){
                                        exporting = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                //load up liquids
                if(payload.block().hasLiquids && liquids.total() >= 0.001f){
                    Liquid liq = liquids.current();
                    float total = liquids.currentAmount();
                    float flow = Math.min(Math.min(liquidsLoaded * edelta(), payload.block().liquidCapacity - payload.build.liquids.get(liq)), total);
                    //TODO potential crash here
                    if(payload.build.acceptLiquid(payload.build, liq)){
                        payload.build.liquids.add(liq, flow);
                        liquids.remove(liq, flow);
                    }
                }
            }
        }

        public float fraction(){
            return payload == null ? 0f : payload.build.items.total() / (float)payload.build.block.itemCapacity;
        }

        public boolean shouldExport(){
            return payload != null && (
                exporting ||
                (payload.block().hasLiquids && liquids.total() >= 0.1f && payload.build.liquids.total() >= payload.block().liquidCapacity - 0.001f) ||
                (payload.block().hasItems && items.any() && payload.block().separateItemCapacity && content.items().contains(i -> payload.build.items.get(i) >= payload.block().itemCapacity)));
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
