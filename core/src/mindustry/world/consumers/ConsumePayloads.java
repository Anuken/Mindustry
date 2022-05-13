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
    public float efficiency(Building build){
        return build.getPayloads().contains(payloads) ? 1f : 0f;
    }

    @Override
    public void trigger(Building build){
        build.getPayloads().remove(payloads);
    }

    @Override
    public void display(Stats stats){

        for(var stack : payloads){
            stats.add(Stat.input, t -> {
                t.add(new ItemImage(stack));
                t.add(stack.item.localizedName).padLeft(4).padRight(4);
            });
        }
    }

    @Override
    public void build(Building build, Table table){
        var inv = build.getPayloads();

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
