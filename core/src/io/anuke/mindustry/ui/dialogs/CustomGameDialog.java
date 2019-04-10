package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Scaling;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.Gamemode;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class CustomGameDialog extends FloatingDialog{
    Difficulty difficulty = Difficulty.normal;
    CustomRulesDialog dialog = new CustomRulesDialog();
    Rules rules;
    Gamemode selectedGamemode;

    public CustomGameDialog(){
        super("$customgame");
        addCloseButton();
        selectedGamemode = Gamemode.survival;
        rules = selectedGamemode.get();
        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        cont.clear();

        Table maps = new Table();
        maps.marginRight(14);
        ScrollPane pane = new ScrollPane(maps);
        pane.setFadeScrollBars(false);

        int maxwidth = (Core.graphics.getHeight() > Core.graphics.getHeight() ? 2 : 4);

        Table selmode = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        selmode.add("$level.mode").colspan(4);
        selmode.row();
        int i = 0;

        Table modes = new Table();
        modes.marginBottom(5);

        for(Gamemode mode : Gamemode.values()){
            modes.addButton(mode.toString(), "toggle", () -> {
                selectedGamemode = mode;
                rules = mode.get();
            }).update(b -> b.setChecked(selectedGamemode == mode)).group(group).size(140f, 54f);
        }
        selmode.add(modes);
        selmode.addButton("?", this::displayGameModeHelp).size(50f, 54f).padLeft(18f);

        cont.add(selmode);
        cont.row();

        Difficulty[] ds = Difficulty.values();

        float s = 50f;

        Table sdif = new Table();

        sdif.add("$setting.difficulty.name").colspan(3);
        sdif.row();
        sdif.defaults().height(s + 4);
        sdif.addImageButton("icon-arrow-left", 10 * 3, () -> {
            difficulty = (ds[Mathf.mod(difficulty.ordinal() - 1, ds.length)]);
            state.wavetime = difficulty.waveTime;
        }).width(s);

        sdif.addButton("", () -> {
        })
        .update(t -> {
            t.setText(difficulty.toString());
            t.touchable(Touchable.disabled);
        }).width(180f);

        sdif.addImageButton("icon-arrow-right", 10 * 3, () -> {
            difficulty = (ds[Mathf.mod(difficulty.ordinal() + 1, ds.length)]);
            state.wavetime = difficulty.waveTime;
        }).width(s);

        cont.add(sdif);//.visible(() -> lastPreset != null);
        cont.row();

        cont.addButton("$rules.modifyRules", dialog::show).width(280).padTop(10);
        cont.row();

        float images = 146f;

        i = 0;
        maps.defaults().width(170).fillY().top().pad(4f);
        for(Map map : world.maps.all()){

            if(i % maxwidth == 0){
                maps.row();
            }

            ImageButton image = new ImageButton(new TextureRegion(map.texture), "clear");
            image.margin(5);
            image.getImageCell().size(images);
            image.top();
            image.row();
            image.add("[accent]" + map.name()).pad(3f).growX().wrap().get().setAlignment(Align.center, Align.center);
            image.row();
            image.label((() -> Core.bundle.format("level.highscore", map.getHightScore()))).pad(3f);

            BorderImage border = new BorderImage(map.texture, 3f);
            border.setScaling(Scaling.fit);
            image.replaceImage(border);

            image.clicked(() -> {
                hide();
                control.playMap(map, rules);
            });

            maps.add(image);

            i++;
        }

        if(world.maps.all().size == 0){
            maps.add("$maps.none").pad(50);
        }

        cont.add(pane).uniformX();
    }

    private void displayGameModeHelp(){
        FloatingDialog d = new FloatingDialog(Core.bundle.get("mode.help.title"));
        d.setFillParent(false);
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table);
        pane.setFadeScrollBars(false);
        table.row();
        for(Gamemode mode : Gamemode.values()){
            table.labelWrap("[accent]" + mode.toString() + ":[] [lightgray]" + mode.description()).width(400f);
            table.row();
        }

        d.cont.add(pane);
        d.buttons.addButton("$ok", d::hide).size(110, 50).pad(10f);
        d.show();
    }

    private class CustomRulesDialog extends FloatingDialog{
        private Table main;

        public CustomRulesDialog(){
            super("$mode.custom");

            setFillParent(true);
            shown(this::setup);
            addCloseButton();
        }

        void setup(){
            cont.clear();
            cont.pane(m -> main = m);
            main.margin(10f);
            main.setWidth(1000f);
            main.addButton("$rules.restoreDefault", () -> {rules = selectedGamemode.get(); setup();}).size(300f, 50f);
            main.left().defaults().fillX().left().pad(5);
            main.row();
            title("$rules.title.waves", Gamemode.survival);
            check("$rules.waves", b -> rules.waves = b, () -> rules.waves, ()->false, Gamemode.survival);
            check("$rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer, ()->rules.waves, Gamemode.survival);
            check("$rules.waitForWaveToEnd", b -> rules.waitForWaveToEnd = b, () -> rules.waitForWaveToEnd, ()->rules.waves, Gamemode.survival);
            number("$rules.wavespacing", f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f, ()->rules.waves, Gamemode.survival);
            title("$rules.title.respawns");
            check("$rules.limitedRespawns", b -> rules.limitedRespawns= b, () -> rules.limitedRespawns, ()->true);
            number("$rules.respawns", f -> rules.respawns = (int) f, () -> rules.respawns, ()->rules.limitedRespawns);
            number("$rules.respawntime", f -> rules.respawnTime = f * 60f, () -> rules.respawnTime / 60f, ()->true);
            title("$rules.title.resourcesbuilding", Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
            check("$rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources, ()->true, Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
            number("$rules.buildcostmultiplier", f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, ()->!rules.infiniteResources, Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
            number("$rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier, ()->true, Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
            title("$rules.title.player");
            number("$rules.playerdamagemultiplier", f -> rules.playerDamageMultiplier = f, () -> rules.playerDamageMultiplier, ()->true);
            number("$rules.playerhealthmultiplier", f -> rules.playerHealthMultiplier = f, () -> rules.playerHealthMultiplier, ()-> true);
            title("$rules.title.unit");
            check("$rules.unitdrops", b -> rules.unitDrops = b, () -> rules.unitDrops, ()->true);
            number("$rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier, ()->true);
            number("$rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier, ()->true);
            number("$rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier, ()->true);
            title("$rules.title.enemy", Gamemode.attack, Gamemode.pvp);
            number("$rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200), ()->true, Gamemode.attack, Gamemode.pvp);
        }

        void number(String text, FloatConsumer cons, FloatProvider prov, BooleanProvider condition){
            main.table(t -> {
                t.left();
                t.add(text).left().padRight(5)
                        .update(a->a.setColor(condition.get() ? Color.WHITE : Color.GRAY));
                Platform.instance.addDialog(t.addField(prov.get() + "", s -> cons.accept(Strings.parseFloat(s)))
                        .padRight(100f)
                        .update(a -> a.setDisabled(!condition.get()))
                        .valid(Strings::canParsePositiveFloat).width(120f) .left().get());
            }).padTop(0);
            main.row();
        }

        void number(String text, FloatConsumer cons, FloatProvider prov, BooleanProvider condition, Gamemode... gamemodes){
            if (Arrays.asList(gamemodes).contains(selectedGamemode)) {
                number(text, cons, prov, condition);
            }
        }

        void check(String text, BooleanConsumer cons, BooleanProvider prov, BooleanProvider condition) {
            main.addCheck(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f);
            main.row();
        }

        void check(String text, BooleanConsumer cons, BooleanProvider prov, BooleanProvider condition, Gamemode... gamemodes) {
            if(Arrays.asList(gamemodes).contains(selectedGamemode)) {
                check(text, cons, prov, condition);
            }
        }

        void title(String text, Gamemode... gamemodes) {
            if(Arrays.asList(gamemodes).contains(selectedGamemode)) {
                title(text);
            }
        }

        void title(String text) {
            main.add(text).color(Color.CORAL).fontScale(1.5f).padTop(40).padBottom(20).padRight(100f);
            main.row();
        }
    }
}