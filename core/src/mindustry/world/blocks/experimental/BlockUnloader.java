package mindustry.world.blocks.experimental;

import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.content;

public class BlockUnloader extends BlockLoader{

    public BlockUnloader(String name){
        super(name);
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class BlockUnloaderBuild extends BlockLoaderBuild{

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public void updateTile(){
            if(shouldExport()){
                moveOutPayload();
            }else if(moveInPayload()){

                //load up items
                if(payload.block().hasItems && !full()){
                    if(timer(timerLoad, loadTime)){
                        //load up items a set amount of times
                        for(int j = 0; j < itemsLoaded && !full(); j++){
                            for(int i = 0; i < items.length(); i++){
                                if(payload.entity.items.get(i) > 0){
                                    Item item = content.item(i);
                                    payload.entity.items.remove(item, 1);
                                    items.add(item, 1);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            dump();
        }

        public boolean full(){
            return items.total() >= itemCapacity;
        }

        @Override
        public float fraction(){
            return payload == null ? 0f : 1f - payload.entity.items.total() / (float)payload.entity.block.itemCapacity;
        }

        @Override
        public boolean shouldExport(){
            return payload != null && (payload.block().hasItems && payload.entity.items.empty());
        }
    }
}
