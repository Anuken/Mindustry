package mindustry.annotations;

import java.lang.annotation.*;

public class Annotations{
    //region entity interfaces

    public enum DrawLayer{
        floor,
        floorOver,
        groundShadows,
        groundUnder,
        ground,
        flyingShadows,
        flying,
        bullets,
        effects,
        overlays,
        names,
        weather
    }

    /** Indicates that a method overrides other methods. */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Replace{
    }

    /** Indicates that a method should be final in all implementing classes. */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Final{
    }

    /** Indicates that a component field is imported from other components. */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Import{
    }

    /** Indicates that a component field is read-only. */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReadOnly{
    }

    /** Indicates multiple inheritance on a component type. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Component{
    }

    /** Indicates that a method is implemented by the annotation processor. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface InternalImpl{
    }

    /** Indicates priority of a method in an entity. Methods with higher priority are done last. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface MethodPriority{
        float value();
    }

    /** Indicates that a component def is present on all entities. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface BaseComponent{
    }

    /** Creates a group that only examines entities that have all the components listed. */
    @Retention(RetentionPolicy.SOURCE)
    public @interface GroupDef{
        Class[] value();
        Class[] collide() default {};
        boolean spatial() default false;
        boolean mapping() default false;
    }

    /** Indicates an entity definition. */
    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityDef{
        /** List of component interfaces */
        Class[] value();
        /** Whether the class is final */
        boolean isFinal() default true;
        /** If true, entities are recycled. */
        boolean pooled() default false;
        /** Whether to serialize (makes the serialize method return this value) */
        boolean serialize() default true;
        /** Whether to generate IO code */
        boolean genio() default true;
    }

    /** Indicates an internal interface for entity components. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityInterface{
    }

    //endregion
    //region misc. utility

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface StyleDefaults{
    }

    /** Indicates that a method should always call its super version. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface CallSuper{

    }

    /** Annotation that allows overriding CallSuper annotation. To be used on method that overrides method with CallSuper annotation from parent class. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface OverrideCallSuper{
    }

    /** Marks a class as serializable. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Serialize{

    }

    //endregion
    //region struct

    /** Marks a class as a special value type struct. Class name must end in 'Struct'. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Struct{

    }

    /** Marks a field of a struct. Optional. */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface StructField{
        /** Size of a struct field in bits. Not valid on booleans or floating point numbers. */
        int value();
    }

    //endregion
    //region remote

    public enum PacketPriority{
        /** Gets put in a queue and processed if not connected. */
        normal,
        /** Gets handled immediately, regardless of connection status. */
        high,
        /** Does not get handled unless client is connected. */
        low
    }

    /** A set of two booleans, one specifying server and one specifying client. */
    public enum Loc{
        /** Method can only be invoked on the client from the server. */
        server(true, false),
        /** Method can only be invoked on the server from the client. */
        client(false, true),
        /** Method can be invoked from anywhere */
        both(true, true),
        /** Neither server nor client. */
        none(false, false);

        /** If true, this method can be invoked ON clients FROM servers. */
        public final boolean isServer;
        /** If true, this method can be invoked ON servers FROM clients. */
        public final boolean isClient;

        Loc(boolean server, boolean client){
            this.isServer = server;
            this.isClient = client;
        }
    }

    public enum Variant{
        /** Method can only be invoked targeting one player. */
        one(true, false),
        /** Method can only be invoked targeting all players. */
        all(false, true),
        /** Method targets both one player and all players. */
        both(true, true);

        public final boolean isOne, isAll;

        Variant(boolean isOne, boolean isAll){
            this.isOne = isOne;
            this.isAll = isAll;
        }
    }

    /** Marks a method as invokable remotely across a server/client connection. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Remote{
        /** Specifies the locations from which this method can be invoked. */
        Loc targets() default Loc.server;

        /** Specifies which methods are generated. Only affects server-to-client methods. */
        Variant variants() default Variant.all;

        /** The local locations where this method is called locally, when invoked. */
        Loc called() default Loc.none;

        /** Whether to forward this packet to all other clients upon recieval. Client only. */
        boolean forward() default false;

        /**
         * Whether the packet for this method is sent with UDP instead of TCP.
         * UDP is faster, but is prone to packet loss and duplication.
         */
        boolean unreliable() default false;

        /** Priority of this event. */
        PacketPriority priority() default PacketPriority.normal;
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    public @interface TypeIOHandler{
    }

    //endregion
}
