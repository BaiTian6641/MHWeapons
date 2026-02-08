package org.example.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import org.example.common.menu.DecorationWorkbenchMenu;

public class DecorationWorkbenchBlock extends Block {
    public DecorationWorkbenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("null")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> new DecorationWorkbenchMenu(id, inv),
                Component.literal("Decoration Workbench")
        );
        player.openMenu(provider);
        return InteractionResult.CONSUME;
    }
}