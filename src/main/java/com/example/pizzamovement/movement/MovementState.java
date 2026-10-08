package com.example.pizzamovement.movement;

public enum MovementState {
    NORMAL,          // stage 0, vanilla movement
    ACCELERATING,    // stages 1-3
    RUNNING,         // stage 4 (max)
    WALL_RUNNING,
    CEILING_RUNNING,
    WALL_JUMPING;

    private static final MovementState[] VALUES = values();

    public static MovementState byOrdinal(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : NORMAL;
    }
}
