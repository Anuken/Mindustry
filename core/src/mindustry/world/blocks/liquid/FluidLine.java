package mindustry.world.blocks.liquid;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.liquid.Conduit.*;
import mindustry.world.modules.*;

/*
TODO:
- junction support
- disable support
- update support
- remove conduits from update()
- visuals
- calculate flow rate based on network contents
- networks for just junctions or just conduits
- network for routers/tanks/etc
*/

/** A line of fluid blocks, usually conduits. Has exactly one output and any number of external inputs. */
public class FluidLine{
    private static final float noneAmount = 0.1f;

    private Seq<Building> builds = new Seq<>();
    private @Nullable Building head;
    private @Nullable FluidLineUpdater updater;
    private LiquidModule liquids = new LiquidModule();

    private float capacity;

    public FluidLine(){

    }

    //check for updater existence
    private void check(){
        if(updater == null){
            updater = FluidLineUpdater.create();
            updater.line = this;
            updater.add();
        }
    }

    public float acceptLiquid(Liquid liquid, float amount){
        //different liquid.
        if(liquid != liquids.current() && liquids.currentAmount() > noneAmount) return 0f;

        float accepted = Math.min(capacity - liquids.get(liquid), amount);
        liquids.add(liquid, accepted);
        return accepted;
    }

    //remove line after its main block is removed
    public void remove(){
        if(updater != null){
            updater.remove();
        }
    }

    public void update(){
        if(head != null && liquids.currentAmount() > 0){
            Building next = head.front();
            if(next.team == head.team){
                liquids.remove(liquids.current(), next.acceptLiquid(head, liquids.current(), liquids.currentAmount()));
            }
        }
    }
    
    public void forward(ConduitBuild start){
        check();
        start.line = null;
        start.liquids = liquids;
        ConduitBuild cur = start;
        while(cur != null && cur.line != this){
            head = cur;
            if(cur != start){
                add(cur);
            }else{
                cur.line = this;
            }

            Building next = cur.front();
            if(next instanceof ConduitBuild conduit){
                //check for inlets
                ConduitBuild left = inlet(next, 1), right = inlet(next, -1), back = inlet(next, 2);


                //fluid lines terminate when:
                //1. nowhere to go
                //2. there is another inlet; the priority is left, right, back

                boolean valid =
                    cur == back ? left == null && right == null :
                    cur == right ? left == null :
                    cur == left;

                cur = valid ? conduit : null;
            }else{
                cur = null;
            }
        }
    }

    public void backward(ConduitBuild start){
        check();
        start.line = null;
        start.liquids = liquids;
        ConduitBuild cur = start;
        while(cur != null && cur.line != this){
            if(cur != start){
                add(cur);
            }else{
                cur.line = this;
            }

            ConduitBuild next = null;

            //check every conduit facing this one.
            for(int i = 0; i < 4; i++){
                var in = inlet(cur, i);
                //current direction has an inlet, let's see if it's valid
                if(in != null){
                    //check for inlets
                    ConduitBuild left = inlet(cur, 1), right = inlet(cur, -1), back = inlet(cur, -2);

                    //fluid lines terminate when:
                    //1. nowhere to go
                    //2. there is another inlet; the priority is left, right, back

                    boolean valid =
                        in == back ? left == null && right == null :
                        in == right ? left == null :
                        in == left;

                    if(valid){
                        next = in;
                        break;
                    }
                }
            }

            cur = next;
        }
    }

    void add(ConduitBuild build){
        if(build.line != this){
            builds.add(build);
            if(build.line.updater != null){
                build.line.updater.remove();
            }
            build.line = this;
            build.liquids = liquids;

            capacity += build.block.liquidCapacity;
        }
    }

    @Nullable
    static ConduitBuild inlet(Building build, int dir){
        int rot = Mathf.mod(build.rotation + dir, 4);
        Building other = build.nearby(rot);
        //make sure other is facing this one, but not in opposite directions
        return other instanceof ConduitBuild cond && other.front() == build && (other.rotation != (build.rotation + 2)%4) ? cond : null;
    }

    @EntityDef(value = FluidLineUpdaterc.class, genio = false, serialize = false)
    @Component(base = true)
    static abstract class FluidLineUpdaterComp{
        public FluidLine line;

        void update(){
            if(line != null){
                line.update();
            }
        }
    }
}
