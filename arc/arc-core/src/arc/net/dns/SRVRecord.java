package arc.net.dns;

public final class SRVRecord implements Comparable<SRVRecord>{
    public final long ttl;
    public final int priority;
    public final int weight;
    public final int port;
    public final String target;

    public SRVRecord(long ttl, int priority, int weight, int port, String target){
        this.ttl = ttl;
        this.priority = priority;
        this.weight = weight;
        this.port = port;
        this.target = target;
    }

    @Override
    public String toString(){
        return "SRVRecord{" +
        "ttl=" + ttl +
        ", priority=" + priority +
        ", weight=" + weight +
        ", port=" + port +
        ", target='" + target + '\'' +
        '}';
    }

    @Override
    public int compareTo(SRVRecord o){
        if(this.priority != o.priority){
            return Integer.compare(this.priority, o.priority);
        }else{
            return Integer.compare(this.weight, o.weight);
        }
    }
}
