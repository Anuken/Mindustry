package io.anuke.mindustry.world.consumers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockStats;
import io.anuke.ucore.scene.ui.Tooltip;
import io.anuke.ucore.scene.ui.layout.Table;

public abstract class Consume {
    private boolean optional;
    private boolean update = true;

    public Consume optional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public Consume update(boolean update){
        this.update = update;
        return this;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isUpdate() {
        return update;
    }

    public void build(Table table){
        Table t = new Table("clear");
        t.margin(4);
        buildTooltip(t);

        table.table("clear", out -> {
            out.addImage(getIcon()).size(10*4).color(Color.RED);
        }).size(10*4).get().addListener(new Tooltip<>(t));
    }

    public void buildTooltip(Table table){
        table.add("no " + ClassReflection.getSimpleName(getClass()).replace("Consume", ""));
    }

    public String getIcon(){
        return "icon-power";
    }

    public abstract void update(Block block, TileEntity entity);
    public abstract boolean valid(Block block, TileEntity entity);
    public abstract void display(BlockStats stats);
}
