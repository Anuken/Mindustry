package io.anuke.mindustry.editor;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.actions.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.blocks.storage.*;

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
        super("");

        background(Styles.black);

        editor = new MapEditor();
        view = new MapView(editor);
        infoDialog = new MapInfoDialog(editor);
        generateDialog = new MapGenerateDialog(editor, true);

        menu = new FloatingDialog("$menu");
        menu.addCloseButton();

        float swidth = 180f;

        menu.cont.table(t -> {
            t.defaults().size(swidth, 60f).padBottom(5).padRight(5).padLeft(5);

            t.addImageTextButton("$editor.savemap", Icon.floppy16Small, this::save);

            t.addImageTextButton("$editor.mapinfo", Icon.pencilSmall, () -> {
                infoDialog.show();
                menu.hide();
            });

            t.row();

            t.addImageTextButton("$editor.generate", Icon.editorSmall, () -> {
                generateDialog.show(generateDialog::applyToEditor);
                menu.hide();
            });

            t.addImageTextButton("$editor.resize", Icon.resizeSmall, () -> {
                resizeDialog.show();
                menu.hide();
            });

            t.row();

            t.addImageTextButton("$editor.import", Icon.loadMapSmall, () ->
            createDialog("$editor.import",
            "$editor.importmap", "$editor.importmap.description", Icon.loadMap, (Runnable)loadDialog::show,
            "$editor.importfile", "$editor.importfile.description", Icon.file, (Runnable)() ->
            platform.showFileChooser(true, mapExtension, file -> ui.loadAnd(() -> {
                maps.tryCatchMapError(() -> {
                    if(MapIO.isImage(file)){
                        ui.showInfo("$editor.errorimage");
                    }else{
                        editor.beginEdit(MapIO.createMap(file, true));
                    }
                });
            })),

            "$editor.importimage", "$editor.importimage.description", Icon.fileImage, (Runnable)() ->
            platform.showFileChooser(true, "png", file ->
            ui.loadAnd(() -> {
                try{
                    Pixmap pixmap = new Pixmap(file);
                    editor.beginEdit(pixmap);
                    pixmap.dispose();
                }catch(Exception e){
                    ui.showException("$editor.errorload", e);
                    Log.err(e);
                }
            })))
            );

            t.addImageTextButton("$editor.export", Icon.saveMapSmall, () -> {
                if(!ios){
                    platform.showFileChooser(false, mapExtension, file -> {
                        ui.loadAnd(() -> {
                            try{
                                if(!editor.getTags().containsKey("name")){
                                    editor.getTags().put("name", file.nameWithoutExtension());
                                }
                                MapIO.writeMap(file, editor.createMap(file));
                            }catch(Exception e){
                                ui.showException("$editor.errorsave", e);
                                Log.err(e);
                            }
                        });
                    });
                }else{
                    ui.loadAnd(() -> {
                        try{
                            FileHandle result = Core.files.local(editor.getTags().get("name", "unknown") + "." + mapExtension);
                            MapIO.writeMap(result, editor.createMap(result));
                            platform.shareFile(result);
                        }catch(Exception e){
                            ui.showException("$editor.errorsave", e);
                            Log.err(e);
                        }
                    });
                }
            });
        });

        menu.cont.row();

        if(steam){
            menu.cont.addImageTextButton("$editor.publish.workshop", Icon.linkSmall, () -> {
                Map builtin = maps.all().find(m -> m.name().equals(editor.getTags().get("name", "").trim()));
                if(editor.getTags().containsKey("steamid") && builtin != null && !builtin.custom){
                    platform.viewListing(editor.getTags().get("steamid"));
                    return;
                }

                Map map = save();

                if(editor.getTags().containsKey("steamid") && map != null){
                    platform.viewMapListingInfo(map);
                    return;
                }

                if(map == null) return;

                if(map.tags.get("description", "").length() < 4){
                    ui.showErrorMessage("$editor.nodescription");
                    return;
                }

                if(!Structs.contains(Gamemode.all, g -> g.valid(map))){
                    ui.showErrorMessage("$map.nospawn");
                    return;
                }

                platform.publishMap(map);
            }).padTop(-3).size(swidth * 2f + 10, 60f).update(b -> b.setText(editor.getTags().containsKey("steamid") ? editor.getTags().get("author").equals(player.name) ? "$workshop.listing" : "$view.workshop" : "$editor.publish.workshop"));

            menu.cont.row();
        }

        menu.cont.addImageTextButton("$editor.ingame", Icon.arrowSmall, this::playtest).padTop(!steam ? -3 : 1).size(swidth * 2f + 10, 60f);

        menu.cont.row();

        menu.cont.addImageTextButton("$quit", Icon.backSmall, () -> {
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
                ui.showException("$editor.errorload", e);
                Log.err(e);
            }
        }));

        setFillParent(true);

        clearChildren();
        margin(0);

        update(() -> {
            if(Core.scene.getKeyboardFocus() instanceof Dialog && Core.scene.getKeyboardFocus() != this){
                return;
            }

            if(Core.scene != null && Core.scene.getKeyboardFocus() == this){
                doInput();
            }
        });

        shown(() -> {

            saved = true;
            if(!Core.settings.getBool("landscape")) platform.beginForceLandscape();
            editor.clearOp();
            Core.scene.setScrollFocus(view);
            if(!shownWithMap){
                //clear units, rules and other unnecessary stuff
                logic.reset();
                state.rules = new Rules();
                editor.beginEdit(200, 200);
            }
            shownWithMap = false;

            Time.runTask(10f, platform::updateRPC);
        });

        hidden(() -> {
            editor.clearOp();
            platform.updateRPC();
            if(!Core.settings.getBool("landscape")) platform.endForceLandscape();
        });

        shown(this::build);
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
            state.teams = new Teams();
            player.reset();
            state.rules = Gamemode.editor.apply(lastSavedRules.copy());
            state.rules.zone = null;
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

    public @Nullable Map save(){
        boolean isEditor = state.rules.editor;
        state.rules.editor = false;
        String name = editor.getTags().get("name", "").trim();
        editor.getTags().put("rules", JsonIO.write(state.rules));
        editor.getTags().remove("width");
        editor.getTags().remove("height");
        player.dead = true;

        Map returned = null;

        if(name.isEmpty()){
            infoDialog.show();
            Core.app.post(() -> ui.showErrorMessage("$editor.save.noname"));
        }else{
            Map map = maps.all().find(m -> m.name().equals(name));
            if(map != null && !map.custom){
                handleSaveBuiltin(map);
            }else{
                returned = maps.saveMap(editor.getTags());
                ui.showInfoFade("$editor.saved");
            }
        }

        menu.hide();
        saved = true;
        state.rules.editor = isEditor;
        return returned;
    }

    /** Called when a built-in map save is attempted.*/
    protected void handleSaveBuiltin(Map map){
        ui.showErrorMessage("$editor.save.overwrite");
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
            Drawable iconname = (Drawable)arguments[i + 2];
            Runnable listenable = (Runnable)arguments[i + 3];

            TextButton button = dialog.cont.addButton(name, () -> {
                listenable.run();
                dialog.hide();
                menu.hide();
            }).left().margin(0).get();

            button.clearChildren();
            button.addImage(iconname).padLeft(10);
            button.table(t -> {
                t.add(name).growX().wrap();
                t.row();
                t.add(description).color(Color.gray).growX().wrap();
            }).growX().pad(10f).padLeft(5);

            button.row();

            dialog.cont.row();
        }

        dialog.addCloseButton();
        dialog.show();
    }

    @Override
    public Dialog show(){
        return super.show(Core.scene, Actions.sequence(Actions.alpha(1f)));
    }

    @Override
    public void hide(){
        super.hide(Actions.sequence(Actions.alpha(0f)));
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
                ui.showException("$editor.errorload", e);
            }
        });
    }

    public MapView getView(){
        return view;
    }

    public MapGenerateDialog getGenerateDialog(){
        return generateDialog;
    }

    public void resetSaved(){
        saved = false;
    }

    public boolean hasPane(){
        return Core.scene.getScrollFocus() == pane || Core.scene.getKeyboardFocus() != this;
    }

    public void build(){
        float size = 60f;

        clearChildren();
        table(cont -> {
            cont.left();

            cont.table(mid -> {
                mid.top();

                Table tools = new Table().top();

                ButtonGroup<ImageButton> group = new ButtonGroup<>();
                Table[] lastTable = {null};

                Consumer<EditorTool> addTool = tool -> {

                    ImageButton button = new ImageButton(Core.atlas.drawable("icon-" + tool.name() + "-small"), Styles.clearTogglei);
                    button.clicked(() -> {
                        view.setTool(tool);
                        if(lastTable[0] != null){
                            lastTable[0].remove();
                        }
                    });
                    button.update(() -> button.setChecked(view.getTool() == tool));
                    group.add(button);

                    if(tool.altModes.length > 0){
                        button.clicked(l -> {
                            if(!mobile){
                                //desktop: rightclick
                                l.setButton(KeyCode.MOUSE_RIGHT);
                            }
                        }, e -> {
                            //need to double tap
                            if(mobile && e.getTapCount() < 2){
                                return;
                            }

                            if(lastTable[0] != null){
                                lastTable[0].remove();
                            }

                            Table table = new Table(Styles.black9);
                            table.defaults().size(300f, 70f);

                            for(int i = 0; i < tool.altModes.length; i++){
                                int mode = i;
                                String name = tool.altModes[i];

                                table.addButton(b -> {
                                    b.left();
                                    b.marginLeft(6);
                                    b.setStyle(Styles.clearTogglet);
                                    b.add(Core.bundle.get("toolmode." + name)).left();
                                    b.row();
                                    b.add(Core.bundle.get("toolmode." + name + ".description")).color(Color.lightGray).left();
                                }, () -> {
                                    tool.mode = (tool.mode == mode ? -1 : mode);
                                    table.remove();
                                }).update(b -> b.setChecked(tool.mode == mode));
                                table.row();
                            }

                            table.update(() -> {
                                Vector2 v = button.localToStageCoordinates(Tmp.v1.setZero());
                                table.setPosition(v.x, v.y, Align.topLeft);
                                if(!isShown()){
                                    table.remove();
                                    lastTable[0] = null;
                                }
                            });

                            table.pack();
                            table.act(Core.graphics.getDeltaTime());

                            addChild(table);
                            lastTable[0] = table;
                        });
                    }


                    Label mode = new Label("");
                    mode.setColor(Pal.remove);
                    mode.update(() -> mode.setText(tool.mode == -1 ? "" : "M" + (tool.mode + 1) + " "));
                    mode.setAlignment(Align.bottomRight, Align.bottomRight);
                    mode.touchable(Touchable.disabled);

                    tools.stack(button, mode);
                };

                tools.defaults().size(size, size);

                tools.addImageButton(Icon.menuLargeSmall, Styles.cleari, menu::show);

                ImageButton grid = tools.addImageButton(Icon.gridSmall, Styles.clearTogglei, () -> view.setGrid(!view.isGrid())).get();

                addTool.accept(EditorTool.zoom);

                tools.row();

                ImageButton undo = tools.addImageButton(Icon.undoSmall, Styles.cleari, editor::undo).get();
                ImageButton redo = tools.addImageButton(Icon.redoSmall, Styles.cleari, editor::redo).get();

                addTool.accept(EditorTool.pick);

                tools.row();

                undo.setDisabled(() -> !editor.canUndo());
                redo.setDisabled(() -> !editor.canRedo());

                undo.update(() -> undo.getImage().setColor(undo.isDisabled() ? Color.gray : Color.white));
                redo.update(() -> redo.getImage().setColor(redo.isDisabled() ? Color.gray : Color.white));
                grid.update(() -> grid.setChecked(view.isGrid()));

                addTool.accept(EditorTool.line);
                addTool.accept(EditorTool.pencil);
                addTool.accept(EditorTool.eraser);

                tools.row();

                addTool.accept(EditorTool.fill);
                addTool.accept(EditorTool.spray);

                ImageButton rotate = tools.addImageButton(Icon.arrow16Small, Styles.cleari, () -> editor.rotation = (editor.rotation + 1) % 4).get();
                rotate.getImage().update(() -> {
                    rotate.getImage().setRotation(editor.rotation * 90);
                    rotate.getImage().setOrigin(Align.center);
                });

                tools.row();

                tools.table(Tex.underline, t -> t.add("$editor.teams"))
                .colspan(3).height(40).width(size * 3f + 3f).padBottom(3);

                tools.row();

                ButtonGroup<ImageButton> teamgroup = new ButtonGroup<>();

                int i = 0;

                for(Team team : Team.all){
                    ImageButton button = new ImageButton(Tex.whiteui, Styles.clearTogglePartiali);
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

                mid.table(Tex.underline, t -> {
                    Slider slider = new Slider(0, MapEditor.brushSizes.length - 1, 1, false);
                    slider.moved(f -> editor.brushSize = MapEditor.brushSizes[(int)(float)f]);
                    for(int j = 0; j < MapEditor.brushSizes.length; j++){
                        if(MapEditor.brushSizes[j] == editor.brushSize){
                            slider.setValue(j);
                        }
                    }

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

        if(Core.input.ctrl()){
            //alt mode select
            for(int i = 0; i < view.getTool().altModes.length + 1; i++){
                if(Core.input.keyTap(KeyCode.valueOf("NUM_" + (i + 1)))){
                    view.getTool().mode = i - 1;
                }
            }
        }else{
            //tool select
            for(int i = 0; i < EditorTool.values().length; i++){
                if(Core.input.keyTap(KeyCode.valueOf("NUM_" + (i + 1)))){
                    view.setTool(EditorTool.values()[i]);
                    break;
                }
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

            //more undocumented features, fantastic
            if(Core.input.keyTap(KeyCode.T)){

                //clears all 'decoration' from the map
                for(int x = 0; x < editor.width(); x++){
                    for(int y = 0; y < editor.height(); y++){
                        Tile tile = editor.tile(x, y);
                        if(tile.block().breakable && tile.block() instanceof Rock){
                            tile.setBlock(Blocks.air);
                            editor.renderer().updatePoint(x, y);
                        }

                        if(tile.overlay() != Blocks.air && tile.overlay() != Blocks.spawn){
                            tile.setOverlay(Blocks.air);
                            editor.renderer().updatePoint(x, y);
                        }
                    }
                }

                editor.flushOp();
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
        pane.exited(() -> {
            if(pane.hasScroll()){
                Core.scene.setScrollFocus(view);
            }
        });
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
            TextureRegion region = block.icon(Cicon.medium);

            if(!Core.atlas.isFound(region)) continue;

            ImageButton button = new ImageButton(Tex.whiteui, Styles.clearTogglei);
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

        table.table(Tex.underline, extra -> extra.labelWrap(() -> editor.drawBlock.localizedName).width(200f).center()).growX();
        table.row();
        table.add(pane).growY().fillX();
    }
}
