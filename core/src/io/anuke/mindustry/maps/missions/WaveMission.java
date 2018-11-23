package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Waves;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class WaveMission extends MissionWithStartingCore{
    private final int target;

    /**
     * Creates a wave survival mission with the player core being in the center of the map.
     * @param target The number of waves to be survived.
     */
    public WaveMission(int target){
        super();
        this.target = target;
    }

    /**
     * Creates a wave survival with the player core being at a custom location.
     * @param target The number of waves to be survived.
     * @param xCorePos The X coordinate of the custom core position.
     * @param yCorePos The Y coordinate of the custom core position.
     */
    public WaveMission(int target, int xCorePos, int yCorePos){
        super(xCorePos, yCorePos);
        this.target = target;
    }

    @Override
    public Array<SpawnGroup> getWaves(Sector sector){
        return Waves.getSpawns();
    }

    @Override
    public void generate(Generation gen){
        generateCoreAtFirstSpawnPoint(gen, Team.blue);
    }

    @Override
    public void onBegin(){
        super.onBegin();

        world.pathfinder.activateTeamPath(waveTeam);
    }

    @Override
    public GameMode getMode(){
        return GameMode.waves;
    }

    @Override
    public String displayString(){
        return state.wave > target ?
            Bundles.format(
                state.enemies() > 1 ?
                "text.mission.wave.enemies" :
                "text.mission.wave.enemy", target, target, state.enemies()) :
            Bundles.format("text.mission.wave", state.wave, target, (int)(state.wavetime/60));
    }

    @Override
    public String menuDisplayString(){
        return Bundles.format("text.mission.wave.menu", target);
    }

    @Override
    public void update(){
        if(state.wave > target){
            state.mode = GameMode.noWaves;
        }
    }

    @Override
    public boolean isComplete(){
        return state.wave > target && state.enemies() == 0;
    }
}
