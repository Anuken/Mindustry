package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.anuke.annotations.Annotations.Serialize;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.*;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.utils.UIUtils;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.*;

public class JoinDialog extends FloatingDialog{
    Array<Server> servers = new Array<>();
    Dialog add;
    Server renaming;
    Table local = new Table();
    Table remote = new Table();
    Table hosts = new Table();
    int totalHosts;

    public JoinDialog(){
        super("$text.joingame");

        loadServers();

        buttons().add().width(60f);
        buttons().add().growX();

        addCloseButton();

        buttons().add().growX();
        buttons().addButton("?", () -> ui.showInfo("$text.join.info")).size(60f, 64f);

        add = new FloatingDialog("$text.joingame.title");
        add.content().add("$text.joingame.ip").padRight(5f).left();

        TextField field = add.content().addField(Settings.getString("ip"), text -> {
            Settings.putString("ip", text);
            Settings.save();
        }).size(320f, 54f).get();

        Platform.instance.addDialog(field, 100);

        add.content().row();
        add.buttons().defaults().size(140f, 60f).pad(4f);
        add.buttons().addButton("$text.cancel", add::hide);
        add.buttons().addButton("$text.ok", () -> {
            if(renaming == null){
                Server server = new Server();
                server.setIP(Settings.getString("ip"));
                servers.add(server);
                saveServers();
                setupRemote();
                refreshRemote();
            }else{
                renaming.setIP(Settings.getString("ip"));
                saveServers();
                setupRemote();
                refreshRemote();
            }
            add.hide();
        }).disabled(b -> Settings.getString("ip").isEmpty() || Net.active());

        add.shown(() -> {
            add.getTitleLabel().setText(renaming != null ? "$text.server.edit" : "$text.server.add");
            if(renaming != null){
                field.setText(renaming.displayIP());
            }
        });

        shown(() -> {
            setup();
            refreshLocal();
            refreshRemote();
        });

        onResize(this::setup);
    }

