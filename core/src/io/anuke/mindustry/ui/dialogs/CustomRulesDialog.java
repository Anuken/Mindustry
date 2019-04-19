package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.function.BooleanConsumer;
import io.anuke.arc.function.BooleanProvider;
import io.anuke.arc.function.FloatConsumer;
import io.anuke.arc.function.FloatProvider;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.game.Gamemode;
import io.anuke.mindustry.game.Rules;
import io.anuke.mindustry.graphics.Pal;

import static io.anuke.mindustry.Vars.tilesize;

public class CustomRulesDialog extends FloatingDialog{
    private Table main;
    public Rules rules;
    public Gamemode selectedGamemode;

    public CustomRulesDialog(){
        super("$mode.custom");

        setFillParent(true);
        shown(this::setup);
        addCloseButton();
    }

    public void show(Rules rules, Gamemode gamemode){
        this.rules = rules;
        this.selectedGamemode = gamemode;
        show();
    }

    void setup(){
        cont.clear();
        cont.pane(m -> main = m);
        main.margin(10f);
        main.addButton("$settings.reset", () -> {rules = selectedGamemode.get(); setup();}).size(300f, 50f);
        main.left().defaults().fillX().left().pad(5);
        main.row();
        title("$rules.title.waves", Gamemode.survival, Gamemode.sandbox);
        check("$rules.waves", b -> rules.waves = b, () -> rules.waves, ()->selectedGamemode!=Gamemode.survival, Gamemode.survival, Gamemode.sandbox);
        check("$rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer, ()->rules.waves, Gamemode.survival, Gamemode.sandbox);
        check("$rules.waitForWaveToEnd", b -> rules.waitForWaveToEnd = b, () -> rules.waitForWaveToEnd, ()->rules.waves, Gamemode.survival, Gamemode.sandbox);
        f_number("$rules.wavespacing", f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f, ()->rules.waves, Gamemode.survival, Gamemode.sandbox);
        title("$rules.title.respawns");
        check("$rules.limitedRespawns", b -> rules.limitedRespawns= b, () -> rules.limitedRespawns, ()->true);
        i_number("$rules.respawns", f -> rules.respawns = (int) f, () -> rules.respawns, ()->rules.limitedRespawns);
        f_number("$rules.respawntime", f -> rules.respawnTime = f * 60f, () -> rules.respawnTime / 60f, ()->true);
        title("$rules.title.resourcesbuilding", Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
        check("$rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources, ()->true, Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
        f_number("$rules.buildcostmultiplier", f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, ()->!rules.infiniteResources, Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
        f_number("$rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier, ()->true, Gamemode.attack, Gamemode.pvp, Gamemode.survival, Gamemode.sandbox);
        title("$rules.title.player");
        f_number("$rules.playerdamagemultiplier", f -> rules.playerDamageMultiplier = f, () -> rules.playerDamageMultiplier, ()->true);
        f_number("$rules.playerhealthmultiplier", f -> rules.playerHealthMultiplier = f, () -> rules.playerHealthMultiplier, ()-> true);
        title("$rules.title.unit");
        check("$rules.unitdrops", b -> rules.unitDrops = b, () -> rules.unitDrops, ()->true);
        f_number("$rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier, ()->true);
        f_number("$rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier, ()->true);
        f_number("$rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier, ()->true);
        title("$rules.title.enemy", Gamemode.attack, Gamemode.pvp);
        f_number("$rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200), ()->true, Gamemode.attack, Gamemode.pvp);
    }

    void f_number(String text, FloatConsumer cons, FloatProvider prov, BooleanProvider condition){
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

    void f_number(String text, FloatConsumer cons, FloatProvider prov, BooleanProvider condition, Gamemode... gamemodes){
        if(Structs.contains(gamemodes, selectedGamemode)){
            f_number(text, cons, prov, condition);
        }
    }

    void i_number(String text, FloatConsumer cons, FloatProvider prov, BooleanProvider condition){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
                    .update(a->a.setColor(condition.get() ? Color.WHITE : Color.GRAY));
            Platform.instance.addDialog(t.addField(((int) prov.get()) + "", s -> cons.accept(Strings.parseFloat(s)))
                    .padRight(100f)
                    .update(a -> a.setDisabled(!condition.get()))
                    .valid(Strings::canParsePostiveInt).width(120f) .left().get());
        }).padTop(0);
        main.row();
    }

    void i_number(String text, FloatConsumer cons, FloatProvider prov, BooleanProvider condition, Gamemode... gamemodes){
        if(Structs.contains(gamemodes, selectedGamemode)){
            i_number(text, cons, prov, condition);
        }
    }

    void check(String text, BooleanConsumer cons, BooleanProvider prov, BooleanProvider condition){
        main.addCheck(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f);
        main.row();
    }

    void check(String text, BooleanConsumer cons, BooleanProvider prov, BooleanProvider condition, Gamemode... gamemodes){
        if(Structs.contains(gamemodes, selectedGamemode)){
            check(text, cons, prov, condition);
        }
    }

    void title(String text, Gamemode... gamemodes){
        if(Structs.contains(gamemodes, selectedGamemode)){
            title(text);
        }
    }

    void title(String text){
        main.add(text).color(Pal.accent).padTop(20).padBottom(20).padRight(100f);
        main.row();
    }
}