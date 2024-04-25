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
        float mult = multiplier.get(build);
        for(PayloadStack stack : payloads){
            if(!build.getPayloads().contains(stack.item, Math.round(stack.amount * mult))){
                return 0f;
            }
        }
        return 1f;
    }

    @Override
    public void trigger(Building build){
        float mult = multiplier.get(build);
        for(PayloadStack stack : payloads){
            build.getPayloads().remove(stack.item, Math.round(stack.amount * mult));
        }
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
                c.add(new ReqImage(new ItemImage(stack.item.uiIcon, Math.round(stack.amount * multiplier.get(build))),
                () -> inv.contains(stack.item, Math.round(stack.amount * multiplier.get(build))))).padRight(8);
                if(++i % 4 == 0) c.row();
            }
        }).left();
    }
}
