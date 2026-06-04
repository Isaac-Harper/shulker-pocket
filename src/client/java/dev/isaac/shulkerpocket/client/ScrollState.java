package dev.isaac.shulkerpocket.client;

/** Client-side cooldown gate so one physical flick of the wheel doesn't fire many events. */
public final class ScrollState {
    private long lastFireMs = 0L;

    /** Returns true (and arms the cooldown) if at least {@code cooldownMs} has elapsed. */
    public boolean tryFire(long cooldownMs) {
        long now = System.currentTimeMillis();
        if (now - lastFireMs < cooldownMs) return false;
        lastFireMs = now;
        return true;
    }
}
