package io.anuke.mindustry.editor;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Pixmap.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.ImageButton.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.async.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.filters.*;
import io.anuke.mindustry.maps.filters.GenerateFilter.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import static io.anuke.mindustry.Vars.*;

@SuppressWarnings("unchecked")
public class MapGenerateDialog extends FloatingDialog{
    private final Supplier<GenerateFilter>[] filterTypes = new Supplier[]{
        NoiseFilter::new, ScatterFilter::new, TerrainFilter::new, DistortFilter::new,
        RiverNoiseFilter::new, OreFilter::new, OreMedianFilter::new, MedianFilter::new,
        BlendFilter::new, MirrorFilter::new, ClearFilter::new
    };
    private final MapEditor editor;
    private final boolean applied;

    private Pixmap pixmap;
    private Texture texture;
    private GenerateInput input = new GenerateInput();
    private Array<GenerateFilter> filters = new Array<>();
    private int scaling = mobile ? 3 : 1;
    private Table filterTable;

    private AsyncExecutor executor = new AsyncExecutor(1);
    private AsyncResult<Void> result;
    private boolean generating;
    private GenTile returnTile = new GenTile();

    private GenTile[][] buffer1, buffer2;
    private Consumer<Array<GenerateFilter>> applier;
    private CachedTile ctile = new CachedTile(){
        //nothing.
        @Override
        protected void changed(){

        }
    };

    /** @param applied whether or not to use the applied in-game mode. */
    public MapGenerateDialog(MapEditor editor, boolean applied){
        super("$editor.generate");
        this.editor = editor;
        this.applied = applied;

        shown(this::setup);
        addCloseButton();
        if(applied){
            buttons.addButton("$editor.apply", () -> {
                ui.loadAnd(() -> {
                    apply();
                    hide();
                });
            }).size(160f, 64f);
        }else{
            buttons.addButton("$settings.reset", () -> {
                filters.set(maps.readFilters(""));
                rebuildFilters();
                update();
            }).size(160f, 64f);
        }
        buttons.addButton("$editor.randomize", () -> {
            for(GenerateFilter filter : filters){
                filter.randomize();
            }
            update();
        }).size(160f, 64f);

        buttons.addImageTextButton("$add", Icon.add, this::showAdd).height(64f).width(140f);

        if(!applied){
            hidden(this::apply);
        }

        onResize(this::rebuildFilters);
    }

    public void show(Array<GenerateFilter> filters, Consumer<Array<GenerateFilter>> applier){
        this.filters = filters;
        this.applier = applier;
        show();
    }

    public void show(Consumer<Array<GenerateFilter>> applier){
        show(this.filters, applier);
    }

    /** Applies the specified filters to the editor. */
    public void applyToEditor(Array<GenerateFilter> filters){
        //writeback buffer
        GenTile[][] writeTiles = new GenTile[editor.width()][editor.height()];

        for(int x = 0; x < editor.width(); x++){
            for(int y = 0; y < editor.height(); y++){
                writeTiles[x][y] = new GenTile();
            }
        }

        for(GenerateFilter filter : filters){
            input.begin(filter, editor.width(), editor.height(), editor::tile);
            //write to buffer
            for(int x = 0; x < editor.width(); x++){
                for(int y = 0; y < editor.height(); y++){
                    Tile tile = editor.tile(x, y);
                    input.apply(x, y, tile.floor(), tile.block(), tile.overlay());
                    filter.apply(input);
                    writeTiles[x][y].set(input.floor, input.block, input.ore, tile.getTeam(), tile.rotation());
                }
            }

            editor.load(() -> {
                //read from buffer back into tiles
                for(int x = 0; x < editor.width(); x++){
                    for(int y = 0; y < editor.height(); y++){
                        Tile tile = editor.tile(x, y);
                        GenTile write = writeTiles[x][y];

                        tile.rotation(write.rotation);
                        tile.setFloor((Floor)content.block(write.floor));
                        tile.setBlock(content.block(write.block));
                        tile.setTeam(Team.all[write.team]);
                        tile.setOverlay(content.block(write.ore));
                    }
                }
            });
        }

        //reset undo stack as generation... messes things up
        editor.load(editor::checkLinkedTiles);
        editor.renderer().updateAll();
        editor.clearOp();
    }

