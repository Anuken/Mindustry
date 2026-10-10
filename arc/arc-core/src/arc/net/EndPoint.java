

package arc.net;

import java.io.IOException;

/**
 * Represents the local end point of a connection.
 * @author Nathan Sweet <misc@n4te.com>
 */
public interface EndPoint extends Runnable{

    /**
     * Adds a listener to the endpoint. If the listener already exists, it is
     * <i>not</i> added again.
     */
    void addListener(NetListener listener);

    void removeListener(NetListener listener);

    /**
     * Continually updates this end point until {@link #stop()} is called.
     */
    void run();

    /**
     * Starts a new thread that calls {@link #run()}.
     */
    void start();

    /**
     * Closes this end point and causes {@link #run()} to return.
     */
    void stop();

    /**
     * @see Client
     * @see Server
     */
    void close();

    /**
     * @see Client#update(int)
     * @see Server#update(int)
     */
    void update(int timeout) throws IOException;

    /**
     * Returns the last thread that called {@link #update(int)} for this end
     * point. This can be useful to detect when long running code will be run on
     * the update thread.
     */
    Thread getUpdateThread();
}
