package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

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

        Draw.alpha(entity.heat);
        Draw.rect(name + "-top", tile.drawx(), tile.drawy());
        Draw.color();

        if(entity.current != null) {
            TextureRegion region = Draw.region(entity.current.name);

            Shaders.build.region = region;
            Shaders.build.progress = entity.progress;
            Shaders.build.color.set(Palette.accent);
            Shaders.build.time = -entity.time / 10f;

            Graphics.shader(Shaders.build, false);
            Shaders.build.apply();
            Draw.rect(region, tile.drawx(), tile.drawy());
            Graphics.shader();

            Draw.color(Palette.accent);

            Lines.lineAngleCenter(
                    tile.drawx() + Mathf.sin(entity.time, 6f, Vars.tilesize / 3f * size),
                    tile.drawy(),
                    90,
                    size * Vars.tilesize /2f);

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
                Effects.effect(Fx.spawn, entity);
                entity.progress = 0;

                //TODO what now?
            }
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public void buildTable(Tile tile, Table table) {

        Table content = new Table();

        /*
        for(Upgrade upgrade : Upgrade.all()){
            if(!(upgrade instanceof Weapon)) continue;
            Weapon weapon = (Weapon)upgrade;

            ItemStack[] requirements = UpgradeRecipes.get(weapon);

            Table tiptable = new Table();

            Listenable run = ()->{
                tiptable.clearChildren();

                String description = weapon.description;

                tiptable.background("pane");
                tiptable.add("[orange]" + weapon.localized(), 0.5f).left().padBottom(2f);

                Table reqtable = new Table();

                tiptable.row();
                tiptable.add(reqtable).left();

                if(!control.upgrades().hasWeapon(weapon)){
                    for(ItemStack s : requirements){

                        int amount = Math.min(state.inventory.getAmount(s.item), s.amount);
                        reqtable.addImage(s.item.region).padRight(3).size(8*2);
                        reqtable.add(
                                (amount >= s.amount ? "" : "[RED]")
                                        + amount + " / " +s.amount, 0.5f).left();
                        reqtable.row();
                    }
                }

                tiptable.row();
                tiptable.add().size(4);
                tiptable.row();
                tiptable.add("[gray]" + description).left();
                tiptable.row();
                if(control.upgrades().hasWeapon(weapon)){
                    tiptable.add("$text.purchased").padTop(4).left();
                }
                tiptable.margin(8f);
            };

            run.listen();

            Tooltip<Table> tip = new Tooltip<>(tiptable, run);

            tip.setInstant(true);

            ImageButton button = content.addImageButton("white", 8*4, () -> {

                if(Net.client()){
                    NetEvents.handleUpgrade(weapon);
                }else{
                    state.inventory.removeItems(requirements);
                    control.upgrades().addWeapon(weapon);
                    run.listen();
                    Effects.sound("purchase");
                }
            }).size(49f, 54f).padBottom(-5).get();

            button.setDisabled(() -> control.upgrades().hasWeapon(weapon) || !state.inventory.hasItems(requirements));
            button.getStyle().imageUp = new TextureRegionDrawable(Draw.region(weapon.name));
            button.addListener(tip);

            if(++i % 3 == 0){
                content.row();
            }
        }*/

        table.add(content).padTop(140f);
    }

    @Override
    public TileEntity getEntity() {
        return new WeaponFactoryEntity();
    }

    public class WeaponFactoryEntity extends TileEntity{
        public Weapon current;
        public float progress;
        public float time;
        public float heat;
    }
}
