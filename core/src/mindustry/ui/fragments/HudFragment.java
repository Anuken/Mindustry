package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.gen.Tex.*;

public class HudFragment{
    private static final float dsize = 65f, pauseHeight = 36f;

    public final PlacementFragment blockfrag = new PlacementFragment();
    public boolean shown = true;

    private ImageButton flip;
    private CoreItemsDisplay coreItems = new CoreItemsDisplay();

    private String hudText = "";
    private boolean showHudText;

    private Table lastUnlockTable;
    private Table lastUnlockLayout;
    private long lastToast;

    public void build(Group parent){

        //warn about guardian/boss waves
        Events.on(WaveEvent.class, e -> {
            int max = 10;
            int winWave = state.isCampaign() && state.rules.winWave > 0 ? state.rules.winWave : Integer.MAX_VALUE;
            outer:
            for(int i = state.wave - 1; i <= Math.min(state.wave + max, winWave - 2); i++){
                for(SpawnGroup group : state.rules.spawns){
                    if(group.effect == StatusEffects.boss && group.getSpawned(i) > 0){
                        int diff = (i + 2) - state.wave;

                        //increments at which to warn about incoming guardian
                        if(diff == 1 || diff == 2 || diff == 5 || diff == 10){
                            showToast(Icon.warning, group.type.emoji() + " " + Core.bundle.format("wave.guardianwarn" + (diff == 1 ? ".one" : ""), diff));
                        }

                        break outer;
                    }
                }
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            showToast(Core.bundle.format("sector.captured", e.sector.isBeingPlayed() ? "" : e.sector.name() + " "));
        });

        Events.on(SectorLoseEvent.class, e -> {
            showToast(Icon.warning, Core.bundle.format("sector.lost", e.sector.name()));
        });

        Events.on(SectorInvasionEvent.class, e -> {
            showToast(Icon.warning, Core.bundle.format("sector.attacked", e.sector.name()));
        });

        Events.on(ResetEvent.class, e -> {
            coreItems.resetUsed();
            coreItems.clear();
        });

        //paused table
        parent.fill(t -> {
            t.name = "paused";
            t.top().visible(() -> state.isPaused() && shown && !netServer.isWaitingForPlayers()).touchable = Touchable.disabled;
            t.table(Styles.black6, top -> top.label(() -> state.gameOver && state.isCampaign() ? "@sector.curlost" : "@paused")
                .style(Styles.outlineLabel).pad(8f)).height(pauseHeight).growX();
            //.padLeft(dsize * 5 + 4f) to prevent alpha overlap on left
        });

        //"waiting for players"
        parent.fill(t -> {
            t.name = "waiting";
            t.visible(() -> netServer.isWaitingForPlayers() && state.isPaused() && shown).touchable = Touchable.disabled;
            t.table(Styles.black6, top -> top.add("@waiting.players").style(Styles.outlineLabel).pad(18f));
        });

        //minimap + position
        parent.fill(t -> {
            t.name = "minimap/position";
            t.visible(() -> Core.settings.getBool("minimap") && shown);
            //minimap
            t.add(new Minimap()).name("minimap");
            t.row();
            //position
            t.label(() ->
                (Core.settings.getBool("position") ? player.tileX() + "," + player.tileY() + "\n" : "") +
                (Core.settings.getBool("mouseposition") ? "[lightgray]" + World.toTile(Core.input.mouseWorldX()) + "," + World.toTile(Core.input.mouseWorldY()) : ""))
            .visible(() -> Core.settings.getBool("position") || Core.settings.getBool("mouseposition"))
            .touchable(Touchable.disabled)
            .style(Styles.outlineLabel)
            .name("position");
            t.top().right();
        });

        ui.hints.build(parent);

        //menu at top left
        parent.fill(cont -> {
            cont.name = "overlaymarker";
            cont.top().left();

            if(mobile){
                //for better inset visuals
                cont.rect((x, y, w, h) -> {
                    if(Core.scene.marginTop > 0){
                        Tex.paneRight.draw(x, y, w, Core.scene.marginTop);
                    }
                }).fillX().row();

                cont.table(select -> {
                    select.name = "mobile buttons";
                    select.left();
                    select.defaults().size(dsize).left();

                    ImageButtonStyle style = Styles.cleari;

                    select.button(Icon.menu, style, ui.paused::show).name("menu");
                    flip = select.button(Icon.upOpen, style, this::toggleMenus).get();
                    flip.name = "flip";

                    select.button(Icon.paste, style, ui.schematics::show)
                    .name("schematics");

                    select.button(Icon.pause, style, () -> {
                        if(net.active()){
                            ui.listfrag.toggle();
                        }else{
                            state.set(state.isPaused() ? State.playing : State.paused);
                        }
                    }).name("pause").update(i -> {
                        if(net.active()){
                            i.getStyle().imageUp = Icon.players;
                        }else{
                            i.setDisabled(false);
                            i.getStyle().imageUp = state.isPaused() ? Icon.play : Icon.pause;
                        }
                    });

                    select.button(Icon.chat, style,() -> {
                        if(net.active() && mobile){
                            if(ui.chatfrag.shown()){
                                ui.chatfrag.hide();
                            }else{
                                ui.chatfrag.toggle();
                            }
                        }else if(state.isCampaign()){
                            ui.research.show();
                        }else{
                            ui.database.show();
                        }
                    }).name("chat").update(i -> {
                        if(net.active() && mobile){
                            i.getStyle().imageUp = Icon.chat;
                        }else if(state.isCampaign()){
                            i.getStyle().imageUp = Icon.tree;
                        }else{
                            i.getStyle().imageUp = Icon.book;
                        }
                    });

                    select.image().color(Pal.gray).width(4f).fillY();
                });

                cont.row();
                cont.image().height(4f).color(Pal.gray).fillX();
                cont.row();
            }

            cont.update(() -> {
                if(Core.input.keyTap(Binding.toggle_menus) && !ui.chatfrag.shown() && !Core.scene.hasDialog() && !Core.scene.hasField()){
                    Core.settings.getBoolOnce("ui-hidden", () -> {
                        ui.announce(Core.bundle.format("showui",  Core.keybinds.get(Binding.toggle_menus).key.toString(), 11));
                    });
                    toggleMenus();
                }
            });

            Table wavesMain, editorMain;

            cont.stack(wavesMain = new Table(), editorMain = new Table()).height(wavesMain.getPrefHeight())
            .name("waves/editor");

            wavesMain.visible(() -> shown && !state.isEditor());
            wavesMain.top().left().name = "waves";

            wavesMain.table(s -> {
                //wave info button with text
                s.add(makeStatusTable()).grow().name("status");

                var rightStyle = new ImageButtonStyle(){{
                    over = buttonRightOver;
                    down = buttonRightDown;
                    up = buttonRight;
                    disabled = buttonRightDisabled;
                    imageDisabledColor = Color.clear;
                    imageUpColor = Color.white;
                }};

                //table with button to skip wave
                s.button(Icon.play, rightStyle, 30f, () -> {
                    if(net.client() && player.admin){
                        Call.adminRequest(player, AdminAction.wave);
                    }else{
                        logic.skipWave();
                    }
                }).growY().fillX().right().width(40f).disabled(b -> !canSkipWave()).name("skip").get().toBack();
            }).width(dsize * 5 + 4f).name("statustable");

            wavesMain.row();

            addInfoTable(wavesMain.table().width(dsize * 5f + 4f).left().get());

            editorMain.name = "editor";
            editorMain.table(Tex.buttonEdge4, t -> {
                //t.margin(0f);
                t.name = "teams";
                t.add("@editor.teams").growX().left();
                t.row();
                t.table(teams -> {
                    teams.left();
                    int i = 0;
                    for(Team team : Team.baseTeams){
                        ImageButton button = teams.button(Tex.whiteui, Styles.clearNoneTogglei, 40f, () -> Call.setPlayerTeamEditor(player, team))
                        .size(50f).margin(6f).get();
                        button.getImageCell().grow();
                        button.getStyle().imageUpColor = team.color;
                        button.update(() -> button.setChecked(player.team() == team));

                        if(++i % 3 == 0){
                            teams.row();
                        }
                    }
                }).left();
            }).width(dsize * 5 + 4f);
            editorMain.visible(() -> shown && state.isEditor());

            //fps display
            cont.table(info -> {
                info.name = "fps/ping";
                info.touchable = Touchable.disabled;
                info.top().left().margin(4).visible(() -> Core.settings.getBool("fps") && shown);
                IntFormat fps = new IntFormat("fps");
                IntFormat ping = new IntFormat("ping");
                IntFormat tps = new IntFormat("tps");
                IntFormat mem = new IntFormat("memory");
                IntFormat memnative = new IntFormat("memory2");

                info.label(() -> fps.get(Core.graphics.getFramesPerSecond())).left().style(Styles.outlineLabel).name("fps");
                info.row();

                if(android){
                    info.label(() -> memnative.get((int)(Core.app.getJavaHeap() / 1024 / 1024), (int)(Core.app.getNativeHeap() / 1024 / 1024))).left().style(Styles.outlineLabel).name("memory2");
                }else{
                    info.label(() -> mem.get((int)(Core.app.getJavaHeap() / 1024 / 1024))).left().style(Styles.outlineLabel).name("memory");
                }
                info.row();

                info.label(() -> ping.get(netClient.getPing())).visible(net::client).left().style(Styles.outlineLabel).name("ping").row();
                info.label(() -> tps.get(state.serverTps == -1 ? 60 : state.serverTps)).visible(net::client).left().style(Styles.outlineLabel).name("tps").row();

            }).top().left();
        });

        //core info
        parent.fill(t -> {
            t.top();
            t.visible(() -> shown);

            t.name = "coreinfo";

            t.collapser(v -> v.add().height(pauseHeight), () -> state.isPaused() && !netServer.isWaitingForPlayers()).row();

            t.table(c -> {
                //core items
                c.top().collapser(coreItems, () -> Core.settings.getBool("coreitems") && !mobile && shown).fillX().row();

                float notifDuration = 240f;
                float[] coreAttackTime = {0};

                Events.run(Trigger.teamCoreDamage, () -> coreAttackTime[0] = notifDuration);

                //'core is under attack' table
                c.collapser(top -> top.background(Styles.black6).add("@coreattack").pad(8)
                .update(label -> label.color.set(Color.orange).lerp(Color.scarlet, Mathf.absin(Time.time, 2f, 1f))), true,
                () -> {
                    if(!shown || state.isPaused()) return false;
                    if(state.isMenu() || !player.team().data().hasCore()){
                        coreAttackTime[0] = 0f;
                        return false;
                    }

                    return (coreAttackTime[0] -= Time.delta) > 0;
                })
                .touchable(Touchable.disabled)
                .fillX().row();
            }).row();

            var bossb = new StringBuilder();
            var bossText = Core.bundle.get("guardian");
            int maxBosses = 6;

            t.table(v -> v.margin(10f)
            .add(new Bar(() -> {
                bossb.setLength(0);
                for(int i = 0; i < Math.min(state.teams.bosses.size, maxBosses); i++){
                    bossb.append(state.teams.bosses.get(i).type.emoji());
                }
                if(state.teams.bosses.size > maxBosses){
                    bossb.append("[accent]+[]");
                }
                bossb.append(" ");
                bossb.append(bossText);
                return bossb;
            }, () -> Pal.health, () -> {
                if(state.boss() == null) return 0f;
                float max = 0f, val = 0f;
                for(var boss : state.teams.bosses){
                    max += boss.maxHealth;
                    val += boss.health;
                }
                return max == 0f ? 0f : val / max;
            }).blink(Color.white).outline(new Color(0, 0, 0, 0.6f), 7f)).grow())
            .fillX().width(320f).height(60f).name("boss").visible(() -> state.rules.waves && state.boss() != null && !(mobile && Core.graphics.isPortrait())).padTop(7).row();

            t.table(Styles.black3, p -> p.margin(4).label(() -> hudText).style(Styles.outlineLabel)).touchable(Touchable.disabled).with(p -> p.visible(() -> {
                p.color.a = Mathf.lerpDelta(p.color.a, Mathf.num(showHudText), 0.2f);
                if(state.isMenu()){
                    p.color.a = 0f;
                    showHudText = false;
                }

                return p.color.a >= 0.001f;
            }));
        });

        //spawner warning
        parent.fill(t -> {
            t.name = "nearpoint";
            t.touchable = Touchable.disabled;
            t.table(Styles.black6, c -> c.add("@nearpoint")
            .update(l -> l.setColor(Tmp.c1.set(Color.white).lerp(Color.scarlet, Mathf.absin(Time.time, 10f, 1f))))
            .labelAlign(Align.center, Align.center))
            .margin(6).update(u -> u.color.a = Mathf.lerpDelta(u.color.a, Mathf.num(spawner.playerNear()), 0.1f)).get().color.a = 0f;
        });

        //'saving' indicator
        parent.fill(t -> {
            t.name = "saving";
            t.bottom().visible(() -> control.saves.isSaving());
            t.add("@saving").style(Styles.outlineLabel);
        });

        //TODO DEBUG: rate table
        if(false)
            parent.fill(t -> {
                t.bottom().left();
                t.table(Styles.black6, c -> {
                    Bits used = new Bits(content.items().size);

                    Runnable rebuild = () -> {
                        c.clearChildren();

                        for(Item item : content.items()){
                            if(state.rules.sector != null && state.rules.sector.info.getExport(item) >= 1){
                                c.image(item.uiIcon);
                                c.label(() -> (int)state.rules.sector.info.getExport(item) + " /s").color(Color.lightGray);
                                c.row();
                            }
                        }
                    };

                    c.update(() -> {
                        boolean wrong = false;
                        for(Item item : content.items()){
                            boolean has = state.rules.sector != null && state.rules.sector.info.getExport(item) >= 1;
                            if(used.get(item.id) != has){
                                used.set(item.id, has);
                                wrong = true;
                            }
                        }
                        if(wrong){
                            rebuild.run();
                        }
                    });
                }).visible(() -> state.isCampaign() && content.items().contains(i -> state.rules.sector != null && state.rules.sector.info.getExport(i) > 0));
            });

        blockfrag.build(parent);
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.both)
    public static void setPlayerTeamEditor(Player player, Team team){
        if(state.isEditor() && player != null){
            player.team(team);
        }
    }

    public void setHudText(String text){
        showHudText = true;
        hudText = text;
    }

    public void toggleHudText(boolean shown){
        showHudText = shown;
    }

    private void scheduleToast(Runnable run){
        long duration = (int)(3.5 * 1000);
        long since = Time.timeSinceMillis(lastToast);
        if(since > duration){
            lastToast = Time.millis();
            run.run();
        }else{
            Time.runTask((duration - since) / 1000f * 60f, run);
            lastToast += duration;
        }
    }

    public boolean hasToast(){
        return Time.timeSinceMillis(lastToast) < 3.5f * 1000f;
    }

    public void showToast(String text){
        showToast(Icon.ok, text);
    }

    public void showToast(Drawable icon, String text){
        showToast(icon, -1, text);
    }

    public void showToast(Drawable icon, float size, String text){
        if(state.isMenu()) return;

        scheduleToast(() -> {
            Sounds.message.play();

            Table table = new Table(Tex.button);
            table.update(() -> {
                if(state.isMenu() || !ui.hudfrag.shown){
                    table.remove();
                }
            });
            table.margin(12);
            var cell = table.image(icon).pad(3);
            if(size > 0) cell.size(size);
            table.add(text).wrap().width(280f).get().setAlignment(Align.center, Align.center);
            table.pack();

            //create container table which will align and move
            Table container = Core.scene.table();
            container.top().add(table);
            container.setTranslation(0, table.getPrefHeight());
            container.actions(Actions.translateBy(0, -table.getPrefHeight(), 1f, Interp.fade), Actions.delay(2.5f),
            //nesting actions() calls is necessary so the right prefHeight() is used
            Actions.run(() -> container.actions(Actions.translateBy(0, table.getPrefHeight(), 1f, Interp.fade), Actions.remove())));
        });
    }

    /** Show unlock notification for a new recipe. */
    public void showUnlock(UnlockableContent content){
        //some content may not have icons... yet
        //also don't play in the tutorial to prevent confusion
        if(state.isMenu()) return;

        Sounds.message.play();

        //if there's currently no unlock notification...
        if(lastUnlockTable == null){
            scheduleToast(() -> {
                Table table = new Table(Tex.button);
                table.update(() -> {
                    if(state.isMenu()){
                        table.remove();
                        lastUnlockLayout = null;
                        lastUnlockTable = null;
                    }
                });
                table.margin(12);

                Table in = new Table();

                //create texture stack for displaying
                Image image = new Image(content.uiIcon);
                image.setScaling(Scaling.fit);

                in.add(image).size(8 * 6).pad(2);

                //add to table
                table.add(in).padRight(8);
                table.add("@unlocked");
                table.pack();

                //create container table which will align and move
                Table container = Core.scene.table();
                container.top().add(table);
                container.setTranslation(0, table.getPrefHeight());
                container.actions(Actions.translateBy(0, -table.getPrefHeight(), 1f, Interp.fade), Actions.delay(2.5f),
                //nesting actions() calls is necessary so the right prefHeight() is used
                Actions.run(() -> container.actions(Actions.translateBy(0, table.getPrefHeight(), 1f, Interp.fade), Actions.run(() -> {
                    lastUnlockTable = null;
                    lastUnlockLayout = null;
                }), Actions.remove())));

                lastUnlockTable = container;
                lastUnlockLayout = in;
            });
        }else{
            //max column size
            int col = 3;
            //max amount of elements minus extra 'plus'
            int cap = col * col - 1;

            //get old elements
            Seq<Element> elements = new Seq<>(lastUnlockLayout.getChildren());
            int esize = elements.size;

            //...if it's already reached the cap, ignore everything
            if(esize > cap) return;

            //get size of each element
            float size = 48f / Math.min(elements.size + 1, col);

            lastUnlockLayout.clearChildren();
            lastUnlockLayout.defaults().size(size).pad(2);

            for(int i = 0; i < esize; i++){
                lastUnlockLayout.add(elements.get(i));

                if(i % col == col - 1){
                    lastUnlockLayout.row();
                }
            }

            //if there's space, add it
            if(esize < cap){

                Image image = new Image(content.uiIcon);
                image.setScaling(Scaling.fit);

                lastUnlockLayout.add(image);
            }else{ //else, add a specific icon to denote no more space
                lastUnlockLayout.image(Icon.add);
            }

            lastUnlockLayout.pack();
        }
    }

    public void showLaunch(){
        float margin = 30f;

        Image image = new Image();
        image.color.a = 0f;
        image.touchable = Touchable.disabled;
        image.setFillParent(true);
        image.actions(Actions.delay((coreLandDuration - margin) / 60f), Actions.fadeIn(margin / 60f, Interp.pow2In), Actions.delay(6f / 60f), Actions.remove());
        image.update(() -> {
            image.toFront();
            ui.loadfrag.toFront();
            if(state.isMenu()){
                image.remove();
            }
        });
        Core.scene.add(image);
    }

    public void showLand(){
        Image image = new Image();
        image.color.a = 1f;
        image.touchable = Touchable.disabled;
        image.setFillParent(true);
        image.actions(Actions.fadeOut(35f / 60f), Actions.remove());
        image.update(() -> {
            image.toFront();
            ui.loadfrag.toFront();
            if(state.isMenu()){
                image.remove();
            }
        });
        Core.scene.add(image);
    }

    private void toggleMenus(){
        if(flip != null){
            flip.getStyle().imageUp = shown ? Icon.downOpen : Icon.upOpen;
        }

        shown = !shown;
    }

    private Table makeStatusTable(){
        Table table = new Table(Tex.wavepane);

        StringBuilder ibuild = new StringBuilder();

        IntFormat
        wavef = new IntFormat("wave"),
        wavefc = new IntFormat("wave.cap"),
        enemyf = new IntFormat("wave.enemy"),
        enemiesf = new IntFormat("wave.enemies"),
        enemycf = new IntFormat("wave.enemycore"),
        enemycsf = new IntFormat("wave.enemycores"),
        waitingf = new IntFormat("wave.waiting", i -> {
            ibuild.setLength(0);
            int m = i/60;
            int s = i % 60;
            if(m > 0){
                ibuild.append(m);
                ibuild.append(":");
                if(s < 10){
                    ibuild.append("0");
                }
            }
            ibuild.append(s);
            return ibuild.toString();
        });

        table.touchable = Touchable.enabled;

        StringBuilder builder = new StringBuilder();

        table.name = "waves";

        table.marginTop(0).marginBottom(4).marginLeft(4);

        class SideBar extends Element{
            public final Floatp amount;
            public final boolean flip;
            public final Boolp flash;

            float last, blink, value;

            public SideBar(Floatp amount, Boolp flash, boolean flip){
                this.amount = amount;
                this.flip = flip;
                this.flash = flash;

                setColor(Pal.health);
            }

            @Override
            public void draw(){
                float next = amount.get();

                if(Float.isNaN(next) || Float.isInfinite(next)) next = 1f;

                if(next < last && flash.get()){
                    blink = 1f;
                }

                blink = Mathf.lerpDelta(blink, 0f, 0.2f);
                value = Mathf.lerpDelta(value, next, 0.15f);
                last = next;

                if(Float.isNaN(value) || Float.isInfinite(value)) value = 1f;

                drawInner(Pal.darkishGray, 1f);
                drawInner(Tmp.c1.set(color).lerp(Color.white, blink), value);
            }

            void drawInner(Color color, float fract){
                if(fract < 0) return;

                fract = Mathf.clamp(fract);
                if(flip){
                    x += width;
                    width = -width;
                }

                float stroke = width * 0.35f;
                float bh = height/2f;
                Draw.color(color, parentAlpha);

                float f1 = Math.min(fract * 2f, 1f), f2 = (fract - 0.5f) * 2f;

                float bo = -(1f - f1) * (width - stroke);

                Fill.quad(
                x, y,
                x + stroke, y,
                x + width + bo, y + bh * f1,
                x + width - stroke + bo, y + bh * f1
                );

                if(f2 > 0){
                    float bx = x + (width - stroke) * (1f - f2);
                    Fill.quad(
                    x + width, y + bh,
                    x + width - stroke, y + bh,
                    bx, y + height * fract,
                    bx + stroke, y + height * fract
                    );
                }

                Draw.reset();

                if(flip){
                    width = -width;
                    x -= width;
                }
            }
        }

        table.stack(
        new Element(){
            @Override
            public void draw(){
                Draw.color(Pal.darkerGray, parentAlpha);
                Fill.poly(x + width/2f, y + height/2f, 6, height / Mathf.sqrt3);
                Draw.reset();
                Drawf.shadow(x + width/2f, y + height/2f, height * 1.13f, parentAlpha);
            }
        },
        new Table(t -> {
            float bw = 40f;
            float pad = -20;
            t.margin(0);
            t.clicked(() -> {
                if(!player.dead() && mobile){
                    Call.unitClear(player);
                    control.input.recentRespawnTimer = 1f;
                    control.input.controlledType = null;
                }
            });

            t.add(new SideBar(() -> player.unit().healthf(), () -> true, true)).width(bw).growY().padRight(pad);
            t.image(() -> player.icon()).scaling(Scaling.bounded).grow().maxWidth(54f);
            t.add(new SideBar(() -> player.dead() ? 0f : player.displayAmmo() ? player.unit().ammof() : player.unit().healthf(), () -> !player.displayAmmo(), false)).width(bw).growY().padLeft(pad).update(b -> {
                b.color.set(player.displayAmmo() ? player.dead() || player.unit() instanceof BlockUnitc ? Pal.ammo : player.unit().type.ammoType.color() : Pal.health);
            });

            t.getChildren().get(1).toFront();
        })).size(120f, 80).padRight(4);

        Cell[] lcell = {null};
        boolean[] couldSkip = {true};

        lcell[0] = table.labelWrap(() -> {

            //update padding depend on whether the button to the right is there
            boolean can = canSkipWave();
            if(can != couldSkip[0]){
                if(canSkipWave()){
                    lcell[0].padRight(8f);
                }else{
                    lcell[0].padRight(-42f);
                }
                table.invalidateHierarchy();
                table.pack();
                couldSkip[0] = can;
            }

            builder.setLength(0);

            //mission overrides everything
            if(state.rules.mission != null && state.rules.mission.length() > 0){
                builder.append(state.rules.mission);
                return builder;
            }

            //objectives override mission?
            if(state.rules.objectives.any()){
                boolean first = true;
                for(var obj : state.rules.objectives){
                    if(!obj.qualified()) continue;

                    String text = obj.text();
                    if(text != null && !text.isEmpty()){
                        if(!first) builder.append("\n[white]");
                        builder.append(text);

                        first = false;
                    }
                }

                //TODO: display standard status when empty objective?
                if(builder.length() > 0){
                    return builder;
                }
            }

            if(!state.rules.waves && state.rules.attackMode){
                int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1);
                builder.append(sum > 1 ? enemycsf.get(sum) : enemycf.get(sum));
                return builder;
            }

            if(!state.rules.waves && state.isCampaign()){
                builder.append("[lightgray]").append(Core.bundle.get("sector.curcapture"));
            }

            if(!state.rules.waves){
                return builder;
            }

            if(state.rules.winWave > 1 && state.rules.winWave >= state.wave && state.isCampaign()){
                builder.append(wavefc.get(state.wave, state.rules.winWave));
            }else{
                builder.append(wavef.get(state.wave));
            }
            builder.append("\n");

            if(state.enemies > 0){
                if(state.enemies == 1){
                    builder.append(enemyf.get(state.enemies));
                }else{
                    builder.append(enemiesf.get(state.enemies));
                }
                builder.append("\n");
            }

            if(state.rules.waveTimer){
                builder.append((logic.isWaitingWave() ? Core.bundle.get("wave.waveInProgress") : (waitingf.get((int)(state.wavetime/60)))));
            }else if(state.enemies == 0){
                builder.append(Core.bundle.get("waiting"));
            }

            return builder;
        }).growX().pad(8f);

        table.row();

        //TODO nobody reads details anyway.
        /*
        table.clicked(() -> {
            if(state.rules.objectives.any()){
                StringBuilder text = new StringBuilder();

                boolean first = true;
                for(var obj : state.rules.objectives){
                    if(!obj.qualified()) continue;

                    String details = obj.details();
                    if(details != null){
                        if(!first) text.append('\n');
                        text.append(details);

                        first = false;
                    }
                }

                //TODO this, as said before, could be much better.
                ui.showInfo(text.toString());
            }
        });*/

        return table;
    }

