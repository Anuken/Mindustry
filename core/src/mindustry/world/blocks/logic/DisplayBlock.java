package mindustry.world.blocks.logic;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;

public class DisplayBlock extends AcceptorLogicBlock{

    public DisplayBlock(String name){
        super(name);
        rotate = false;
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name)};
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy());
        LogicEntity entity = tile.ent();

        float dw = 2, dh = 2, xs = 2f, ys = 2f;

        int w = 5, h = 5;
        for(int i = 0; i < w * h; i++){
            int x = i % w;
            int y = i / w;

            if((entity.nextSignal & (1 << i)) != 0){
                Fill.rect(tile.drawx() + x*xs - (w-1) * xs/2f, tile.drawy() + y*ys - (h-1) * ys/2f, dw, dh);
            }
        }
    }
}
