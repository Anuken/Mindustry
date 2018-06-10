package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.world.Edges;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.graphics.Shapes;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;
import io.anuke.ucore.util.Translator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class PowerDistributor extends PowerBlock{
	public static final float thicknessScl = 0.7f;
    public static final float flashScl = 0.12f;

	//last distribution block placed
	private static int lastPlaced = -1;

	protected Translator t1 = new Translator();
	protected Translator t2 = new Translator();

	protected float laserRange = 6;
	protected float powerSpeed = 0.5f;
	protected int maxNodes = 3;

	public PowerDistributor(String name){
		super(name);
		expanded = true;
		layer = Layer.power;
		hasItems = false;
		powerCapacity = 5f;
		configurable = true;
	}

	@Override
	public void setBars(){}

	@Override
	public void placed(Tile tile) {
		Tile before = world.tile(lastPlaced);
		if(linkValid(tile, before) && before.block() instanceof PowerDistributor){
			CallBlocks.linkPowerDistributors(null, tile, before);
		}

		lastPlaced = tile.packedPosition();
	}

	@Override
	public void setStats(){
		super.setStats();

		stats.add("lasertilerange", laserRange);
		stats.add("maxpowertransfersecond", Strings.toFixed(powerSpeed * 60, 2));
	}

	@Override
	public void update(Tile tile){
        distributeLaserPower(tile);
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
		DistributorEntity entity = tile.entity();
		other = other.target();

		if(linkValid(tile, other)){
			if(linked(tile, other)){
				CallBlocks.unlinkPowerDistributors(null, tile, other);
			}else if(entity.links.size < maxNodes){
				CallBlocks.linkPowerDistributors(null, tile, other);
			}
			return false;
		}
		return true;
	}

	@Override
	public void drawSelect(Tile tile){
		super.drawSelect(tile);

        Draw.color(Palette.power);
        Lines.stroke(1f);

        Lines.poly(Edges.getPixelPolygon(laserRange), tile.worldx() - tilesize/2, tile.worldy() - tilesize/2, tilesize);

        Draw.reset();
	}

	@Override
	public void drawConfigure(Tile tile){
		DistributorEntity entity = tile.entity();

		Draw.color(Palette.accent);

		Lines.stroke(1f);
		Lines.square(tile.drawx(), tile.drawy(),
				tile.block().size * tilesize / 2f + 1f + Mathf.absin(Timers.time(), 4f, 1f));

		Lines.stroke(1f);

		Lines.poly(Edges.getPixelPolygon(laserRange), tile.worldx() - tilesize/2, tile.worldy() - tilesize/2, tilesize);

		Draw.color(Palette.power);

		for(int x = (int)(tile.x - laserRange); x <= tile.x + laserRange; x ++){
			for(int y = (int)(tile.y - laserRange); y <= tile.y + laserRange; y ++){
				Tile link = world.tile(x, y);
				if(link != null) link = link.target();

				if(link != tile && linkValid(tile, link)){
					boolean linked = linked(tile, link);
					Draw.color(linked ? Palette.place : Palette.breakInvalid);

					Lines.square(link.drawx(), link.drawy(),
							link.block().size * tilesize / 2f + 1f + (linked ? 0f : Mathf.absin(Timers.time(), 4f, 1f)));

					if(entity.links.size >= maxNodes && !linked){
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
        Draw.color(Palette.place);
        Lines.stroke(1f);

        Lines.poly(Edges.getPixelPolygon(laserRange), x * tilesize - tilesize/2, y * tilesize - tilesize/2, tilesize);

        Draw.reset();
	}

	@Override
	public void drawLayer(Tile tile){
		if(!Settings.getBool("lasers")) return;

		DistributorEntity entity = tile.entity();

		entity.laserColor = Mathf.lerpDelta(entity.laserColor, Mathf.clamp(entity.powerRecieved/(powerSpeed)), 0.08f);

		Draw.color(Palette.powerLaserFrom, Palette.powerLaserTo, entity.laserColor * (1f-flashScl) + Mathf.sin(Timers.time(), 1.7f, flashScl));

		for(int i = 0; i < entity.links.size; i ++){
			Tile link = world.tile(entity.links.get(i));
		    if(linkValid(tile, link)) drawLaser(tile, link);
        }

		Draw.color();
	}

	@Override
	public float addPower(Tile tile, float amount){
		DistributorEntity entity = tile.entity();

		if(entity.lastRecieved != threads.getFrameID()){
			entity.lastRecieved = threads.getFrameID();
			entity.powerRecieved = 0f;
		}

		float added = super.addPower(tile, amount);
		entity.powerRecieved += added;
		return added;
	}

	protected boolean shouldDistribute(Tile tile, Tile other) {
		return !(other.block() instanceof PowerDistributor)
				|| other.entity.power.amount / other.block().powerCapacity < tile.entity.power.amount / powerCapacity;
	}

	protected void distributeLaserPower(Tile tile){
		DistributorEntity entity = tile.entity();

		int targets = 0;

		//validate everything first.
		for(int i = 0; i < entity.links.size; i ++){
			Tile target = world.tile(entity.links.get(i));
			if(!linkValid(tile, target)) {
				entity.links.removeIndex(i);
				i --;
			}else if(shouldDistribute(tile, target)){
				targets ++;
			}
		}

		float result = Math.min(entity.power.amount / targets, powerSpeed * Timers.delta());

		for(int i = 0; i < entity.links.size; i ++){
			Tile target = world.tile(entity.links.get(i));
			if(!shouldDistribute(tile, target)) continue;

			float transmit = Math.min(result * Timers.delta(), entity.power.amount);
			if(target.block().acceptPower(target, tile, transmit)){
				entity.power.amount -= target.block().addPower(target, transmit);
			}
		}
	}
/*
	protected void link(Tile tile, Tile other){
		DistributorEntity entity = tile.entity();

		if(!entity.links.contains(other.packedPosition())){
			entity.links.add(other.packedPosition());
		}

		if(other.block() instanceof PowerDistributor){
			DistributorEntity oe = other.entity();

			if(!oe.links.contains(tile.packedPosition()) ){
				oe.links.add(tile.packedPosition());
			}
		}
	}

	protected void unlink(Tile tile, Tile other){
		DistributorEntity entity = tile.entity();

		entity.links.removeValue(other.packedPosition());

		if(other.block() instanceof PowerDistributor){
			DistributorEntity oe = other.entity();

			oe.links.removeValue(tile.packedPosition());
		}
	}*/

	protected boolean linked(Tile tile, Tile other){
		return tile.<DistributorEntity>entity().links.contains(other.packedPosition());
	}

	protected boolean linkValid(Tile tile, Tile link){
		if(!(tile != link && link != null && link.block().hasPower)) return false;

		if(link.block() instanceof PowerDistributor){
			DistributorEntity oe = link.entity();

			return Vector2.dst(tile.drawx(), tile.drawy(), link.drawx(), link.drawy()) <= Math.max(laserRange * tilesize,
					((PowerDistributor)link.block()).laserRange * tilesize) - tilesize/2f
					+ (link.block().size-1)*tilesize/2f + (tile.block().size-1)*tilesize/2f &&
					(oe.links.size < ((PowerDistributor)link.block()).maxNodes || oe.links.contains(tile.packedPosition()));
		}else{
			return Vector2.dst(tile.drawx(), tile.drawy(), link.drawx(), link.drawy())
					<= laserRange * tilesize - tilesize/2f + (link.block().size-1)*tilesize;
		}
	}

	protected void drawLaser(Tile tile, Tile target){
        float x1 = tile.drawx(), y1 = tile.drawy(),
                x2 = target.drawx(), y2 = target.drawy();

        float angle1 = Angles.angle(x1, y1, x2, y2);
        float angle2 = angle1 + 180f;

        t1.trns(angle1, tile.block().size * tilesize/2f + 1f);
        t2.trns(angle2, target.block().size * tilesize/2f + 1f);

        Shapes.laser("laser", "laser-end", x1 + t1.x, y1 + t1.y,
                x2 + t2.x, y2 + t2.y, thicknessScl);
	}

    @Override
    public TileEntity getEntity() {
        return new DistributorEntity();
    }

    @Remote(targets = Loc.both, called = Loc.both, in = In.blocks, forward = true)
    public static void linkPowerDistributors(Player player, Tile tile, Tile other){
		DistributorEntity entity = tile.entity();

		if(!entity.links.contains(other.packedPosition())){
			entity.links.add(other.packedPosition());
		}

		if(other.block() instanceof PowerDistributor){
			DistributorEntity oe = other.entity();

			if(!oe.links.contains(tile.packedPosition()) ){
				oe.links.add(tile.packedPosition());
			}
		}
	}

	@Remote(targets = Loc.both, called = Loc.server, in = In.blocks, forward = true)
	public static void unlinkPowerDistributors(Player player, Tile tile, Tile other){
		DistributorEntity entity = tile.entity();

		entity.links.removeValue(other.packedPosition());

		if(other.block() instanceof PowerDistributor){
			DistributorEntity oe = other.entity();

			oe.links.removeValue(tile.packedPosition());
		}
	}

    public static class DistributorEntity extends TileEntity{
        public float laserColor = 0f;
        public float powerRecieved = 0f;
        public long lastRecieved = 0;
        public IntArray links = new IntArray();

		@Override
		public void write(DataOutputStream stream) throws IOException {
			stream.writeShort(links.size);
			for(int i = 0; i < links.size; i ++){
				stream.writeInt(links.get(i));
			}
		}

		@Override
		public void read(DataInputStream stream) throws IOException {
			short amount = stream.readShort();
			for(int i = 0; i < amount; i ++){
				links.add(stream.readInt());
			}
		}
	}

}
