package mindustry.world.blocks.campaign;

import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;

public class CoreLauncher extends Block{

    public CoreLauncher(String name){
        super(name);

        hasItems = true;
        configurable = true;
    }

    public class CoreLauncherEntity extends TileEntity{

        @Override
        public void updateTile(){
            super.updateTile();
        }

        @Override
        public boolean configTapped(){
            //TODO show w/ sector
            Vars.ui.planet.show();

            return false;
        }


    }
}
