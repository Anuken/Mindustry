package mindustry.world.consumers;

import arc.math.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Causes a block to explode when explosive items are moved into it. */
public class ConsumeItemExplode extends ConsumeItemFilter{
    public float damage = 4f;
    public float threshold, baseChance = 0.06f;
    public Effect explodeEffect = Fx.generatespark;

    public ConsumeItemExplode(float threshold){
        this.filter = item -> item.explosiveness >= this.threshold;
        this.threshold = threshold;
    }

    public ConsumeItemExplode(){
        this(0.5f);
    }

    @Override
    public void update(Building build){
        var item = getConsumed(build);

        if(item != null){
            if(Vars.state.rules.reactorExplosions && Mathf.chance(build.delta() * baseChance * Mathf.clamp(item.explosiveness - threshold))){
                build.damage(damage);
                explodeEffect.at(build.x + Mathf.range(build.block.size * tilesize / 2f), build.y + Mathf.range(build.block.size * tilesize / 2f));
            }
        }
    }

    //as this consumer doesn't actually consume anything, all methods below are empty

    @Override
    public void build(Building build, Table table){}

    @Override
    public void trigger(Building build){}

    @Override
    public void display(Stats stats){}

    @Override
    public void apply(Block block){}

    @Override
    public float efficiency(Building build){
        return 1f;
    }
}
