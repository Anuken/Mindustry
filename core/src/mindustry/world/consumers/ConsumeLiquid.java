package mindustry.world.consumers;

import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//TODO replace with ConsumeLiquids
public class ConsumeLiquid extends ConsumeLiquidBase{
    public final Liquid liquid;

    public ConsumeLiquid(Liquid liquid, float amount){
        super(amount);
        this.liquid = liquid;
    }

    protected ConsumeLiquid(){
        this(null, 0f);
    }

    @Override
    public void apply(Block block){
        super.apply(block);
        block.liquidFilter[liquid.id] = true;
    }

    @Override
    public void build(Building build, Table table){
        table.add(new ReqImage(liquid.uiIcon, () -> valid(build))).size(iconMed).top().left();
    }

    @Override
    public void update(Building build){
        build.liquids.remove(liquid, Math.min(use(build), build.liquids.get(liquid)));
    }

    @Override
    public boolean valid(Building build){
        return build.liquids != null && build.liquids.get(liquid) >= amount * build.delta();
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, liquid, amount * 60f, true);
    }
}
