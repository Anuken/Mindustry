package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class DuctRouter extends Block{
    public float speed = 5f;

    public @Load(value = "@-top") TextureRegion topRegion;

    public DuctRouter(String name){
        super(name);

        group = BlockGroup.transportation;
        update = true;
        solid = false;
        hasItems = true;
        unloadable = false;
        itemCapacity = 1;
        noUpdateDisabled = true;
        configurable = true;
        saveConfig = true;
        rotate = true;
        clearOnDoubleTap = true;
        underBullets = true;
        priority = TargetPriority.transport;
        envEnabled = Env.space | Env.terrestrial | Env.underwater;

        config(Item.class, (DuctRouterBuild tile, Item item) -> tile.sortItem = item);
        configClear((DuctRouterBuild tile) -> tile.sortItem = null);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, 60f / speed * itemCapacity, StatUnit.itemsSecond);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(topRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    @Override
    public int minimapColor(Tile tile){
        var build = (DuctRouterBuild)tile.build;
        return build == null || build.sortItem == null ? 0 : build.sortItem.color.rgba();
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    public class DuctRouterBuild extends Building{
        public @Nullable Item sortItem;

        public float progress;
        public @Nullable Item current;

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            if(sortItem != null){
                Draw.color(sortItem.color);
                Draw.rect("center", x, y);
                Draw.color();
            }else{
                Draw.rect(topRegion, x, y, rotdeg());
            }
        }

        @Override
        public void updateTile(){
            progress += edelta() / speed * 2f;

            if(current != null){
                if(progress >= (1f - 1f/speed)){
                    var target = target();
                    if(target != null){
                        target.handleItem(this, current);
                        int mod = sortItem != null && current != sortItem ? 2 : 3;
                        cdump = ((cdump + 1) % mod);
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

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(DuctRouter.this, table, content.items(), () -> sortItem, this::configure);
        }

        @Nullable
        public Building target(){
            if(current == null) return null;

            for(int i = -1; i <= 1; i++){
                int dir = Mathf.mod(rotation + (((i + cdump + 1) % 3) - 1), 4);
                if(sortItem != null && (current == sortItem) != (dir == rotation)) continue;
                Building other = nearby(dir);
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

        @Override
        public Item config(){
            return sortItem;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            sortItem = content.item(read.s());
        }
    }
}
