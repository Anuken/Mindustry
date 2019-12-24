package io.anuke.mindustry.world.consumers;

import io.anuke.arc.collection.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

public class ConsumeLiquids extends ConsumeLiquidBase{
    public final LiquidStack[] liquids;

    public ConsumeLiquids(LiquidStack[] liquids){
        super(liquidAmount(liquids)); // Not used in valid() anyways
        this.liquids = liquids;
    }

    protected ConsumeLiquids(){
        this(new LiquidStack[]{});
    }

    protected static float liquidAmount(LiquidStack[] liquids){
		float amount = 0;
		for(LiquidStack liquid : liquids){
			amount += liquid.amount;
		}
		return amount;
	}

    /* Liquids after first are ignored. */
    @Override
    public void applyLiquidFilter(@NonNull Bits filter){
        filter.set(liquids[0].liquid.id);
    }

    @Override
    public void build(Tile tile, Table table){
        for(LiquidStack liquid : this.liquids){
            table.add(new ReqImage(liquid.liquid.icon(Cicon.medium), () -> valid(tile.entity))).size(8 * 4);
        }
    }

    @Override
    public String getIcon(){
        return "icon-liquid-consume";
    }

    @Override
    public void update(TileEntity entity){
        for(LiquidStack liquid : this.liquids){
            entity.liquids.remove(liquid.liquid, Math.min(use(entity), entity.liquids.get(liquid.liquid)));
        }
    }

    @Override
    public boolean valid(TileEntity entity){
        if(entity == null || entity.liquids == null) return false;
        for(LiquidStack liquid : this.liquids){
            if (entity.liquids.get(liquid.liquid) < use(entity, liquid.amount)) return false;
        }
        return true;
    }

    @Override
    public void display(BlockStats stats){
        for(LiquidStack liquid : this.liquids){
            stats.add(booster ? BlockStat.booster : BlockStat.input, liquid.liquid, amount * timePeriod, timePeriod == 60);
        }
    }
}