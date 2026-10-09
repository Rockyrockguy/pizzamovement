package com.example.pizzamovement.jetpack;

import com.example.pizzamovement.config.PizzaConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/**
 * At (almost) maximum speed with the engine on, breaks the blocks the player is about to fly into.
 * Uses the normal player block-break path, so protection plugins/events still apply. Skips unbreakable blocks,
 * blocks harder than the configured limit, fluids, spawn-protected blocks and anything with a tile entity.
 */
public final class JetpackBreaker {
    private static final int MAX_BLOCKS_PER_TICK = 16;

    private JetpackBreaker() {}

    public static void tick(ServerPlayerEntity p, ItemStack stack, JetpackState.Entry e) {
        PizzaConfig.Server cfg = PizzaConfig.SERVER;
        if (!cfg.jetpackBreakBlocks.get() || !e.hasPos) return;

        Vector3d pos = p.position();
        Vector3d move = pos.subtract(e.lx, e.ly, e.lz);
        double speed = move.length();                                   // blocks per tick, measured by the server
        if (speed < cfg.jetpackMaxSpeed.get() * cfg.jetpackBreakSpeedFraction.get()) return;

        Vector3d dir = move.scale(1.0 / speed);
        double reach = Math.max(1.5, speed * 1.5);                      // look ahead so blocks go before we hit them
        AxisAlignedBB box = p.getBoundingBox().expandTowards(dir.scale(reach)).inflate(0.1);

        int broken = 0;
        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX) && broken < MAX_BLOCKS_PER_TICK; x++) {
            for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY) && broken < MAX_BLOCKS_PER_TICK; y++) {
                for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ) && broken < MAX_BLOCKS_PER_TICK; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    // only blocks that are really in front of us
                    Vector3d toBlock = new Vector3d(x + 0.5 - pos.x, y + 0.5 - (pos.y + p.getBbHeight() * 0.5), z + 0.5 - pos.z);
                    if (toBlock.dot(dir) <= 0.0) continue;

                    BlockState s = p.level.getBlockState(bp);
                    if (s.isAir() || !s.getFluidState().isEmpty() || s.hasTileEntity()) continue;
                    float hardness = s.getDestroySpeed(p.level, bp);
                    if (hardness < 0.0F || hardness > cfg.jetpackBreakMaxHardness.get()) continue;
                    if (!p.level.mayInteract(p, bp)) continue;

                    if (p.gameMode.destroyBlock(bp)) {
                        broken++;
                        if (++e.broken % 4 == 0) {                      // every 4th block costs one fuel point
                            stack.hurtAndBreak(1, p, pl -> pl.broadcastBreakEvent(EquipmentSlotType.CHEST));
                        }
                    }
                }
            }
        }
    }
}
