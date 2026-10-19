package arc.net.dns;

import arc.struct.*;

import java.net.*;

import static arc.net.dns.ArcDns.dnsResolverPort;

public final class WellKnownNameserverProvider implements NameserverProvider{
    private final Seq<InetSocketAddress> nameservers = Seq.with(
    new InetSocketAddress("1.1.1.1", dnsResolverPort),   // Cloudflare
    new InetSocketAddress("8.8.8.8", dnsResolverPort)    // Google
    );

    @Override
    public Seq<InetSocketAddress> getNameservers(){
        return nameservers;
    }
}
