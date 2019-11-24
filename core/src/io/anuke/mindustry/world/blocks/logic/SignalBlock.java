package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class SignalBlock extends LogicBlock{

    public SignalBlock(String name){
        super(name);
        configurable = true;
        entityType = SignalLogicEntity::new;
    }

    @Override
    public void buildTable(Tile tile, Table table){
        LogicEntity entity = tile.entity();

        table.addImageButton(Icon.pencilSmall, () -> {
            ui.showTextInput("$block.editsignal", "", 8, entity.nextSignal + "", true, result -> {
                entity.nextSignal = Strings.parseInt(result, 0);
            });
            control.input.frag.config.hideConfig();
        }).size(40f);
    }

    @Override
    public int signal(Tile tile){
        return tile.<LogicEntity>entity().nextSignal;
    }

    @Override
    public void configured(Tile tile, @Nullable Player player, int value){
        LogicEntity entity = tile.entity();
        entity.nextSignal = value;
    }

    public class SignalLogicEntity extends LogicEntity{
        @Override
        public int config(){
            return nextSignal;
        }
    }
}
