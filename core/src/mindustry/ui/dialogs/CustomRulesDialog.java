package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.ui.*;
import mindustry.world.*;

import static arc.util.Time.*;
import static mindustry.Vars.*;

public class CustomRulesDialog extends BaseDialog{
    private Table main;
    Rules rules;
    private Prov<Rules> resetter;
    private LoadoutDialog loadoutDialog;
    private BaseDialog banDialog;

    public CustomRulesDialog(){
        super("@mode.custom");

        loadoutDialog = new LoadoutDialog();
        banDialog = new BaseDialog("@bannedblocks");
        banDialog.addCloseButton();

        banDialog.shown(this::rebuildBanned);
        banDialog.buttons.button("@addall", Icon.add, () -> {
            rules.bannedBlocks.addAll(content.blocks().select(Block::canBeBuilt));
            rebuildBanned();
        }).size(180, 64f);

        banDialog.buttons.button("@clear", Icon.trash, () -> {
            rules.bannedBlocks.clear();
            rebuildBanned();
        }).size(180, 64f);

        setFillParent(true);
        shown(this::setup);
        addCloseButton();
    }

    private void rebuildBanned(){
        float previousScroll = banDialog.cont.getChildren().isEmpty() ? 0f : ((ScrollPane)banDialog.cont.getChildren().first()).getScrollY();
        banDialog.cont.clear();
        banDialog.cont.pane(t -> {
            t.margin(10f);

            if(rules.bannedBlocks.isEmpty()){
                t.add("@empty");
            }

            Seq<Block> array = Seq.with(rules.bannedBlocks);
            array.sort();

            int cols = mobile && Core.graphics.isPortrait() ? 1 : mobile ? 2 : 3;
            int i = 0;

            for(Block block : array){
                t.table(Tex.underline, b -> {
                    b.left().margin(4f);
                    b.image(block.icon(Cicon.medium)).size(Cicon.medium.size).padRight(3);
                    b.add(block.localizedName).color(Color.lightGray).padLeft(3).growX().left().wrap();

                    b.button(Icon.cancel, Styles.clearPartiali, () -> {
                       rules.bannedBlocks.remove(block);
                       rebuildBanned();
                    }).size(70f).pad(-4f).padLeft(0f);
                }).size(300f, 70f).padRight(5);

                if(++i % cols == 0){
                    t.row();
                }
            }
        }).get().setScrollYForce(previousScroll);
        banDialog.cont.row();
        banDialog.cont.button("@add", Icon.add, () -> {
            BaseDialog dialog = new BaseDialog("@add");
            dialog.cont.pane(t -> {
                t.left().margin(14f);
                int[] i = {0};
                content.blocks().each(b -> !rules.bannedBlocks.contains(b) && b.canBeBuilt(), b -> {
                    int cols = mobile && Core.graphics.isPortrait() ? 4 : 12;
                    t.button(new TextureRegionDrawable(b.icon(Cicon.medium)), Styles.cleari, () -> {
                        rules.bannedBlocks.add(b);
                        rebuildBanned();
                        dialog.hide();
                    }).size(60f).get().resizeImage(Cicon.medium.size);

                    if(++i[0] % cols == 0){
                        t.row();
                    }
                });
            });

            dialog.addCloseButton();
            dialog.show();
        }).size(300f, 64f);
    }

    public void show(Rules rules, Prov<Rules> resetter){
        this.rules = rules;
        this.resetter = resetter;
        show();
    }

