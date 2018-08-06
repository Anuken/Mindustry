package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.ucore.scene.ui.layout.Table;

public interface Mission{
    boolean isComplete();
    String displayString();
    GameMode getMode();
    void display(Table table);

    default Array<SpawnGroup> getWaves(Sector sector){
        return new Array<>();
    }

    default Array<GridPoint2> getSpawnPoints(Generation gen){
        return Array.with();
    }

    default void generate(Generation gen){}

    default void generateCoreAt(Generation gen, int coreX, int coreY, Team team){
        gen.tiles[coreX][coreY].setBlock(StorageBlocks.core);
        gen.tiles[coreX][coreY].setTeam(team);
    }
}
