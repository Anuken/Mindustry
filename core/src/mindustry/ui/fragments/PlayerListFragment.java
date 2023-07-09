package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class PlayerListFragment{
    public Table content = new Table().marginRight(13f).marginLeft(13f);
    private boolean visible = false;
    private Interval timer = new Interval();
    private TextField search;
    private Seq<Player> players = new Seq<>();

    public void build(Group parent){
        content.name = "players";
        parent.fill(cont -> {
            cont.name = "playerlist";
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!(net.active() && state.isGame())){
                    visible = false;
                    return;
                }

                if(visible && timer.get(20)){
                    rebuild();
                    content.pack();
                    content.act(Core.graphics.getDeltaTime());
                    //hacky
                    Core.scene.act(0f);
                }
            });

            cont.table(Tex.buttonTrans, pane -> {
                pane.label(() -> Core.bundle.format(Groups.player.size() == 1 ? "players.single" : "players", Groups.player.size()));
                pane.row();

                search = pane.field(null, text -> rebuild()).grow().pad(8).name("search").maxTextLength(maxNameLength).get();
                search.setMessageText(Core.bundle.get("players.search"));

                pane.row();
                pane.pane(content).grow().scrollX(false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().growX().height(50f).fillY();
                    menu.name = "menu";

                    menu.button("@server.bans", ui.bans::show).disabled(b -> net.client());
                    menu.button("@server.admins", ui.admins::show).disabled(b -> net.client());
                    menu.button("@close", this::toggle);
                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f).minWidth(360f);
        });

        rebuild();
    }

    public void rebuild(){
        content.clear();

        float h = 50f;
        boolean found = false;

        players.clear();
        Groups.player.copy(players);

        players.sort(Structs.comps(Structs.comparing(Player::team), Structs.comparingBool(p -> !p.admin)));
        if(search.getText().length() > 0){
            players.retainAll(p -> Strings.stripColors(p.name().toLowerCase()).contains(search.getText().toLowerCase()));
        }

        for(var user : players){
            found = true;
            NetConnection connection = user.con;

            if(connection == null && net.server() && !user.isLocal()) return;

            Table button = new Table();
            button.left();
            button.margin(5).marginBottom(10);

            ClickListener listener = new ClickListener();

            Table iconTable = new Table(){
                @Override
                public void draw(){
                    super.draw();
                    Draw.colorMul(user.team().color, listener.isOver() ? 1.3f : 1f);
                    Draw.alpha(parentAlpha);
                    Lines.stroke(Scl.scl(4f));
                    Lines.rect(x, y, width, height);
                    Draw.reset();
                }
            };

            boolean clickable = !(state.rules.fog && state.rules.pvp && user.team() != player.team());

            if(clickable){
                iconTable.addListener(listener);
                iconTable.addListener(new HandCursorListener());
            }
            iconTable.margin(8);
            iconTable.add(new Image(user.icon()).setScaling(Scaling.bounded)).grow();
            iconTable.name = user.name();
            iconTable.touchable = Touchable.enabled;

            iconTable.tapped(() -> {
                if(!user.dead() && clickable){
                    Core.camera.position.set(user.unit());
                    ui.showInfoFade(Core.bundle.format("viewplayer", user.name), 1f);
                    if(control.input instanceof DesktopInput input){
                        input.panning = true;
                    }
                }
            });

            button.add(iconTable).size(h);
            button.labelWrap("[#" + user.color().toString().toUpperCase() + "]" + user.name()).style(Styles.outlineLabel).width(170f).pad(10);
            button.add().grow();

            button.background(Tex.underline);

            button.image(Icon.admin).visible(() -> user.admin && !(!user.isLocal() && net.server())).padRight(5).get().updateVisibility();

            var style = new ImageButtonStyle(){{
                down = Styles.none;
                up = Styles.none;
                imageCheckedColor = Pal.accent;
                imageDownColor = Pal.accent;
                imageUpColor = Color.white;
                imageOverColor = Color.lightGray;
            }};

            var ustyle = new ImageButtonStyle(){{
                down = Styles.none;
                up = Styles.none;
                imageDownColor = Pal.accent;
                imageUpColor = Color.white;
                imageOverColor = Color.lightGray;
            }};

            if(net.server() || (player.admin && (!user.admin || user == player))){
                button.add().growY();

                button.button(Icon.menu, ustyle, () -> {
                    var dialog = new BaseDialog(user.coloredName());

                    dialog.title.setColor(Color.white);
                    dialog.titleTable.remove();

                    dialog.closeOnBack();

                    var bstyle = Styles.defaultt;

                    dialog.cont.add(user.coloredName()).row();
                    dialog.cont.image(Tex.whiteui, Pal.accent).fillX().height(3f).pad(4f).row();

                    dialog.cont.pane(t -> {
                        t.defaults().size(220f, 55f).pad(3f);

                        if(user != player){
                            t.button("@player.ban", Icon.hammer, bstyle, () -> {
                                ui.showConfirm("@confirm", Core.bundle.format("confirmban",  user.name()), () -> Call.adminRequest(user, AdminAction.ban, null));
                                dialog.hide();
                            }).row();

                            t.button("@player.kick", Icon.cancel, bstyle, () -> {
                                ui.showConfirm("@confirm", Core.bundle.format("confirmkick",  user.name()), () -> Call.adminRequest(user, AdminAction.kick, null));
                                dialog.hide();
                            }).row();
                        }

                        if(!user.isLocal()){
                            t.button("@player.trace", Icon.zoom, bstyle, () -> {
                                Call.adminRequest(user, AdminAction.trace, null);
                                dialog.hide();
                            }).row();
                        }

                        //there's generally no reason to team switch outside PvP or sandbox, and it's basically an easy way to cheat
                        if(!state.isCampaign() && (state.rules.pvp || state.rules.infiniteResources)){
                            t.button("@player.team", Icon.redo, bstyle, () -> {
                                var teamSelect = new BaseDialog(Core.bundle.get("player.team") + ": " + user.name);
                                teamSelect.setFillParent(false);

                                var group = new ButtonGroup<>();

                                int i = 0;

                                for(Team team : Team.baseTeams){
                                    var b = new ImageButton(Tex.whiteui, Styles.clearNoneTogglei);
                                    b.margin(4f);
                                    b.getImageCell().grow();
                                    b.getStyle().imageUpColor = team.color;
                                    b.clicked(() -> {
                                        Call.adminRequest(user, AdminAction.switchTeam, team);
                                        teamSelect.hide();
                                    });
                                    teamSelect.cont.add(b).size(50f).checked(a -> user.team() == team).group(group);

                                    if(i++ % 3 == 2) teamSelect.cont.row();
                                }

                                teamSelect.addCloseButton();
                                teamSelect.show();

                                dialog.hide();
                            }).row();
                        }

                        if(!net.client() && !user.isLocal()){
                            t.button("@player.admin", Icon.admin, Styles.togglet, () -> {
                                dialog.hide();
                                String id = user.uuid();

                                if(user.admin){
                                    ui.showConfirm("@confirm", Core.bundle.format("confirmunadmin",  user.name()), () -> {
                                        netServer.admins.unAdminPlayer(id);
                                        user.admin = false;
                                    });
                                }else{
                                    ui.showConfirm("@confirm", Core.bundle.format("confirmadmin",  user.name()), () -> {
                                        netServer.admins.adminPlayer(id, user.usid());
                                        user.admin = true;
                                    });
                                }
                            }).checked(b -> user.admin).row();
                        }
                    }).row();

                    dialog.cont.button("@back", Icon.left, dialog::hide).padTop(-1f).size(220f, 55f);

                    dialog.show();


                }).size(h);
            }else if(!user.isLocal() && !user.admin && net.client() && Groups.player.size() >= 3 && player.team() == user.team()){ //votekick
                button.add().growY();

                button.button(Icon.hammer, ustyle,
                    () -> ui.showTextInput("@votekick.reason", Core.bundle.format("votekick.reason.message", user.name()), "",
                    reason -> Call.sendChatMessage("/votekick #" + user.id + " " + reason)))
                .size(h);
            }

            content.add(button).width(350f).height(h + 14);
            content.row();
        }

        if(!found){
            content.add(Core.bundle.format("players.notfound")).padBottom(6).width(350f).maxHeight(h + 14);
        }

        content.marginBottom(5);
    }

    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
        }else{
            Core.scene.setKeyboardFocus(null);
            search.clearText();
        }
    }

}
