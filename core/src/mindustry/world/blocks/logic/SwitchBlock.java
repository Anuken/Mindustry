package mindustry.world.blocks.logic;

import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.util.ArcAnnotate.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.world.*;

public class SwitchBlock extends LogicBlock{

    public SwitchBlock(String name){
        super(name);
        consumesTap = true;
        entityType = SwitchEntity::new;
    }

    @Override
    public Cursor getCursor(Tile tile){
        return SystemCursor.hand;
    }

    @Override
    public void configured(Tile tile, @Nullable Player player, int value){
        tile.<LogicEntity>ent().nextSignal = value;
    }

    @Override
    public void tapped(Tile tile, Player player){
        tile.<LogicEntity>ent().nextSignal ^= 1;
        Sounds.buttonClick.at(tile);
    }

    @Override
    public int signal(Tile tile){
        return tile.<LogicEntity>ent().nextSignal;
    }

    public class SwitchEntity extends LogicEntity{
        @Override
        public int config(){
            return nextSignal;
        }
    }
}
