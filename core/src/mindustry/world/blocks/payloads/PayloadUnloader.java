package mindustry.world.blocks.payloads;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.gen.*;
import mindustry.logic.LAccess;
import mindustry.type.*;

import static mindustry.Vars.*;

public class PayloadUnloader extends PayloadLoader{
    public int offloadSpeed = 4;
    public int itemLoadMin = 0;
    public boolean exporting = false;

    public PayloadUnloader(String name){
        super(name);
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    public class PayloadUnloaderBuild extends PayloadLoaderBuild{

        @Override
        public void handlePayload(Building source, Payload payload) {
            super.handlePayload(source, payload);
            exporting = false;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return false;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(shouldExport()){
                //one-use, disposable block
                if(payload.block().instantDeconstruct){
                    payload.block().breakEffect.at(this, payload.block().size);
                    payload = null;
                }else{
                    moveOutPayload();
                }
            }else if(moveInPayload()){

                //unload items
                if(payload.block().hasItems && !full()){
                    if(efficiency() > 0.01f && timer(timerLoad, loadTime / efficiency())){
                        //load up items a set amount of times
                        for(int j = 0; j < itemsLoaded && !full(); j++){
                            for(int i = 0; i < items.length(); i++){
                                if(payload.build.items.get(i) > itemLoadMin){
                                    Item item = content.item(i);
                                    payload.build.items.remove(item, 1);
                                    items.add(item, 1);
                                    break;
                                }
                            }
                        }
                    }
                }

                //unload liquids
                //TODO tile is null may crash
                if(payload.block().hasLiquids && payload.build.liquids.currentAmount() >= 0.01f &&
                    (liquids.current() == payload.build.liquids.current() || liquids.currentAmount() <= 0.2f)){
                    var liq = payload.build.liquids.current();
                    float remaining = liquidCapacity - liquids.currentAmount();
                    float flow = Math.min(Math.min(liquidsLoaded * delta(), remaining), payload.build.liquids.currentAmount());

                    liquids.add(liq, flow);
                    payload.build.liquids.remove(liq, flow);
                }
            }

            dumpLiquid(liquids.current());
            for(int i = 0; i < offloadSpeed; i++){
                dumpAccumulate();
            }
        }

        public boolean full(){
            return items.total() >= itemCapacity;
        }

        @Override
        public boolean shouldExport(){
            if (payload == null){
                return false;
            }
            if (exporting == true){
                return true;
            }
            boolean result = true;
            for (int i = 0; i < content.items().size; i++){
                if (payload.build.items.get(i) > itemLoadMin){
                    result = false;
                    break;
                }
            }
            return result &&
                (!payload.block().hasLiquids || payload.build.liquids.currentAmount() <= 0.001f)
            ;
        }

        @Override
        public void control(LAccess type, double p1, double p2, double p3, double p4){
            if (type == LAccess.shoot){
                exporting = true;
            } else if (type == LAccess.config) {
                p1 += 0.0001;
                itemLoadMin = (int) p1;
            }
        }
        @Override
        public void displayBars(Table table) {
            super.displayBars(table);
            if (itemLoadMin != 0) {
                table.table(e -> {
                    e.row();
                    e.left();
                    e.label(() -> "Item Load Min: " + itemLoadMin).color(Color.lightGray);
                }).left();
            }
        }
    }
}
