package mindustry.world.blocks.sandbox;

import arc.*;
import arc.struct.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.entities.traits.BuilderTrait.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.Cicon;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

public class LiquidSource extends Block{
    public static Liquid lastLiquid;

    public LiquidSource(String name){
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        liquidCapacity = 100f;
        configurable = true;
        outputsLiquid = true;
        entityType = LiquidSourceEntity::new;
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastLiquid != null){
            Core.app.post(() -> tile.configure(lastLiquid.id));
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.remove("liquid");
    }

    @Override
    public void update(Tile tile){
        LiquidSourceEntity entity = tile.ent();

        if(entity.source == null){
            tile.entity.liquids.clear();
        }else{
            tile.entity.liquids.add(entity.source, liquidCapacity);
            tryDumpLiquid(tile, entity.source);
        }
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, content.liquid(req.config), "center");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        LiquidSourceEntity entity = tile.ent();

        if(entity.source != null){
            Draw.color(entity.source.color);
            Draw.rect("center", tile.worldx(), tile.worldy());
            Draw.color();
        }
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        LiquidSourceEntity entity = tile.ent();

        Array<Liquid> items = content.liquids();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        Table cont = new Table();

        for(int i = 0; i < items.size; i++){
            final int f = i;
            ImageButton button = cont.addImageButton(Tex.clear, Styles.clearToggleTransi, 24, () -> control.input.frag.config.hideConfig()).size(38).group(group).get();
            button.changed(() -> {
                tile.configure(button.isChecked() ? items.get(f).id : -1);
                control.input.frag.config.hideConfig();
                lastLiquid = items.get(f);
            });
            button.getStyle().imageUp = new TextureRegionDrawable(items.get(i).icon(Cicon.medium));
            button.setChecked(entity.source == items.get(i));

            if(i % 4 == 3){
                cont.row();
            }
        }

        table.add(cont);
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<LiquidSourceEntity>ent().source = value == -1 ? null : content.liquid(value);
    }

    class LiquidSourceEntity extends TileEntity{
        public @Nullable Liquid source = null;

        @Override
        public int config(){
            return source == null ? -1 : source.id;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeByte(source == null ? -1 : source.id);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            byte id = stream.readByte();
            source = id == -1 ? null : content.liquid(id);
        }
    }
}
