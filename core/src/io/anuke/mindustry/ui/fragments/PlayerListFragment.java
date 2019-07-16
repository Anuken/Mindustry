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
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.ui.IntFormat;

import static io.anuke.mindustry.Vars.*;

public class PlayerListFragment extends Fragment{
    private boolean visible = false;
    private Table content = new Table().marginRight(13f).marginLeft(13f);
    private Interval timer = new Interval();
    private StringBuilder builder = new StringBuilder();
    private IntFormat pointsFormat = new IntFormat("points.lightgray");
    private IntFormat livesFormat = new IntFormat("lifes");


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

        if(state.rules.resourcesWar){
            playerGroup.all().sort((p1, p2) -> state.points(p2.getTeam())-state.points(p1.getTeam()) + p1.getTeam().compareTo(p2.getTeam()));
        }else{
            playerGroup.all().sort((p1, p2) -> p1.getTeam().compareTo(p2.getTeam()));
        }

        for(int i=0; i<playerGroup.all().size; i++){
            Player user = playerGroup.all().get(i);
            NetConnection connection = user.con;

            if(connection == null && Net.server() && !user.isLocal) return;

            Table button = new Table();
            button.left();
            button.margin(5).marginBottom(10);

            Table table = new Table(){
                @Override
                public void draw(){
                    super.draw();
                    Draw.color(Pal.gray);
                    Draw.alpha(parentAlpha);
                    Lines.stroke(Unit.dp.scl(4f));
                    Lines.rect(x, y, width, height);
                    Draw.reset();
                }
            };
            table.margin(8);
            table.add(new Image(user.mech.getContentIcon()).setScaling(Scaling.none)).grow();

            button.add(table).size(h);
            button.labelWrap(()->{
                builder.setLength(0);
                builder.append("[#" + user.color.toString().toUpperCase() + "]"+ user.name);
                return builder.toString();
            }).width(170f).pad(10);
            button.add().grow();

            button.addImage("icon-admin").size(iconsize).visible(() -> user.isAdmin && !(!user.isLocal && Net.server())).padRight(5).get().updateVisibility();

            if((Net.server() || player.isAdmin) && !user.isLocal && (!user.isAdmin || Net.server())){
                button.add().growY();

                float bs = (h) / 2f;

                button.table(t -> {
                    t.defaults().size(bs);

                    t.addImageButton("icon-ban-small", "clear-partial", iconsizesmall,
                    () -> ui.showConfirm("$confirm", "$confirmban", () -> Call.onAdminRequest(user, AdminAction.ban)));
                    t.addImageButton("icon-cancel-small", "clear-partial", iconsizesmall,
                    () -> ui.showConfirm("$confirm", "$confirmkick", () -> Call.onAdminRequest(user, AdminAction.kick)));

                    t.row();

                    t.addImageButton("icon-admin-small", "clear-toggle-partial", iconsizesmall, () -> {
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

                    t.addImageButton("icon-zoom-small", "clear-partial", iconsizesmall, () -> Call.onAdminRequest(user, AdminAction.trace));

                }).padRight(12).size(bs + 10f, bs);
            }

            content.row();
            if(i == 0 || playerGroup.all().get(i-1).getTeam() != user.getTeam()){
                content.addImage("whiteui").height(4f).color(state.rules.pvp ? user.getTeam().color : Pal.gray).growX().padTop(6).padBottom(6);
                content.row();
                content.labelWrap(()->{
                    builder.setLength(0);
                    if(state.rules.resourcesWar && state.teams.isActive(user.getTeam())){
                        builder.append(pointsFormat.get(state.points(user.getTeam())));
                    }
                    if(state.rules.resourcesWar && state.rules.enableLifes && state.teams.isActive(user.getTeam())){
                        builder.append("    ");
                        builder.append(livesFormat.get(state.lifes[user.getTeam().ordinal()]));
                    }
                    return builder.toString();
                }).left().padBottom(6).marginLeft(5);
                content.row();
            }
            content.add(button).padBottom(-6).width(350f).maxHeight(h + 14);
        }

        content.marginBottom(5);
    }

    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
        }
    }

}
