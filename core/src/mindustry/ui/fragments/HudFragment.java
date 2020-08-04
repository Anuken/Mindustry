package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.input.*;
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
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class HudFragment extends Fragment{
    private static final float dsize = 47.2f;

    public final PlacementFragment blockfrag = new PlacementFragment();

    private ImageButton flip;
    private Table lastUnlockTable;
    private Table lastUnlockLayout;
    private boolean shown = true;
    private CoreItemsDisplay coreItems = new CoreItemsDisplay();

    private String hudText = "";
    private boolean showHudText;

    private long lastToast;

    @Override
    public void build(Group parent){

        //TODO details and stuff
        Events.on(SectorCaptureEvent.class, e ->{
            //TODO localize
            showToast("Sector[accent] captured[]!");
        });

        //TODO localize
        Events.on(SectorLoseEvent.class, e -> {
            showToast(Icon.warning, "Sector " + e.sector.id + " [scarlet]lost!");
        });

        //TODO full implementation
        Events.on(ResetEvent.class, e -> {
            coreItems.resetUsed();
            coreItems.clear();
        });

        Events.on(TurnEvent.class, e -> {
            ui.announce("[accent][[ Turn " + universe.turn() + " ]");
        });

        //paused table
        parent.fill(t -> {
            t.top().visible(() -> state.isPaused() && !state.isOutOfTime()).touchable = Touchable.disabled;
            t.table(Styles.black5, top -> top.add("@paused").style(Styles.outlineLabel).pad(8f)).growX();
        });

        //TODO tear this all down
        //menu at top left
        parent.fill(cont -> {
            cont.name = "overlaymarker";
            cont.top().left();

            if(mobile){
                cont.table(select -> {
                    select.left();
                    select.defaults().size(dsize).left();

                    ImageButtonStyle style = Styles.clearTransi;

                    select.button(Icon.menu, style, ui.paused::show);
                    flip = select.button(Icon.upOpen, style, this::toggleMenus).get();

                    select.button(Icon.paste, style, ui.schematics::show);

                    select.button(Icon.pause, style, () -> {
                        if(net.active()){
                            ui.listfrag.toggle();
                        }else{
                            state.set(state.is(State.paused) ? State.playing : State.paused);
                        }
                    }).name("pause").update(i -> {
                        if(net.active()){
                            i.getStyle().imageUp = Icon.players;
                        }else{
                            i.setDisabled(false);
                            i.getStyle().imageUp = state.is(State.paused) ? Icon.play : Icon.pause;
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
                    }).update(i -> {
                        if(net.active() && mobile){
                            i.getStyle().imageUp = Icon.chat;
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
                if(Core.input.keyTap(Binding.toggle_menus) && !ui.chatfrag.shown() && !Core.scene.hasDialog() && !(Core.scene.getKeyboardFocus() instanceof TextField)){
                    toggleMenus();
                }
            });

            Table wavesMain, editorMain;

            cont.stack(wavesMain = new Table(), editorMain = new Table()).height(wavesMain.getPrefHeight());

            {
                wavesMain.visible(() -> shown && !state.isEditor());
                wavesMain.top().left();
                Stack stack = new Stack();
                Button waves = new Button(Styles.waveb);
                Table btable = new Table().margin(0);

                stack.add(waves);
                stack.add(btable);

                addWaveTable(waves);
                addPlayButton(btable);
                wavesMain.add(stack).width(dsize * 5 + 4f);
                wavesMain.row();
                wavesMain.table(Tex.button, t -> t.margin(10f).add(new Bar("boss.health", Pal.health, () -> state.boss() == null ? 0f : state.boss().healthf()).blink(Color.white))
                .grow()).fillX().visible(() -> state.rules.waves && state.boss() != null).height(60f).get();
                wavesMain.row();
            }

            {
                editorMain.table(Tex.buttonEdge4, t -> {
                    //t.margin(0f);
                    t.add("@editor.teams").growX().left();
                    t.row();
                    t.table(teams -> {
                        teams.left();
                        int i = 0;
                        for(Team team : Team.baseTeams){
                            ImageButton button = teams.button(Tex.whiteui, Styles.clearTogglePartiali, 40f, () -> Call.setPlayerTeamEditor(player, team))
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
            }

            //fps display
            cont.table(info -> {
                info.touchable = Touchable.disabled;
                info.top().left().margin(4).visible(() -> Core.settings.getBool("fps") && shown);
                info.update(() -> info.setTranslation(state.rules.waves || state.isEditor() ? 0f : -Scl.scl(dsize * 4 + 3), 0));
                IntFormat fps = new IntFormat("fps");
                IntFormat ping = new IntFormat("ping");

                info.label(() -> fps.get(Core.graphics.getFramesPerSecond())).left().style(Styles.outlineLabel);
                info.row();
                info.label(() -> ping.get(netClient.getPing())).visible(net::client).left().style(Styles.outlineLabel);
            }).top().left();
        });

        parent.fill(t -> {
            t.visible(() -> Core.settings.getBool("minimap") && !state.rules.tutorial && shown);
            //minimap
            t.add(new Minimap());
            t.row();
            //position
            t.label(() -> player.tileX() + "," + player.tileY())
                .visible(() -> Core.settings.getBool("position") && !state.rules.tutorial)
                .touchable(Touchable.disabled);
            t.top().right();
        });

        //core items
        parent.fill(t -> {
            t.top().add(coreItems);
            t.visible(() -> Core.settings.getBool("coreitems") && !mobile && !state.isPaused());
        });

        //spawner warning
        parent.fill(t -> {
            t.touchable = Touchable.disabled;
            t.table(Styles.black, c -> c.add("@nearpoint")
            .update(l -> l.setColor(Tmp.c1.set(Color.white).lerp(Color.scarlet, Mathf.absin(Time.time(), 10f, 1f))))
            .get().setAlignment(Align.center, Align.center))
            .margin(6).update(u -> u.color.a = Mathf.lerpDelta(u.color.a, Mathf.num(spawner.playerNear()), 0.1f)).get().color.a = 0f;
        });

        parent.fill(t -> {
            t.visible(() -> netServer.isWaitingForPlayers());
            t.table(Tex.button, c -> c.add("@waiting.players"));
        });

        //'core is under attack' table
        parent.fill(t -> {
            t.touchable = Touchable.disabled;
            float notifDuration = 240f;
            float[] coreAttackTime = {0};
            float[] coreAttackOpacity = {0};

            Events.on(Trigger.teamCoreDamage, () -> {
                coreAttackTime[0] = notifDuration;
            });

            t.top().visible(() -> {
                if(state.isMenu() || !state.teams.get(player.team()).hasCore()){
                    coreAttackTime[0] = 0f;
                    return false;
                }

                t.color.a = coreAttackOpacity[0];
                if(coreAttackTime[0] > 0){
                    coreAttackOpacity[0] = Mathf.lerpDelta(coreAttackOpacity[0], 1f, 0.1f);
                }else{
                    coreAttackOpacity[0] = Mathf.lerpDelta(coreAttackOpacity[0], 0f, 0.1f);
                }

                coreAttackTime[0] -= Time.delta;

                return coreAttackOpacity[0] > 0;
            });
            t.table(Tex.button, top -> top.add("@coreattack").pad(2)
            .update(label -> label.color.set(Color.orange).lerp(Color.scarlet, Mathf.absin(Time.time(), 2f, 1f)))).touchable(Touchable.disabled);
        });

        //paused table for when the player is out of time
        parent.fill(t -> {
            t.top().visible(() -> state.isOutOfTime());
            t.table(Styles.black5, top -> {
                //TODO localize when done
                top.add("Out of sector time.").style(Styles.outlineLabel).color(Pal.accent).update(l -> l.color.a = Mathf.absin(Time.globalTime(), 7f, 1f)).colspan(2);
                top.row();

                top.defaults().pad(2).size(150f, 54f);
                top.button("Next Turn", () -> {
                    universe.runTurn();
                    state.set(State.playing);
                });

                top.button("Back to Planet", () -> {
                    ui.paused.runExitSave();
                    ui.planet.show();
                });
            }).margin(8).growX();
        });

        //tutorial text
        parent.fill(t -> {
            Runnable resize = () -> {
                t.clearChildren();
                t.top().right().visible(() -> state.rules.tutorial);
                t.stack(new Button(){{
                    marginLeft(48f);
                    labelWrap(() -> control.tutorial.stage.text() + (control.tutorial.canNext() ? "\n\n" + Core.bundle.get("tutorial.next") : "")).width(!Core.graphics.isPortrait() ? 400f : 160f).pad(2f);
                    clicked(() -> control.tutorial.nextSentence());
                    setDisabled(() -> !control.tutorial.canNext());
                }},
                new Table(f -> {
                    f.left().button(Icon.left, Styles.emptyi, () -> {
                        control.tutorial.prevSentence();
                    }).width(44f).growY().visible(() -> control.tutorial.canPrev());
                }));
            };

            resize.run();
            Events.on(ResizeEvent.class, e -> resize.run());
        });

        //'saving' indicator
        parent.fill(t -> {
            t.bottom().visible(() -> control.saves.isSaving());
            t.add("@saving").style(Styles.outlineLabel);
        });

        parent.fill(p -> {
            p.top().table(Styles.black3, t -> t.margin(4).label(() -> hudText)
            .style(Styles.outlineLabel)).padTop(10).visible(p.color.a >= 0.001f);
            p.update(() -> {
                p.color.a = Mathf.lerpDelta(p.color.a, Mathf.num(showHudText), 0.2f);
                if(state.isMenu()){
                    p.color.a = 0f;
                    showHudText = false;
                }
            });
            p.touchable = Touchable.disabled;
        });

        //TODO DEBUG: rate table
        parent.fill(t -> {
            t.bottom().left();
            t.table(Styles.black6, c -> {
                Bits used = new Bits(content.items().size);

                Runnable rebuild = () -> {
                    c.clearChildren();

                    for(Item item : content.items()){
                        if(state.secinfo.getExport(item) >= 1){
                            c.image(item.icon(Cicon.small));
                            c.label(() -> (int)state.secinfo.getExport(item) + " /s").color(Color.lightGray);
                            c.row();
                        }
                    }
                };

                c.update(() -> {
                    boolean wrong = false;
                    for(Item item : content.items()){
                        boolean has = state.secinfo.getExport(item) >= 1;
                        if(used.get(item.id) != has){
                            used.set(item.id, has);
                            wrong = true;
                        }
                    }
                    if(wrong){
                        rebuild.run();
                    }
                });
            }).visible(() -> state.isCampaign() && content.items().contains(i -> state.secinfo.getExport(i) > 0));
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

    public void showToast(String text){
        showToast(Icon.ok, text);
    }

    public void showToast(Drawable icon, String text){
        if(state.isMenu()) return;

        scheduleToast(() -> {
            Sounds.message.play();

            Table table = new Table(Tex.button);
            table.update(() -> {
                if(state.isMenu()){
                    table.remove();
                }
            });
            table.margin(12);
            table.image(icon).pad(3);
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

    public boolean shown(){
        return shown;
    }

    /** Show unlock notification for a new recipe. */
    public void showUnlock(UnlockableContent content){
        //some content may not have icons... yet
        //also don't play in the tutorial to prevent confusion
        if(state.isMenu() || state.rules.tutorial) return;

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
                Image image = new Image(content.icon(Cicon.xlarge));
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

                Image image = new Image(content.icon(Cicon.medium));
                image.setScaling(Scaling.fit);

                lastUnlockLayout.add(image);
            }else{ //else, add a specific icon to denote no more space
                lastUnlockLayout.image(Icon.add);
            }

            lastUnlockLayout.pack();
        }
    }

    public void showLaunchDirect(){
        Image image = new Image();
        image.color.a = 0f;
        image.setFillParent(true);
        image.actions(Actions.fadeIn(launchDuration / 60f, Interp.pow2In), Actions.delay(8f / 60f), Actions.remove());
        Core.scene.add(image);
    }

    public void showLaunch(){
        Image image = new Image();
        image.color.a = 0f;
        image.setFillParent(true);
        image.actions(Actions.fadeIn(40f / 60f));
        image.update(() -> {
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
        image.actions(Actions.fadeOut(0.8f), Actions.remove());
        image.update(() -> {
            image.toFront();
            if(state.isMenu()){
                image.remove();
            }
        });
        Core.scene.add(image);
    }

    private void showLaunchConfirm(){
        BaseDialog dialog = new BaseDialog("@launch");
        dialog.update(() -> {
            if(!inLaunchWave()){
                dialog.hide();
            }
        });
        dialog.cont.add("@launch.confirm").width(500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        dialog.buttons.defaults().size(200f, 54f).pad(2f);
        dialog.setFillParent(false);
        dialog.buttons.button("@cancel", dialog::hide);
        dialog.buttons.button("@ok", () -> {
            dialog.hide();
            Call.launchZone();
        });
        dialog.keyDown(KeyCode.escape, dialog::hide);
        dialog.keyDown(KeyCode.back, dialog::hide);
        dialog.show();
    }

    //TODO launching is disabled, possibly forever
    private boolean inLaunchWave(){
        return false;
        /*
        return state.hasSector() &&
            state.getSector().metCondition() &&
            !net.client() &&
            state.wave % state.getSector().launchPeriod == 0 && !spawner.isSpawning();*/
    }

    private boolean canLaunch(){
        return inLaunchWave() && state.enemies <= 0;
    }

    private void toggleMenus(){
        if(flip != null){
            flip.getStyle().imageUp = shown ? Icon.downOpen : Icon.upOpen;
        }

        shown = !shown;
    }

    private void addWaveTable(Button table){
        StringBuilder ibuild = new StringBuilder();

        IntFormat wavef = new IntFormat("wave");
        IntFormat enemyf = new IntFormat("wave.enemy");
        IntFormat enemiesf = new IntFormat("wave.enemies");
        IntFormat waitingf = new IntFormat("wave.waiting", i -> {
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

        table.clearChildren();
        table.touchable = Touchable.enabled;

        StringBuilder builder = new StringBuilder();

        table.name = "waves";
        table.labelWrap(() -> {
            builder.setLength(0);
            builder.append(wavef.get(state.wave));
            builder.append("\n");

            if(inLaunchWave()){
                builder.append("[#");
                Tmp.c1.set(Color.white).lerp(state.enemies > 0 ? Color.white : Color.scarlet, Mathf.absin(Time.time(), 2f, 1f)).toString(builder);
                builder.append("]");

                if(!canLaunch()){
                    builder.append(Core.bundle.get("launch.unable2"));
                }else{
                    builder.append(Core.bundle.get("launch"));
                    builder.append("\n");
                    builder.append(Core.bundle.format("launch.next", state.wave + state.getSector().launchPeriod));
                    builder.append("\n");
                }
                builder.append("[]\n");
            }

            if(state.enemies > 0){
                if(state.enemies == 1){
                    builder.append(enemyf.get(state.enemies));
                }else{
                    builder.append(enemiesf.get(state.enemies));
                }
                builder.append("\n");
            }

            if(state.rules.waveTimer){
                builder.append((logic.isWaitingWave() ? Core.bundle.get("wave.waveInProgress") : ( waitingf.get((int)(state.wavetime/60)))));
            }else if(state.enemies == 0){
                builder.append(Core.bundle.get("waiting"));
            }

            return builder;
        }).growX().pad(8f);

        table.setDisabled(() -> !canLaunch());
        table.visible(() -> state.rules.waves);
        table.clicked(() -> {
            if(canLaunch()){
                showLaunchConfirm();
            }
        });
    }

    private boolean canSkipWave(){
        return state.rules.waves && ((net.server() || player.admin) || !net.active()) && state.enemies == 0 && !spawner.isSpawning() && !state.rules.tutorial;
    }

    private void addPlayButton(Table table){
        table.right().button(Icon.play, Styles.righti, 30f, () -> {
            if(net.client() && player.admin){
                Call.adminRequest(player, AdminAction.wave);
            }else if(inLaunchWave()){
                ui.showConfirm("@confirm", "@launch.skip.confirm", () -> !canSkipWave(), () -> logic.skipWave());
            }else{
                logic.skipWave();
            }
        }).growY().fillX().right().width(40f)
        .visible(this::canSkipWave);
    }
}
