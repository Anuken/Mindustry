package io.anuke.mindustry.client;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListener;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packet;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Registrator;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Strings;

import java.io.IOException;
import java.nio.ByteBuffer;

public class WebsocketClient implements ClientProvider {
    Websocket socket;
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    @Override
    public void connect(String ip, int port) throws IOException {
        socket = new Websocket("ws://" + ip + ":" + Vars.webPort);
        socket.addListener(new WebsocketListener() {
            public void onMessage(byte[] bytes) {
                try {
                    ByteBuffer buffer = ByteBuffer.wrap(bytes);
                    byte id = buffer.get();
                    if(id == -2){
                    //this is a framework message... do nothing yet?
                    }else {
                        Class<?> type = Registrator.getByID(id);
                        Packet packet = (Packet) ClassReflection.newInstance(type);
                        packet.read(buffer);
                        Net.handleClientReceived(packet);
                    }
                }catch (ReflectionException e){
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onClose() {
                Disconnect disconnect = new Disconnect();
                Net.handleClientReceived(disconnect);
            }

            @Override
            public void onMessage(String msg) {
                onMessage(Base64Coder.decode(msg));
            }

            @Override
            public void onOpen() {
                Connect connect = new Connect();
                Net.handleClientReceived(connect);
            }
        });
        socket.open();
    }

    @Override
    public void send(Object object, SendMode mode) {
        if(!(object instanceof Packet)) throw new RuntimeException("All sent objects must be packets!");
        Packet p = (Packet)object;
        buffer.position(0);
        buffer.put(Registrator.getID(object.getClass()));
        p.write(buffer);
        int pos = buffer.position();
        buffer.position(0);
        byte[] out = new byte[pos];
        buffer.get(out);
        String string = new String(Base64Coder.encode(out));
        socket.send(string);
    }

    @Override
    public void updatePing() {

    }

    @Override
    public int getPing() {
        return 0;
    }

    @Override
    public void disconnect() {
        socket.close();
    }

    @Override
    public Array<Host> discover() {
        return new Array<>();
    }

    @Override
    public void pingHost(String address, int port, Consumer<Host> valid, Consumer<IOException> failed) {
        failed.accept(new IOException());
        Websocket socket = new Websocket("ws://" + address + ":" + Vars.webPort);
        final boolean[] accepted = {false};
        socket.addListener(new WebsocketListener() {
            @Override
            public void onClose() {
                if(!accepted[0]) failed.accept(new IOException("Failed to connect to host."));
            }

            @Override
            public void onMessage(String msg) {
                String[] text = msg.split("\\|");
                Host host = new Host(text[1], address, Strings.parseInt(text[0]));
                valid.accept(host);
                accepted[0] = true;
                socket.close();
            }

            @Override
            public void onOpen() {
                socket.send("_ping_");
            }
        });
        socket.open();
        Timers.runTask(60f*5, () -> {
            if(!accepted[0]){
                failed.accept(new IOException("Failed to connect to host."));
                socket.close();
            }
        });
    }

    @Override
    public void register(Class<?>... types) { }

    @Override
    public void dispose() {
        socket.close();
    }
}
