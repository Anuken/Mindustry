package mindustry.world.consumers;

import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

//TODO test!
public class ConsumeLiquids extends Consume{
    public final LiquidStack[] liquids;

    public ConsumeLiquids(LiquidStack[] liquids){
        this.liquids = liquids;
    }

    /** Mods.*/
    protected ConsumeLiquids(){
        this(LiquidStack.empty);
    }

    @Override
    public void apply(Block block){
        block.hasLiquids = true;
        for(var stack : liquids){
            block.liquidFilter[stack.liquid.id] = true;
        }
    }

    @Override
    public void build(Building build, Table table){
        table.table(c -> {
            int i = 0;
            for(var stack : liquids){
                c.add(new ReqImage(stack.liquid.uiIcon,
                () -> build.liquids.get(stack.liquid) >= stack.amount * build.delta())).size(Vars.iconMed).padRight(8);
                if(++i % 4 == 0) c.row();
            }
        }).left();
    }

    @Override
    public void update(Building build){
        for(var stack : liquids){
            build.liquids.remove(stack.liquid, Math.min(use(stack.amount, build), build.liquids.get(stack.liquid)));
        }
    }

    @Override
    public boolean valid(Building build){
        for(var stack : liquids){
            if(build.liquids.get(stack.liquid) < stack.amount * build.delta()){
                return false;
            }
        }
        return true;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquids(1f, true, liquids));
    }

    protected float use(float amount, Building build){
        return Math.min(amount * build.edelta(), build.block.liquidCapacity);
    }
}
