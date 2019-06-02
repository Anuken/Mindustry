package io.anuke.mindustry.editor;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.StringMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.*;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.dialogs.FileChooser;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.OverlayFloor;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;

import static io.anuke.mindustry.Vars.*;

public class MapEditorDialog extends Dialog implements Disposable{
    public final MapEditor editor;

    private MapView view;
    private MapInfoDialog infoDialog;
    private MapLoadDialog loadDialog;
    private MapResizeDialog resizeDialog;
    private MapGenerateDialog generateDialog;
    private ScrollPane pane;
    private FloatingDialog menu;
    private Rules lastSavedRules;
    private boolean saved = false;
    private boolean shownWithMap = false;
    private Array<Block> blocksOut = new Array<>();

    public MapEditorDialog(){
        super("", "dialog");

        background("dark");

        editor = new MapEditor();
        view = new MapView(editor);
        infoDialog = new MapInfoDialog(editor);
        generateDialog = new MapGenerateDialog(editor);

        menu = new FloatingDialog("$menu");
        menu.addCloseButton();

        float isize = 16 * 2f;
        float swidth = 180f;

        menu.cont.table(t -> {
            t.defaults().size(swidth, 60f).padBottom(5).padRight(5).padLeft(5);

            t.addImageTextButton("$editor.savemap", "icon-floppy-16", isize, this::save);

            t.addImageTextButton("$editor.mapinfo", "icon-pencil", isize, () -> {
                infoDialog.show();
                menu.hide();
            });

            t.row();

            t.addImageTextButton("$editor.generate", "icon-editor", isize, () -> {
                generateDialog.show();
                menu.hide();
            });

            t.addImageTextButton("$editor.resize", "icon-resize", isize, () -> {
                resizeDialog.show();
                menu.hide();
            });

            t.row();

            t.addImageTextButton("$editor.import", "icon-load-map", isize, () ->
                createDialog("$editor.import",
                "$editor.importmap", "$editor.importmap.description", "icon-load-map", (Runnable)loadDialog::show,
                "$editor.importfile", "$editor.importfile.description", "icon-file", (Runnable)() ->
                Platform.instance.showFileChooser("$editor.loadmap", "Map Files", file -> ui.loadAnd(() -> {
                    world.maps.tryCatchMapError(() -> {
                        if(MapIO.isImage(file)){
                            ui.showInfo("$editor.errorimage");
                        }else if(file.extension().equalsIgnoreCase(oldMapExtension)){
                            editor.beginEdit(world.maps.makeLegacyMap(file));
                        }else{
                            editor.beginEdit(MapIO.createMap(file, true));
                        }
                    });
                }), true, FileChooser.anyMapFiles),

                "$editor.importimage", "$editor.importimage.description", "icon-file-image", (Runnable)() ->
                Platform.instance.showFileChooser("$loadimage", "Image Files", file ->
                ui.loadAnd(() -> {
                    try{
                        Pixmap pixmap = new Pixmap(file);
                        editor.beginEdit(pixmap);
                        pixmap.dispose();
                    }catch(Exception e){
                        ui.showError(Core.bundle.format("editor.errorload", Strings.parseException(e, true)));
                        Log.err(e);
                    }
                }), true, FileChooser.pngFiles))
            );

            t.addImageTextButton("$editor.export", "icon-save-map", isize, () ->
                Platform.instance.showFileChooser("$editor.savemap", "Map Files", file -> {
                    file = file.parent().child(file.nameWithoutExtension() + "." + mapExtension);
                    FileHandle result = file;
                    ui.loadAnd(() -> {
                        try{
                            if(!editor.getTags().containsKey("name")){
                                editor.getTags().put("name", result.nameWithoutExtension());
                            }
                            MapIO.writeMap(result, editor.createMap(result));
                        }catch(Exception e){
                            ui.showError(Core.bundle.format("editor.errorsave", Strings.parseException(e, true)));
                            Log.err(e);
                        }
                    });
                }, false, FileChooser.mapFiles));
        });

        menu.cont.row();

        menu.cont.addImageTextButton("$editor.ingame", "icon-arrow", isize, this::playtest).padTop(-5).size(swidth * 2f + 10, 60f);

        menu.cont.row();

        menu.cont.addImageTextButton("$quit", "icon-back", isize, () -> {
            tryExit();
            menu.hide();
        }).size(swidth * 2f + 10, 60f);

        resizeDialog = new MapResizeDialog(editor, (x, y) -> {
            if(!(editor.width() == x && editor.height() == y)){
                ui.loadAnd(() -> {
                    editor.resize(x, y);
                });
            }
        });

        loadDialog = new MapLoadDialog(map -> ui.loadAnd(() -> {
            try{
                editor.beginEdit(map);
            }catch(Exception e){
                ui.showError(Core.bundle.format("editor.errorload", Strings.parseException(e, true)));
                Log.err(e);
            }
        }));

        setFillParent(true);

        clearChildren();
        margin(0);
        shown(this::build);

        update(() -> {
            if(Core.scene.getKeyboardFocus() instanceof Dialog && Core.scene.getKeyboardFocus() != this){
                return;
            }

            Vector2 v = pane.stageToLocalCoordinates(Core.input.mouse());

            if(v.x >= 0 && v.y >= 0 && v.x <= pane.getWidth() && v.y <= pane.getHeight()){
                Core.scene.setScrollFocus(pane);
            }else{
                Core.scene.setScrollFocus(null);
            }

            if(Core.scene != null && Core.scene.getKeyboardFocus() == this){
                doInput();
            }
        });

        shown(() -> {

            saved = true;
            if(!Core.settings.getBool("landscape")) Platform.instance.beginForceLandscape();
            editor.clearOp();
            Core.scene.setScrollFocus(view);
            if(!shownWithMap){
                //clear units, rules and other unnecessary stuff
                logic.reset();
                state.rules = new Rules();
                editor.beginEdit(200, 200);
            }
            shownWithMap = false;

            Time.runTask(10f, Platform.instance::updateRPC);
        });

        hidden(() -> {
            editor.clearOp();
            Platform.instance.updateRPC();
            if(!Core.settings.getBool("landscape")) Platform.instance.endForceLandscape();
        });
    }

