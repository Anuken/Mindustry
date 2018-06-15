package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.EditLog;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Element;
import io.anuke.ucore.scene.Group;
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
    ObjectMap<Player, Boolean> checkmap = new ObjectMap<>();

    @Override
    public void build(Group parent){
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
//                        CallClient.friendlyFireChange(b);
                    }).growX().update(i -> i.setChecked(state.friendlyFire)).disabled(b -> Net.client()).padRight(5);

                    new button("$text.server.bans", () -> {
                        ui.bans.show();
                    }).padTop(-12).padBottom(-12).fillY().cell.disabled(b -> Net.client());

                    new button("$text.server.admins", () -> {
                        ui.admins.show();
                    }).padTop(-12).padBottom(-12).padRight(-12).fillY().cell.disabled(b -> Net.client());
    
                    new button("$text.server.rollback", () -> {
                        ui.rollback.show();
                    }).padTop(-12).padBottom(-12).padRight(-12).fillY().cell.disabled(b -> !players[0].isAdmin);

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
                boolean rebuild = false;
                for(Player player : playerGroup.all()){
                    if(!checkmap.containsKey(player) || checkmap.get(player, false) != player.isAdmin){
                        rebuild = true;
                    }
                    checkmap.put(player, player.isAdmin);
                }
                if(rebuild) rebuild();
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
            BorderImage image = new BorderImage(Draw.region(player.mech.name), 3f);

            stack.add(image);

            stack.add(new Element(){
                public void draw(){
                    float s = getWidth() / 12f;
                    for(int i : Mathf.signs){
                        Draw.rect((player.mech.weapon.name)
                                + "-equip", x + s * 6 + i * 3*s, y + s*6 + 2*s, -8*s*i, 8*s);
                    }
                }
            });

            button.add(stack).size(h);
            button.labelWrap("[#" + player.color.toString().toUpperCase() + "]" + player.name).width(170f).pad(10);
            button.add().grow();

            button.addImage("icon-admin").size(14*2).visible(() -> player.isAdmin && !(!player.isLocal && Net.server())).padRight(5);

            if((Net.server() || players[0].isAdmin) && !player.isLocal && (!player.isAdmin || Net.server())){
                button.add().growY();

                float bs = (h + 14)/2f;

                button.table(t -> {
                    t.defaults().size(bs - 1, bs + 3);
                    //TODO requests.

                    t.addImageButton("icon-ban", 14*2, () -> {
                        ui.showConfirm("$text.confirm", "$text.confirmban", () -> Call.onAdminRequest(player, AdminAction.ban));
                    }).padBottom(-5.1f);

                    t.addImageButton("icon-cancel", 14*2, () -> Call.onAdminRequest(player, AdminAction.kick)).padBottom(-5.1f);

                    t.row();

                    t.addImageButton("icon-admin", "toggle", 14*2, () -> {
                        if(Net.client()) return;

                        String id = netServer.admins.getTraceByID(player.uuid).uuid;

                        if(netServer.admins.isAdmin(id, connection.address)){
                            ui.showConfirm("$text.confirm", "$text.confirmunadmin", () -> {
                                netServer.admins.unAdminPlayer(id);
                            });
                        }else{
                            ui.showConfirm("$text.confirm", "$text.confirmadmin", () -> {
                                netServer.admins.adminPlayer(id, player.usid);
                            });
                        }
                    }).update(b -> {
                        b.setChecked(player.isAdmin);
                        b.setDisabled(Net.client());
                    }).get().setTouchable(() -> Net.client() ? Touchable.disabled : Touchable.enabled);

                    t.addImageButton("icon-zoom-small", 14*2, () -> Call.onAdminRequest(player, AdminAction.trace));

                }).padRight(12).padTop(-5).padLeft(0).padBottom(-10).size(bs + 10f, bs);


            }

            content.add(button).padBottom(-6).width(350f).maxHeight(h + 14);
            content.row();
        }

        content.marginBottom(5);
    }

    public void showBlockLogs(Array<EditLog> currentEditLogs, int x, int y){
        boolean wasPaused = state.is(State.paused);
        state.set(State.paused);

        FloatingDialog d = new FloatingDialog("$text.blocks.editlogs");
        Table table = new Table();
        table.defaults().pad(1f);
        ScrollPane pane = new ScrollPane(table, "clear");
        pane.setFadeScrollBars(false);
        Table top = new Table();
        top.left();
        top.add("[accent]Edit logs for: "+ x + ", " + y);
        table.add(top).fill().left();
        table.row();

        d.content().add(pane).grow();

        if(currentEditLogs == null || currentEditLogs.size == 0) {
            table.add("$text.block.editlogsnotfound").left();
            table.row();
        }

        else {
            for(int i = 0; i < currentEditLogs.size; i++) {
                EditLog log = currentEditLogs.get(i);
                //TODO display log info.
                table.add("[gold]" + (i + 1) + ". [white]INVALID").left();
                table.row();
            }
        }

        d.buttons().addButton("$text.ok", () -> {
            if(!wasPaused)
                state.set(State.playing);
            d.hide();
        }).size(110, 50).pad(10f);

        d.show();
    }

}
