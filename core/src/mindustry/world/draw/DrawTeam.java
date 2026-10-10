package mindustry.world.draw;

import arc.graphics.g2d.*;
import mindustry.*;
import mindustry.gen.*;

/** Draws a block's team regions. */
public class DrawTeam extends DrawBlock{
    /** 0 to not override */
    public float z = 0f;
    /** If true, only drawn when the observer is on the different team. */
    public boolean onlyEnemy = false;

    @Override
    public void draw(Building build){
        if(onlyEnemy && Vars.player.team() == build.team) return;
        float prev = Draw.z();
        if(z != 0f) Draw.z(z);
        build.drawTeamTop();
        if(z != 0f) Draw.z(prev);
    }
}
