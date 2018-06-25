package io.anuke.mindustry.world.blocks.units;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitType;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.modules.InventoryModule;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UnitFactory extends Block {
    protected UnitType type;
    protected ItemStack[] requirements;
    protected float produceTime = 1000f;
    protected float powerUse = 0.1f;
    protected float openDuration = 50f;
    protected float launchVelocity = 0f;
    protected String unitRegion;

    public UnitFactory(String name) {
        super(name);
        update = true;
        hasPower = true;
        hasItems = true;
        solidifes = true;
    }

    @Override
    public boolean isSolidFor(Tile tile) {
        UnitFactoryEntity entity = tile.entity();
        return type.isFlying || !entity.open;
    }

    @Override
    public void setBars() {
        super.setBars();

        bars.add(new BlockBar(BarType.production, true, tile -> tile.<UnitFactoryEntity>entity().buildTime / produceTime));
        bars.remove(BarType.inventory);
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{
            Draw.region(name),
            Draw.region(name + "-top")
        };
    }

    @Override
    public void draw(Tile tile) {
        UnitFactoryEntity entity = tile.entity();
        TextureRegion region = Draw.region(unitRegion == null ? type.name : unitRegion);

        Draw.rect(name(), tile.drawx(), tile.drawy());

        Shaders.build.region = region;
        Shaders.build.progress = entity.buildTime/produceTime;
        Shaders.build.color.set(Palette.accent);
        Shaders.build.color.a = entity.speedScl;
        Shaders.build.time = -entity.time / 10f;

        Graphics.shader(Shaders.build, false);
        Shaders.build.apply();
        Draw.rect(region, tile.drawx(), tile.drawy());
        Graphics.shader();

        Draw.color(Palette.accent);
        Draw.alpha(entity.speedScl);

        Lines.lineAngleCenter(
                tile.drawx() + Mathf.sin(entity.time, 6f, Vars.tilesize/2f*size - 2f),
                tile.drawy(),
                90,
                size * Vars.tilesize - 4f);

        Draw.reset();

        Draw.rect(name + (entity.open ? "-top-open" : "-top"), tile.drawx(), tile.drawy());
    }

    @Override
    public void update(Tile tile) {
        UnitFactoryEntity entity = tile.entity();

        float used = Math.min(powerUse * Timers.delta(), powerCapacity);

        entity.time += Timers.delta() * entity.speedScl;

        if(entity.openCountdown > 0){
            if(entity.openCountdown > Timers.delta()){
                entity.openCountdown -= Timers.delta();
            }else{
                if(type.isFlying || !Units.anyEntities(tile)) {
                    entity.open = false;
                    entity.openCountdown = -1;
                }else{
                    entity.speedScl = Mathf.lerpDelta(entity.speedScl, 0f, 0.1f);
                }
            }
        }

        if(!entity.hasSpawned && hasRequirements(entity.items, entity.buildTime/produceTime) &&
                entity.power.amount >= used && !entity.open){

            entity.buildTime += Timers.delta();
            entity.power.amount -= used;
            entity.speedScl = Mathf.lerpDelta(entity.speedScl, 1f, 0.05f);
        }else{
            if(!entity.open) entity.speedScl = Mathf.lerpDelta(entity.speedScl, 0f, 0.05f);
        }

        if(entity.buildTime >= produceTime && !entity.open){
            entity.open = true;

            Timers.run(openDuration / 1.5f, () -> CallBlocks.onUnitFactorySpawn(tile));

            entity.openCountdown = openDuration;

            for(ItemStack stack : requirements){
                entity.items.removeItem(stack.item, stack.amount);
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source) {
        for(ItemStack stack : requirements){
            if(item == stack.item && tile.entity.items.getItem(item) <= stack.amount*2){
                return true;
            }
        }
        return false;
    }

    @Override
    public TileEntity getEntity() {
        return new UnitFactoryEntity();
    }

    protected boolean hasRequirements(InventoryModule inv, float fraction){
        for(ItemStack stack : requirements){
            if(!inv.hasItem(stack.item, (int)(fraction * stack.amount))){
                return false;
            }
        }
        return true;
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void onUnitFactorySpawn(Tile tile){
        UnitFactoryEntity entity = tile.entity();
        UnitFactory factory = (UnitFactory)tile.block();

        entity.buildTime = 0f;
        entity.hasSpawned = true;

        Effects.shake(2f, 3f, entity);
        Effects.effect(BlockFx.producesmoke, tile.drawx(), tile.drawy());

        if(!Net.client()) {
            BaseUnit unit = factory.type.create(tile.getTeam());
            unit.setSpawner(entity);
            unit.set(tile.drawx(), tile.drawy());
            unit.add();
            unit.getVelocity().y = factory.launchVelocity;
        }
    }

    public static class UnitFactoryEntity extends TileEntity{
        public float buildTime;
        public boolean open;
        public float openCountdown;
        public float time;
        public float speedScl;
        public boolean hasSpawned;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeFloat(buildTime);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            buildTime = stream.readFloat();
        }
    }
}
