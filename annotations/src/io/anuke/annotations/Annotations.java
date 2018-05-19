package io.anuke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Goal: To create a system to send events to the server from the client and vice versa.<br>
 * These events may optionally also trigger on the caller client/server as well.<br>
 *<br>
 * Three annotations are used for this purpose.<br>
 * {@link RemoteClient}: Marks a method as able to be invoked remotely on a client from a server.<br>
 * {@link RemoteServer}: Marks a method as able to be invoked remotely on a server from a client.<br>
 * {@link Local}: Makes this method get invoked locally as well as remotely.<br>
 *<br>
 * All RemoteClient methods are put in the class CallClient, and all RemoteServer methods are put in the class CallServer.<br>
 */
public class Annotations {

    /**Marks a method as invokable remotely from a server on a client.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface RemoteClient {}

    /**Marks a method as invokable remotely from a client on a server.
     * All RemoteServer methods must have their first formal parameter be of type Player.
     * This player is the invoker of the method.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface RemoteServer {}

    /**Marks a method to be locally invoked as well as remotely invoked on the caller
     * Must be used with {@link RemoteClient}/{@link RemoteServer} annotations.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Local{}
}