    @Override
    protected void drawBackground(float x, float y){
        drawDefaultBackground(x, y);
    }

    public void resumeEditing(){
        state.set(State.menu);
        shownWithMap = true;
        show();
        state.rules = (lastSavedRules == null ? new Rules() : lastSavedRules);
        lastSavedRules = null;
        editor.renderer().updateAll();
    }

    private void playtest(){
        menu.hide();
        ui.loadAnd(() -> {
            lastSavedRules = state.rules;
            hide();
            //only reset the player; logic.reset() will clear entities, which we do not want
            player.reset();
            state.rules = Gamemode.editor.apply(lastSavedRules.copy());
            world.setMap(new Map(StringMap.of(
                "name", "Editor Playtesting",
                "width", editor.width(),
                "height", editor.height()
            )));
            world.endMapLoad();
            //add entities so they update. is this really needed?
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    Tile tile = world.rawTile(x, y);
                    if(tile.entity != null){
                        tile.entity.add();
                    }
                }
            }
            player.set(world.width() * tilesize/2f, world.height() * tilesize/2f);
            player.setDead(false);
            logic.play();
        });
    }

    private void save(){
        String name = editor.getTags().get("name", "").trim();
        editor.getTags().put("rules", JsonIO.write(state.rules));

        if(name.isEmpty()){
            infoDialog.show();
            Core.app.post(() -> ui.showError("$editor.save.noname"));
        }else{
            Map map = world.maps.all().find(m -> m.name().equals(name));
            if(map != null && !map.custom){
                ui.showError("$editor.save.overwrite");
            }else{
                world.maps.saveMap(editor.getTags());
                ui.showInfoFade("$editor.saved");
            }
        }

        menu.hide();
        saved = true;
    }

    /**
     * Argument format:
     * 0) button name
     * 1) description
     * 2) icon name
     * 3) listener
     */
    private void createDialog(String title, Object... arguments){
        FloatingDialog dialog = new FloatingDialog(title);

        float h = 90f;

        dialog.cont.defaults().size(360f, h).padBottom(5).padRight(5).padLeft(5);

        for(int i = 0; i < arguments.length; i += 4){
            String name = (String)arguments[i];
            String description = (String)arguments[i + 1];
            String iconname = (String)arguments[i + 2];
            Runnable listenable = (Runnable)arguments[i + 3];

            TextButton button = dialog.cont.addButton(name, () -> {
                listenable.run();
                dialog.hide();
                menu.hide();
            }).left().margin(0).get();

            button.clearChildren();
            button.addImage(iconname).size(16 * 3).padLeft(10);
            button.table(t -> {
                t.add(name).growX().wrap();
                t.row();
                t.add(description).color(Color.GRAY).growX().wrap();
            }).growX().pad(10f).padLeft(5);

            button.row();

            dialog.cont.row();
        }

        dialog.addCloseButton();
        dialog.show();
    }

    @Override
    public Dialog show(){
        return super.show(Core.scene, Actions.sequence(Actions.alpha(0f), Actions.scaleTo(1f, 1f), Actions.fadeIn(0.3f)));
    }

    @Override
    public void dispose(){
        editor.renderer().dispose();
    }

    public void beginEditMap(FileHandle file){
        ui.loadAnd(() -> {
            try{
                shownWithMap = true;
                editor.beginEdit(MapIO.createMap(file, true));
                show();
            }catch(Exception e){
                Log.err(e);
                ui.showError(Core.bundle.format("editor.errorload", Strings.parseException(e, true)));
            }
        });
    }

    public MapView getView(){
        return view;
    }

    public void resetSaved(){
        saved = false;
    }

    public boolean hasPane(){
        return Core.scene.getScrollFocus() == pane || Core.scene.getKeyboardFocus() != this;
    }

    public void build(){
        float amount = 10f, baseSize = 60f;

        float size = mobile ? (int)(Math.min(Core.graphics.getHeight(), Core.graphics.getWidth()) / amount / Unit.dp.scl(1f)) :
        Math.min(Core.graphics.getDisplayMode().height / amount, baseSize);

        clearChildren();
        table(cont -> {
            cont.left();

            cont.table(mid -> {
                mid.top();

                Table tools = new Table().top();

                ButtonGroup<ImageButton> group = new ButtonGroup<>();

                Consumer<EditorTool> addTool = tool -> {
                    ImageButton button = new ImageButton("icon-" + tool.name(), "clear-toggle");
                    button.clicked(() -> view.setTool(tool));
                    button.resizeImage(16 * 2f);
                    button.update(() -> button.setChecked(view.getTool() == tool));
                    group.add(button);
                    if(tool == EditorTool.pencil)
                        button.setChecked(true);

                    tools.add(button);
                };

                tools.defaults().size(size, size);

                tools.addImageButton("icon-menu-large", "clear", 16 * 2f, menu::show);

                ImageButton grid = tools.addImageButton("icon-grid", "clear-toggle", 16 * 2f, () -> view.setGrid(!view.isGrid())).get();

                addTool.accept(EditorTool.zoom);

                tools.row();

                ImageButton undo = tools.addImageButton("icon-undo", "clear", 16 * 2f, editor::undo).get();
                ImageButton redo = tools.addImageButton("icon-redo", "clear", 16 * 2f, editor::redo).get();

                addTool.accept(EditorTool.pick);

                tools.row();

                undo.setDisabled(() -> !editor.canUndo());
                redo.setDisabled(() -> !editor.canRedo());

                undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.GRAY : Color.WHITE));
                redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.GRAY : Color.WHITE));
                grid.update(() -> grid.setChecked(view.isGrid()));

                addTool.accept(EditorTool.line);
                addTool.accept(EditorTool.pencil);
                addTool.accept(EditorTool.eraser);

                tools.row();

                addTool.accept(EditorTool.fill);
                addTool.accept(EditorTool.spray);

                ImageButton rotate = tools.addImageButton("icon-arrow-16", "clear", 16 * 2f, () -> editor.rotation = (editor.rotation + 1) % 4).get();
                rotate.getImage().update(() -> {
                    rotate.getImage().setRotation(editor.rotation * 90);
                    rotate.getImage().setOrigin(Align.center);
                });

                tools.row();

                tools.table("underline", t -> t.add("$editor.teams"))
                .colspan(3).height(40).width(size * 3f + 3f).padBottom(3);

                tools.row();

                ButtonGroup<ImageButton> teamgroup = new ButtonGroup<>();

                int i = 0;

                for(Team team : Team.all){
                    ImageButton button = new ImageButton("white", "clear-toggle-partial");
                    button.margin(4f);
                    button.getImageCell().grow();
                    button.getStyle().imageUpColor = team.color;
                    button.clicked(() -> editor.drawTeam = team);
                    button.update(() -> button.setChecked(editor.drawTeam == team));
                    teamgroup.add(button);
                    tools.add(button);

                    if(i++ % 3 == 2) tools.row();
                }

                mid.add(tools).top().padBottom(-6);

                mid.row();

                mid.table("underline", t -> {
                    Slider slider = new Slider(0, MapEditor.brushSizes.length - 1, 1, false);
                    slider.moved(f -> editor.brushSize = MapEditor.brushSizes[(int)(float)f]);

                    t.top();
                    t.add("$editor.brush");
                    t.row();
                    t.add(slider).width(size * 3f - 20).padTop(4f);
                }).padTop(5).growX().top();

            }).margin(0).left().growY();


            cont.table(t -> t.add(view).grow()).grow();

            cont.table(this::addBlockSelection).right().growY();

        }).grow();
    }

    private void doInput(){
        //tool select
        for(int i = 0; i < EditorTool.values().length; i++){
            if(Core.input.keyTap(KeyCode.valueOf("NUM_" + (i + 1)))){
                view.setTool(EditorTool.values()[i]);
                break;
            }
        }

        if(Core.input.keyTap(KeyCode.ESCAPE)){
            if(!menu.isShown()){
                menu.show();
            }
        }

        if(Core.input.keyTap(KeyCode.R)){
            editor.rotation = Mathf.mod(editor.rotation + 1, 4);
        }

        if(Core.input.keyTap(KeyCode.E)){
            editor.rotation = Mathf.mod(editor.rotation - 1, 4);
        }

        //ctrl keys (undo, redo, save)
        if(Core.input.ctrl()){
            if(Core.input.keyTap(KeyCode.Z)){
                editor.undo();
            }

            if(Core.input.keyTap(KeyCode.Y)){
                editor.redo();
            }

            if(Core.input.keyTap(KeyCode.S)){
                save();
            }

            if(Core.input.keyTap(KeyCode.G)){
                view.setGrid(!view.isGrid());
            }
        }
    }

    private void tryExit(){
        if(!saved){
            ui.showConfirm("$confirm", "$editor.unsaved", this::hide);
        }else{
            hide();
        }
    }

    private void addBlockSelection(Table table){
        Table content = new Table();
        pane = new ScrollPane(content);
        pane.setFadeScrollBars(false);
        pane.setOverscroll(true, false);
        ButtonGroup<ImageButton> group = new ButtonGroup<>();

        int i = 0;

        blocksOut.clear();
        blocksOut.addAll(Vars.content.blocks());
        blocksOut.sort((b1, b2) -> {
            int core = -Boolean.compare(b1 instanceof CoreBlock, b2 instanceof CoreBlock);
            if(core != 0) return core;
            int synth = Boolean.compare(b1.synthetic(), b2.synthetic());
            if(synth != 0) return synth;
            int ore = Boolean.compare(b1 instanceof OverlayFloor, b2 instanceof OverlayFloor);
            if(ore != 0) return ore;
            return Integer.compare(b1.id, b2.id);
        });

        for(Block block : blocksOut){
            TextureRegion region = block.icon(Icon.medium);

            if(!Core.atlas.isFound(region)) continue;

            ImageButton button = new ImageButton("white", "clear-toggle");
            button.getStyle().imageUp = new TextureRegionDrawable(region);
            button.clicked(() -> editor.drawBlock = block);
            button.resizeImage(8 * 4f);
            button.update(() -> button.setChecked(editor.drawBlock == block));
            group.add(button);
            content.add(button).size(50f);

            if(++i % 4 == 0){
                content.row();
            }
        }

        group.getButtons().get(2).setChecked(true);

        table.table("underline", extra -> extra.labelWrap(() -> editor.drawBlock.localizedName).width(200f).center()).growX();
        table.row();
        table.add(pane).growY().fillX();
    }
}
