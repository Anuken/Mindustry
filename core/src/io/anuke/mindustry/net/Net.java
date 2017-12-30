package io.anuke.mindustry.net;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.ucore.function.Consumer;

//TODO stub
public class Net{
	private static boolean server;
	private static boolean active;
	private static ObjectMap<Class<?>, Consumer> clientListeners = new ObjectMap<>();
	private static ObjectMap<Class<?>, Consumer> serverListeners = new ObjectMap<>();
	private static ClientProvider clientProvider;
	private static ServerProvider serverProvider;
	
	/**Connect to an address.*/
	public static void connect(String ip, int port) throws IOException{
		clientProvider.connect(ip, port);
	}

	/**Host a server at an address*/
	public static void host(int port) throws IOException{
		active = true;
		server = true;
		serverProvider.host(port);
	}

	/**Closes the server.*/
	public static void closeServer(){
        serverProvider.close();
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
		if(clientListeners.get(object.getClass()) != null){
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
		/**Register classes to be sent.*/
		public void register(Class<?>... types);
	}

	/**Server implementation.*/
	public static interface ServerProvider {
		/**Host a server at specified port.*/
		public void host(int port) throws IOException;
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
