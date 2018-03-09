package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.player;
import static io.anuke.mindustry.Vars.ui;

public class JoinDialog extends FloatingDialog {
    Array<Server> servers = new Array<>();
    Dialog add;
    Server renaming;
    Table local = new Table();
    Table remote = new Table();
    Table hosts = new Table();
    float w = 500;

    public JoinDialog(){
        super("$text.joingame");

        loadServers();

        addCloseButton();

        add = new FloatingDialog("$text.joingame.title");
        add.content().add("$text.joingame.ip").padRight(5f).left();

        Platform.instance.addDialog(add.content().addField(Settings.getString("ip"), text ->{
            Settings.putString("ip", text);
            Settings.save();
        }).size(320f, 54f).get(), 100);

        add.content().row();
        add.buttons().defaults().size(140f, 60f).pad(4f);
        add.buttons().addButton("$text.cancel", add::hide);
        add.buttons().addButton("$text.ok", () -> {
            if(renaming == null) {
                Server server = new Server(Settings.getString("ip"), Strings.parseInt(Settings.getString("port")));
                servers.add(server);
                saveServers();
                setupRemote();
                refreshRemote();
            }else{
                renaming.ip = Settings.getString("ip");
                saveServers();
                setupRemote();
                refreshRemote();
            }
            add.hide();
        }).disabled(b -> Settings.getString("ip").isEmpty() || Net.active());

        add.shown(() -> {
            add.getTitleLabel().setText(renaming != null ? "$text.server.edit" : "$text.server.add");
        });

        setup();

        shown(() -> {
            refreshLocal();
            refreshRemote();
        });
    }

    void setupRemote(){
        remote.clear();
        for (Server server : servers) {
            //why are java lambdas this bad
            TextButton[] buttons = {null};

            TextButton button = buttons[0] = remote.addButton("[accent]"+server.ip, "clear", () -> {
                if(!buttons[0].childrenPressed()) connect(server.ip, Vars.port);
            }).width(w).height(150f).pad(4f).get();

            button.getLabel().setWrap(true);

            Table inner = new Table();
            button.clearChildren();
            button.add(inner).growX();

            inner.add(button.getLabel()).growX();

            inner.addImageButton("icon-loading", "empty", 16*2, () -> {
                refreshServer(server);
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton("icon-pencil", "empty", 16*2, () -> {
                renaming = server;
                add.show();
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton("icon-trash-16", "empty", 16*2, () -> {
                ui.showConfirm("$text.confirm", "$text.server.delete", () -> {
                    servers.removeValue(server, true);
                    saveServers();
                    setupRemote();
                    refreshRemote();
                });
            }).margin(3f).pad(6).top().right();

            button.row();

            server.content = button.table(t -> {}).grow().get();

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

            if(host.version == -1) {
                versionString = Bundles.format("text.server.version", Bundles.get("text.server.custombuild"));
            }else if(host.version == 0){
                versionString = Bundles.get("text.server.outdated");
            }else if(host.version < Version.build && Version.build != -1){
                versionString = Bundles.get("text.server.outdated") + "\n" +
                        Bundles.format("text.server.version", host.version);
            }else if(host.version > Version.build && Version.build != -1){
                versionString = Bundles.get("text.server.outdated.client") + "\n" +
                        Bundles.format("text.server.version", host.version);
            }else{
                versionString = Bundles.format("text.server.version", host.version);
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

            //server.content.add(versionString).top().expandY().top().expandX();

        }, e -> {
            server.content.clear();
            server.content.add("$text.host.invalid");
        });
    }

    void refreshLocal(){
        if(!Vars.gwt) {
            local.clear();
            local.background("button");
            local.label(() -> "[accent]" + Bundles.get("text.hosts.discovering") + Strings.animated(4, 10f, ".")).pad(10f);
            Net.discoverServers(this::addLocalHosts);
        }
    }

    void setup(){
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
                if(text.isEmpty()) return;
                Vars.player.name = text;
                Settings.put("name", text);
                Settings.save();
            }).grow().pad(8).get().setMaxLength(40);

            ImageButton button = t.addImageButton("white", 40, () -> {
                new ColorPickDialog().show(color -> {
                    player.color.set(color);
                    Settings.putInt("color", Color.rgba8888(color));
                    Settings.save();
                });
            }).size(50f, 54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.getColor());
        }).width(w).height(70f).pad(4);
        content().row();
        content().add(pane).width(w + 34).pad(0);
        content().row();
        content().addCenteredImageTextButton("$text.server.add", "icon-add", "clear", 14*3, () -> {
            renaming = null;
            add.show();
        }).marginLeft(6).width(w).height(80f).update(button -> {
            float pw = w;
            float pad = 0f;
            if(pane.getChildren().first().getPrefHeight() > pane.getHeight()){
                pw = w + 30;
                pad = 6;
            }

            Cell<TextButton> cell = ((Table)pane.getParent()).getCell(button);

            if(!MathUtils.isEqual(cell.getMinWidth(), pw)){
                cell.width(pw);
                cell.padLeft(pad);
                pane.getParent().invalidateHierarchy();
            }
        });
    }

