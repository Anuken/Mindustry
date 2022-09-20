package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawTurbines extends DrawBlock{
    public TextureRegion[] turbines = new TextureRegion[2];
    public TextureRegion cap;
    public float turbineSpeed = 2f;

    @Override
    public void draw(Building build){
        float totalTime = build.totalProgress();
        Draw.rect(turbines[0], build.x, build.y, totalTime * turbineSpeed);
        Draw.rect(turbines[1], build.x, build.y, -totalTime * turbineSpeed);

        if(cap.found()){
            Draw.rect(cap, build.x, build.y);
        }
    }

    @Override
    public void load(Block block){
        super.load(block);

        cap = Core.atlas.find(block.name + "-cap");

        for(int i = 0; i < 2; i++){
            turbines[i] = Core.atlas.find(block.name + "-turbine" + i);
        }
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{turbines[0], turbines[1], cap};
    }
}
