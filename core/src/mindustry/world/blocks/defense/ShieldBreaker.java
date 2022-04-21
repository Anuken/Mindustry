package mindustry.world.blocks.defense;

import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;

public class ShieldBreaker extends Block{
    public @Nullable Block toDestroy;
    public Effect effect = Fx.shockwave, breakEffect = Fx.reactorExplosion, selfKillEffect = Fx.massiveExplosion;

    public ShieldBreaker(String name){
        super(name);

        solid = update = true;
    }

    @Override
    public boolean canBreak(Tile tile){
        return false;
    }

    public class ShieldBreakerBuild extends Building{

        @Override
        public void updateTile(){
            if(Mathf.equal(efficiency, 1f)){
                if(toDestroy != null){
                    effect.at(this);
                    for(var other : Vars.state.teams.active){
                        if(team != other.team){
                            other.getBuildings(toDestroy).copy().each(b -> {
                                breakEffect.at(b);
                                b.kill();
                            });
                        }
                    }
                    selfKillEffect.at(this);
                    kill();
                }
            }
        }
    }
}
