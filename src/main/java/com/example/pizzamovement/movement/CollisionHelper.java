package com.example.pizzamovement.movement;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/** Cheap surface probes: at most 4 walls x 2 heights + 1 ceiling check per call, no raycasts. */
public final class CollisionHelper {
    private CollisionHelper() {}

    /** True when the block at pos has a full, flat, solid face pointing toward 'face' (the face touching the player). */
    public static boolean isSolidFace(World w, BlockPos pos, Direction face) {
        BlockState s = w.getBlockState(pos);
        if (s.isAir() || s.getMaterial().isLiquid()) return false;
        return s.isFaceSturdy(w, pos, face);
    }

    /** Ceiling surfaces must be flat and solid on their underside. */
    public static boolean isValidCeilingSurface(World w, BlockPos pos) {
        return isSolidFace(w, pos, Direction.DOWN);
    }

    /** Wall in direction d (d points from the player toward the wall). Checks low and high body points. */
    public static boolean hasWall(PlayerEntity p, Direction d) {
        double r = p.getBbWidth() / 2.0 + 0.12;
        double[] heights = {0.4, p.getBbHeight() - 0.2};
        for (double h : heights) {
            BlockPos pos = new BlockPos(p.getX() + d.getStepX() * r, p.getY() + h, p.getZ() + d.getStepZ() * r);
            if (!isSolidFace(p.level, pos, d.getOpposite())) return false;
        }
        return true;
    }

    /**
     * Picks the wall that best matches the movement intent (ix, iz = normalised horizontal input direction).
     * Front, left, right and back are all candidates; a wall directly behind the movement is rejected.
     */
    public static Direction findWall(PlayerEntity p, double ix, double iz) {
        Direction best = null;
        double bestScore = -2.0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!hasWall(p, d)) continue;
            double score = ix * d.getStepX() + iz * d.getStepZ();
            if (score < -0.35) continue;
            if (score > bestScore) {
                bestScore = score;
                best = d;
            }
        }
        return best;
    }

    public static boolean anyWall(PlayerEntity p) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (hasWall(p, d)) return true;
        }
        return false;
    }

    /** Valid ceiling within 'reach' blocks above the head. */
    public static boolean hasCeiling(PlayerEntity p, double reach) {
        BlockPos pos = new BlockPos(p.getX(), p.getY() + p.getBbHeight() + reach, p.getZ());
        return isValidCeilingSurface(p.level, pos);
    }

    /** Unit vector pointing away from the wall. */
    public static Vector3d normal(Direction wallDir) {
        return new Vector3d(-wallDir.getStepX(), 0.0, -wallDir.getStepZ());
    }
}
