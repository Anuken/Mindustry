package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.actions.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.maps.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class MapPlayDialog extends BaseDialog{
    public @Nullable Runnable playListener;

    CustomRulesDialog dialog = new CustomRulesDialog();
    Rules rules;
    Gamemode selectedGamemode = Gamemode.survival;
    Seq<Gamemode> validModes = new Seq<>();
    Map lastMap;
    int modeIndex = 0;

    public MapPlayDialog(){
        super("");
        titleTable.clear();

        cont.setTransform(true);

        onResize(() -> {
            if(lastMap != null){
                Rules rules = this.rules;
                show(lastMap);
                this.rules = rules;
            }
        });
    }

    public void show(Map map){
        show(map, false);
    }

    public void show(Map map, boolean playtesting){
        this.lastMap = map;
        cont.clearChildren();

        validModes.clear();
        modeIndex = 0;
        for(Gamemode mode : Gamemode.all){
            if(!mode.hidden && mode.valid(map)){
                validModes.add(mode);
            }
        }

        //reset to any valid mode after switching to attack (one must exist)
        if(!selectedGamemode.valid(map)){
            selectedGamemode = validModes.first();
            if(selectedGamemode == null){
                selectedGamemode = Gamemode.survival;
            }
        }

        rules = map.applyRules(selectedGamemode);

        if(mobile){
            buildMobile(map, playtesting);
        }else{
            buildDesktop(map, playtesting);
        }

        show();
    }

    private void buildMobile(Map map, boolean playtesting){
        float width = Core.graphics.getWidth() * 0.8f;

        cont.table(Styles.grayPanel, t -> {
            t.margin(15f);
            t.table(header -> {
                header.image(Icon.map).size(30);
                header.label(() -> map.name() + (Core.settings.getBool("console") ? "\n[gray]" + map.file.nameWithoutExtension() : "")).growX().left().padLeft(10f).get().setAlignment(Align.left);
            }).growX().padBottom(15f);
            t.row();
            t.add(new BorderImage(map.safeTexture(), 3f)).size(width).get().setScaling(Scaling.fit);

            t.row();
            t.label(() -> Core.bundle.get("editor.author") + " " + map.author()).padTop(8f).left().get().setAlignment(Align.left);

            if(Gamemode.survival.valid(map)){
                t.row();
                t.label((() -> Core.bundle.format("level.highscore", map.getHightScore()))).pad(3f).left().get().setAlignment(Align.left);;
            }
        });
        cont.row();
        cont.table(selmode -> {
            selmode.table(Styles.grayPanel, modes -> {
                modes.margin(15f);
                modes.button(Icon.left, Styles.grayi, () -> {
                    modeIndex = (modeIndex - 1) % validModes.size;
                    if(modeIndex < 0) modeIndex = validModes.size - 1;
                    selectedGamemode = validModes.get(modeIndex);
                }).size(30f);
                modes.label(() -> selectedGamemode.toString()).growX().get().setAlignment(Align.center);
                modes.button(Icon.right, Styles.grayi, () -> {
                    modeIndex = (modeIndex + 1) % validModes.size;
                    selectedGamemode = validModes.get(modeIndex);
                }).size(30f);
            }).growX().height(50f);

            selmode.button("?", Styles.grayt, this::displayGameModeHelp).size(50f).padLeft(15f);
        }).fillX().padTop(15f);
        cont.row();
        cont.table(buttons -> {
            buttons.defaults().height(50f);
            buttons.button("@customize", Icon.settings, Styles.grayt, () -> dialog.show(rules, () -> rules = map.applyRules(selectedGamemode))).growX().padBottom(15f).margin(10f).colspan(2);
            buttons.row();
            buttons.button("@back", Icon.left, Styles.grayt, this::hide).growX().padRight(15f).margin(10f);
            buttons.button("@play", Icon.play, Styles.grayt, () -> {
                if(playListener != null) playListener.run();
                control.playMap(map, rules, playtesting);
                hide();
                ui.custom.hide();
            }).growX().margin(10f);;
        }).fillX().padTop(15f);
    }

    private void buildDesktop(Map map, boolean playtesting){
        boolean hasDesc = map.tags.containsKey("description") && !map.tags.get("description").trim().isEmpty();

        Table buttonsTable = new Table();
        buttonsTable.table(selmode -> {
            selmode.table(Styles.grayPanel, modes -> {
                for(Gamemode mode : validModes){
                    TextureRegionDrawable icon = ui.getIcon("mode" + Strings.capitalize(mode.name()));
                    if(!Core.atlas.isFound(icon.getRegion())) icon = Icon.none;

                    modes.button(icon, Styles.emptyTogglei, 30f, () -> {
                        selectedGamemode = mode;
                        rules = map.applyRules(mode);
                    }).height(30f).growX()
                    .update(b -> b.setChecked(selectedGamemode == mode))
                    .size(140f, 54f)
                    .tooltip(mode.toString());
                }
            }).growX();

            selmode.button("?", Styles.grayt, this::displayGameModeHelp).size(50f).padLeft(15f);
        }).top().left().growX().padBottom(15f);

        buttonsTable.row();
        buttonsTable.table(buttons -> {
            buttons.defaults().height(50f);
            buttons.button("@back", Icon.left, Styles.grayt, this::hide).growX().padRight(15f).margin(10f);
            buttons.button("@play", Icon.play, Styles.grayt, () -> {
                if(playListener != null) playListener.run();
                control.playMap(map, rules, playtesting);
                hide();
                ui.custom.hide();
            }).growX().margin(10f);;
            buttons.button("@customize", Icon.settings, Styles.grayt, () -> dialog.show(rules, () -> rules = map.applyRules(selectedGamemode))).growX().padLeft(15f).margin(10f);;
        }).growX();


        cont.table(Styles.grayPanel, t -> {
            t.margin(15f);
            t.table(header -> {
                header.image(Icon.map).size(30);
                header.label(() -> map.name() + (Core.settings.getBool("console") ? "\n[gray]" + map.file.nameWithoutExtension() : "")).growX().left().padLeft(10f).get().setAlignment(Align.left);
            }).growX().padBottom(15f);
            t.row();
            t.add(new BorderImage(map.safeTexture(), 3f)).size(Core.graphics.getWidth() / 4f).get().setScaling(Scaling.fit);

            if(!hasDesc){
                t.row();
                t.label(() -> Core.bundle.get("editor.author") + " " + map.author()).padTop(8f).left().get().setAlignment(Align.left);
            }

            if(Gamemode.survival.valid(map)){
                t.row();
                t.label((() -> Core.bundle.format("level.highscore", map.getHightScore()))).pad(3f).left().get().setAlignment(Align.left);;
            }
        });

        if(hasDesc){
            cont.table(t -> {
                t.table(Styles.grayPanel, desc -> {
                    desc.margin(15f);
                    desc.pane(descPane -> {
                        descPane.labelWrap(map.description()).grow().top().left().get().setAlignment(Align.topLeft);
                    }).grow().padBottom(15f);
                    desc.row();
                    desc.label(() -> Core.bundle.get("editor.author") + " " + map.author()).left().growX();
                }).top().left().grow().padBottom(15f);
                t.row();

                t.add(buttonsTable).growX();
            }).fillY().width(Core.graphics.getWidth() / 4f * 1.5f).padLeft(10f);
        }else{
            cont.row();
            cont.add(buttonsTable).fillX().padTop(10f);
        }
    }

    private void displayGameModeHelp(){
        BaseDialog d = new BaseDialog(Core.bundle.get("mode.help.title"));
        d.setFillParent(false);
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);
        table.row();
        for(Gamemode mode : Gamemode.values()){
            if(mode.hidden) continue;
            table.labelWrap("[accent]" + mode + ":[] [lightgray]" + mode.description()).width(400f);
            table.row();
        }

        d.cont.add(pane);
        d.buttons.button("@ok", d::hide).size(110, 50).pad(10f);
        d.show();
    }
}
