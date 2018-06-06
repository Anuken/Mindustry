package io.anuke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Goal: To create a system to send events to the server from the client and vice versa, without creating a new packet type each time.<br>
 * These events may optionally also trigger on the caller client/server as well.<br>
 *<br>
 * Three annotations are used for this purpose.<br>
 * {@link RemoteClient}: Marks a method as able to be invoked remotely on a client from a server.<br>
 * {@link RemoteServer}: Marks a method as able to be invoked remotely on a server from a client.<br>
 * {@link Local}: Makes this method get invoked locally as well as remotely.<br>
 *<br>
 * All RemoteClient methods are put in the class io.anuke.mindustry.gen.CallClient.<br>
 * All RemoteServer methods are put in the class io.anuke.mindustry.gen.CallServer.<br>
 */
public class Annotations {

    /**Marks a method as invokable remotely from a server on a client.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface RemoteClient {
        /**Whether a client-specific method is generated that accepts a connecton ID and sends to only one player. Default is false.*/
        boolean one() default false;
        /**Whether a 'global' method is generated that sends the event to all players. Default is true.*/
        boolean all() default true;
    }

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

    /**Marks a method to be invoked unreliably, e.g. with UDP instead of TCP.
     * This is faster, but is prone to packet loss and duplication.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Unreliable{}

    /**Specifies that this method will be placed in the class specified by its value.
     * Only use constants for this value!*/ //TODO enforce this
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface In{
        String value();
    }

    /**Specifies that this method will be used to write classes of the type returned by {@link #value()}.<br>
     * This method must return void and have two parameters, the first being of type {@link java.nio.ByteBuffer} and the second
     * being the type returned by {@link #value()}.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface WriteClass {
        Class<?> value();
    }

    /**Specifies that this method will be used to read classes of the type returned by {@link #value()}. <br>
     * This method must return the type returned by {@link #value()},
     * and have one parameter, being of type {@link java.nio.ByteBuffer}.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface ReadClass {
        Class<?> value();
    }
}
