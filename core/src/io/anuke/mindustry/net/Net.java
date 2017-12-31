package io.anuke.mindustry.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.net.Streamable.StreamBegin;
import io.anuke.mindustry.net.Streamable.StreamBuilder;
import io.anuke.mindustry.net.Streamable.StreamChunk;
import io.anuke.ucore.function.Consumer;

//TODO stub
public class Net{
	private static boolean server;
	private static boolean active;
	private static ObjectMap<Class<?>, Consumer> clientListeners = new ObjectMap<>();
	private static ObjectMap<Class<?>, Consumer> serverListeners = new ObjectMap<>();
	private static ClientProvider clientProvider;
	private static ServerProvider serverProvider;

	private static IntMap<StreamBuilder> streams = new IntMap<>();
	
	/**Connect to an address.*/
	public static void connect(String ip, int port) throws IOException{
		clientProvider.connect(ip, port);
		active = true;
		server = false;
	}

	/**Host a server at an address*/
	public static void host(int port) throws IOException{
		serverProvider.host(port);
		active = true;
		server = true;
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
	
	/**Send an object to all connected clients, or to the server if this is a client.*/
	public static void send(Object object, SendMode mode){
		if(server){
			serverProvider.send(object, mode);
		}else {
			clientProvider.send(object, mode);
		}
	}

	/**Send an object to a certain client. Server-side only*/
	public static void sendTo(int id, Object object, SendMode mode){
		serverProvider.sendTo(id, object, mode);
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
	
	/**Registers a client listener for when an object is recieved.*/
	public static <T> void handle(Class<T> type, Consumer<T> listener){
		clientListeners.put(type, listener);
	}

	/**Registers a server listener for when an object is recieved.*/
	public static <T> void handleServer(Class<T> type, Consumer<T> listener){
		serverListeners.put(type, listener);
	}
	
	/**Call to handle a packet being recieved for the client.*/
	public static void handleClientReceived(Object object){
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
		}else if(clientListeners.get(object.getClass()) != null){
			clientListeners.get(object.getClass()).accept(object);
		}else{
			Gdx.app.error("Mindustry::Net", "Unhandled packet type: '" + object.getClass() + "'!");
		}
	}

	/**Call to handle a packet being recieved for the server.*/
	public static void handleServerReceived(Object object){
		if(serverListeners.get(object.getClass()) != null){
			serverListeners.get(object.getClass()).accept(object);
		}else{
			Gdx.app.error("Mindustry::Net", "Unhandled packet type: '" + object.getClass() + "'!");
		}
	}
	
	/**Whether the net is active, e.g. whether this is a multiplayer game.*/
	public static boolean active(){
		return active;
	}
	
	/**Whether this is a server or not.*/
	public static boolean server(){
		return server;
	}

	/**Whether this is a client or not.*/
	public static boolean client(){
		return !server;
	}

	/**Register classes that will be sent. Must be done for all classes.*/
	public static void registerClasses(Class<?>... classes){
		clientProvider.register(classes);
		serverProvider.register(classes);
	}

	/**Client implementation.*/
	public static interface ClientProvider {
		/**Connect to a server.*/
		public void connect(String ip, int port) throws IOException;
		/**Send an object to the server.*/
		public void send(Object object, SendMode mode);
		/**Update the ping. Should be done every second or so.*/
		public void updatePing();
		/**Get ping in milliseconds. Will only be valid after a call to updatePing.*/
		public int getPing();
		/**Disconnect from the server.*/
		public void disconnect();
		/**Register classes to be sent.*/
		public void register(Class<?>... types);
	}

	/**Server implementation.*/
	public static interface ServerProvider {
		/**Host a server at specified port.*/
		public void host(int port) throws IOException;
		/**Sends a large stream of data to a specific client.*/
		public void sendStream(int id, Streamable stream);
		/**Send an object to everyone connected.*/
		public void send(Object object, SendMode mode);
		/**Send an object to a specific client ID.*/
		public void sendTo(int id, Object object, SendMode mode);
		/**Send an object to everyone <i>except</i> a client ID.*/
		public void sendExcept(int id, Object object, SendMode mode);
		/**Close the server connection.*/
		public void close();
		/**Register classes to be sent.*/
		public void register(Class<?>... types);
	}

	public enum SendMode{
		tcp, udp
	}
}
