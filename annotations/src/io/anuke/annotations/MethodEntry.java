package io.anuke.annotations;

import javax.lang.model.element.ExecutableElement;

/**Class that repesents a remote method to be constructed and put into a class.*/
public class MethodEntry {
    /**Simple target class name.*/
    public final String className;
    /**Fully qualified target method to call.*/
    public final String targetMethod;
    /**Whether this method can be called on a client/server.*/
    public final boolean client, server;
    /**Whether an additional 'one' and 'all' method variant is generated. At least one of these must be true.
     * Only applicable to client (server-invoked) methods.*/
    public final boolean allVariant, oneVariant;
    /**Whether this method is called locally as well as remotely.*/
    public final boolean local;
    /**Whether this method is unreliable and uses UDP.*/
    public final boolean unreliable;
    /**Unique method ID.*/
    public final int id;
    /**The element method associated with this entry.*/
    public final ExecutableElement element;

    public MethodEntry(String className, String targetMethod, boolean client, boolean server,
                       boolean allVariant, boolean oneVariant, boolean local, boolean unreliable, int id, ExecutableElement element) {
        this.className = className;
        this.targetMethod = targetMethod;
        this.client = client;
        this.server = server;
        this.allVariant = allVariant;
        this.oneVariant = oneVariant;
        this.local = local;
        this.id = id;
        this.element = element;
        this.unreliable = unreliable;
    }

    @Override
    public int hashCode() {
        return targetMethod.hashCode();
    }
}
