package mcjty.lib.tileentity;

import mcjty.lib.blocks.LogicSlabBlock;
import mcjty.lib.varia.LogicFacing;
import mcjty.lib.varia.Logging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.BlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;

import static mcjty.lib.blocks.LogicSlabBlock.LOGIC_FACING;
import static mcjty.lib.blocks.LogicSlabBlock.META_INTERMEDIATE;

public class LogicTileEntity extends GenericTileEntity {

    private LogicFacing facing;

    protected int powerOutput = 0;

    @Override
    public void onLoad() {
        if(facing == null) {
            BlockState state = getWorld().getBlockState(getPos());
            if(state.getBlock() instanceof LogicSlabBlock) {
                setFacing(state.getValue(LOGIC_FACING));
            }
        }
        super.onLoad();
    }

    public LogicFacing getFacing(BlockState state) {
        // Should not be needed but apparently it sometimes is
        if (facing == null) {
            Logging.getLogger().log(Level.WARN, "LogicTileEntity has null facing!");
            return LogicFacing.DOWN_TOEAST;
        } else if (!(state.getBlock() instanceof LogicSlabBlock)) {
            Logging.getLogger().log(Level.WARN, "LogicTileEntity expected LogicSlabBlock but had " + state.getBlock().getClass().getName());
            return LogicFacing.DOWN_TOEAST;
        }
        Integer meta = state.getValue(META_INTERMEDIATE);
        return LogicFacing.getFacingWithMeta(facing, meta);
    }

    public void setFacing(LogicFacing facing) {
        if(facing != this.facing) {
            this.facing = facing;
            markDirty();
        }
    }

    public int getPowerOutput() {
        return powerOutput;
    }

    protected void setRedstoneState(int newout) {
        if (powerOutput == newout) {
            return;
        }
        powerOutput = newout;
        markDirty();
        Direction outputSide = getFacing(getWorld().getBlockState(this.pos)).getInputSide().getOpposite();
        getWorld().neighborChanged(this.pos.offset(outputSide), this.getBlockType(), this.pos);
        //        getWorld().notifyNeighborsOfStateChange(this.pos, this.getBlockType());
    }

    @Override
    public void checkRedstone(World world, BlockPos pos) {
        Direction inputSide = getFacing(world.getBlockState(pos)).getInputSide();
        int power = getInputStrength(world, pos, inputSide);
        setPowerInput(power);
    }

    /**
     * Returns the signal strength at one input of the block
     */
    protected int getInputStrength(World world, BlockPos pos, Direction side) {
        int power = world.getRedstonePower(pos.offset(side), side);
        if (power < 15) {
            // Check if there is no redstone wire there. If there is a 'bend' in the redstone wire it is
            // not detected with world.getRedstonePower().
            // Not exactly pretty, but it's how vanilla redstone repeaters do it.
            BlockState blockState = world.getBlockState(pos.offset(side));
            Block b = blockState.getBlock();
            if (b == Blocks.REDSTONE_WIRE) {
                power = Math.max(power, blockState.getValue(BlockRedstoneWire.POWER));
            }
        }

        return power;
    }

    @Override
    public int getRedstoneOutput(BlockState state, IBlockAccess world, BlockPos pos, Direction side) {
        if (side == getFacing(state).getInputSide()) {
            return getPowerOutput();
        } else {
            return 0;
        }
    }

    @Override
    public void readFromNBT(CompoundNBT tagCompound) {
        super.readFromNBT(tagCompound);
        facing = LogicFacing.VALUES[tagCompound.getInteger("lf")];
    }

    @Override
    public CompoundNBT writeToNBT(CompoundNBT tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setInteger("lf", facing.ordinal());
        return tagCompound;
    }

    @Override
    public BlockState getActualState(BlockState state) {
        int meta = state.getValue(META_INTERMEDIATE);
        LogicFacing facing = getFacing(state);
        facing = LogicFacing.getFacingWithMeta(facing, meta);
        return state.withProperty(LOGIC_FACING, facing);
    }
}
