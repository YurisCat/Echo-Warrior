package com.yuriscat.echowarrior.compat.client;

public final class SummonerInsertionParticle1211 {
    private final double centerX;
    private final double centerY;
    private final double angle;
    private final double distance;
    private final double spin;
    private final int lifetime;
    private final int color;
    private int age;

    private SummonerInsertionParticle1211(
            double centerX,
            double centerY,
            double angle,
            double distance,
            double spin,
            int lifetime,
            int color) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.angle = angle;
        this.distance = distance;
        this.spin = spin;
        this.lifetime = lifetime;
        this.color = color;
    }

    public static SummonerInsertionParticle1211 fuel(
            double centerX,
            double centerY,
            double angle,
            double distance,
            double spin,
            int lifetime,
            int color) {
        return new SummonerInsertionParticle1211(
                centerX, centerY, angle, distance, spin, lifetime, color);
    }

    public double centerX() {
        return this.centerX;
    }

    public double centerY() {
        return this.centerY;
    }

    public double angle() {
        return this.angle;
    }

    public double distance() {
        return this.distance;
    }

    public double spin() {
        return this.spin;
    }

    public int color() {
        return this.color;
    }

    public double progress(float partialTick) {
        return Math.min(1.0, (this.age + partialTick) / this.lifetime);
    }

    public boolean tickAndExpired() {
        return ++this.age > this.lifetime;
    }
}