    private void addInfoTable(Table table){
        table.name = "infotable";
        table.left();

        var count = new float[]{-1};
        table.table().update(t -> {
            if(player.unit() instanceof Payloadc payload){
                if(count[0] != payload.payloadUsed()){
                    payload.contentInfo(t, 8 * 2, 275f);
                    count[0] = payload.payloadUsed();
                }
            }else{
                count[0] = -1;
                t.clear();
            }
        }).growX().visible(() -> player.unit() instanceof Payloadc p && p.payloadUsed() > 0).colspan(2);
        table.row();

        Bits statuses = new Bits();

        table.table().update(t -> {
            t.left();
            Bits applied = player.unit().statusBits();
            if(!statuses.equals(applied)){
                t.clear();

                if(applied != null){
                    for(StatusEffect effect : content.statusEffects()){
                        if(applied.get(effect.id) && !effect.isHidden()){
                            t.image(effect.uiIcon).size(iconMed).get()
                            .addListener(new Tooltip(l -> l.label(() ->
                                effect.localizedName + " [lightgray]" + UI.formatTime(player.unit().getDuration(effect))).style(Styles.outlineLabel)));
                        }
                    }

                    statuses.set(applied);
                }
            }
        }).left();
    }

    private boolean canSkipWave(){
        return state.rules.waves && state.rules.waveSending && ((net.server() || player.admin) || !net.active()) && state.enemies == 0 && !spawner.isSpawning();
    }

}
