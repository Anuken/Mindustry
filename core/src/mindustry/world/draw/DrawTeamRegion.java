package mindustry.world.draw;

import mindustry.gen.*;

public class DrawTeamRegion extends DrawBlock{
    @Override
    public void draw(Building build){
        build.drawTeamTop();
    }
}
