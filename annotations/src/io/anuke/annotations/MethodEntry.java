package io.anuke.annotations;

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

    public MethodEntry(String className, String targetMethod, boolean client, boolean server, boolean allVariant, boolean oneVariant) {
        this.className = className;
        this.targetMethod = targetMethod;
        this.client = client;
        this.server = server;
        this.allVariant = allVariant;
        this.oneVariant = oneVariant;
    }
}
