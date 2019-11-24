package io.anuke.mindustry.world.blocks.logic;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.func.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import java.io.*;

public class AnalyzerBlock extends LogicBlock{
    private static final int modeItem = 0, modeLiquid = 1, modePowerBalance = 2, modePowerBattery = 3;

    public AnalyzerBlock(String name){
        super(name);
        entityType = AnaylzerEntity::new;
        configurable = true;
    }

    @Override
    public int signal(Tile tile){
        Tile back = tile.back();
        AnaylzerEntity entity = tile.entity();

        int mode = AnalyzeMode.mode(entity.mode);
        int selection = AnalyzeMode.selection(entity.mode);

        if(back == null){
            return 0;
        }else if(mode == modePowerBalance && back.block().hasPower){
            return Math.round(back.entity.power.graph.getPowerBalance()*60);
        }else if(mode == modePowerBattery && back.block().hasPower){
            return Math.round(back.entity.power.graph.getBatteryStored());
        }else if(mode == modeItem && back.block().hasItems){
            Item item = Vars.content.item(selection);
            return item == null ? back.entity.items.total() : back.entity.items.get(item);
        }else if(mode == modeLiquid && back.block().hasLiquids){
            Liquid liquid = Vars.content.liquid(selection);
            return liquid == null ? (int)back.entity.liquids.total() : (int)back.entity.liquids.get(liquid);
        }

        return 0;
    }

    @Override
    public void buildTable(Tile tile, Table table){
        AnaylzerEntity entity = tile.entity();
        Runnable[] rebuild = {null};
        rebuild[0] = () -> {
            table.clearChildren();
            ButtonGroup<Button> group = new ButtonGroup<>();

            Cons2<Integer, Drawable> toggler = (mode, tex) -> {
                table.addImageButton(tex, Styles.clearToggleTransi, () -> {
                    entity.mode = AnalyzeMode.get(mode, 0);
                    rebuild[0].run();
                }).group(group).size(40f).checked(AnalyzeMode.mode(entity.mode) == mode);
            };

            toggler.get(modeItem, Icon.itemSmall);
            toggler.get(modeLiquid, Icon.liquidSmall);
            toggler.get(modePowerBalance, Icon.powerSmall);
            toggler.get(modePowerBattery, Icon.batterySmall);
            table.row();
            Table next = table.table().colspan(4).get();

            int mode = AnalyzeMode.mode(entity.mode);
            if(mode == modeItem){
                ItemSelection.buildTable(next, Vars.content.items(), () -> Vars.content.item(AnalyzeMode.selection(entity.mode)), item -> {
                    tile.configure(AnalyzeMode.get(modeItem, item == null ? Short.MAX_VALUE: item.id));
                });
            }else if(mode == modeLiquid){
                ItemSelection.buildTable(next, Vars.content.liquids(), () -> Vars.content.liquid(AnalyzeMode.selection(entity.mode)), item -> {
                    tile.configure(AnalyzeMode.get(modeLiquid, item == null ? Short.MAX_VALUE: item.id));
                });
            }

            table.pack();
        };

        rebuild[0].run();
    }

    @Override
    public void configured(Tile tile, @Nullable Player player, int value){
        AnaylzerEntity entity = tile.entity();
        entity.mode = value;
    }

    public class AnaylzerEntity extends LogicEntity{
        public int mode;

        @Override
        public int config(){
            return mode;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeInt(mode);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            mode = stream.readInt();
        }
    }

    @Struct
    class AnalyzeModeStruct{
        /** mode of analysis, e.g. power, liquid, item */
        @StructField(16)
        int mode;
        /** mode-specific selection, e.g. liquid/item id or scan mode */
        @StructField(16)
        int selection;
    }
}
