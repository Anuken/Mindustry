package mindustry.editor;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import java.util.concurrent.*;

import static mindustry.Vars.*;

@SuppressWarnings("unchecked")
public class MapGenerateDialog extends BaseDialog{
    final boolean applied;

    Pixmap pixmap;
    Texture texture;
    GenerateInput input = new GenerateInput();
    Seq<GenerateFilter> filters = new Seq<>();
    int scaling = mobile ? 3 : 1;
    Table filterTable;
    BorderImage preview;

    Future<?> result;
    boolean generating;
    boolean updateQueued;
    MirrorFilter dragging;
    Rect dragRect = new Rect();
    Vec2 dragAxis = new Vec2();
    long lastDrag;

    long[] buffer1, buffer2;
    Cons<Seq<GenerateFilter>> applier;
    CachedTile ctile = new CachedTile(){
        //nothing.
        @Override
        protected void changeBuild(Team team, Prov<Building> entityprov, int rotation){

        }

        @Override
        public void setBlock(Block type, Team team, int rotation, Prov<Building> entityprov){
            this.block = type;
        }
    };

    /** @param applied whether or not to use the applied in-game mode. */
    public MapGenerateDialog(boolean applied){
        super("@editor.generate");
        this.applied = applied;

        shown(this::setup);
        addCloseListener();

        var style = Styles.flatt;

        buttons.defaults().size(180f, 64f).pad(2f);
        buttons.button("@back", Icon.left, this::hide);

        if(applied){
            buttons.button("@editor.apply", Icon.ok, () -> {
                ui.loadAnd(() -> {
                    apply();
                    hide();
                });
            });
        }

        buttons.button("@editor.randomize", Icon.refresh, () -> {
            for(GenerateFilter filter : filters){
                filter.randomize();
            }
            update();
        });

        buttons.button("@edit", Icon.edit, () -> {
            BaseDialog dialog = new BaseDialog("@editor.export");
            dialog.cont.pane(p -> {
                p.margin(10f);
                p.table(Tex.button, in -> {
                    in.defaults().size(280f, 60f).left();

                    in.button("@waves.copy", Icon.copy, style, () -> {
                        dialog.hide();

                        Core.app.setClipboardText(JsonIO.write(filters));
                    }).marginLeft(12f).row();
                    in.button("@waves.load", Icon.download, style, () -> {
                        dialog.hide();
                        try{
                            filters.set(JsonIO.read(Seq.class, Core.app.getClipboardText()));

                            rebuildFilters();
                            update();
                        }catch(Throwable e){
                            ui.showException(e);
                        }
                    }).marginLeft(12f).disabled(b -> Core.app.getClipboardText() == null).row();
                    in.button("@clear", Icon.none, style, () -> {
                        dialog.hide();
                        filters.clear();
                        rebuildFilters();
                        update();
                    }).marginLeft(12f).row();
                    if(!applied){
                        in.button("@settings.reset", Icon.refresh, style, () -> {
                            dialog.hide();
                            filters.set(maps.readFilters(""));
                            rebuildFilters();
                            update();
                        }).marginLeft(12f).row();
                    }
                });
            });

            dialog.addCloseButton();
            dialog.show();
        });

        buttons.button("@add", Icon.add, this::showAdd);

        if(!applied){
            hidden(this::apply);
        }

        onResize(this::rebuildFilters);
    }

    public void show(Seq<GenerateFilter> filters, Cons<Seq<GenerateFilter>> applier){
        this.filters = filters;
        this.applier = applier;
        show();
    }

    public void show(Cons<Seq<GenerateFilter>> applier){
        show(this.filters, applier);
    }

