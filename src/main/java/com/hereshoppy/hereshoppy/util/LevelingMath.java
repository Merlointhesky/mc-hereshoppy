package com.hereshoppy.hereshoppy.util;

public class LevelingMath {
    public static int calculateLevel(int totalXp) {
        if (totalXp < 20) return 0;
        // Derived from TotalXP = 5N^2 + 15N
        double level = (-15.0 + Math.sqrt(225.0 + (20.0 * totalXp))) / 10.0;
        return (int) Math.floor(level);
    }
}
