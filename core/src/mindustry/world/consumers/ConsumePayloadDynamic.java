package mindustry.world.consumers;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class ConsumePayloadDynamic extends Consume{
    public final Func<Building, Seq<PayloadStack>> payloads;

    @SuppressWarnings("unchecked")
    public <T extends Building>  ConsumePayloadDynamic(Func<T, Seq<PayloadStack>> payloads){
        this.payloads = (Func<Building, Seq<PayloadStack>>)payloads;
    }

    @Override
    public float efficiency(Building build){
        float mult = multiplier.get(build);
        for(PayloadStack stack : payloads.get(build)){
            if(!build.getPayloads().contains(stack.item, Math.round(stack.amount * mult))){
                return 0f;
            }
        }
        return 1f;
    }

    @Override
    public void trigger(Building build){
        float mult = multiplier.get(build);
        for(PayloadStack stack : payloads.get(build)){
            build.getPayloads().remove(stack.item, Math.round(stack.amount * mult));
        }
    }

    @Override
    public void display(Stats stats){
        //needs to be implemented by the block itself, not enough info to display here
    }

    @Override
    public void build(Building build, Table table){
        Seq[] current = {payloads.get(build)};

        table.table(cont -> {
            table.update(() -> {
                if(current[0] != payloads.get(build)){
                    rebuild(build, cont);
                    current[0] = payloads.get(build);
                }
            });

            rebuild(build, cont);
        });
    }

    private void rebuild(Building build, Table table){
        var inv = build.getPayloads();
        var pay = payloads.get(build);

        table.clear();
        table.table(c -> {
            int i = 0;
            for(var stack : pay){
                c.add(new ReqImage(StatValues.stack(stack.item, Math.round(stack.amount * multiplier.get(build))),
                () -> inv.contains(stack.item, Math.round(stack.amount * multiplier.get(build))))).padRight(8);
                if(++i % 4 == 0) c.row();
            }
        }).left();
    }
}
