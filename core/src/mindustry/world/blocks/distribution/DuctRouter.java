package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class DuctRouter extends Block{
    public float speed = 5f;

    public @Load(value = "@-top") TextureRegion topRegion;

    public DuctRouter(String name){
        super(name);

        group = BlockGroup.transportation;
        update = true;
        solid = false;
        hasItems = true;
        conveyorPlacement = true;
        unloadable = false;
        itemCapacity = 1;
        noUpdateDisabled = true;
        rotate = true;
        envEnabled = Env.space | Env.terrestrial | Env.underwater;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, 60f / speed, StatUnit.itemsSecond);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(topRegion, req.drawx(), req.drawy(), req.rotation * 90);
    }

    public class DuctRouterBuild extends Building{
        public float progress;
        public @Nullable Item current;

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(topRegion, x, y, rotdeg());
        }

        @Override
        public void updateTile(){
            progress += edelta() / speed * 2f;

            if(current != null){
                if(progress >= (1f - 1f/speed)){
                    var target = target();
                    if(target != null){
                        target.handleItem(this, current);
                        cdump = (byte)((cdump + 1) % 3);
                        items.remove(current, 1);
                        current = null;
                        progress %= (1f - 1f/speed);
                    }
                }
            }else{
                progress = 0;
            }

            if(current == null && items.total() > 0){
                current = items.first();
            }
        }

        @Nullable
        public Building target(){
            if(current == null) return null;

            for(int i = -1; i <= 1; i++){
                Building other = nearby(Mathf.mod(rotation + (((i + cdump + 1) % 3) - 1), 4));
                if(other != null && other.team == team && other.acceptItem(this, current)){
                    return other;
                }
            }
            return null;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return current == null && items.total() == 0 &&
                (Edges.getFacingEdge(source.tile(), tile).relativeTo(tile) == rotation);
        }

        @Override
        public int removeStack(Item item, int amount){
            int removed = super.removeStack(item, amount);
            if(item == current) current = null;
            return removed;
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            super.handleStack(item, amount, source);
            current = item;
        }

        @Override
        public void handleItem(Building source, Item item){
            current = item;
            progress = -1f;
            items.add(item, 1);
            noSleep();
        }
    }
}
