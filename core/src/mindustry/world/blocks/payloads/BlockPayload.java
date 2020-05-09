package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.tilesize;

public class BlockPayload implements Payload{
    public Block block;

    public BlockPayload(Block block){
        this.block = block;
    }

    @Override
    public void draw(float x, float y, float rotation){
        Drawf.shadow(x, y, block.size * tilesize * 2f);
        Draw.rect(block.icon(Cicon.full), x, y, 0);
    }
}
