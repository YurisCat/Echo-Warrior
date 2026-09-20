package com.yuriscat.echowarrior.compat.test;

/** No client classes: the startup safety policy is also deterministic-testable on a server. */
public final class JoinPausePolicy1201 {
    private final boolean automated;
    private final boolean pauseOnJoin;
    private boolean pauseShown;

    public JoinPausePolicy1201(boolean automated, boolean pauseOnJoin) {
        this.automated = automated;
        this.pauseOnJoin = pauseOnJoin;
    }

    public boolean enabled() { return automated || pauseOnJoin; }
    public boolean needsPause() { return enabled() && !pauseShown; }
    public boolean blockMouseCapture() { return automated || (pauseOnJoin && !pauseShown); }
    public boolean shouldAutoClose() { return automated && pauseShown; }
    public void onPauseShown() { pauseShown = true; }
    public void onDisconnected() { pauseShown = false; }
}
