package mindustry.world.blocks.logic;

import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;

public class LogicProcessor extends Block{

    public LogicProcessor(String name){
        super(name);
        update = true;
        configurable = true;
    }

    public class LogicEntity extends Building{

        @Override
        public void updateTile(){

        }

        @Override
        public boolean configTapped(){
            Vars.ui.logic.show();
            return false;
        }
    }
}