    void setupRemote(){
        remote.clear();
        for(Server server : servers){
            //why are java lambdas this bad
            TextButton[] buttons = {null};

            TextButton button = buttons[0] = remote.addButton("[accent]" + server.displayIP(), "clear", () -> {
                if(!buttons[0].childrenPressed()){
                    connect(server.ip, server.port);
                }
            }).width(targetWidth()).height(150f).pad(4f).get();

            button.getLabel().setWrap(true);

            Table inner = new Table();
            button.clearChildren();
            button.add(inner).growX();

            inner.add(button.getLabel()).growX();

            inner.addImageButton("icon-loading", "empty", 16 * 2, () -> {
                refreshServer(server);
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton("icon-pencil", "empty", 16 * 2, () -> {
                renaming = server;
                add.show();
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton("icon-trash-16", "empty", 16 * 2, () -> {
                ui.showConfirm("$text.confirm", "$text.server.delete", () -> {
                    servers.removeValue(server, true);
                    saveServers();
                    setupRemote();
                    refreshRemote();
                });
            }).margin(3f).pad(6).top().right();

            button.row();

            server.content = button.table(t -> {
            }).grow().get();

            remote.row();
        }
    }

    void refreshRemote(){
        for(Server server : servers){
            refreshServer(server);
        }
    }

    void refreshServer(Server server){
        server.content.clear();
        server.content.label(() -> Bundles.get("text.server.refreshing") + Strings.animated(4, 11, "."));

        Net.pingHost(server.ip, server.port, host -> {
            String versionString;

            if(host.version == -1){
                versionString = Bundles.format("text.server.version", Bundles.get("text.server.custombuild"), "");
            }else if(host.version == 0){
                versionString = Bundles.get("text.server.outdated");
            }else if(host.version < Version.build && Version.build != -1){
                versionString = Bundles.get("text.server.outdated") + "\n" +
                        Bundles.format("text.server.version", host.version);
            }else if(host.version > Version.build && Version.build != -1){
                versionString = Bundles.get("text.server.outdated.client") + "\n" +
                        Bundles.format("text.server.version", host.version);
            }else{
                versionString = Bundles.format("text.server.version", host.version, host.versionType);
            }

            server.content.clear();

            server.content.table(t -> {
                t.add(versionString).left();
                t.row();
                t.add("[lightgray]" + Bundles.format("text.server.hostname", host.name)).left();
                t.row();
                t.add("[lightgray]" + (host.players != 1 ? Bundles.format("text.players", host.players) :
                        Bundles.format("text.players.single", host.players))).left();
                t.row();
                t.add("[lightgray]" + Bundles.format("text.save.map", host.mapname) + " / " + Bundles.format("text.save.wave", host.wave)).left();
            }).expand().left().bottom().padLeft(12f).padBottom(8);

        }, e -> {
            server.content.clear();
            server.content.add("$text.host.invalid");
        });
    }

    void setup(){
        float w = targetWidth();

        Player player = players[0];

        hosts.clear();

        hosts.add(remote).growX();
        hosts.row();
        hosts.add(local).width(w);

        ScrollPane pane = new ScrollPane(hosts, "clear");
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        setupRemote();
        refreshRemote();

        content().clear();
        content().table(t -> {
            t.add("$text.name").padRight(10);
            t.addField(Settings.getString("name"), text -> {
                player.name = text;
                Settings.put("name", text);
                Settings.save();
            }).grow().pad(8).get().setMaxLength(maxNameLength);

            ImageButton button = t.addImageButton("white", 40, () -> {
                new ColorPickDialog().show(color -> {
                    player.color.set(color);
                    Settings.putInt("color-0", Color.rgba8888(color));
                    Settings.save();
                });
            }).size(50f, 54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.color);
        }).width(w).height(70f).pad(4);
        content().row();
        content().add(pane).width(w + 38).pad(0);
        content().row();
        content().addCenteredImageTextButton("$text.server.add", "icon-add", "clear", 14 * 3, () -> {
            renaming = null;
            add.show();
        }).marginLeft(6).width(w).height(80f).update(button -> {
            float pw = w;
            float pad = 0f;
            if(pane.getChildren().first().getPrefHeight() > pane.getHeight()){
                pw = w + 30;
                pad = 6;
            }

            Cell<TextButton> cell = ((Table) pane.getParent()).getCell(button);

            if(!MathUtils.isEqual(cell.getMinWidth(), pw)){
                cell.width(pw);
                cell.padLeft(pad);
                pane.getParent().invalidateHierarchy();
            }
        });
    }

    void refreshLocal(){
        totalHosts = 0;

        local.clear();
        local.background((Drawable)null);
        local.table("button", t -> t.label(() -> "[accent]" + Bundles.get("text.hosts.discovering") + Strings.animated(4, 10f, ".")).pad(10f)).growX();
        Net.discoverServers(this::addLocalHost, this::finishLocalHosts);
    }

    void finishLocalHosts(){
        if(totalHosts == 0){
            local.clear();
            local.background("button");
            local.add("$text.hosts.none").pad(10f);
            local.add().growX();
            local.addImageButton("icon-loading", 16 * 2f, this::refreshLocal).pad(-10f).padLeft(0).padTop(-6).size(70f, 74f);
        }else{
            local.background((Drawable) null);
        }
    }

    void addLocalHost(Host host){
        if(totalHosts == 0){
            local.clear();
        }
        totalHosts ++;
        float w = targetWidth();

        local.row();

        TextButton button = local.addButton("[accent]" + host.name, "clear", () -> connect(host.address, port))
        .width(w).height(80f).pad(4f).get();
        button.left();
        button.row();
        button.add("[lightgray]" + (host.players != 1 ? Bundles.format("text.players", host.players) :
        Bundles.format("text.players.single", host.players)));
        button.row();
        button.add("[lightgray]" + host.address).pad(4).left();
    }

    void connect(String ip, int port){
        if(Settings.getString("name").trim().isEmpty()){
            ui.showInfo("$text.noname");
            return;
        }

        ui.loadfrag.show("$text.connecting");

        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
        });

        Timers.runTask(2f, () -> {
            Vars.netClient.beginConnecting();
            Net.connect(ip, port, () -> {
                hide();
                add.hide();
            });
        });
    }

    float targetWidth(){
        return UIUtils.portrait() ? 350f : 500f;
    }

    private void loadServers(){
        servers = Settings.getObject("server-list", Array.class, Array::new);
    }

    private void saveServers(){
        Settings.putObject("server-list", servers);
        Settings.save();
    }

    @Serialize
    public static class Server{
        public String ip;
        public int port;

        transient Table content;

        void setIP(String ip){

            //parse ip:port, if unsuccessful, use default values
            if(ip.lastIndexOf(':') != -1 && ip.lastIndexOf(':') != ip.length()-1){
                try{
                    int idx = ip.lastIndexOf(':');
                    this.ip = ip.substring(0, idx);
                    this.port = Integer.parseInt(ip.substring(idx + 1));
                }catch(Exception e){
                    this.ip = ip;
                    this.port = Vars.port;
                }
            }else{
                this.ip = ip;
                this.port = Vars.port;
            }
        }

        String displayIP(){
            return ip + (port != Vars.port ? ":" + port : "");
        }

        public Server(){}
    }
}
