package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Interval;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.AdminAction;

import static io.anuke.mindustry.Vars.*;

public class PlayerListFragment extends Fragment{
    private boolean visible = false;
    private Table content = new Table().marginRight(13f).marginLeft(13f);
    private Interval timer = new Interval();

    @Override
    public void build(Group parent){
        parent.fill(cont -> {
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!(Net.active() && !state.is(State.menu))){
                    visible = false;
                    return;
                }

                if(visible && timer.get(20)){
                    rebuild();
                    content.pack();
                    content.act(Core.graphics.getDeltaTime());
                    //TODO hack
                    Core.scene.act(0f);
                }
            });

            cont.table("button", pane -> {
                pane.label(() -> Core.bundle.format(playerGroup.size() == 1 ? "players.single" : "players", playerGroup.size()));
                pane.row();
                pane.pane(content).grow().get().setScrollingDisabled(true, false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().growX().height(50f).fillY();

                    menu.addButton("$server.bans", ui.bans::show).disabled(b -> Net.client());
                    menu.addButton("$server.admins", ui.admins::show).disabled(b -> Net.client());
                    menu.addButton("$close", this::toggle);
                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f);
        });

        rebuild();
    }

    public void rebuild(){
        content.clear();

        float h = 74f;

        playerGroup.all().sort((p1, p2) -> p1.getTeam().compareTo(p2.getTeam()));
        playerGroup.all().each(user -> {
            NetConnection connection = user.con;

            if(connection == null && Net.server() && !user.isLocal) return;

            Table button = new Table();
            button.left();
            button.margin(5).marginBottom(10);

            Table table = new Table(){
                @Override
                public void draw(){
                    super.draw();
                    Draw.color(Pal.accent);
                    Draw.alpha(parentAlpha);
                    Lines.stroke(Unit.dp.scl(3f));
                    Lines.rect(x, y, width, height);
                    Draw.reset();
                }
            };
            table.margin(8);
            table.add(new Image(user.mech.iconRegion)).grow();

            button.add(table).size(h);
            button.labelWrap("[#" + user.color.toString().toUpperCase() + "]" + user.name).width(170f).pad(10);
            button.add().grow();

            button.addImage("icon-admin").size(14 * 2).visible(() -> user.isAdmin && !(!user.isLocal && Net.server())).padRight(5).get().updateVisibility();

            if((Net.server() || player.isAdmin) && !user.isLocal && (!user.isAdmin || Net.server())){
                button.add().growY();

                float bs = (h) / 2f;

                button.table(t -> {
                    t.defaults().size(bs);

                    t.addImageButton("icon-ban", "clear-partial", 14 * 2,
                    () -> ui.showConfirm("$confirm", "$confirmban", () -> Call.onAdminRequest(user, AdminAction.ban)));
                    t.addImageButton("icon-cancel", "clear-partial", 16 * 2,
                    () -> ui.showConfirm("$confirm", "$confirmkick", () -> Call.onAdminRequest(user, AdminAction.kick)));

                    t.row();

                    t.addImageButton("icon-admin", "clear-toggle-partial", 14 * 2, () -> {
                        if(Net.client()) return;

                        String id = user.uuid;

                        if(netServer.admins.isAdmin(id, connection.address)){
                            ui.showConfirm("$confirm", "$confirmunadmin", () -> netServer.admins.unAdminPlayer(id));
                        }else{
                            ui.showConfirm("$confirm", "$confirmadmin", () -> netServer.admins.adminPlayer(id, user.usid));
                        }
                    })
                    .update(b -> b.setChecked(user.isAdmin))
                    .disabled(b -> Net.client())
                    .touchable(() -> Net.client() ? Touchable.disabled : Touchable.enabled)
                    .checked(user.isAdmin);

                    t.addImageButton("icon-zoom-small", "clear-partial", 14 * 2, () -> Call.onAdminRequest(user, AdminAction.trace));

                }).padRight(12).size(bs + 10f, bs);
            }

            content.add(button).padBottom(-6).width(350f).maxHeight(h + 14);
            content.row();
            content.addImage("blank").height(3f).color(state.rules.pvp ? user.getTeam().color : Pal.accent).growX();
            content.row();
        });

        content.marginBottom(5);
    }

    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
        }
    }

}
