package io.anuke.mindustry.world.blocks.logic;

import io.anuke.arc.Graphics.*;
import io.anuke.arc.Graphics.Cursor.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.world.*;

public class SwitchBlock extends LogicBlock{

    public SwitchBlock(String name){
        super(name);
        consumesTap = true;
    }

    @Override
    public Cursor getCursor(Tile tile){
        return SystemCursor.hand;
    }

    @Override
    public void tapped(Tile tile, Player player){
        tile.<LogicEntity>entity().signal ^= 1;
        Sounds.buttonClick.at(tile);
    }

    @Override
    public int signal(Tile tile){
        return tile.<LogicEntity>entity().signal;
    }
}
