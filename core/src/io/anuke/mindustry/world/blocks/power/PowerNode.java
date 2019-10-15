package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

public class PowerNode extends PowerBlock{
    protected ObjectSet<PowerGraph> graphs = new ObjectSet<>();
    protected Vector2 t1 = new Vector2(), t2 = new Vector2();
    protected TextureRegion laser, laserEnd;

    protected float laserRange = 6;
    protected int maxNodes = 3;

    public PowerNode(String name){
        super(name);
        expanded = true;
        layer = Layer.power;
        configurable = true;
        consumesPower = false;
        outputsPower = false;
    }
/*
    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void linkPowerNodes(Player player, Tile tile, Tile other){
        if(tile.entity == null || other == null || tile.entity.power == null || !((PowerNode)tile.block()).linkValid(tile, other)
        || tile.entity.power.links.size >= ((PowerNode)tile.block()).maxNodes) return;
        if(!Units.canInteract(player, tile)) return;

        TileEntity entity = tile.entity();

        if(!entity.power.links.contains(other.pos())){
            entity.power.links.add(other.pos());
        }

        if(other.getTeamID() == tile.getTeamID()){

            if(!other.entity.power.links.contains(tile.pos())){
                other.entity.power.links.add(tile.pos());
            }
        }

        entity.power.graph.add(other.entity.power.graph);
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unlinkPowerNodes(Player player, Tile tile, Tile other){
        if(tile.entity.power == null || other.entity == null || other.entity.power == null) return;
        if(!Units.canInteract(player, tile)) return;

        TileEntity entity = tile.entity();

        entity.power.links.removeValue(other.pos());
        other.entity.power.links.removeValue(tile.pos());

        PowerGraph newgraph = new PowerGraph();

        //reflow from this point, covering all tiles on this side
        newgraph.reflow(tile);

        if(other.entity.power.graph != newgraph){
            //create new graph for other end
            PowerGraph og = new PowerGraph();
            //reflow from other end
            og.reflow(other);
        }
    }

 */

    @Override
    public void configured(Tile tile, Player player, int value){
        TileEntity entity = tile.entity;
        Tile other = world.tile(value);
        boolean contains = entity.power.links.contains(value), valid = other != null && other.entity != null && other.entity.power != null;

        if(contains){
            //unlink
            entity.power.links.removeValue(value);
            if(valid) other.entity.power.links.removeValue(tile.pos());

            PowerGraph newgraph = new PowerGraph();

            //reflow from this point, covering all tiles on this side
            newgraph.reflow(tile);

            if(valid && other.entity.power.graph != newgraph){
                //create new graph for other end
                PowerGraph og = new PowerGraph();
                //reflow from other end
                og.reflow(other);
            }
        }else if(linkValid(tile, other) && valid && entity.power.links.size < maxNodes){

            if(!entity.power.links.contains(other.pos())){
                entity.power.links.add(other.pos());
            }

            if(other.getTeamID() == tile.getTeamID()){

                if(!other.entity.power.links.contains(tile.pos())){
                    other.entity.power.links.add(tile.pos());
                }
            }

            entity.power.graph.add(other.entity.power.graph);
        }
    }

    @Override
    public void load(){
        super.load();

        laser = Core.atlas.find("laser");
        laserEnd = Core.atlas.find("laser-end");
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("power", entity -> new Bar(() ->
        Core.bundle.format("bar.powerbalance",
        ((entity.power.graph.getPowerBalance() >= 0 ? "+" : "") + Strings.fixed(entity.power.graph.getPowerBalance() * 60, 1))),
        () -> Pal.powerBar,
        () -> Mathf.clamp(entity.power.graph.getPowerProduced() / entity.power.graph.getPowerNeeded())));

        bars.add("batteries", entity -> new Bar(() ->
        Core.bundle.format("bar.powerstored",
        (ui.formatAmount((int)entity.power.graph.getBatteryStored())), ui.formatAmount((int)entity.power.graph.getTotalBatteryCapacity())),
        () -> Pal.powerBar,
        () -> Mathf.clamp(entity.power.graph.getBatteryStored() / entity.power.graph.getTotalBatteryCapacity())));
    }

    @Override
    public void placed(Tile tile){
        if(net.client()) return;

        Predicate<Tile> valid = other -> other != null && other != tile && ((!other.block().outputsPower && other.block().consumesPower) || (other.block().outputsPower && !other.block().consumesPower) || other.block() instanceof PowerNode) && linkValid(tile, other)
        && !other.entity.proximity().contains(tile) && other.entity.power.graph != tile.entity.power.graph;

        tempTiles.clear();
        Geometry.circle(tile.x, tile.y, (int)(laserRange + 1), (x, y) -> {
            Tile other = world.ltile(x, y);
            if(valid.test(other)){
                tempTiles.add(other);
            }
        });

        tempTiles.sort(Structs.comparingFloat(t -> t.dst2(tile)));
        tempTiles.each(valid, other -> {
            if(!tile.entity.power.links.contains(other.pos())){
                tile.configure(other.pos());
            }
        });

        super.placed(tile);
    }

