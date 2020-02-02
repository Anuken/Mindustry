package mindustry.server;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class PiertotumLocomotor implements ApplicationListener{

    private Interval timer = new Interval();

    private Array<Tile> in = new Array<>();

    @Override
    public void update(){
        if(!state.is(State.playing)) return;
        if(!timer.get(160f)) return;

        indexer.getAllied(Team.sharded, BlockFlag.repair).each(heart -> {
            in.clear();
            rasterize(heart);

            if(in.size <= 1) return;

            int rotation = heart.rotation;

            int conflicts = in.count(t -> {
                Tile to = t.getNearby(rotation);
                if (to.block() == Blocks.air) return false;
                if (in.contains(to)) return false;

                return true;
            });

            if(conflicts > 0){
                Core.app.post(() -> heart.setNet(heart.block, heart.getTeam(), Mathf.random(0, 3)));
                return;
            }

            in.each(t -> {
                Tile outTile = t.getNearby(rotation);
                Block outBlock = t.block;
                Team outTeam = Team.get(t.team);
                int config = t.entity.config();

                Time.run(1f, t::removeNet);
                Time.run(2f, () -> {
                    outTile.setNet(outBlock, outTeam, rotation);
                    if(config > -1) outTile.configureAny(config);
                });
            });
        });

    }

    private void rasterize(Tile tile){
        in.add(tile);

        tile.entity.proximity()
        .select(t -> t.block() instanceof Sorter && !in.contains(t))
        .each(this::rasterize);
    }
}
