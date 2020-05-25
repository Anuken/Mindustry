package mindustry.world.blocks.experimental;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.production.*;

import static mindustry.Vars.*;

//pointless, will definitely be removed eventually
public class BlockLauncher extends PayloadAcceptor{
    static final IntArray positions = new IntArray();

    public float range = 150;

    public BlockLauncher(String name){
        super(name);

        update = true;
        size = 3;
    }

    public class BlockLauncherEntity extends PayloadAcceptorEntity<BlockPayload>{

        @Override
        public void draw(){
            super.draw();

            drawPayload();
        }

        @Override
        public boolean acceptPayload(Tilec source, Payload payload){
            return this.payload == null && payload instanceof BlockPayload;
        }

        @Override
        public void updateTile(){
            if(moveInPayload() && efficiency() >= 0.99f){
                Effects.shake(4f, 4f, this);
                Fx.producesmoke.at(this);

                positions.clear();

                Geometry.circle(tileX(), tileY(), world.width(), world.height(), (int)(range / tilesize), (cx, cy) -> {
                    if(Build.validPlace(team, cx, cy, payload.block, 0)){
                        positions.add(Point2.pack(cx, cy));
                    }
                });

                if(positions.isEmpty()) return;

                int pick = positions.random();
                LaunchedBlock launch = new LaunchedBlock(Point2.x(pick), Point2.y(pick), payload.block, team);
                Fx.blockTransfer.at(x, y, 0, launch);
                Time.run(Fx.blockTransfer.lifetime, () -> {
                    float ex = launch.x * tilesize + launch.block.offset(), ey = launch.y * tilesize + launch.block.offset();
                    if(Build.validPlace(launch.team, launch.x, launch.y, launch.block, 0)){
                        world.tile(launch.x, launch.y).setBlock(launch.block, launch.team);
                        Fx.placeBlock.at(ex, ey, launch.block.size);
                    }else{
                        Fx.breakBlock.at(ex, ey, launch.block.size);
                        Fx.explosion.at(ex, ey);
                    }
                });
                payload = null;
            }
        }
    }

    public static class LaunchedBlock{
        public final int x, y;
        public final Block block;
        public final Team team;

        public LaunchedBlock(int x, int y, Block block, Team team){
            this.x = x;
            this.y = y;
            this.block = block;
            this.team = team;
        }
    }
}
