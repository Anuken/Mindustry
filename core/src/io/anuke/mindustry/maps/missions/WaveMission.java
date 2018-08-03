package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.state;

public class WaveMission implements Mission{
    private final int target;

    public WaveMission(int target){
        this.target = target;
    }

    @Override
    public void generate(Tile[][] tiles, Sector sector){
        int coreX = tiles.length/2, coreY = tiles.length/2;
        generateCoreAt(tiles, coreX, coreY, Team.blue);
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
}