    /** Applies the specified filters to the editor. */
    public void applyToEditor(Seq<GenerateFilter> filters){
        //writeback buffer
        long[] writeTiles = new long[editor.width() * editor.height()];
        long[] writeData = new long[writeTiles.length];

        for(GenerateFilter filter : filters){
            input.begin(editor.width(), editor.height(), editor::tile);

            //write to buffer
            for(int x = 0; x < editor.width(); x++){
                for(int y = 0; y < editor.height(); y++){
                    input.set(editor.tile(x, y));
                    filter.apply(input);
                    writeTiles[x + y*world.width()] = PackTile.get(input.block.id, input.floor.id, input.overlay.id);
                    writeData[x + y*world.width()] = input.packedData;
                }
            }

            editor.load(() -> {
                //read from buffer back into tiles
                for(int i = 0; i < editor.width() * editor.height(); i++){
                    Tile tile = world.tiles.geti(i);
                    long write = writeTiles[i];

                    Block block = content.block(PackTile.block(write)), floor = content.block(PackTile.floor(write)), overlay = content.block(PackTile.overlay(write));

                    //don't mess up synthetic stuff.
                    if(!tile.synthetic() && !block.synthetic()){
                        tile.setBlock(block);
                    }

                    tile.setPackedData(writeData[i]);

                    tile.setFloor((Floor)floor);
                    tile.setOverlay(overlay);
                }
            });
        }

        //reset undo stack as generation... messes things up
        editor.renderer.recache();
        editor.clearOp();
    }

    MirrorFilter hit(float x, float y){
        if(preview == null) return null;

        MirrorFilter.drawRect(preview, dragRect);
        if(dragRect.width <= 0f || dragRect.height <= 0f) return null;

        MirrorFilter out = null;
        float dst = Float.MAX_VALUE;
        float pad = Scl.scl(16f);
        int amount = 0;

        for(var filter : filters){
            if(filter instanceof MirrorFilter){
                MirrorFilter mirror = (MirrorFilter)filter;
                float px = dragRect.x + Mathf.clamp(mirror.axisX, 0f, 1f) * dragRect.width;
                float py = dragRect.y + Mathf.clamp(mirror.axisY, 0f, 1f) * dragRect.height;

                dragAxis.trnsExact(mirror.angle - 90f, 1f);
                float ndst = Math.abs((x - px) * dragAxis.y - (y - py) * dragAxis.x);
                if(ndst < dst){
                    dst = ndst;
                    out = mirror;
                }
                amount++;
            }
        }

        if(out != null && dst <= pad) return out;
        if(amount == 1 && out != null && x >= dragRect.x - pad && y >= dragRect.y - pad && x <= dragRect.x + dragRect.width + pad && y <= dragRect.y + dragRect.height + pad) return out;
        return null;
    }

    void drag(float x, float y){
        if(preview == null || dragging == null) return;

        MirrorFilter.drawRect(preview, dragRect);
        if(dragRect.width <= 0f || dragRect.height <= 0f) return;

        dragging.axisX = Mathf.clamp((x - dragRect.x) / dragRect.width, 0f, 1f);
        dragging.axisY = Mathf.clamp((y - dragRect.y) / dragRect.height, 0f, 1f);
    }

    void dragUpdate(boolean force){
        long now = Time.millis();
        if(!force && now - lastDrag < 33L) return;
        lastDrag = now;
        update();
    }

    void setup(){
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
            pixmap = null;
            texture = null;
        }

        pixmap = new Pixmap(editor.width() / scaling, editor.height() / scaling);
        texture = new Texture(pixmap);

