package io.anuke.mindustry.world.blocks.logic;

public abstract class BinaryLogicBlock extends LogicBlock{

    public BinaryLogicBlock(String name){
        super(name);
    }

    public abstract byte process(byte left, byte right);
}
