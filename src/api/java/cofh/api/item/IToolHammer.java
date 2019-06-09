package cofh.api.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Implement this interface on subclasses of Item to have that item work as a tool for CoFH mods.
 */
public interface IToolHammer {

    /**
     * Called to ensure that the wrench can be used.
     *
     * @param item The itemstack for the tool. Not required to match equipped item (e.g., multi-tools that contain other tools)
     * @param user The entity using the tool
     * @param pos  Coordinates of the block
     * @return True if this tool can be used
     */
    boolean isUsable(ItemStack item, MobEntity user, BlockPos pos);

    /**
     * Called to ensure that the tool can be used on an entity.
     *
     * @param item   The ItemStack for the tool. Not required to match equipped item (e.g., multi-tools that contain other tools).
     * @param user   The entity using the tool.
     * @param entity The entity the tool is being used on.
     * @return True if this tool can be used.
     */
    boolean isUsable(ItemStack item, MobEntity user, Entity entity);

    /**
     * Callback for when the tool has been used reactively.
     *
     * @param item The ItemStack for the tool. Not required to match equipped item (e.g., multi-tools that contain other tools).
     * @param user The entity using the tool.
     * @param pos  Coordinates of the block.
     */
    void toolUsed(ItemStack item, MobEntity user, BlockPos pos);

    /**
     * Callback for when the tool has been used reactively.
     *
     * @param item   The ItemStack for the tool. Not required to match equipped item (e.g., multi-tools that contain other tools).
     * @param user   The entity using the tool.
     * @param entity The entity the tool is being used on.
     */
    void toolUsed(ItemStack item, MobEntity user, Entity entity);
}