package io.anuke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Goal: To create a system to send events to the server from the client and vice versa, without creating a new packet type each time.<br>
 * These events may optionally also trigger on the caller client/server as well.<br>
 */
public class Annotations {

    /**Marks a method as invokable remotely from a server on a client.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Remote {
        /**Whether this method can be invoked from remote clients.*/
        boolean client() default false;
        /**Whether this method can be invoked from the remote server.*/
        boolean server() default true;
        /**Whether a client-specific method is generated that accepts a connecton ID and sends to only one player. Default is false.
         * Only affects client methods.*/
        boolean one() default false;
        /**Whether a 'global' method is generated that sends the event to all players. Default is true.
         * Only affects client methods.*/
        boolean all() default true;
        /**Whether this method is invoked locally as well as remotely.*/
        boolean local() default true;
        /**Whether the packet for this method is sent with UDP instead of TCP.
         * UDP is faster, but is prone to packet loss and duplication.*/
        boolean unreliable() default false;
        /**The simple class name where this method is placed.*/
        String target() default "Call";
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
