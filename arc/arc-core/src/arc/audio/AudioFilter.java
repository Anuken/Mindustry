package arc.audio;

public abstract class AudioFilter{
    protected long handle;

    protected AudioFilter(long handle){
        this.handle = handle;
    }
}
