package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.state;

public class WaveMission implements Mission{
    private final int target;

    public WaveMission(int target){
        this.target = target;
    }

    @Override
    public void generate(Tile[][] tiles, Sector sector){
        int coreX = tiles.length/2, coreY = tiles.length/2;
        float targetElevation = Math.max(tiles[coreX][coreY].getElevation(), 1);

        int lerpDst = 20;
        for(int x = -lerpDst/2; x <= lerpDst/2; x++){
            for(int y = -lerpDst/2; y <= lerpDst/2; y++){
                int wx = tiles.length/2 + x, wy = tiles[0].length/2 + y;

                float dst = Vector2.dst(wx, wy, coreX, coreY);
                float elevation = tiles[wx][wy].getElevation();

                if(dst < lerpDst){
                    elevation = Mathf.lerp(elevation, targetElevation, Mathf.clamp(2*(1f-(dst / lerpDst))));
                }

                if(tiles[wx][wy].floor().liquidDrop == null){
                    tiles[wx][wy].setElevation((int) elevation);
                }else{
                    tiles[wx][wy].setFloor((Floor) Blocks.sand);
                }
            }
        }

        tiles[coreX][coreY].setBlock(StorageBlocks.core);
        tiles[coreX][coreY].setTeam(Team.blue);
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
