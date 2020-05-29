package mindustry.world.blocks.production;

import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.world.*;

public class ResearchBlock extends Block{
    public float researchSpeed = 1f;

    public ResearchBlock(String name){
        super(name);

        update = true;
        solid = true;
        hasPower = true;
        hasItems = true;
        configurable = true;
    }

    public class ResearchBlockEntity extends TileEntity{
        public @Nullable UnlockableContent researching;

        @Override
        public void updateTile(){

        }

        @Override
        public void buildConfiguration(Table table){

        }

        @Override
        public void write(Writes write){
            super.write(write);

            if(researching != null){
                write.b(researching.getContentType().ordinal());
                write.s(researching.id);
            }else{
                write.b(-1);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            byte type = read.b();
            if(type != -1){
                researching = Vars.content.getByID(ContentType.all[type], read.s());
            }else{
                researching = null;
            }
        }
    }
}
