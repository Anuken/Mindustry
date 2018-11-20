package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.math.Vector2;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import static io.anuke.mindustry.Vars.*;

public class PowerNode extends PowerBlock{
    public static final float thicknessScl = 0.7f;
    public static final float flashScl = 0.12f;

    //last distribution block placed
    private static int lastPlaced = -1;

    protected Translator t1 = new Translator();
    protected Translator t2 = new Translator();

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
        if(tile.entity.power == null || !((PowerNode)tile.block()).linkValid(tile, other)) return;

        TileEntity entity = tile.entity();

        if(!entity.power.links.contains(other.packedPosition())){
            entity.power.links.add(other.packedPosition());
        }

        if(other.getTeamID() == tile.getTeamID()){

            if(!other.entity.power.links.contains(tile.packedPosition())){
                other.entity.power.links.add(tile.packedPosition());
            }
        }

        entity.power.graph.add(other.entity.power.graph);
    }

    @Remote(targets = Loc.both, called = Loc.server, forward = true)
    public static void unlinkPowerNodes(Player player, Tile tile, Tile other){
        if(tile.entity.power == null) return;

        TileEntity entity = tile.entity();

        //clear all graph data first
        PowerGraph tg = entity.power.graph;
        tg.clear();

        entity.power.links.removeValue(other.packedPosition());
        other.entity.power.links.removeValue(tile.packedPosition());

        //reflow from this point, covering all tiles on this side
        tg.reflow(tile);

        if(other.entity.power.graph != tg){
            //create new graph for other end
            PowerGraph og = new PowerGraph();
            //reflow from other end
            og.reflow(other);
        }
    }

    @Override
    public void setBars(){
    }

    @Override
    public void playerPlaced(Tile tile){
        Tile before = world.tile(lastPlaced);
        if(linkValid(tile, before) && before.block() instanceof PowerNode){
            for(Tile near : before.entity.proximity()){
                if(near.target() == tile){
                    lastPlaced = tile.packedPosition();
                    return;
                }
            }
            Call.linkPowerNodes(null, tile, before);
        }

        lastPlaced = tile.packedPosition();
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
        other = other.target();

        Tile result = other;

        if(linkValid(tile, other)){
            if(linked(tile, other)){
                threads.run(() -> Call.unlinkPowerNodes(null, tile, result));
            }else if(entity.power.links.size < maxNodes){
                threads.run(() -> Call.linkPowerNodes(null, tile, result));
            }
            return false;
        }
        return true;
    }

    @Override
    public void drawSelect(Tile tile){
        super.drawSelect(tile);

        Lines.stroke(1f);

        Draw.color(Palette.accent);
        Lines.poly(tile.drawx(), tile.drawy(), 50, laserRange*tilesize);
        Draw.reset();
    }

    @Override
    public void drawConfigure(Tile tile){
        TileEntity entity = tile.entity();

        Draw.color(Palette.accent);

        Lines.stroke(1f);
        Lines.circle(tile.drawx(), tile.drawy(),
                tile.block().size * tilesize / 2f + 1f + Mathf.absin(Timers.time(), 4f, 1f));

        Lines.poly(tile.drawx(), tile.drawy(), 50, laserRange*tilesize);

        for(int x = (int) (tile.x - laserRange); x <= tile.x + laserRange; x++){
            for(int y = (int) (tile.y - laserRange); y <= tile.y + laserRange; y++){
                Tile link = world.tile(x, y);
                if(link != null) link = link.target();

                if(link != tile && linkValid(tile, link, false)){
                    boolean linked = linked(tile, link);
                    Draw.color(linked ? Palette.place : Palette.breakInvalid);

                    Lines.circle(link.drawx(), link.drawy(),
                            link.block().size * tilesize / 2f + 1f + (linked ? 0f : Mathf.absin(Timers.time(), 4f, 1f)));

                    if((entity.power.links.size >= maxNodes || (link.block() instanceof PowerNode && link.entity.power.links.size >= ((PowerNode) link.block()).maxNodes)) && !linked){
                        Draw.color();
                        Draw.rect("cross-" + link.block().size, link.drawx(), link.drawy());
                    }
                }
            }
        }

        Draw.reset();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Lines.stroke(1f);
        Draw.color(Palette.placing);
        Lines.poly(x * tilesize + offset(), y * tilesize + offset(), 50, laserRange*tilesize);
        Draw.reset();
    }

    @Override
    public void drawLayer(Tile tile){
        if(!Settings.getBool("lasers")) return;

        TileEntity entity = tile.entity();

        for(int i = 0; i < entity.power.links.size; i++){
            Tile link = world.tile(entity.power.links.get(i));
            if(linkValid(tile, link) && (!(link.block() instanceof PowerNode)
                || ((tile.block().size > link.block().size) || (tile.block().size == link.block().size && tile.id() < link.id())))){
                drawLaser(tile, link);
            }
        }

        Draw.reset();
    }

    protected boolean linked(Tile tile, Tile other){
        return tile.entity.power.links.contains(other.packedPosition());
    }

    protected boolean linkValid(Tile tile, Tile link){
        return linkValid(tile, link, true);
    }

    protected boolean linkValid(Tile tile, Tile link, boolean checkMaxNodes){
        if(!(tile != link && link != null && link.block().hasPower) || tile.getTeamID() != link.getTeamID()) return false;

        if(link.block() instanceof PowerNode){
            TileEntity oe = link.entity();

            return Vector2.dst(tile.drawx(), tile.drawy(), link.drawx(), link.drawy()) <= Math.max(laserRange * tilesize,
                    ((PowerNode) link.block()).laserRange * tilesize)
                    + (link.block().size - 1) * tilesize / 2f + (tile.block().size - 1) * tilesize / 2f &&
                    (!checkMaxNodes || (oe.power.links.size < ((PowerNode) link.block()).maxNodes || oe.power.links.contains(tile.packedPosition())));
        }else{
            return Vector2.dst(tile.drawx(), tile.drawy(), link.drawx(), link.drawy())
                    <= laserRange * tilesize + (link.block().size - 1) * tilesize;
        }
    }

    protected void drawLaser(Tile tile, Tile target){
        float x1 = tile.drawx(), y1 = tile.drawy(),
                x2 = target.drawx(), y2 = target.drawy();

        float angle1 = Angles.angle(x1, y1, x2, y2);
        float angle2 = angle1 + 180f;

        t1.trns(angle1, tile.block().size * tilesize / 2f - 1f);
        t2.trns(angle2, target.block().size * tilesize / 2f - 1f);

        x1 += t1.x;
        y1 += t1.y;
        x2 += t2.x;
        y2 += t2.y;

        float space = Vector2.dst(x1, y1, x2, y2);
        float scl = 4f, mag = 2f, tscl = 4f, segscl = 3f;

        int segments = Mathf.ceil(space / segscl);

        Draw.color(Palette.power, Palette.powerLight, Mathf.absin(Timers.time(), 5f, 1f));
        Lines.stroke(1f);

        for(int i = 0; i < segments; i++){
            float f1 = (float)i / segments;
            float f2 = (float)(i+1) / segments;
            t1.trns(angle1 + 90f, Mathf.lerp(Mathf.sin(tile.entity.id * 124f + Timers.time()/tscl + f1 * space, scl, mag), 0f, Math.abs(f1 - 0.5f)*2f));
            t2.trns(angle1 + 90f, Mathf.lerp(Mathf.sin(tile.entity.id * 124f + Timers.time()/tscl + f2 * space, scl, mag), 0f, Math.abs(f2 - 0.5f)*2f));

            Lines.line(x1 + (x2 - x1) * f1 + t1.x, y1 + (y2 - y1) * f1 + t1.y,
                        x1 + (x2 - x1) * f2 + t2.x, y1 + (y2 - y1) * f2 + t2.y);
        }
    }

}
