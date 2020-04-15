package mindustry.world.blocks.distribution;

import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

public class MassConveyor extends Block{
    public float moveTime = 70f;

    public MassConveyor(String name){
        super(name);

        layer = Layer.overlay;
        size = 3;
    }

    public class MassConveyorEntity extends TileEntity{
        public Conveyable item;
        public float progress;
        public @Nullable Tilec next;

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            next = front();
            //next block must be aligned and of the same size
            if(next.block().size != size || x + Geometry.d4[rotation()].x * size != next.tileX() || y + Geometry.d4[rotation()].y * size != next.tileY()){
                next = null;
            }
        }

        @Override
        public void updateTile(){
            progress += edelta();
            if(progress >= moveTime){
                //todo step
                progress = 0;
            }
        }

        @Override
        public void draw(){
            super.draw();
        }

        @Override
        public void drawLayer(){

        }
    }

    public interface Conveyable{
        void drawConvey(float x, float y, float rotation);
    }
}