        cont.clear();
        cont.table(t -> {
            t.margin(8f);
            preview = new BorderImage(texture){
                {
                    setScaling(Scaling.fit);
                }

                @Override
                public void draw(){
                    super.draw();
                    for(var filter : filters){
                        filter.draw(this);
                    }
                }
            };
            preview.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(button != KeyCode.mouseLeft) return false;

                    dragging = hit(x, y);
                    if(dragging == null) return false;

                    drag(x, y);
                    dragUpdate(true);
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    if(dragging == null) return;
                    drag(x, y);
                    dragUpdate(false);
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(dragging == null) return;
                    drag(x, y);
                    dragUpdate(true);
                    dragging = null;
                }
            });
            t.stack(preview, new Stack(){{
                add(new Image(Styles.black8));
                add(new Image(Icon.refresh, Scaling.none));
                visible(() -> generating && !updateEditorOnChange && dragging == null);
            }}).uniformX().grow().padRight(10);
            t.pane(p -> filterTable = p.marginRight(6)).update(pane -> {
                if(Core.scene.getKeyboardFocus() instanceof Dialog && Core.scene.getKeyboardFocus() != this){
                    return;
                }

                Vec2 v = pane.stageToLocalCoordinates(Core.input.mouse());

                if(v.x >= 0 && v.y >= 0 && v.x <= pane.getWidth() && v.y <= pane.getHeight()){
                    Core.scene.setScrollFocus(pane);
                }else{
                    Core.scene.setScrollFocus(null);
                }
            }).grow().uniformX().scrollX(false);
        }).grow();

        buffer1 = create();
        buffer2 = create();

        update();
        rebuildFilters();
    }

    long[] create(){
        return new long[(editor.width() / scaling) * (editor.height() / scaling)];
    }

    void rebuildFilters(){
        int cols = Math.max((int)(Core.graphics.getWidth()/2f / Scl.scl(290f)), 1);
        filterTable.clearChildren();
        filterTable.top().left();
        int i = 0;

        for(var filter : filters){

            //main container
            filterTable.table(Tex.pane, c -> {
                c.margin(0);

                //icons to perform actions
                c.table(Tex.whiteui, t -> {
                    t.setColor(Pal.gray);

                    t.top().left();
                    t.add(filter.name()).left().padLeft(6).width(100f).wrap();

                    t.add().growX();

                    ImageButtonStyle style = Styles.geni;
                    t.defaults().size(42f).padLeft(-5f);

                    t.button(Icon.refresh, style, () -> {
                        filter.randomize();
                        update();
                    }).padLeft(-16f).tooltip("@editor.randomize");

                    if(filter != filters.first()){
                        t.button(Icon.upOpen, style, () -> {
                            int idx = filters.indexOf(filter);
                            filters.swap(idx, Math.max(0, idx - 1));
                            rebuildFilters();
                            update();
                        }).tooltip("@editor.moveup");
                    }

                    if(filter != filters.peek()){
                        t.button(Icon.downOpen, style, () -> {
                            int idx = filters.indexOf(filter);
                            filters.swap(idx, Math.min(filters.size - 1, idx + 1));
                            rebuildFilters();
                            update();
                        }).tooltip("@editor.movedown");
                    }

                    t.button(Icon.copy, style, () -> {
                        GenerateFilter copy = filter.copy();
                        copy.randomize();
                        filters.insert(filters.indexOf(filter) + 1, copy);
                        rebuildFilters();
                        update();
                    }).tooltip("@editor.copy");

                    t.button(Icon.cancel, style, () -> {
                        filters.remove(filter);
                        rebuildFilters();
                        update();
                    }).tooltip("@waves.remove");
                }).growX();

                c.row();
                //all the options
                c.table(f -> {
                    f.left().top();
                    for(FilterOption option : filter.options()){
                        option.changed = this::update;

                        f.table(t -> {
                            t.left();
                            option.build(t);
                        }).growX().left();
                        f.row();
                    }
                }).grow().left().pad(6).top();
            }).width(280f).pad(3).top().left().fillY();

            if(++i % cols == 0){
                filterTable.row();
            }
        }

        if(filters.isEmpty()){
            filterTable.add("@filters.empty").wrap().width(200f);
        }
    }

    void showAdd(){
        var selection = new BaseDialog("@add");
        selection.cont.pane(p -> {
            p.background(Tex.button);
            p.marginRight(14);
            p.defaults().size(195f, 56f);
            int i = 0;
            for(var gen : Maps.allFilterTypes){
                var filter = gen.get();
                var icon = filter.icon();

                if(filter.isPost() && applied) continue;

                p.button((icon == '\0' ? "" : icon + " ") + filter.name(), Styles.flatt, () -> {
                    filter.randomize();
                    filters.add(filter);
                    rebuildFilters();
                    update();
                    selection.hide();
                }).with(Table::left).get().getLabelCell().growX().left().padLeft(5).labelAlign(Align.left);
                if(++i % 3 == 0) p.row();
            }

            p.button(Iconc.refresh + " " + Core.bundle.get("filter.defaultores"), Styles.flatt, () -> {
                maps.addDefaultOres(filters);
                rebuildFilters();
                update();
                selection.hide();
            }).with(Table::left).get().getLabelCell().growX().left().padLeft(5).labelAlign(Align.left);
        }).scrollX(false);

        selection.addCloseButton();
        selection.show();
    }

    long pack(Tile tile){
        return PackTile.get(tile.blockID(), tile.floorID(), tile.overlayID());
    }

    Tile unpack(long tile){
        ctile.setFloor((Floor)content.block(PackTile.floor(tile)));
        ctile.setBlock(content.block(PackTile.block(tile)));
        ctile.setOverlay(content.block(PackTile.overlay(tile)));
        return ctile;
    }

    void apply(){
        if(result != null){
            //ignore errors yay
            try{
                result.get();
            }catch(Exception e){}
        }

        buffer1 = null;
        buffer2 = null;
        generating = false;
        preview = null;
        dragging = null;
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
            pixmap = null;
            texture = null;
        }

        applier.get(filters);
    }

    void update(){

        if(generating){
            updateQueued = true;
            return;
        }

        Seq<GenerateFilter> copy = new Seq<>(filters.size);
        for(var filter : filters){
            copy.add(filter.copy());
        }

        generating = true;
        updateQueued = false;

        result = mainExecutor.submit(() -> {
            try{
                int w = pixmap.width;
                world.setGenerating(true);

                if(!copy.isEmpty()){
                    //write to buffer1 for reading
                    for(int px = 0; px < pixmap.width; px++){
                        for(int py = 0; py < pixmap.height; py++){
                            buffer1[px + py*w] = pack(editor.tile(px * scaling, py * scaling));
                        }
                    }
                }

                for(var filter : copy){
                    input.begin(editor.width(), editor.height(), (x, y) -> unpack(buffer1[Mathf.clamp(x / scaling, 0, pixmap.width -1) + w* Mathf.clamp(y / scaling, 0, pixmap.height -1)]));

                    //read from buffer1 and write to buffer2
                    pixmap.each((px, py) -> {
                        int x = px * scaling, y = py * scaling;
                        long tile = buffer1[px + py * w];
                        input.set(x, y, content.block(PackTile.block(tile)), content.block(PackTile.floor(tile)), content.block(PackTile.overlay(tile)), 0);
                        filter.apply(input);
                        buffer2[px + py * w] = PackTile.get(input.block.id, input.floor.id, input.overlay.id);
                    });

                    pixmap.each((px, py) -> buffer1[px + py*w] = buffer2[px + py*w]);
                }

                for(int px = 0; px < pixmap.width; px++){
                    for(int py = 0; py < pixmap.height; py++){
                        int color;
                        //get result from buffer1 if there's filters left, otherwise get from editor directly
                        if(copy.isEmpty()){
                            Tile tile = editor.tile(px * scaling, py * scaling);
                            color = MapIO.colorFor(tile.block(), tile.floor(), tile.overlay(), Team.derelict);
                        }else{
                            long tile = buffer1[px + py*w];
                            color = MapIO.colorFor(content.block(PackTile.block(tile)), content.block(PackTile.floor(tile)), content.block(PackTile.overlay(tile)), Team.derelict);
                        }
                        pixmap.set(px, pixmap.height - 1 - py, color);
                    }
                }

                Core.app.post(() -> {
                    if(pixmap == null || texture == null){
                        generating = false;
                        return;
                    }
                    texture.draw(pixmap);
                    generating = false;
                    if(updateQueued){
                        update();
                    }
                });
            }catch(Exception e){
                generating = false;
                Log.err(e);
                Core.app.post(() -> {
                    if(updateQueued){
                        update();
                    }
                });
            }
            world.setGenerating(false);
        });
    }
}
