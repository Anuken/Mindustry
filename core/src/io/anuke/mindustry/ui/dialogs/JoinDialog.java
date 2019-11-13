package io.anuke.mindustry.ui.dialogs;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Packets.*;
import io.anuke.mindustry.ui.*;

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
        super("$joingame");

        loadServers();

        buttons.add().width(60f);
        buttons.add().growX();

        addCloseButton();

        buttons.add().growX();
        if(!steam){
            buttons.addButton("?", () -> ui.showInfo("$join.info")).size(60f, 64f);
        }

        add = new FloatingDialog("$joingame.title");
        add.cont.add("$joingame.ip").padRight(5f).left();

        TextField field = add.cont.addField(Core.settings.getString("ip"), text -> {
            Core.settings.put("ip", text);
            Core.settings.save();
        }).size(320f, 54f).get();

        platform.addDialog(field, 100);

        add.cont.row();
        add.buttons.defaults().size(140f, 60f).pad(4f);
        add.buttons.addButton("$cancel", add::hide);
        add.buttons.addButton("$ok", () -> {
            if(renaming == null){
                Server server = new Server();
                server.setIP(Core.settings.getString("ip"));
                servers.add(server);
                saveServers();
                setupRemote();
                refreshRemote();
            }else{
                renaming.setIP(Core.settings.getString("ip"));
                saveServers();
                setupRemote();
                refreshRemote();
            }
            add.hide();
        }).disabled(b -> Core.settings.getString("ip").isEmpty() || net.active());

        add.shown(() -> {
            add.title.setText(renaming != null ? "$server.edit" : "$server.add");
            if(renaming != null){
                field.setText(renaming.displayIP());
            }
        });

        keyDown(KeyCode.F5, () -> {
            refreshLocal();
            refreshRemote();
        });

        shown(() -> {
            setup();
            refreshLocal();
            refreshRemote();

            if(!steam){
                Core.app.post(() -> Core.settings.getBoolOnce("joininfo", () -> ui.showInfo("$join.info")));
            }
        });

        onResize(this::setup);
    }

    void setupRemote(){
        remote.clear();
        for(Server server : servers){
            //why are java lambdas this bad
            TextButton[] buttons = {null};

            TextButton button = buttons[0] = remote.addButton("[accent]" + server.displayIP(), Styles.cleart, () -> {
                if(!buttons[0].childrenPressed()){
                    if(server.lastHost != null){
                        safeConnect(server.ip, server.port, server.lastHost.version);
                    }else{
                        connect(server.ip, server.port);
                    }
                }
            }).width(targetWidth()).pad(4f).get();

            button.getLabel().setWrap(true);

            Table inner = new Table();
            button.clearChildren();
            button.add(inner).growX();

            inner.add(button.getLabel()).growX();

            inner.addImageButton(Icon.arrowUpSmall, Styles.emptyi, () -> {
                int index = servers.indexOf(server);
                if(index > 0){
                    servers.remove(index);
                    servers.insert(0, server);

                    saveServers();
                    setupRemote();
                    for(Server other : servers){
                        if(other.lastHost != null){
                            setupServer(other, other.lastHost);
                        }else{
                            refreshServer(other);
                        }
                    }
                }

            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton(Icon.loadingSmall, Styles.emptyi, () -> {
                refreshServer(server);
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton(Icon.pencilSmall, Styles.emptyi, () -> {
                renaming = server;
                add.show();
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton(Icon.trash16Small, Styles.emptyi, () -> {
                ui.showConfirm("$confirm", "$server.delete", () -> {
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
        server.content.label(() -> Core.bundle.get("server.refreshing") + Strings.animated(Time.time(), 4, 11, "."));

        net.pingHost(server.ip, server.port, host -> setupServer(server, host), e -> {
            server.content.clear();
            server.content.add("$host.invalid");
        });
    }

    void setupServer(Server server, Host host){
        server.lastHost = host;
        server.content.clear();
        buildServer(host, server.content);
    }

    void buildServer(Host host, Table content){
        String versionString;

        if(host.version == -1){
            versionString = Core.bundle.format("server.version", Core.bundle.get("server.custombuild"), "");
        }else if(host.version == 0){
            versionString = Core.bundle.get("server.outdated");
        }else if(host.version < Version.build && Version.build != -1){
            versionString = Core.bundle.get("server.outdated") + "\n" +
            Core.bundle.format("server.version", host.version, "");
        }else if(host.version > Version.build && Version.build != -1){
            versionString = Core.bundle.get("server.outdated.client") + "\n" +
            Core.bundle.format("server.version", host.version, "");
        }else if(host.version == Version.build && Version.type.equals(host.versionType)){
            //not important
            versionString = "";
        }else{
            versionString = Core.bundle.format("server.version", host.version, host.versionType);
        }

        content.table(t -> {
            t.add("[lightgray]" + host.name + "   " + versionString).width(targetWidth() - 10f).left().get().setEllipsis(true);
            t.row();
            t.add("[lightgray]" + (Core.bundle.format("players" + (host.players == 1 && host.playerLimit <= 0 ? ".single" : ""), (host.players == 0 ? "[lightgray]" : "[accent]") + host.players + (host.playerLimit > 0 ? "[lightgray]/[accent]" + host.playerLimit : "")+ "[lightgray]"))).left();
            t.row();
            t.add("[lightgray]" + Core.bundle.format("save.map", host.mapname) + "[lightgray] / " + host.mode.toString()).width(targetWidth() - 10f).left().get().setEllipsis(true);
        }).expand().left().bottom().padLeft(12f).padBottom(8);
    }

    void setup(){
        float w = targetWidth();

        hosts.clear();

        hosts.add(remote).growX();
        hosts.row();
        hosts.add(local).width(w);

        ScrollPane pane = new ScrollPane(hosts);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        setupRemote();
        refreshRemote();

        cont.clear();
        cont.table(t -> {
            t.add("$name").padRight(10);
            if(!steam){
                t.addField(Core.settings.getString("name"), text -> {
                    player.name = text;
                    Core.settings.put("name", text);
                    Core.settings.save();
                }).grow().pad(8).get().setMaxLength(maxNameLength);
            }else{
                t.add(player.name).update(l -> l.setColor(player.color)).grow().pad(8);
            }

            ImageButton button = t.addImageButton(Tex.whiteui, Styles.clearFulli, 40, () -> {
                new ColorPickDialog().show(color -> {
                    player.color.set(color);
                    Core.settings.put("color-0", Color.rgba8888(color));
                    Core.settings.save();
                });
            }).size(54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.color);
        }).width(w).height(70f).pad(4);
        cont.row();
        cont.add(pane).width(w + 38).pad(0);
        cont.row();
        cont.addCenteredImageTextButton("$server.add", Icon.add, () -> {
            renaming = null;
            add.show();
        }).marginLeft(6).width(w).height(80f).update(button -> {
            float pw = w;
            float pad = 0f;
            if(pane.getChildren().first().getPrefHeight() > pane.getHeight()){
                pw = w + 30;
                pad = 6;
            }

            Cell cell = ((Table)pane.getParent()).getCell(button);

            if(!Mathf.equal(cell.minWidth(), pw)){
                cell.width(pw);
                cell.padLeft(pad);
                pane.getParent().invalidateHierarchy();
            }
        });
    }

    void refreshLocal(){
        totalHosts = 0;

        local.clear();
        local.background(null);
        local.table(Tex.button, t -> t.label(() -> "[accent]" + Core.bundle.get("hosts.discovering.any") + Strings.animated(Time.time(), 4, 10f, ".")).pad(10f)).growX();
        net.discoverServers(this::addLocalHost, this::finishLocalHosts);
        for(String host : defaultServers){
            net.pingHost(host, port, this::addLocalHost, e -> {});
        }
    }

    void finishLocalHosts(){
        if(totalHosts == 0){
            local.clear();
            local.background(Tex.button);
            local.add("$hosts.none").pad(10f);
            local.add().growX();
            local.addImageButton(Icon.loading, this::refreshLocal).pad(-12f).padLeft(0).size(70f);
        }else{
            local.background(null);
        }
    }

    void addLocalHost(Host host){
        if(totalHosts == 0){
            local.clear();
        }
        local.background(null);
        totalHosts++;
        float w = targetWidth();

        local.row();

        TextButton button = local.addButton("", Styles.cleart, () -> safeConnect(host.address, port, host.version))
        .width(w).pad(5f).get();
        button.clearChildren();
        buildServer(host, button);
    }

    public void connect(String ip, int port){
        if(player.name.trim().isEmpty()){
            ui.showInfo("$noname");
            return;
        }

        ui.loadfrag.show("$connecting");

        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
        });

        Time.runTask(2f, () -> {
            logic.reset();
            net.reset();
            Vars.netClient.beginConnecting();
            net.connect(ip, port, () -> {
                hide();
                add.hide();
            });
        });
    }

    void safeConnect(String ip, int port, int version){
        if(version != Version.build && Version.build != -1 && version != -1){
            ui.showInfo("[scarlet]" + (version > Version.build ? KickReason.clientOutdated : KickReason.serverOutdated).toString() + "\n[]" +
            Core.bundle.format("server.versions", Version.build, version));
        }else{
            connect(ip, port);
        }
    }

    float targetWidth(){
        return Core.graphics.isPortrait() ? 350f : 500f;
    }

    @SuppressWarnings("unchecked")
    private void loadServers(){
        servers = Core.settings.getObject("server-list", Array.class, Array::new);
    }

    private void saveServers(){
        Core.settings.putObject("server-list", servers);
        Core.settings.save();
    }

    @Serialize
    public static class Server{
        public String ip;
        public int port;

        transient Table content;
        transient Host lastHost;

        void setIP(String ip){

            //parse ip:port, if unsuccessful, use default values
            if(ip.lastIndexOf(':') != -1 && ip.lastIndexOf(':') != ip.length() - 1){
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

        public Server(){
        }
    }
}
