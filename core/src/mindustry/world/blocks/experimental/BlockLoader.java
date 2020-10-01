package mindustry.world.blocks.experimental;

import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.content;

public class BlockLoader extends PayloadAcceptor{
    public final int timerLoad = timers++;

    public float loadTime = 2f;
    public int itemsLoaded = 5;
    public float liquidsLoaded = 5f;

    public BlockLoader(String name){
        super(name);

        hasItems = true;
        itemCapacity = 25;
        //liquidCapacity = 25;
        update = true;
        outputsPayload = true;
        size = 3;
        rotate = true;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("progress", entity -> new Bar("bar.progress", Pal.ammo, ((BlockLoaderBuild)entity)::fraction));
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(outRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy());
    }

    public class BlockLoaderBuild extends PayloadAcceptorBuild<BlockPayload>{

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return super.acceptPayload(source, payload) &&
                (payload instanceof BlockPayload) &&
                ((((BlockPayload)payload).entity.block.hasItems && ((BlockPayload)payload).block().unloadable && ((BlockPayload)payload).block().itemCapacity >= 10)/* ||
                ((BlockPayload)payload).entity.block().hasLiquids && ((BlockPayload)payload).block().liquidCapacity >= 10f)*/);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.total() < itemCapacity;
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

            Draw.z(Layer.blockOver);
            payRotation = rotdeg();
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            if(shouldExport()){
                moveOutPayload();
            }else if(moveInPayload()){

                //load up items
                if(payload.block().hasItems && items.any()){
                    if(timer(timerLoad, loadTime)){
                        //load up items a set amount of times
                        for(int j = 0; j < itemsLoaded && items.any(); j++){

                            for(int i = 0; i < items.length(); i++){
                                if(items.get(i) > 0){
                                    Item item = content.item(i);
                                    if(payload.entity.acceptItem(payload.entity, item)){
                                        payload.entity.handleItem(payload.entity, item);
                                        items.remove(item, 1);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                //load up liquids (disabled)
                /*
                if(payload.block().hasLiquids && liquids.total() >= 0.001f){
                    Liquid liq = liquids.current();
                    float total = liquids.currentAmount();
                    float flow = Math.min(Math.min(liquidsLoaded * delta(), payload.block().liquidCapacity - payload.entity.liquids.get(liq) - 0.0001f), total);
                    if(payload.entity.acceptLiquid(payload.entity, liq, flow)){
                        payload.entity.liquids.add(liq, flow);
                        liquids.remove(liq, flow);
                    }
                }*/
            }
        }

        public float fraction(){
            return payload == null ? 0f : payload.entity.items.total() / (float)payload.entity.block.itemCapacity;
        }

        public boolean shouldExport(){
            return payload != null &&
                ((payload.block().hasLiquids && payload.entity.liquids.total() >= payload.block().liquidCapacity - 0.001f) ||
                (payload.block().hasItems && payload.entity.items.total() >= payload.block().itemCapacity));
        }
    }
}
