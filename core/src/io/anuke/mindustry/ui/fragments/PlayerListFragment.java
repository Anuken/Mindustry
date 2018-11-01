package io.anuke.mindustry.ui.fragments;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.net.Packets.KickReason;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.builders.button;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class PlayerListFragment implements Fragment{
    public boolean visible = false;
    Table content = new Table();
    int last = 0;

    @Override
    public void build(){
        new table(){{
            new table("pane"){{
                touchable(Touchable.enabled);
                margin(14f);
                new label(() -> Bundles.format(playerGroup.size() == 1 ? "text.players.single" :
                        "text.players", playerGroup.size()));
                row();
                content.marginRight(13f).marginLeft(13f);
                ScrollPane pane = new ScrollPane(content, "clear");
                pane.setScrollingDisabled(true, false);
                pane.setFadeScrollBars(false);
                add(pane).grow();
                row();
                new table("pane"){{
                    margin(12f);

                    get().addCheck("$text.server.friendlyfire", b -> {
                        state.friendlyFire = b;
                        NetEvents.handleFriendlyFireChange(b);
                    }).growX().update(i -> i.setChecked(state.friendlyFire)).disabled(b -> Net.client()).padRight(5);

                    new button("$text.server.bans", () -> {
                        ui.bans.show();
                    }).padTop(-12).padBottom(-12).fillY().cell.disabled(b -> Net.client());

                    new button("$text.server.admins", () -> {
                        ui.admins.show();
                    }).padTop(-12).padBottom(-12).padRight(-12).fillY().cell.disabled(b -> Net.client());

                }}.pad(10f).growX().end();
            }}.end();

            update(t -> {
                if(!mobile){
                    if(Inputs.keyTap("player_list")){
                        visible = !visible;
                    }
                }
                if(!(Net.active() && !state.is(State.menu))){
                    visible = false;
                }
                if(playerGroup.size() != last){
                    rebuild();
                    last = playerGroup.size();
                }
            });

            visible(() -> visible);
        }}.end();

        rebuild();
    }

    public void rebuild(){
        content.clear();

        float h = 74f;

        for(Player player : playerGroup.all()){
            NetConnection connection = gwt ? null : Net.getConnection(player.clientid);

            if(connection == null && Net.server() && !player.isLocal) continue;

            Table button = new Table("button");
            button.left();
            button.margin(5).marginBottom(10);

            Stack stack = new Stack();
            BorderImage image = new BorderImage(Draw.region(player.isAndroid ? "ship-standard" : "mech-standard-icon"), 3f);

            stack.add(image);

            if(!player.isAndroid) {

                stack.add(new Element(){
                    public void draw(){
                        float s = getWidth() / 12f;
                        for(int i : Mathf.signs){
                            Draw.rect((i < 0 ? player.weaponLeft.name : player.weaponRight.name)
                                    + "-equip", x + s * 6 + i * 3*s, y + s*6 + 2*s, -8*s*i, 8*s);
                        }
                    }
                });
            }
            button.add(stack).size(h);
            button.labelWrap("[#" + player.getColor().toString().toUpperCase() + "]" + player.name).width(170f).pad(10);
            button.add().grow();

            button.addImage("icon-admin").size(14*2).visible(() -> player.isAdmin && !(!player.isLocal && Net.server())).padRight(5);

            if((Net.server() || Vars.player.isAdmin) && !player.isLocal && (!player.isAdmin || Net.server())){
                button.add().growY();

                float bs = (h + 14)/2f;

                button.table(t -> {
                    t.defaults().size(bs - 1, bs + 3);

                    t.addImageButton("icon-ban", 14*2, () -> {
                        ui.showConfirm("$text.confirm", "$text.confirmban", () -> {
                            if(Net.server()) {
                                netServer.admins.banPlayerIP(connection.address);
                                netServer.kick(player.clientid, KickReason.banned);
                            }else{
                                NetEvents.handleAdministerRequest(player, AdminAction.ban);
                            }
                        });
                    }).padBottom(-5.1f);

                    t.addImageButton("icon-cancel", 14*2, () -> {
                        if(Net.server()) {
                            netServer.kick(player.clientid, KickReason.kick);
                        }else{
                            NetEvents.handleAdministerRequest(player, AdminAction.kick);
                        }
                    }).padBottom(-5.1f);

                    t.row();

                    t.addImageButton("icon-admin", "toggle", 14*2, () -> {
                        if(Net.client()) return;

                        String id = netServer.admins.getTrace(connection.address).uuid;

                        if(netServer.admins.isAdmin(id, connection.address)){
                            ui.showConfirm("$text.confirm", "$text.confirmunadmin", () -> {
                                netServer.admins.unAdminPlayer(id);
                                NetEvents.handleAdminSet(player, false);
                            });
                        }else{
                            ui.showConfirm("$text.confirm", "$text.confirmadmin", () -> {
                                netServer.admins.adminPlayer(id, connection.address);
                                NetEvents.handleAdminSet(player, true);
                            });
                        }
                    }).update(b ->{
                        b.setChecked(player.isAdmin);
                        b.setDisabled(Net.client());
                    }).get().setTouchable(() -> Net.client() ? Touchable.disabled : Touchable.enabled);

                    t.addImageButton("icon-zoom-small", 14*2, () -> NetEvents.handleTraceRequest(player));

                }).padRight(12).padTop(-5).padLeft(0).padBottom(-10).size(bs + 10f, bs);


            }

            content.add(button).padBottom(-6).width(350f).maxHeight(h + 14);
            content.row();
        }

        content.marginBottom(5);
    }

}
