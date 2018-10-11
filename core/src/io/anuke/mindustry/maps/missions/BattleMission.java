package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.FortressGenerator;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.SeedRandom;

import static io.anuke.mindustry.Vars.*;

public class BattleMission extends Mission{
    final int spacing = 20;

    @Override
    public GameMode getMode(){
        return GameMode.noWaves;
    }

    @Override
    public String displayString(){
        return Bundles.get("text.mission.battle");
    }

    @Override
    public void onBegin(){
        if(state.teams.get(defaultTeam).cores.size == 0){
            return;
        }
        Tile core = state.teams.get(defaultTeam).cores.first();
        Generation gen = new Generation(world.getSector(), world.getTiles(), world.width(), world.height(), new SeedRandom(world.getSector().getSeed()-1));
        int ex = world.getSector().lastExpandX;
        int ey = world.getSector().lastExpandY;
        int enx = world.width() - 1 - spacing;
        int eny = world.height() - 1 - spacing;
        if(ex < 0) enx = spacing*gen.sector.width;
        if(ex > 0) enx = world.width() - 1 - spacing*gen.sector.width;
        if(ey < 0) eny = spacing*gen.sector.height;
        if(ey > 0) eny = world.height() - 1 - spacing*gen.sector.height;
        new FortressGenerator().generate(gen, Team.red, core.x, core.y, enx, eny);
    }

    @Override
    public boolean isComplete(){
        for(Team team : Vars.state.teams.enemiesOf(Vars.defaultTeam)){
            if(Vars.state.teams.isActive(team)){
                return false;
            }
        }
        return true;
    }
}
