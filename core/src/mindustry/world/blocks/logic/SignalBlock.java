package mindustry.world.blocks.logic;

import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class SignalBlock extends LogicBlock{

    public SignalBlock(String name){
        super(name);
        configurable = true;
        entityType = SignalLogicEntity::new;
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        LogicEntity entity = tile.ent();

        table.addImageButton(Icon.pencilSmall, () -> {
            ui.showTextInput("$block.editsignal", "", 8, entity.nextSignal + "", true, result -> {
                entity.nextSignal = Strings.parseInt(result, 0);
            });
            control.input.frag.config.hideConfig();
        }).size(40f);
    }

    @Override
    public int signal(Tile tile){
        return tile.<LogicEntity>ent().nextSignal;
    }

    @Override
    public void configured(Tile tile, @Nullable Player player, int value){
        LogicEntity entity = tile.ent();
        entity.nextSignal = value;
    }

    public class SignalLogicEntity extends LogicEntity{
        @Override
        public int config(){
            return nextSignal;
        }
    }
}
