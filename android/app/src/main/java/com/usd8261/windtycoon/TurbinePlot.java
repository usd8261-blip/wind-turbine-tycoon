package com.usd8261.windtycoon;

public class TurbinePlot {
    public final int row;
    public final int col;
    public TurbineType type = null;
    public int level = 1;
    public float condition = 100.0f;
    public float bladeAngle = 0.0f;
    public float bladeSpeed = 2.0f; // degrees per frame
    public boolean isBroken = false;
    public boolean isBurning = false;

    public TurbinePlot(int row, int col) {
        this.row = row;
        this.col = col;
        this.bladeAngle = (float) (Math.random() * 360.0);
    }

    public boolean isEmpty() {
        return type == null;
    }

    public double getBuildCost() {
        if (type == null) {
            return TurbineType.SMALL.baseCost;
        }
        return type.baseCost;
    }

    public double getUpgradeCost() {
        if (type == null) return 0;
        return type.baseCost * 0.45 * Math.pow(1.35, level);
    }

    public boolean canEvolve() {
        return type != null && type.getNext() != null;
    }

    public TurbineType getNextEvolutionType() {
        return type != null ? type.getNext() : null;
    }

    public double getEvolutionCost() {
        TurbineType next = getNextEvolutionType();
        if (next == null) return 0;
        return next.baseCost * 0.9;
    }

    public void evolve() {
        TurbineType next = getNextEvolutionType();
        if (next != null) {
            this.type = next;
            this.level = 1;
            this.condition = 100.0f;
            this.isBroken = false;
            this.isBurning = false;
        }
    }

    public void upgrade() {
        if (level < 10) {
            level++;
            condition = Math.min(100.0f, condition + 15.0f);
            isBroken = false;
            isBurning = false;
        }
    }

    public double getRepairCost() {
        if (type == null || condition >= 100.0f) return 0;
        return Math.max(5.0, (100.0f - condition) * (type.baseCost * 0.0015));
    }

    public void repair() {
        condition = 100.0f;
        isBroken = false;
        isBurning = false;
    }

    public void extinguish() {
        isBurning = false;
    }

    public float getOutputKW(float windSpeed, float aeroMultiplier) {
        if (isEmpty() || isBroken || isBurning) return 0.0f;

        // Cut-in speed: 2.5 m/s, rated: 12 m/s, cut-out: 25 m/s
        if (windSpeed < 2.5f || windSpeed > 25.0f) return 0.0f;

        float windFactor = Math.min(1.5f, windSpeed / 10.0f);
        float levelFactor = 1.0f + (level - 1) * 0.25f;
        float conditionFactor = condition / 100.0f;

        return type.baseKW * windFactor * levelFactor * conditionFactor * aeroMultiplier;
    }

    public void update(float windSpeed, boolean autoDrones, boolean hasTech, float wearMultiplier) {
        if (isEmpty()) return;

        // Rotate blades
        if (!isBroken && !isBurning && windSpeed >= 2.5f) {
            float speed = Math.max(1.0f, windSpeed * 0.6f);
            bladeAngle = (bladeAngle + speed) % 360.0f;
        }

        // Fire logic
        if (isBurning) {
            condition = Math.max(0.0f, condition - 0.12f);
            if (condition <= 0.0f) {
                isBroken = true;
            }
            if (autoDrones) {
                isBurning = false;
            }
            return;
        }

        // Potential ignition on extreme wind and low condition
        if (!isBroken && windSpeed > 18.0f && condition < 45.0f && Math.random() < 0.0006) {
            isBurning = true;
        }

        // Wear and tear
        float wearRate = 0.003f + (windSpeed > 14.0f ? 0.008f : 0.0f);
        if (hasTech) wearRate *= 0.4f;
        wearRate *= wearMultiplier;

        if (condition > 0) {
            condition -= wearRate;
            if (condition <= 0) {
                condition = 0;
                isBroken = true;
            }
        }

        // Auto drone repairs
        if (autoDrones && condition < 95.0f) {
            condition = Math.min(100.0f, condition + 0.08f);
            if (condition > 5.0f) {
                isBroken = false;
            }
        }
    }
}
