package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class ConsumePayloads extends Consume{
    //TODO bad, should be part of Building + dynamic
    protected Func<Building, BlockSeq> inventory;

    public Seq<BlockStack> payloads;

    public <T extends Building> ConsumePayloads(Seq<BlockStack> payloads, Func<T, BlockSeq> inventory){
        this.payloads = payloads;
        this.inventory = (Func<Building, BlockSeq>)inventory;
    }

    @Override
    public boolean valid(Building build){
        return inventory.get(build).contains(payloads);
    }

    @Override
    public void trigger(Building build){
        inventory.get(build).remove(payloads);
    }

    @Override
    public void display(Stats stats){
        //TODO

        for(var stack : payloads){
            stats.add(Stat.input, t -> {
                t.add(new ItemImage(stack));
                t.add(stack.block.localizedName).padLeft(4).padRight(4);
            });
        }
    }

    @Override
    public void build(Building build, Table table){
        var inv = inventory.get(build);

        table.table(c -> {
            int i = 0;
            for(var stack : payloads){
                c.add(new ReqImage(new ItemImage(stack.block.uiIcon, stack.amount),
                () -> inv.contains(stack.block, stack.amount))).padRight(8);
                if(++i % 4 == 0) c.row();
            }
        }).left();
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.payload;
    }
}
