package mindustry.world.blocks.production;

import mindustry.world.blocks.payloads.*;

/** @deprecated used PayloadBlock instead. */
@Deprecated
public abstract class PayloadAcceptor extends PayloadBlock{

    public PayloadAcceptor(String name){
        super(name);
    }

    /** @deprecated used PayloadBlockBuild instead. */
    @Deprecated
    public class PayloadAcceptorBuild<T extends Payload> extends PayloadBlockBuild<T>{

    }
}
