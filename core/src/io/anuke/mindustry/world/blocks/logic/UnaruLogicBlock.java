package io.anuke.mindustry.world.blocks.logic;

public abstract class UnaruLogicBlock extends LogicBlock{

    public UnaruLogicBlock(String name){
        super(name);
    }

    public abstract byte process(byte input);
}
