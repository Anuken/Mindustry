package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.state;

public class WaveMission implements Mission{
    private final int target;

    public WaveMission(int target){
        this.target = target;
    }

    @Override
    public Array<SpawnGroup> getWaves(Sector sector){
        Array<SpawnGroup> spawns = new Array<>();
        return Waves.getSpawns();
    }

    @Override
    public void generate(Generation gen){
        int coreX = gen.width/2, coreY = gen.height/2;
        generateCoreAt(gen, coreX, coreY, Team.blue);
    }

    @Override
    public void display(Table table){
        table.add(Bundles.format("text.mission.wave", target));
    }

    @Override
    public GameMode getMode(){
        return GameMode.waves;
    }

    @Override
    public String displayString(){
        return Bundles.format("text.mission.wave", target);
    }

    @Override
    public boolean isComplete(){
        return state.wave >= target;
    }

    @Override
    public Array<GridPoint2> getSpawnPoints(Generation gen){
        return Array.with(new GridPoint2(gen.width/2, gen.height/2));
    }
}