    void setup(){
        cont.clear();
        cont.pane(m -> main = m).get().setScrollingDisabled(true, false);
        main.margin(10f);
        main.button("@settings.reset", () -> {
            rules = resetter.get();
            setup();
            requestKeyboard();
            requestScroll();
        }).size(300f, 50f);
        main.left().defaults().fillX().left().pad(5);
        main.row();

        title("@rules.title.waves");
        check("@rules.waves", b -> rules.waves = b, () -> rules.waves);
        check("@rules.wavetimer", b -> rules.waveTimer = b, () -> rules.waveTimer);
        check("@rules.waitForWaveToEnd", b -> rules.waitEnemies = b, () -> rules.waitEnemies);
        number("@rules.wavespacing", false, f -> rules.waveSpacing = f * 60f, () -> rules.waveSpacing / 60f, () -> true);
        number("@rules.dropzoneradius", false, f -> rules.dropZoneRadius = f * tilesize, () -> rules.dropZoneRadius / tilesize, () -> true);

        title("@rules.title.resourcesbuilding");
        check("@rules.infiniteresources", b -> rules.infiniteResources = b, () -> rules.infiniteResources);
        check("@rules.reactorexplosions", b -> rules.reactorExplosions = b, () -> rules.reactorExplosions);
        number("@rules.buildcostmultiplier", false, f -> rules.buildCostMultiplier = f, () -> rules.buildCostMultiplier, () -> !rules.infiniteResources);
        number("@rules.buildspeedmultiplier", f -> rules.buildSpeedMultiplier = f, () -> rules.buildSpeedMultiplier);
        number("@rules.deconstructrefundmultiplier", false, f -> rules.deconstructRefundMultiplier = f, () -> rules.deconstructRefundMultiplier, () -> !rules.infiniteResources);
        number("@rules.blockhealthmultiplier", f -> rules.blockHealthMultiplier = f, () -> rules.blockHealthMultiplier);
        number("@rules.blockdamagemultiplier", f -> rules.blockDamageMultiplier = f, () -> rules.blockDamageMultiplier);

        main.button("@configure",
            () -> loadoutDialog.show(Blocks.coreShard.itemCapacity, rules.loadout,
                i -> true,
                () -> rules.loadout.clear().add(new ItemStack(Items.copper, 100)),
                () -> {}, () -> {}
        )).left().width(300f);
        main.row();

        main.button("@bannedblocks", banDialog::show).left().width(300f);
        main.row();

        title("@rules.title.unit");
        check("@rules.unitammo", b -> rules.unitAmmo = b, () -> rules.unitAmmo);
        number("@rules.unithealthmultiplier", f -> rules.unitHealthMultiplier = f, () -> rules.unitHealthMultiplier);
        number("@rules.unitdamagemultiplier", f -> rules.unitDamageMultiplier = f, () -> rules.unitDamageMultiplier);
        number("@rules.unitbuildspeedmultiplier", f -> rules.unitBuildSpeedMultiplier = f, () -> rules.unitBuildSpeedMultiplier);

        title("@rules.title.enemy");
        check("@rules.attack", b -> rules.attackMode = b, () -> rules.attackMode);
        check("@rules.buildai", b -> rules.waveTeam.rules().ai = b, () -> rules.waveTeam.rules().ai);
        number("@rules.enemycorebuildradius", f -> rules.enemyCoreBuildRadius = f * tilesize, () -> Math.min(rules.enemyCoreBuildRadius / tilesize, 200));

        title("@rules.title.environment");
        check("@rules.explosions", b -> rules.damageExplosions = b, () -> rules.damageExplosions);
        check("@rules.fire", b -> rules.fire = b, () -> rules.fire);
        check("@rules.lighting", b -> rules.lighting = b, () -> rules.lighting);

        main.button(b -> {
            b.left();
            b.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(rules.ambientLight));
                }}).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@rules.ambientlight");
        }, () -> ui.picker.show(rules.ambientLight, rules.ambientLight::set)).left().width(250f).row();

        main.button("@rules.weather", this::weatherDialog).width(250f).left().row();

        //TODO add weather patterns
    }

    void number(String text, Floatc cons, Floatp prov){
        number(text, false, cons, prov, () -> true);
    }

    void number(String text, boolean integer, Floatc cons, Floatp prov, Boolp condition){
        main.table(t -> {
            t.left();
            t.add(text).left().padRight(5)
            .update(a -> a.setColor(condition.get() ? Color.white : Color.gray));
            t.field((integer ? (int)prov.get() : prov.get()) + "", s -> cons.get(Strings.parseFloat(s)))
            .padRight(100f)
            .update(a -> a.setDisabled(!condition.get()))
            .valid(Strings::canParsePositiveFloat).width(120f).left().addInputDialog();
        }).padTop(0);
        main.row();
    }

    void check(String text, Boolc cons, Boolp prov){
        check(text, cons, prov, () -> true);
    }

    void check(String text, Boolc cons, Boolp prov, Boolp condition){
        main.check(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get())).padRight(100f).get().left();
        main.row();
    }

    void title(String text){
        main.add(text).color(Pal.accent).padTop(20).padRight(100f).padBottom(-3);
        main.row();
        main.image().color(Pal.accent).height(3f).padRight(100f).padBottom(20);
        main.row();
    }

    Cell<TextField> field(Table table, float value, Floatc setter){
        return table.field(Strings.autoFixed(value, 2), v -> setter.get(Strings.parseFloat(v)))
            .valid(Strings::canParsePositiveFloat)
            .size(90f, 40f).pad(2f).addInputDialog();
    }

    void weatherDialog(){
        BaseDialog dialog = new BaseDialog("@rules.weather");
        Runnable[] rebuild = {null};

        dialog.cont.pane(base -> {

            rebuild[0] = () -> {
                base.clearChildren();
                int cols = Math.max(1, Core.graphics.getWidth() / 460);
                int idx = 0;

                for(WeatherEntry entry : rules.weather){
                    base.top();
                    //main container
                    base.table(Tex.pane, c -> {
                        c.margin(0);

                        //icons to perform actions
                        c.table(Tex.whiteui, t -> {
                            t.setColor(Pal.gray);

                            t.top().left();
                            t.add(entry.weather.localizedName).left().padLeft(6);

                            t.add().growX();

                            ImageButtonStyle style = Styles.geni;
                            t.defaults().size(42f);

                            t.button(Icon.cancel, style, () -> {
                                rules.weather.remove(entry);
                                rebuild[0].run();
                            });
                        }).growX();

                        c.row();

                        //all the options
                        c.table(f -> {
                            f.marginLeft(4);
                            f.left().top();

                            f.defaults().padRight(4).left();

                            f.add("@rules.weather.duration");
                            field(f, entry.minDuration / toMinutes, v -> entry.minDuration = v * toMinutes);
                            f.add("@waves.to");
                            field(f, entry.maxDuration / toMinutes, v -> entry.maxDuration = v * toMinutes);
                            f.add("@unit.minutes");

                            f.row();

                            f.add("@rules.weather.frequency");
                            field(f, entry.minFrequency / toMinutes, v -> entry.minFrequency = v * toMinutes);
                            f.add("@waves.to");
                            field(f, entry.maxFrequency / toMinutes, v -> entry.maxFrequency = v * toMinutes);
                            f.add("@unit.minutes");

                            //intensity can't currently be customized

                        }).grow().left().pad(6).top();
                    }).width(410f).pad(3).top().left().fillY();

                    if(++idx % cols == 0){
                        base.row();
                    }
                }
            };

            rebuild[0].run();
        }).grow();

        dialog.addCloseButton();

        dialog.buttons.button("@add", Icon.add, () -> {
            BaseDialog addd = new BaseDialog("@add");
            addd.cont.pane(t -> {
                t.background(Tex.button);
                int i = 0;
                for(Weather weather : content.<Weather>getBy(ContentType.weather)){

                    t.button(weather.localizedName, Styles.cleart, () -> {
                        rules.weather.add(new WeatherEntry(weather));
                        rebuild[0].run();

                        addd.hide();
                    }).size(140f, 50f);
                    if(++i % 2 == 0) t.row();
                }
            });
            addd.addCloseButton();
            addd.show();
        }).width(170f);

        //reset cooldown to random number
        dialog.hidden(() -> {
            float sum = 0;
            Seq<WeatherEntry> sh = rules.weather.copy();
            sh.shuffle();

            for(WeatherEntry w : sh){
                //add the previous cooldowns to the sum so weather events are staggered and don't happen all at once.
                w.cooldown = sum + Mathf.random(w.minFrequency, w.maxFrequency);
                sum += w.cooldown;
            }
        });

        dialog.show();
    }
}
