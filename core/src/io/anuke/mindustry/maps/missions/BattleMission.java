package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.FortressGenerator;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class BattleMission extends MissionWithStartingCore{
    final int spacing = 30;
    public static final int defaultXCorePos = 50;
    public static final int defaultYCorePos = 50;

    /** Creates a battle mission with the player core being at (@defaultXCorePos, @defaultYCorePos) */
    public BattleMission(){
        this(defaultXCorePos, defaultYCorePos);
    }

    /**
     * Creates a wave survival with the player core being at a custom location.
     * @param xCorePos The X coordinate of the custom core position.
     * @param yCorePos The Y coordinate of the custom core position.
     */
    public BattleMission(int xCorePos, int yCorePos){
        super(xCorePos, yCorePos);
    }

    @Override
    public String getIcon(){
        return "icon-mission-battle";
    }

    @Override
    public GameMode getMode(){
        return GameMode.attack;
    }

    @Override
    public String displayString(){
        return Bundles.get("text.mission.battle");
    }

    @Override
    public Array<GridPoint2> getSpawnPoints(Generation gen){
        return Array.with(new GridPoint2(50, 50), new GridPoint2(gen.width - 1 - spacing, gen.height - 1 - spacing));
    }

    @Override
    public void generate(Generation gen){
        generateCoreAtFirstSpawnPoint(gen, defaultTeam);

        if(state.teams.get(defaultTeam).cores.size == 0){
            return;
        }

        Tile core = state.teams.get(defaultTeam).cores.first();
        int enx = gen.width - 1 - spacing;
        int eny = gen.height - 1 - spacing;
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
