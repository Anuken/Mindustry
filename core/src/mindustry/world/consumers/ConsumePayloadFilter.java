package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ConsumePayloadFilter extends Consume{
    //cache fitting blocks to prevent search over all blocks later
    protected final Block[] fitting;

    public Boolf<Block> filter;

    public ConsumePayloadFilter(Boolf<Block> filter){
        this.filter = filter;
        this.fitting = Vars.content.blocks().select(filter).toArray(Block.class);
    }

    @Override
    public boolean valid(Building build){
        var payloads = build.getBlockPayloads();
        for(var block : fitting){
            if(payloads.contains(block, 1)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void trigger(Building build){
        var payloads = build.getBlockPayloads();
        for(var block : fitting){
            if(payloads.contains(block, 1)){
                payloads.remove(block);
                return;
            }
        }
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.blocks(filter));
    }

    @Override
    public void build(Building build, Table table){
        var inv = build.getBlockPayloads();

        MultiReqImage image = new MultiReqImage();
        content.blocks().each(i -> filter.get(i) && i.unlockedNow(), block -> image.add(new ReqImage(new ItemImage(block.uiIcon, 1),
        () -> inv.contains(block, 1))));

        table.add(image).size(8 * 4);
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.payload;
    }
}
