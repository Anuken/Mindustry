package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
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
    public boolean armored = false;
    public Color transparentColor = new Color(0.4f, 0.4f, 0.4f, 0.1f);

    public @Load(value = "@-top-#", length = 5) TextureRegion[] topRegions;
    public @Load(value = "@-bottom-#", length = 5, fallback = "duct-bottom-#") TextureRegion[] botRegions;

    public @Nullable Block bridgeReplacement;

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
        underBullets = true;
        rotate = true;
        noSideBlend = true;
        isDuct = true;
        priority = TargetPriority.transport;
        envEnabled = Env.space | Env.terrestrial | Env.underwater;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, 60f / speed, StatUnit.itemsSecond);
    }

    @Override
    public void init(){
        super.init();

        if(bridgeReplacement == null || !(bridgeReplacement instanceof DuctBridge)) bridgeReplacement = Blocks.ductBridge;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        int[] bits = getTiling(plan, list);

        if(bits == null) return;

        Draw.scl(bits[1], bits[2]);
        Draw.alpha(0.5f);
        Draw.rect(botRegions[bits[0]], plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.color();
        Draw.rect(topRegions[bits[0]], plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.scl();
    }

    @Override
    public boolean blendsArmored(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return Point2.equals(tile.x + Geometry.d4(rotation).x, tile.y + Geometry.d4(rotation).y, otherx, othery)
            || ((!otherblock.rotatedOutput(otherx, othery) && Edges.getFacingEdge(otherblock, otherx, othery, tile) != null &&
            Edges.getFacingEdge(otherblock, otherx, othery, tile).relativeTo(tile) == rotation) ||

            ((otherblock.rotatedOutput(otherx, othery)) && (otherblock.isDuct) && Point2.equals(otherx + Geometry.d4(otherrot).x, othery + Geometry.d4(otherrot).y, tile.x, tile.y)));
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        if(!armored){
            return (otherblock.outputsItems() || (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems))
            && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
        }else{
            return (otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock)) || (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems);
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find("duct-bottom"), topRegions[0]};
    }

    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans){
        if(bridgeReplacement == null) return;

        Placement.calculateBridges(plans, (DuctBridge)bridgeReplacement, false, b -> b instanceof Duct || b instanceof StackConveyor || b instanceof Conveyor);
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
                if((blending & (1 << i)) != 0){
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

        @Override
        public void payloadDraw(){
            Draw.rect(fullIcon, x, y);
        }

        protected void drawAt(float x, float y, int bits, float rotation, SliceMode slice){
            Draw.z(Layer.blockUnder);
            Draw.rect(sliced(botRegions[bits], slice), x, y, rotation);

            Draw.z(Layer.blockUnder + 0.2f);
            Draw.color(transparentColor);
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
                (armored ?
                    //armored acceptance
                    ((source.block.rotate && source.front() == this && source.block.hasItems && source.block.isDuct) ||
                    Edges.getFacingEdge(source.tile(), tile).relativeTo(tile) == rotation) :
                    //standard acceptance - do not accept from front
                    !(source.block.rotate && next == source) && Edges.getFacingEdge(source.tile, tile) != null && Math.abs(Edges.getFacingEdge(source.tile, tile).relativeTo(tile.x, tile.y) - rotation) != 2
                );
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
