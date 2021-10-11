package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Duct extends Block implements Autotiler{
    public float speed = 5f;

    public @Load(value = "@-top-#", length = 5) TextureRegion[] topRegions;
    public @Load(value = "@-bottom-#", length = 5, fallback = "duct-bottom-#") TextureRegion[] botRegions;

    public Duct(String name){
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
        noSideBlend = true;
        envEnabled = Env.space | Env.terrestrial | Env.underwater;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, 60f / speed, StatUnit.itemsSecond);
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        Draw.scl(bits[1], bits[2]);
        Draw.alpha(0.5f);
        Draw.rect(botRegions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
        Draw.color();
        Draw.rect(topRegions[bits[0]], req.drawx(), req.drawy(), req.rotation * 90);
        Draw.scl();
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find("duct-bottom"), topRegions[0]};
    }

    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans){
        Placement.calculateDuctBridges(plans, (DuctBridge)Blocks.ductBridge);
    }

    public class DuctBuild extends Building{
        public float progress;
        public @Nullable Item current;
        public int recDir = 0;
        public int blendbits, xscl, yscl, blending;
        public @Nullable Building next;
        public @Nullable DuctBuild nextc;

        @Override
        public void draw(){
            float rotation = rotdeg();
            int r = this.rotation;

            //draw extra ducts facing this one for tiling purposes
            for(int i = 0; i < 4; i++){
                if((blending & (1 << i)) != 0 && !(i == 0 && nextc != null)){
                    int dir = r - i;
                    float rot = i == 0 ? rotation : (dir)*90;
                    drawAt(x + Geometry.d4x(dir) * tilesize*0.75f, y + Geometry.d4y(dir) * tilesize*0.75f, 0, rot, i != 0 ? SliceMode.bottom : SliceMode.top);
                }
            }

            //draw item
            if(current != null){
                Draw.z(Layer.blockUnder + 0.1f);
                Tmp.v1.set(Geometry.d4x(recDir) * tilesize / 2f, Geometry.d4y(recDir) * tilesize / 2f)
                .lerp(Geometry.d4x(r) * tilesize / 2f, Geometry.d4y(r) * tilesize / 2f,
                Mathf.clamp((progress + 1f) / 2f));

                Draw.rect(current.fullIcon, x + Tmp.v1.x, y + Tmp.v1.y, itemSize, itemSize);
            }

            Draw.scl(xscl, yscl);
            drawAt(x, y, blendbits, rotation, SliceMode.none);
            Draw.reset();
        }

        protected void drawAt(float x, float y, int bits, float rotation, SliceMode slice){
            Draw.z(Layer.blockUnder);
            Draw.rect(sliced(botRegions[bits], slice), x, y, rotation);

            Draw.z(Layer.blockUnder + 0.2f);
            Draw.color(0.4f, 0.4f, 0.4f, 0.4f);
            Draw.rect(sliced(botRegions[bits], slice), x, y, rotation);
            Draw.color();
            Draw.rect(sliced(topRegions[bits], slice), x, y, rotation);
        }

        @Override
        public void updateTile(){
            progress += edelta() / speed * 2f;

            if(current != null && next != null){
                if(progress >= (1f - 1f/speed) && moveForward(current)){
                    items.remove(current, 1);
                    current = null;
                    progress %= (1f - 1f/speed);
                }
            }else{
                progress = 0;
            }

            if(current == null && items.total() > 0){
                current = items.first();
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return current == null && items.total() == 0 &&
                ((source.block.rotate && source.front() == this && source.block.hasItems) || Edges.getFacingEdge(source.tile(), tile).relativeTo(tile) == rotation);
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
            recDir = relativeToEdge(source.tile);
            items.add(item, 1);
            noSleep();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int[] bits = buildBlending(tile, rotation, null, true);
            blendbits = bits[0];
            xscl = bits[1];
            yscl = bits[2];
            blending = bits[4];
            next = front();
            nextc = next instanceof DuctBuild d ? d : null;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(recDir);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                recDir = read.b();
            }
            current = items.first();
        }
    }
}
