package org.example.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.example.common.menu.BowgunWorkbenchMenu;

/**
 * Block that opens the Bowgun Modification Workbench GUI.
 * Follows exact same pattern as DecorationWorkbenchBlock.
 */
public class BowgunWorkbenchBlock extends Block {
    public BowgunWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("null")
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> new BowgunWorkbenchMenu(id, inv),
                Component.literal("Bowgun Modification Workbench")
        );
        player.openMenu(provider);
        return InteractionResult.CONSUME;
    }
}
