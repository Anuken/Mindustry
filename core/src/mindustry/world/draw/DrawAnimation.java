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
    public void draw(GenericCrafterBuild build){
        Draw.rect(build.block.region, build.x, build.y);
        Draw.rect(
            sine ?
                frames[(int)Mathf.absin(build.totalProgress, frameSpeed, frameCount - 0.001f)] :
                frames[(int)((build.totalProgress / frameSpeed) % frameCount)],
            build.x, build.y);
        if(build.liquids != null){
            Draw.color(Color.clear, build.liquids.current().color, build.liquids.total() / build.block.liquidCapacity);
            Draw.rect(liquid, build.x, build.y);
            Draw.color();
        }
        if(top.found()){
            Draw.rect(top, build.x, build.y);
        }
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
        return top.found() ? new TextureRegion[]{block.region, top} : new TextureRegion[]{block.region};
    }
}
