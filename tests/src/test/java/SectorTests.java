import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.maps.SectorPresets;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.maps.missions.Mission;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** This class is responsible for testing predefined sectors. */
public class SectorTests{

    private SectorPresets presets;
    private Generation fakeGen;

    @BeforeAll
    static void initializeDependencies(){
        Vars.content = new ContentLoader();
        Vars.content.load();
    }

    @BeforeEach
    void initTest(){
        this.presets = new SectorPresets();

        // Fake away the Generation dependency
        this.fakeGen = new Generation(null, null, 250, 250, null);
    }

    /** Returns true if at least one mission provides a spawn point. */
    private boolean spawnPointIsDefined(Array<Mission> missions){
        for(Mission mission : missions){
            if(mission.getSpawnPoints(this.fakeGen).size > 0){
                return true;
            }
        }
        // No spawn point provided
        return false;
    }

    /**
     * Makes sure that every predefined sector has a position for the player core defined.
     * This is achieved by adding at least one mission which defines a spawn point.
     */
    @Test
    void sectorHasACore(){
        for(SectorPresets.SectorPreset preset : this.presets.getPresets().values()){
            assertTrue(spawnPointIsDefined(preset.missions), "Sector at (" + preset.x + "|" + preset.y + ") contains no missions which define a spawn point. Add a battle or wave mission.");
        }
    }
}
