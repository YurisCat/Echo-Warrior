package com.yuriscat.echowarrior.compat.client;

public final class SummonerInsertionPolish1211 {
    private final int slotX;
    private final int slotY;
    private final int lifetime;
    private final int[] alphaMask;
    private int age;
    private boolean renderDiagnosticLogged;

    public SummonerInsertionPolish1211(int slotX, int slotY, int lifetime, int[] alphaMask) {
        this.slotX = slotX;
        this.slotY = slotY;
        this.lifetime = lifetime;
        this.alphaMask = alphaMask;
    }

    public int slotX() {
        return this.slotX;
    }

    public int slotY() {
        return this.slotY;
    }

    public int[] alphaMask() {
        return this.alphaMask;
    }

    public double progress(float partialTick) {
        return Math.min(1.0, (this.age + partialTick) / this.lifetime);
    }

    public boolean tickAndExpired() {
        return ++this.age > this.lifetime;
    }

    public boolean markFirstVisibleRenderForDiagnostics() {
        if (this.renderDiagnosticLogged) return false;
        this.renderDiagnosticLogged = true;
        return true;
    }
}