    void addLocalHosts(Array<Host> array){
        local.clear();

        if(array.size == 0){
            local.add("$text.hosts.none").pad(10f);
            local.add().growX();
            local.addImageButton("icon-loading", 16*2f, this::refreshLocal).pad(-10f).padLeft(0).padTop(-6).size(70f, 74f);
        }else {
            for (Host a : array) {
                TextButton button = local.addButton("[accent]"+a.name, "clear", () -> {
                    connect(a.address, Vars.port);
                }).width(w).height(80f).pad(4f).get();
                button.left();
                button.row();
                button.add("[lightgray]" + (a.players != 1 ? Bundles.format("text.players", a.players) :
                        Bundles.format("text.players.single", a.players)));
                button.row();
                button.add("[lightgray]" + a.address).pad(4).left();

                local.row();
                local.background((Drawable) null);
            }
        }
    }

    void connect(String ip, int port){
        ui.loadfrag.show("$text.connecting");

        Timers.runTask(2f, () -> {
            try{
                Vars.netClient.beginConnecting();
                Net.connect(ip, port);
                hide();
                add.hide();
            }catch (Exception e) {
                Throwable t = e;
                while(t.getCause() != null){
                    t = t.getCause();
                }
                //TODO localize
                String error = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
                if(error.contains("connection refused")) {
                    error = "connection refused";
                }else if(error.contains("port out of range")){
                    error = "invalid port!";
                }else if(error.contains("invalid argument")) {
                    error = "invalid IP or port!";
                }else if(t.getClass().toString().toLowerCase().contains("sockettimeout")){
                    error = "timed out!\nmake sure the host has port forwarding set up,\nand that the address is correct!";
                }else{
                    error = Strings.parseException(e, false);
                }
                ui.showError(Bundles.format("text.connectfail", error));
                ui.loadfrag.hide();

                Log.err(e);
            }
        });
    }

    private void loadServers(){
        String h = Settings.getString("servers");
        String[] list = h.split("\\|\\|\\|");
        for(String fname : list){
            if(fname.isEmpty()) continue;
            String[] split = fname.split(":");
            String host = split[0];
            int port = Strings.parseInt(split[1]);

            if(port != Integer.MIN_VALUE) servers.add(new Server(host, port));
        }
    }

    private void saveServers(){
        StringBuilder out = new StringBuilder();
        for(Server server : servers){
            out.append(server.ip);
            out.append(":");
            out.append(server.port);
            out.append("|||");
        }
        Settings.putString("servers", out.toString());
        Settings.save();
    }

    private class Server{
        public String ip;
        public int port;
        public Host host;
        public Table content;

        public Server(String ip, int port){
            this.ip = ip;
            this.port = port;
        }
    }
}
