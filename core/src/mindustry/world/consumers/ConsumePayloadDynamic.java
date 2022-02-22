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
        return build.getPayloads().contains(payloads.get(build)) ? 1f : 0f;
    }

    @Override
    public void trigger(Building build){
        build.getPayloads().remove(payloads.get(build));
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

        table.table(c -> {
            int i = 0;
            for(var stack : pay){
                c.add(new ReqImage(new ItemImage(stack.item.uiIcon, stack.amount),
                () -> inv.contains(stack.item, stack.amount))).padRight(8);
                if(++i % 4 == 0) c.row();
            }
        }).left();
    }
}
