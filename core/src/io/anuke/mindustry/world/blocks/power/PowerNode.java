package io.anuke.mindustry.world.blocks.power;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Intersector;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class PowerNode extends PowerBlock{
    //last distribution block placed
    private static int lastPlaced = -1;

    protected Vector2 t1 = new Vector2();
    protected Vector2 t2 = new Vector2();

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

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void linkPowerNodes(Player player, Tile tile, Tile other){
        if(tile.entity == null || other == null || tile.entity.power == null || !((PowerNode)tile.block()).linkValid(tile, other)
        || tile.entity.power.links.size >= ((PowerNode)tile.block()).maxNodes) return;

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

    @Override
    public void setBars(){
        super.setBars();
        bars.add("power", entity -> new Bar(() ->
        Core.bundle.format("bar.powerbalance",
        ((entity.power.graph.getPowerBalance() >= 0 ? "+" : "") + Strings.fixed(entity.power.graph.getPowerBalance() * 60, 1))),
        () -> Pal.powerBar,
        () -> Mathf.clamp(entity.power.graph.getPowerProduced() / entity.power.graph.getPowerNeeded())));
    }

    @Override
    public void playerPlaced(Tile tile){
        Tile before = world.tile(lastPlaced);
        if(linkValid(tile, before) && before.block() instanceof PowerNode){
            for(Tile near : before.entity.proximity()){
                if(near == tile){
                    lastPlaced = tile.pos();
                    return;
                }
            }
            Call.linkPowerNodes(null, tile, before);
        }

        lastPlaced = tile.pos();
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
            if(linked(tile, other)){
                Call.unlinkPowerNodes(null, tile, result);
            }else if(entity.power.links.size < maxNodes){
                Call.linkPowerNodes(null, tile, result);
            }
            return false;
        }
        return true;
    }

    @Override
    public void drawSelect(Tile tile){
        super.drawSelect(tile);

        Lines.stroke(1f);

        Draw.color(Pal.accent);
        Lines.poly(tile.drawx(), tile.drawy(), 50, laserRange * tilesize);
        Draw.reset();
    }

    @Override
    public void drawConfigure(Tile tile){
        TileEntity entity = tile.entity();

        Draw.color(Pal.accent);

        Lines.stroke(1.5f);
        Lines.circle(tile.drawx(), tile.drawy(),
        tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time(), 4f, 1f));

        Lines.poly(tile.drawx(), tile.drawy(), 50, laserRange * tilesize);

        for(int x = (int)(tile.x - laserRange - 1); x <= tile.x + laserRange + 1; x++){
            for(int y = (int)(tile.y - laserRange - 1); y <= tile.y + laserRange + 1; y++){
                Tile link = world.ltile(x, y);

                if(link != tile && linkValid(tile, link, false)){
                    boolean linked = linked(tile, link);
                    Draw.color(linked ? Pal.place : Pal.breakInvalid);

                    Lines.circle(link.drawx(), link.drawy(),
                    link.block().size * tilesize / 2f + 1f + (linked ? 0f : Mathf.absin(Time.time(), 4f, 1f)));

                    if((entity.power.links.size >= maxNodes || (link.block() instanceof PowerNode && link.entity.power.links.size >= ((PowerNode)link.block()).maxNodes)) && !linked){
                        Draw.color(Pal.breakInvalid);
                        Lines.lineAngleCenter(link.drawx(), link.drawy(), 45, link.block().size * Mathf.sqrt2 * tilesize * 0.9f);
                        Draw.color();
                    }
                }
            }
        }

        Draw.reset();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Lines.stroke(1f);
        Draw.color(Pal.placing);
        Lines.poly(x * tilesize + offset(), y * tilesize + offset(), 50, laserRange * tilesize);
        Draw.reset();
    }

    @Override
    public void drawLayer(Tile tile){
        if(!Core.settings.getBool("lasers")) return;

        TileEntity entity = tile.entity();

        for(int i = 0; i < entity.power.links.size; i++){
            Tile link = world.tile(entity.power.links.get(i));
            if(linkValid(tile, link)){
                drawLaser(tile, link);
            }
        }

        Draw.reset();
    }

    protected boolean linked(Tile tile, Tile other){
        return tile.entity.power.links.contains(other.pos());
    }

    protected boolean linkValid(Tile tile, Tile link){
        return linkValid(tile, link, true);
    }

    protected boolean linkValid(Tile tile, Tile link, boolean checkMaxNodes){
        if(tile == link || link == null || !link.block().hasPower || tile.getTeam() != link.getTeam()) return false;

        if(overlaps(tile, link, laserRange * tilesize) || (link.block() instanceof PowerNode && overlaps(link, tile, link.<PowerNode>cblock().laserRange * tilesize))){
            if(checkMaxNodes && link.block() instanceof PowerNode){
                return link.entity.power.links.size < link.<PowerNode>cblock().maxNodes || link.entity.power.links.contains(tile.pos());
            }
            return true;
        }
        return false;
    }

    protected boolean overlaps(Tile src, Tile other, float range){
        return Intersector.overlaps(Tmp.cr1.set(src.drawx(), src.drawy(), range), other.getHitbox(Tmp.r1));
    }

    protected void drawLaser(Tile tile, Tile target){

        float x1 = tile.drawx(), y1 = tile.drawy(),
        x2 = target.drawx(), y2 = target.drawy();

        float angle1 = Angles.angle(x1, y1, x2, y2);
        float angle2 = angle1 + 180f;

        t1.trns(angle1, tile.block().size * tilesize / 2f - 1.5f);
        t2.trns(angle2, target.block().size * tilesize / 2f - 1.5f);

        x1 += t1.x;
        y1 += t1.y;
        x2 += t2.x;
        y2 += t2.y;

        Draw.color(Pal.powerLight, Color.WHITE, Mathf.absin(Time.time(), 8f, 0.3f) + 0.2f);
        //Lines.stroke(2f);
        //Lines.line(x1, y1, x2, y2);

        Shapes.laser("laser", "laser-end", x1, y1, x2, y2, 0.6f);
    }

}