    void setup(){
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
            pixmap = null;
            texture = null;
        }

        pixmap = new Pixmap(editor.width() / scaling, editor.height() / scaling, Format.RGBA8888);
        texture = new Texture(pixmap);

        cont.clear();
        cont.table(t -> {
            t.margin(8f);
            t.stack(new BorderImage(texture){
                {
                    setScaling(Scaling.fit);
                }

                @Override
                public void draw(){
                    super.draw();
                    for(GenerateFilter filter : filters){
                        filter.draw(this);
                    }
                }
            }, new Stack(){{
                add(new Image(Styles.black8));
                add(new Image(Icon.refresh, Scaling.none));
                visible(() -> generating && !updateEditorOnChange);
            }}).grow().padRight(10);
            t.pane(p -> filterTable = p.marginRight(6)).update(pane -> {
                if(Core.scene.getKeyboardFocus() instanceof Dialog && Core.scene.getKeyboardFocus() != this){
                    return;
                }

                Vector2 v = pane.stageToLocalCoordinates(Core.input.mouse());

                if(v.x >= 0 && v.y >= 0 && v.x <= pane.getWidth() && v.y <= pane.getHeight()){
                    Core.scene.setScrollFocus(pane);
                }else{
                    Core.scene.setScrollFocus(null);
                }
            }).grow().get().setScrollingDisabled(true, false);
        }).grow();

        buffer1 = create();
        buffer2 = create();

