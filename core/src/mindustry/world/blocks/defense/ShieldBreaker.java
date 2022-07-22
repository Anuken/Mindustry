package mindustry.world.blocks.defense;

import arc.math.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;

public class ShieldBreaker extends Block{
    public Block[] toDestroy = {};
    public Effect effect = Fx.shockwave, breakEffect = Fx.reactorExplosion, selfKillEffect = Fx.massiveExplosion;

    public ShieldBreaker(String name){
        super(name);

        solid = update = true;
        rebuildable = false;
    }

    @Override
    public boolean canBreak(Tile tile){
        return Vars.state.isEditor();
    }

    public class ShieldBreakerBuild extends Building{

        @Override
        public void updateTile(){
            if(Mathf.equal(efficiency, 1f)){
                effect.at(this);
                for(var other : Vars.state.teams.active){
                    if(team != other.team){
                        for(var block : toDestroy){
                            other.getBuildings(block).copy().each(b -> {
                                breakEffect.at(b);
                                b.kill();
                            });
                        }
                    }
                }
                selfKillEffect.at(this);
                kill();
            }
        }
    }
}
