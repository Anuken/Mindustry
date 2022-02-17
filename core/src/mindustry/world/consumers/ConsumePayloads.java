package mindustry.world.consumers;

import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class ConsumePayloads extends Consume{
    public Seq<PayloadStack> payloads;

    public ConsumePayloads(Seq<PayloadStack> payloads){
        this.payloads = payloads;
    }

    @Override
    public boolean valid(Building build){
        return build.getBlockPayloads().contains(payloads);
    }

    @Override
    public void trigger(Building build){
        build.getBlockPayloads().remove(payloads);
    }

    @Override
    public void display(Stats stats){
        //TODO

        for(var stack : payloads){
            stats.add(Stat.input, t -> {
                t.add(new ItemImage(stack));
                t.add(stack.item.localizedName).padLeft(4).padRight(4);
            });
        }
    }

    @Override
    public void build(Building build, Table table){
        var inv = build.getBlockPayloads();

        table.table(c -> {
            int i = 0;
            for(var stack : payloads){
                c.add(new ReqImage(new ItemImage(stack.item.uiIcon, stack.amount),
                () -> inv.contains(stack.item, stack.amount))).padRight(8);
                if(++i % 4 == 0) c.row();
            }
        }).left();
    }
}
