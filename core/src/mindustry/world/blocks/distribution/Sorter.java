package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Sorter extends Block{
    public boolean invert;

    public Sorter(String name){
        super(name);
        update = true;
        solid = true;
        instantTransfer = true;
        group = BlockGroup.transportation;
        configurable = true;
        unloadable = false;
        saveConfig = true;

        config(Item.class, (SorterBuild tile, Item item) -> tile.sortItem = item);
        configClear((SorterBuild tile) -> tile.sortItem = null);
    }

    @Override
    public void drawRequestConfig(BuildPlan req, Eachable<BuildPlan> list){
        drawRequestConfigCenter(req, req.config, "center");
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public int minimapColor(Tile tile){
        return tile.<SorterBuild>bc().sortItem == null ? 0 : tile.<SorterBuild>bc().sortItem.color.rgba();
    }

    public class SorterBuild extends Building{
        public @Nullable Item sortItem;

        @Override
        public void configured(Unit player, Object value){
            super.configured(player, value);

            if(!headless){
                renderer.minimap.update(tile);
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(sortItem == null){
                Draw.rect("cross", x, y);
            }else{
                Draw.color(sortItem.color);
                Draw.rect("center", x, y);
                Draw.color();
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            Building to = getTileTarget(item, source, false);

            return to != null && to.acceptItem(this, item) && to.team == team;
        }

        @Override
        public void handleItem(Building source, Item item){
            Building to = getTileTarget(item, source, true);

            to.handleItem(this, item);
        }

        public boolean isSame(Building other){
            // comment code below to allow sorter/gate chaining
            return other != null && other.block.instantTransfer;
        }

        public Building getTileTarget(Item item, Building source, boolean flip){
            int dir = source.relativeTo(tile.x, tile.y);
            if(dir == -1) return null;
            Building to;

            if(((item == sortItem) != invert) == enabled){
                //prevent 3-chains
                if(isSame(source) && isSame(nearby(dir))){
                    return null;
                }
                to = nearby(dir);
            }else{
                Building a = nearby(Mathf.mod(dir - 1, 4));
                Building b = nearby(Mathf.mod(dir + 1, 4));
                boolean ac = a != null && !(a.block.instantTransfer && source.block.instantTransfer) &&
                a.acceptItem(this, item);
                boolean bc = b != null && !(b.block.instantTransfer && source.block.instantTransfer) &&
                b.acceptItem(this, item);

                if(ac && !bc){
                    to = a;
                }else if(bc && !ac){
                    to = b;
                }else if(!bc){
                    return null;
                }else{
                    if(rotation == 0){
                        to = a;
                        if(flip) this.rotation = (byte)1;
                    }else{
                        to = b;
                        if(flip) this.rotation = (byte)0;
                    }
                }
            }

            return to;
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table, content.items(), () -> sortItem, this::configure);
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
            if(this == other){
                deselect();
                configure(null);
                return false;
            }

            return true;
        }

        @Override
        public Item config(){
            return sortItem;
        }

        @Override
        public byte version(){
            return 2;
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

            if(revision == 1){
                new DirectionalItemBuffer(20).read(read);
            }
        }
    }
}