    private void getPotentialLinks(Tile tile, Consumer<Tile> others){
        Predicate<Tile> valid = other -> other != null && other != tile && other.entity != null && other.entity.power != null &&
        ((!other.block().outputsPower && other.block().consumesPower) || (other.block().outputsPower && !other.block().consumesPower) || other.block() instanceof PowerNode) &&
        overlaps(tile.x * tilesize + offset(), tile.y *tilesize + offset(), other, laserRange * tilesize) && other.getTeam() == player.getTeam()
        && !other.entity.proximity().contains(tile) && !graphs.contains(other.entity.power.graph);

        tempTiles.clear();
        graphs.clear();
        Geometry.circle(tile.x, tile.y, (int)(laserRange + 1), (x, y) -> {
            Tile other = world.ltile(x, y);
            if(valid.test(other)){
                tempTiles.add(other);
            }
        });

        tempTiles.sort(Structs.comparingFloat(t -> t.dst2(tile)));
        tempTiles.each(valid, t -> {
            graphs.add(t.entity.power.graph);
            others.accept(t);
        });
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.powerRange, laserRange, StatUnit.blocks);
    }

    @Override
    public void update(Tile tile){
        tile.entity.power.graph.update();
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        TileEntity entity = tile.entity();
        other = other.link();

        Tile result = other;

        if(linkValid(tile, other)){
            tile.configure(other.pos());
            return false;
        }
        return true;
    }

    @Override
    public void drawSelect(Tile tile){
        super.drawSelect(tile);

        Lines.stroke(1f);

        Draw.color(Pal.accent);
        Drawf.circles(tile.drawx(), tile.drawy(), laserRange * tilesize);
        Draw.reset();
    }

    @Override
    public void drawConfigure(Tile tile){

        Draw.color(Pal.accent);

        Lines.stroke(1.5f);
        Lines.circle(tile.drawx(), tile.drawy(),
        tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time(), 4f, 1f));

        Drawf.circles(tile.drawx(), tile.drawy(), laserRange * tilesize);

        Lines.stroke(1.5f);

        for(int x = (int)(tile.x - laserRange - 1); x <= tile.x + laserRange + 1; x++){
            for(int y = (int)(tile.y - laserRange - 1); y <= tile.y + laserRange + 1; y++){
                Tile link = world.ltile(x, y);

                if(link != tile && linkValid(tile, link, false)){
                    boolean linked = linked(tile, link);

                    if(linked){
                        Drawf.square(link.drawx(), link.drawy(), link.block().size * tilesize / 2f + 1f, Pal.place);
                    }
                }
            }
        }

        Draw.reset();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Tile tile = world.tile(x, y);

        if(tile == null) return;

        Lines.stroke(1f);
        Draw.color(Pal.placing);
        Drawf.circles(x * tilesize + offset(), y * tilesize + offset(), laserRange * tilesize);

        getPotentialLinks(tile, other -> Drawf.square(other.drawx(), other.drawy(), other.block().size * tilesize / 2f + 2f, Pal.place));

        Draw.reset();
    }

    @Override
    public void drawLayer(Tile tile){
        if(Core.settings.getInt("lasersopacity") == 0) return;

        TileEntity entity = tile.entity();

        for(int i = 0; i < entity.power.links.size; i++){
            Tile link = world.tile(entity.power.links.get(i));

            if(!linkValid(tile, link)) continue;

            if(link.block() instanceof PowerNode && !(link.pos() < tile.pos())) continue;

            drawLaser(tile, link);
        }

        Draw.reset();
    }

    protected boolean linked(Tile tile, Tile other){
        return tile.entity.power.links.contains(other.pos());
    }

    public boolean linkValid(Tile tile, Tile link){
        return linkValid(tile, link, true);
    }

    public boolean linkValid(Tile tile, Tile link, boolean checkMaxNodes){
        if(tile == link || link == null || link.entity == null || tile.entity == null || !link.block().hasPower || tile.getTeam() != link.getTeam()) return false;

        if(overlaps(tile, link, laserRange * tilesize) || (link.block() instanceof PowerNode && overlaps(link, tile, link.<PowerNode>cblock().laserRange * tilesize))){
            if(checkMaxNodes && link.block() instanceof PowerNode){
                return link.entity.power.links.size < link.<PowerNode>cblock().maxNodes || link.entity.power.links.contains(tile.pos());
            }
            return true;
        }
        return false;
    }

    protected boolean overlaps(float srcx, float srcy, Tile other, float range){
        return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), other.getHitbox(Tmp.r1));
    }

    protected boolean overlaps(Tile src, Tile other, float range){
        return overlaps(src.drawx(), src.drawy(), other, range);
    }

    protected void drawLaser(Tile tile, Tile target){
        int opacityPercentage = Core.settings.getInt("lasersopacity");
        if(opacityPercentage == 0) return;

        float opacity = opacityPercentage / 100f;

        float x1 = tile.drawx(), y1 = tile.drawy(),
        x2 = target.drawx(), y2 = target.drawy();

        float angle1 = Angles.angle(x1, y1, x2, y2);
        t1.trns(angle1, tile.block().size * tilesize / 2f - 1.5f);
        t2.trns(angle1 + 180f, target.block().size * tilesize / 2f - 1.5f);

        x1 += t1.x;
        y1 += t1.y;
        x2 += t2.x;
        y2 += t2.y;

        float fract = 1f-tile.entity.power.graph.getSatisfaction();

        Draw.color(Color.white, Pal.powerLight, fract*0.86f + Mathf.absin(3f, 0.1f));
        Draw.alpha(opacity);
        Drawf.laser(laser, laserEnd, x1, y1, x2, y2, 0.25f);
        Draw.color();
    }

}
