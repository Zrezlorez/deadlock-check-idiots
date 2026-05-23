package com.litovskiy.util;

public record GrowthContext(double critChance,
                            double critMultiplier,
                            double failChance,
                            double failPercent,
                            double growthModifier) {
    public GrowthContext withFailChanceModifier(double value) {
        return new GrowthContext(critChance, critMultiplier, value, failPercent, growthModifier);
    }

    public GrowthContext withCritChanceModifier(double value) {
        return new GrowthContext(value, critMultiplier, failChance, failPercent, growthModifier);
    }

    public GrowthContext withGrowthModifier(double value) {
        return new GrowthContext(critChance, critMultiplier, failChance, failPercent, value);
    }

    public GrowthContext withCritMultiplier(double value) {
        return new GrowthContext(critChance, value, failChance, failPercent, growthModifier);
    }

    public GrowthContext withFailPercent(double value) {
        return new GrowthContext(critChance, critMultiplier, failChance, value, growthModifier);
    }
}
