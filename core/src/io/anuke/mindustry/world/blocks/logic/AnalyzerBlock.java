package io.anuke.mindustry.world.blocks.logic;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import java.io.*;

public class AnalyzerBlock extends LogicBlock{
    private static final int modeItem = 0, modeLiquid = 1, modePower = 2, selectBattery = 0, selectBalance = 1;

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
        }else if(mode == modePower && back.block().hasPower){
            return selection == selectBattery ? (int)back.entity.power.graph.getBatteryStored() : (int)back.entity.power.graph.getPowerBalance();
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
        ItemSelection.buildItemTable(table, () -> Vars.content.item(AnalyzeMode.selection(entity.mode)), item -> {
            tile.configure(AnalyzeMode.get(modeItem, item == null ? Short.MAX_VALUE: item.id));
        });
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
