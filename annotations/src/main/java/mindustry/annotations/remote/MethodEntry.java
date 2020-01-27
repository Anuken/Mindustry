package mindustry.annotations.remote;

import mindustry.annotations.Annotations.*;

import javax.lang.model.element.ExecutableElement;

/** Class that repesents a remote method to be constructed and put into a class. */
public class MethodEntry{
    /** Simple target class name. */
    public final String className;
    /** Fully qualified target method to call. */
    public final String targetMethod;
    /** Whether this method can be called on a client/server. */
    public final Loc where;
    /**
     * Whether an additional 'one' and 'all' method variant is generated. At least one of these must be true.
     * Only applicable to client (server-invoked) methods.
     */
    public final Variant target;
    /** Whether this method is called locally as well as remotely. */
    public final Loc local;
    /** Whether this method is unreliable and uses UDP. */
    public final boolean unreliable;
    /** Whether to forward this method call to all other clients when a client invokes it. Server only. */
    public final boolean forward;
    /** Unique method ID. */
    public final int id;
    /** The element method associated with this entry. */
    public final ExecutableElement element;
    /** The assigned packet priority. Only used in clients. */
    public final PacketPriority priority;

    public MethodEntry(String className, String targetMethod, Loc where, Variant target,
                       Loc local, boolean unreliable, boolean forward, int id, ExecutableElement element, PacketPriority priority){
        this.className = className;
        this.forward = forward;
        this.targetMethod = targetMethod;
        this.where = where;
        this.target = target;
        this.local = local;
        this.id = id;
        this.element = element;
        this.unreliable = unreliable;
        this.priority = priority;
    }

    @Override
    public int hashCode(){
        return targetMethod.hashCode();
    }
}
