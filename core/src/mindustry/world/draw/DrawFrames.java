package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawFrames extends DrawBlock{
    /** Number of frames to draw. */
    public int frames = 3;
    /** Ticks between frames. */
    public float interval = 5f;
    /** If true, frames wil alternate back and forth in a sine wave. */
    public boolean sine = true;
    public TextureRegion[] regions;

    @Override
    public void draw(Building build){
        Draw.rect(
            sine ?
                regions[(int)Mathf.absin(build.totalProgress(), interval, frames - 0.001f)] :
                regions[(int)((build.totalProgress() / interval) % frames)],
            build.x, build.y);
    }

    @Override
    public TextureRegion[] icons(Block block){
        return new TextureRegion[]{regions[0]};
    }

    @Override
    public void load(Block block){
        regions = new TextureRegion[frames];
        for(int i = 0; i < frames; i++){
            regions[i] = Core.atlas.find(block.name + "-frame" + i);
        }
    }
}
