package mindustry.world.blocks.campaign;

import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.state;

public class CoreLauncher extends Block{
    public int range = 1;

    public CoreLauncher(String name){
        super(name);

        hasItems = true;
        configurable = true;
        update = true;
    }

    public class CoreLauncherEntity extends TileEntity{

        @Override
        public void updateTile(){
            super.updateTile();
        }

        @Override
        public boolean configTapped(){
            if(state.isCampaign()){
                Vars.ui.planet.show(state.rules.sector, range);
            }
            return false;
        }
    }
}
