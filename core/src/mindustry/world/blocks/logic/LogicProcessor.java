package mindustry.world.blocks.logic;

import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.world.*;

public class LogicProcessor extends Block{

    public LogicProcessor(String name){
        super(name);
        update = true;
        configurable = true;

        config(String.class, (LogicEntity entity, String code) -> {
            if(code != null){
                entity.code = code;

                try{
                    entity.executor.load(entity, code);
                }catch(Exception e){
                    e.printStackTrace();

                    //handle malformed code and replace it with nothing
                    entity.executor.load(entity, "");
                }
            }
        });
    }

    public class LogicEntity extends Building{
        /** logic "source code" as list of json statements */
        String code = "";
        LExecutor executor = new LExecutor();

        @Override
        public void updateTile(){
            if(executor.initialized()){
                executor.runOnce();
            }
        }

        @Override
        public boolean configTapped(){
            Vars.ui.logic.show(code, this::configure);
            return false;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.str(code);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            code = read.str();
            try{
                executor.load(this, code);
            }catch(Exception e){
                e.printStackTrace();

                //handle malformed code and ignore it
                executor = new LExecutor();
            }
        }
    }
}
