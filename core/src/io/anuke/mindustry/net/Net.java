package io.anuke.mindustry.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.net.Packet.ImportantPacket;
import io.anuke.mindustry.net.Packet.UnimportantPacket;
import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamBuilder;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.BiConsumer;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Log;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Net{
	private static boolean server;
	private static boolean active;
	private static boolean clientLoaded;
	private static Array<Object> packetQueue = new Array<>();
	private static ObjectMap<Class<?>, Consumer> listeners = new ObjectMap<>();
	private static ObjectMap<Class<?>, Consumer> clientListeners = new ObjectMap<>();
	private static ObjectMap<Class<?>, BiConsumer<Integer, Object>> serverListeners = new ObjectMap<>();
	private static ClientProvider clientProvider;
	private static ServerProvider serverProvider;

	private static IntMap<StreamBuilder> streams = new IntMap<>();

	/**Display a network error.*/
	public static void showError(String text){
		if(!headless){
			ui.showError(text);
		}else{
			Log.err(text);
		}
	}

	/**Sets the client loaded status, or whether it will recieve normal packets from the server.*/
	public static void setClientLoaded(boolean loaded){
		clientLoaded = loaded;

		if(loaded){
			//handle all packets that were skipped while loading
			for(int i = 0; i < packetQueue.size; i ++){
                Log.info("Processing {0} packet post-load.", ClassReflection.getSimpleName(packetQueue.get(i).getClass()));
				handleClientReceived(packetQueue.get(i));
			}
		}
		//clear inbound packet queue
		packetQueue.clear();
	}
	
	/**Connect to an address.*/
	public static void connect(String ip, int port) throws IOException{
		if(!active) {
			clientProvider.connect(ip, port);
			active = true;
			server = false;
		}else{
			throw new IOException("Already connected!");
		}
	}

	/**Host a server at an address*/
	public static void host(int port) throws IOException{
		serverProvider.host(port);
		active = true;
		server = true;

		Timers.runTask(60f, Platform.instance::updateRPC);
	}

	/**Closes the server.*/
	public static void closeServer(){
        serverProvider.close();
        server = false;
        active = false;
    }

    public static void disconnect(){
		clientProvider.disconnect();
		server = false;
		active = false;
	}

	/**Starts discovering servers on a different thread. Does not work with GWT.
	 * Callback is run on the main libGDX thread.*/
	public static void discoverServers(Consumer<Array<Host>> cons){
		clientProvider.discover(cons);
	}

	/**Returns a list of all connections IDs.*/
	public static Array<NetConnection> getConnections(){
		return (Array<NetConnection>)serverProvider.getConnections();
	}

	/**Returns a connection by ID*/
	public static NetConnection getConnection(int id){
		return serverProvider.getByID(id);
	}
	
	/**Send an object to all connected clients, or to the server if this is a client.*/
	public static void send(Object object, SendMode mode){
		if(server){
			if(serverProvider != null) serverProvider.send(object, mode);
		}else {
			if(clientProvider != null) clientProvider.send(object, mode);
		}
	}

	/**Send an object to a certain client. Server-side only*/
	public static void sendTo(int id, Object object, SendMode mode){
		serverProvider.sendTo(id, object, mode);
	}

	/**Send an object to everyone EXCEPT certain client. Server-side only*/
	public static void sendExcept(int id, Object object, SendMode mode){
		serverProvider.sendExcept(id, object, mode);
	}

	/**Send a stream to a specific client. Server-side only.*/
	public static void sendStream(int id, Streamable stream){
		serverProvider.sendStream(id, stream);
	}
	
	/**Sets the net clientProvider, e.g. what handles sending, recieving and connecting to a server.*/
	public static void setClientProvider(ClientProvider provider){
		Net.clientProvider = provider;
	}

	/**Sets the net serverProvider, e.g. what handles hosting a server.*/
	public static void setServerProvider(ServerProvider provider){
		Net.serverProvider = provider;
	}

	/**Registers a common listener for when an object is recieved. Fired on both client and serve.r*/
	public static <T> void handle(Class<T> type, Consumer<T> listener){
		listeners.put(type, listener);
	}

	/**Registers a client listener for when an object is recieved.*/
	public static <T> void handleClient(Class<T> type, Consumer<T> listener){
		clientListeners.put(type, listener);
	}

	/**Registers a server listener for when an object is recieved.*/
	public static <T> void handleServer(Class<T> type, BiConsumer<Integer, T> listener){
		serverListeners.put(type, (BiConsumer<Integer, Object>) listener);
	}
	
	/**Call to handle a packet being recieved for the client.*/
	public static void handleClientReceived(Object object){
		if(debugNet) clientDebug.handle(object);

		if(object instanceof StreamBegin) {
			StreamBegin b = (StreamBegin) object;
			streams.put(b.id, new StreamBuilder(b));
		}else if(object instanceof StreamChunk) {
			StreamChunk c = (StreamChunk)object;
			StreamBuilder builder = streams.get(c.id);
			if(builder == null){
				throw new RuntimeException("Recieved stream chunk without a StreamBegin beforehand!");
			}
			builder.add(c.data);
			if(builder.isDone()){
				streams.remove(builder.id);
				handleClientReceived(builder.build());
			}
		}else if(clientListeners.get(object.getClass()) != null ||
					listeners.get(object.getClass()) != null){
			if(clientLoaded || object instanceof ImportantPacket){
				if(clientListeners.get(object.getClass()) != null) clientListeners.get(object.getClass()).accept(object);
				if(listeners.get(object.getClass()) != null) listeners.get(object.getClass()).accept(object);
			}else if(!(object instanceof UnimportantPacket)){
				packetQueue.add(object);
				Log.info("Queuing packet {0}.", ClassReflection.getSimpleName(object.getClass()));
			}
		}else{
			Log.err("Unhandled packet type: '{0}'!", ClassReflection.getSimpleName(object.getClass()));
		}
	}

	/**Call to handle a packet being recieved for the server.*/
	public static void handleServerReceived(int connection, Object object){
		if(debugNet) serverDebug.handle(connection, object);

		if(serverListeners.get(object.getClass()) != null || listeners.get(object.getClass()) != null){
			if(serverListeners.get(object.getClass()) != null) serverListeners.get(object.getClass()).accept(connection, object);
			if(listeners.get(object.getClass()) != null) listeners.get(object.getClass()).accept(object);
		}else{
			Log.err("Unhandled packet type: '{0}'!", ClassReflection.getSimpleName(object.getClass()));
		}
	}

	/**Pings a host in an new thread. If an error occured, failed() should be called with the exception. */
	public static void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed){
		clientProvider.pingHost(address, port, valid, failed);
	}

	/**Update client ping.*/
	public static void updatePing(){
		clientProvider.updatePing();
	}

	/**Get the client ping. Only valid after updatePing().*/
	public static int getPing(){
		return server() ? 0 : clientProvider.getPing();
	}
	
	/**Whether the net is active, e.g. whether this is a multiplayer game.*/
	public static boolean active(){
		return active;
	}
	
	/**Whether this is a server or not.*/
	public static boolean server(){
		return server && active;
	}

	/**Whether this is a client or not.*/
	public static boolean client(){
		return !server && active;
	}

	public static void dispose(){
		if(clientProvider != null) clientProvider.dispose();
		if(serverProvider != null) serverProvider.dispose();
		clientProvider = null;
		serverProvider = null;
		server = false;
		active = false;
	}

	public static void http(String url, String method, Consumer<String> listener, Consumer<Throwable> failure){
		HttpRequest req = new HttpRequestBuilder().newRequest()
				.method(method).url(url).build();

		Gdx.net.sendHttpRequest(req, new HttpResponseListener() {
			@Override
			public void handleHttpResponse(HttpResponse httpResponse) {
				listener.accept(httpResponse.getResultAsString());
			}

			@Override
			public void failed(Throwable t) {
				failure.accept(t);
			}

			@Override
			public void cancelled() {}
		});
	}

	/**Client implementation.*/
	public interface ClientProvider {
		/**Connect to a server.*/
		void connect(String ip, int port) throws IOException;
		/**Send an object to the server.*/
		void send(Object object, SendMode mode);
		/**Update the ping. Should be done every second or so.*/
		void updatePing();
		/**Get ping in milliseconds. Will only be valid after a call to updatePing.*/
		int getPing();
		/**Disconnect from the server.*/
		void disconnect();
        /**Discover servers. This should run the callback regardless of whether any servers are found. Should not block.
		 * Callback should be run on libGDX main thread.*/
        void discover(Consumer<Array<Host>> callback);
        /**Ping a host. If an error occured, failed() should be called with the exception. */
        void pingHost(String address, int port, Consumer<Host> valid, Consumer<Exception> failed);
		/**Close all connections.*/
		void dispose();
	}

	/**Server implementation.*/
	public interface ServerProvider {
		/**Host a server at specified port.*/
		void host(int port) throws IOException;
		/**Sends a large stream of data to a specific client.*/
		void sendStream(int id, Streamable stream);
		/**Send an object to everyone connected.*/
		void send(Object object, SendMode mode);
		/**Send an object to a specific client ID.*/
		void sendTo(int id, Object object, SendMode mode);
		/**Send an object to everyone <i>except</i> a client ID.*/
		void sendExcept(int id, Object object, SendMode mode);
		/**Close the server connection.*/
		void close();
		/**Return all connected users.*/
		Array<? extends NetConnection> getConnections();
		/**Returns a connection by ID.*/
		NetConnection getByID(int id);
		/**Close all connections.*/
		void dispose();
	}

	public enum SendMode{
		tcp, udp
	}
}