        update();
        rebuildFilters();
    }

    GenTile[][] create(){
        GenTile[][] out = new GenTile[editor.width() / scaling][editor.height() / scaling];

        for(int x = 0; x < out.length; x++){
            for(int y = 0; y < out[0].length; y++){
                out[x][y] = new GenTile();
            }
        }
        return out;
    }

    void rebuildFilters(){
        int cols = Math.max((int)(Math.max(filterTable.getParent().getWidth(), Core.graphics.getWidth()/2f * 0.9f) / Scl.scl(290f)), 1);
        filterTable.clearChildren();
        filterTable.top().left();
        int i = 0;

        for(GenerateFilter filter : filters){

            //main container
            filterTable.table(Tex.button, c -> {
                //icons to perform actions
                c.table(t -> {
                    t.top();
                    t.add(filter.name()).padTop(5).color(Pal.accent).growX().left();

                    t.row();

                    t.table(b -> {
                        ImageButtonStyle style = Styles.cleari;
                        b.defaults().size(50f);
                        b.addImageButton(Icon.refreshSmall, style, () -> {
                            filter.randomize();
                            update();
                        });

                        b.addImageButton(Icon.arrowUpSmall, style, () -> {
                            int idx = filters.indexOf(filter);
                            filters.swap(idx, Math.max(0, idx - 1));
                            rebuildFilters();
                            update();
                        });
                        b.addImageButton(Icon.arrowDownSmall, style, () -> {
                            int idx = filters.indexOf(filter);
                            filters.swap(idx, Math.min(filters.size - 1, idx + 1));
                            rebuildFilters();
                            update();
                        });
                        b.addImageButton(Icon.trashSmall, style, () -> {
                            filters.remove(filter);
                            rebuildFilters();
                            update();
                        });
                    });
                }).fillX();
                c.row();
                //all the options
                c.table(f -> {
                    f.left().top();
                    for(FilterOption option : filter.options){
                        option.changed = this::update;

                        f.table(t -> {
                            t.left();
                            option.build(t);
                        }).growX().left();
                        f.row();
                    }
                }).grow().left().pad(2).top();
            }).width(280f).pad(3).top().left().fillY();
            if(++i % cols == 0){
                filterTable.row();
            }
        }

        if(filters.isEmpty()){
            filterTable.add("$filters.empty").wrap().width(200f);
        }
    }

    void showAdd(){
        FloatingDialog selection = new FloatingDialog("$add");
        selection.setFillParent(false);
        selection.cont.defaults().size(210f, 60f);
        int i = 0;
        for(Supplier<GenerateFilter> gen : filterTypes){
            GenerateFilter filter = gen.get();

            if(!applied && filter.buffered) continue;

            selection.cont.addButton(filter.name(), () -> {
                filters.add(filter);
                rebuildFilters();
                update();
                selection.hide();
            });
            if(++i % 2 == 0) selection.cont.row();
        }

        selection.cont.addButton("$filter.defaultores", () -> {
            maps.addDefaultOres(filters);
            rebuildFilters();
            update();
            selection.hide();
        });

        selection.addCloseButton();
        selection.show();
    }

    GenTile dset(Tile tile){
        returnTile.set(tile);
        return returnTile;
    }

    void apply(){
        if(result != null){
            result.get();
        }

        buffer1 = null;
        buffer2 = null;
        generating = false;
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
            pixmap = null;
            texture = null;
        }

        applier.accept(filters);
    }

    void update(){

        if(generating){
            return;
        }

        Array<GenerateFilter> copy = new Array<>(filters);

        result = executor.submit(() -> {
            try{
                generating = true;

                if(!filters.isEmpty()){
                    //write to buffer1 for reading
                    for(int px = 0; px < pixmap.getWidth(); px++){
                        for(int py = 0; py < pixmap.getHeight(); py++){
                            buffer1[px][py].set(editor.tile(px * scaling, py * scaling));
                        }
                    }
                }

                for(GenerateFilter filter : copy){
                    input.begin(filter, editor.width(), editor.height(), (x, y) -> buffer1[Mathf.clamp(x / scaling, 0, pixmap.getWidth()-1)][Mathf.clamp(y / scaling, 0, pixmap.getHeight()-1)].tile());
                    //read from buffer1 and write to buffer2
                    for(int px = 0; px < pixmap.getWidth(); px++){
                        for(int py = 0; py < pixmap.getHeight(); py++){
                            int x = px * scaling, y = py * scaling;
                            GenTile tile = buffer1[px][py];
                            input.apply(x, y, content.block(tile.floor), content.block(tile.block), content.block(tile.ore));
                            filter.apply(input);
                            buffer2[px][py].set(input.floor, input.block, input.ore, Team.all[tile.team], tile.rotation);
                        }
                    }
                    for(int px = 0; px < pixmap.getWidth(); px++){
                        for(int py = 0; py < pixmap.getHeight(); py++){
                            buffer1[px][py].set(buffer2[px][py]);
                        }
                    }
                }

                for(int px = 0; px < pixmap.getWidth(); px++){
                    for(int py = 0; py < pixmap.getHeight(); py++){
                        int color;
                        //get result from buffer1 if there's filters left, otherwise get from editor directly
                        if(filters.isEmpty()){
                            Tile tile = editor.tile(px * scaling, py * scaling);
                            color = MapIO.colorFor(tile.floor(), tile.block(), tile.overlay(), Team.derelict);
                        }else{
                            GenTile tile = buffer1[px][py];
                            color = MapIO.colorFor(content.block(tile.floor), content.block(tile.block), content.block(tile.ore), Team.derelict);
                        }
                        pixmap.draw(px, pixmap.getHeight() - 1 - py, color);
                    }
                }

                Core.app.post(() -> {
                    if(pixmap == null || texture == null){
                        return;
                    }
                    texture.draw(pixmap, 0, 0);
                    generating = false;
                });
            }catch(Exception e){
                generating = false;
                e.printStackTrace();
            }
            return null;
        });
    }

    private class GenTile{
        public byte team, rotation;
        public short block, floor, ore;

        public void set(Block floor, Block wall, Block ore, Team team, int rotation){
            this.floor = floor.id;
            this.block = wall.id;
            this.ore = ore.id;
            this.team = (byte)team.ordinal();
            this.rotation = (byte)rotation;
        }

        public void set(GenTile other){
            this.floor = other.floor;
            this.block = other.block;
            this.ore = other.ore;
            this.team = other.team;
            this.rotation = other.rotation;
        }

        public GenTile set(Tile other){
            set(other.floor(), other.block(), other.overlay(), other.getTeam(), other.rotation());
            return this;
        }

        Tile tile(){
            ctile.setFloor((Floor)content.block(floor));
            ctile.setBlock(content.block(block));
            ctile.setOverlay(content.block(ore));
            ctile.rotation(rotation);
            ctile.setTeam(Team.all[team]);
            return ctile;
        }
    }
}
