package io.anuke.mindustry.world.consumers;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStats;

/**An abstract class that defines a type of resource that a block can consume.*/
public abstract class Consume{
    protected boolean optional;
    protected boolean update = true, boost = false;

    public Consume optional(boolean optional){
        this.optional = optional;
        return this;
    }

    public Consume update(boolean update){
        this.update = update;
        return this;
    }

    public Consume boost(boolean boost){
        this.boost = boost;
        return this;
    }

    public boolean isOptional(){
        return optional;
    }

    public boolean isUpdate(){
        return update;
    }

    public abstract void build(Tile tile, Table table);/*{

        Table t = new Table("flat");
        t.margin(4);
        buildTooltip(t);

        int scale = mobile ? 4 : 3;

        table.table(out -> {
            out.addImage(getIcon()).size(10 * scale).color(Color.DARK_GRAY).padRight(-10 * scale).padBottom(-scale * 2);
            out.addImage(getIcon()).size(10 * scale).color(Pal.accent);
            out.addImage("icon-missing").size(10 * scale).color(Pal.remove).padLeft(-10 * scale);
        }).size(10 * scale).get().addListener(new Tooltip<>(t));
    }*/

    /**Called when a consumption is triggered manually.*/
    public void trigger(Block block, TileEntity entity){

    }

    public abstract String getIcon();

    public abstract void update(Block block, TileEntity entity);

    public abstract boolean valid(Block block, TileEntity entity);

    public abstract void display(BlockStats stats);
}
