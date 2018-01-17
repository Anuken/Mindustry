package io.anuke.kryonet;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Net.ClientProvider;
import io.anuke.mindustry.net.Net.SendMode;
import io.anuke.mindustry.net.Packet;
import io.anuke.mindustry.net.Packets.Connect;
import io.anuke.mindustry.net.Packets.Disconnect;
import io.anuke.mindustry.net.Registrator;
import io.anuke.ucore.UCore;
import io.anuke.ucore.function.Consumer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class JavaWebsocketClient implements ClientProvider {
    WebSocketClient socket;
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    boolean debug = false;

    @Override
    public void connect(String ip, int port) throws IOException {
        try {
            URI i = new URI("ws://" + ip + ":" + Vars.webPort);
            UCore.log("Connecting: " + i);
            socket = new WebSocketClient(i, new Draft_6455(), null, 5000) {
                Thread thread;

                @Override
                public void connect() {
                    if(thread != null )
                        throw new IllegalStateException( "WebSocketClient objects are not reuseable" );
                    thread = new Thread(this);
                    thread.setDaemon(true);
                    thread.start();
                }

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    UCore.log("Connected!");
                    Connect connect = new Connect();
                    Net.handleClientReceived(connect);
                }

                @Override
                public void onMessage(String message) {
                    if(debug) UCore.log("Got message: " + message);
                    try {
                        byte[] bytes = Base64Coder.decode(message);
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        byte id = buffer.get();
                        if (id == -2) {
                            //this is a framework message... do nothing yet?
                        } else {
                            Class<?> type = Registrator.getByID(id);
                            if(debug) UCore.log("Got class ID: " + type);
                            Packet packet = (Packet) ClassReflection.newInstance(type);
                            packet.read(buffer);
                            Net.handleClientReceived(packet);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        //throw new RuntimeException(e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if(debug) UCore.log("Closed.");
                    Disconnect disconnect = new Disconnect();
                    Net.handleClientReceived(disconnect);
                }

                @Override
                public void onError(Exception ex) {
                    onClose(0, null, true);
                    ex.printStackTrace();
                }
            };
            socket.connect();
        }catch (URISyntaxException e){
            throw new IOException(e);
        }
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
        if(debug) UCore.log("Sending string: " + string);
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
    }

    @Override
    public void register(Class<?>... types) { }

    @Override
    public void dispose() {
        if(socket != null) socket.close();
        for(Thread thread : Thread.getAllStackTraces().keySet()){
            if(thread.getName().equals("WebsocketWriteThread")) thread.interrupt();
        }
    }
}
