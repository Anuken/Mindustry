package mindustry.world.draw;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter.*;

public class DrawAnimation extends DrawBlock{
    public int frameCount = 3;
    public float frameSpeed = 5f;
    public boolean sine = true;
    public TextureRegion[] frames;
    public TextureRegion liquid, top;

    @Override
    public void draw(GenericCrafterBuild entity){
        Draw.rect(entity.block.region, entity.x, entity.y);
        Draw.rect(
            sine ?
                frames[(int)Mathf.absin(entity.totalProgress, frameSpeed, frameCount - 0.001f)] :
                frames[(int)((entity.totalProgress / frameSpeed) % frameCount)],
            entity.x, entity.y);
        Draw.color(Color.clear, entity.liquids.current().color, entity.liquids.total() / entity.block.liquidCapacity);
        Draw.rect(liquid, entity.x, entity.y);
        Draw.color();
        Draw.rect(top, entity.x, entity.y);
    }

    @Override
    public void load(Block block){
        frames = new TextureRegion[frameCount];
        for(int i = 0; i < frameCount; i++){
            frames[i] = Core.atlas.find(block.name + "-frame" + i);
        }

        liquid = Core.atlas.find(block.name + "-liquid");
        top = Core.atlas.find(block.name + "-top");
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{block.region, top};
    }
}