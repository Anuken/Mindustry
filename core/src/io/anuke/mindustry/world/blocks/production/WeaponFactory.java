package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.type.Upgrade;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class WeaponFactory extends Block{

    public WeaponFactory(String name){
        super(name);
        solid = true;
        destructible = true;
        configurable = true;
        update = true;
    }

    @Override
    public void draw(Tile tile) {
        WeaponFactoryEntity entity = tile.entity();

        Draw.rect(name, tile.drawx(), tile.drawy());

        if(entity.current != null) {
            TextureRegion region = entity.current.equipRegion;

            Shaders.build.region = region;
            Shaders.build.progress = entity.progress;
            Shaders.build.time = -entity.time / 10f;
            Shaders.build.color.set(Palette.accent);

            Graphics.shader(Shaders.build, false);
            Shaders.build.apply();
            Draw.rect(region, tile.drawx(), tile.drawy());
            Graphics.shader();

            Draw.color(Palette.accent);

            Lines.lineAngleCenter(
                    tile.drawx() + Mathf.sin(entity.time, 6f, Vars.tilesize / 3f * size),
                    tile.drawy(),
                    90,
                    size * Vars.tilesize / 2f);

            Draw.reset();
        }
    }

    @Override
    public void update(Tile tile) {
        WeaponFactoryEntity entity = tile.entity();

        if(entity.current != null){
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.time += Timers.delta();
            entity.progress += 1f / Vars.respawnduration;

            if(entity.progress >= 1f){
                CallBlocks.onWeaponFactoryDone(tile, entity.current);
            }
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public void buildTable(Tile tile, Table table) {
        WeaponFactoryEntity entity = tile.entity();

        Table cont = new Table();

        //no result to show, build weapon selection menu
        if(entity.result == null) {
            //show weapon to select and build
            showSelect(tile, cont);
        }else{
            //show weapon to withdraw
            showResult(tile, cont);
        }

        table.add(cont);
    }

    protected void showSelect(Tile tile, Table cont){
        WeaponFactoryEntity entity = tile.entity();

        Array<Upgrade> items = Upgrade.all();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);

        int i = 0;

        for (Upgrade upgrade : items) {
            if (!(upgrade instanceof Weapon)) continue;
            Weapon weapon = (Weapon) upgrade;

            ImageButton button = cont.addImageButton("white", "toggle", 24, () -> CallBlocks.setWeaponFactoryWeapon(null, tile, weapon))
                    .size(38, 42).padBottom(-5.1f).group(group).get();
            button.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(weapon.region));
            button.setChecked(entity.current == weapon);

            if (i++ % 4 == 3) {
                cont.row();
            }
        }

        cont.update(() -> {
            //show result when done
            if(entity.result != null){
                cont.clear();
                cont.update(null);
                showResult(tile, cont);
            }
        });
    }

    protected void showResult(Tile tile, Table cont){
        WeaponFactoryEntity entity = tile.entity();

        Weapon weapon = entity.result;

        ImageButton button = cont.addImageButton("white", "toggle", 24, () -> CallBlocks.setWeaponFactoryWeapon(null, tile, weapon))
                .size(38, 42).padBottom(-5.1f).get();
        button.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(weapon.region));
        button.setChecked(entity.current == weapon);

        cont.update(() -> {
            //show selection menu when result disappears
            if(entity.result == null){
                cont.clear();
                cont.update(null);
                showSelect(tile, cont);
            }
        });
    }

    @Override
    public TileEntity getEntity() {
        return new WeaponFactoryEntity();
    }

    @Remote(targets = Loc.both, called = Loc.server, in = In.blocks, forward = true)
    public static void pickupWeaponFactoryWeapon(Player player, Tile tile){
        WeaponFactoryEntity entity = tile.entity();

        if(entity.current != null){
            player.upgrades.add(entity.current);
            entity.current = null;
            entity.progress = 0;
            entity.result = null;
        }
    }

    @Remote(targets = Loc.both, called = Loc.server, in = In.blocks, forward = true)
    public static void setWeaponFactoryWeapon(Player player, Tile tile, Weapon weapon){
        WeaponFactoryEntity entity = tile.entity();
        entity.current = weapon;
        entity.progress = 0f;
        entity.heat = 0f;
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void onWeaponFactoryDone(Tile tile, Weapon result){
        WeaponFactoryEntity entity = tile.entity();
        Effects.effect(Fx.spawn, entity);
        entity.current = null;
        entity.progress = 0;
        entity.result = result;
    }

    public class WeaponFactoryEntity extends TileEntity{
        public Weapon current;
        public float progress;
        public float time;
        public float heat;
        public Weapon result;

        @Override
        public void write(DataOutputStream stream) throws IOException {
            stream.writeByte(current == null ? -1 : current.id);
            stream.writeByte(result == null ? -1 : result.id);
            stream.writeFloat(progress);
            stream.writeFloat(time);
            stream.writeFloat(heat);
        }

        @Override
        public void read(DataInputStream stream) throws IOException {
            byte id = stream.readByte(), rid = stream.readByte();
            progress = stream.readFloat();
            time = stream.readFloat();
            heat = stream.readFloat();

            if(id != -1){
                current = Upgrade.getByID(id);
            }

            if(rid != -1){
                result = Upgrade.getByID(rid);
            }
        }
    }
}
