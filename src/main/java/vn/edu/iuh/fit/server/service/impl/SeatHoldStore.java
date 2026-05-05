package vn.edu.iuh.fit.server.service.impl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class SeatHoldStore {

  static final long HOLD_TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

  private static final ConcurrentHashMap<String, SeatHold> SEAT_HOLDS = new ConcurrentHashMap<>();
  private static final ScheduledExecutorService HOLD_CLEANER = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "seat-hold-cleaner");
    t.setDaemon(true);
    return t;
  });

  static {
    HOLD_CLEANER.scheduleAtFixedRate(SeatHoldStore::cleanupExpiredHolds, 10, 10, TimeUnit.SECONDS);
  }

  private SeatHoldStore() {
  }

  static SeatHold getActiveHold(String scheduleDetailId, long now) {
    if (scheduleDetailId == null) return null;
    SeatHold hold = SEAT_HOLDS.get(scheduleDetailId);
    if (hold == null) return null;
    if (hold.expiresAtEpochMillis() <= now) {
      SEAT_HOLDS.remove(scheduleDetailId, hold);
      return null;
    }
    return hold;
  }

  static boolean isHeldBy(String scheduleDetailId, String clientSessionId, long now) {
    if (clientSessionId == null) return false;
    SeatHold hold = getActiveHold(scheduleDetailId, now);
    return hold != null && clientSessionId.equals(hold.clientSessionId());
  }

  static boolean tryHold(String scheduleDetailId, String clientSessionId, long expiresAtEpochMillis, long now) {
    if (scheduleDetailId == null || scheduleDetailId.isBlank()) return false;
    if (clientSessionId == null || clientSessionId.isBlank()) return false;

    SeatHold newHold = new SeatHold(clientSessionId, expiresAtEpochMillis);
    AtomicBoolean ok = new AtomicBoolean(false);
    SEAT_HOLDS.compute(scheduleDetailId, (k, existing) -> {
      SeatHold active = existing != null && existing.expiresAtEpochMillis() > now ? existing : null;
      if (active == null || clientSessionId.equals(active.clientSessionId())) {
        ok.set(true);
        return newHold;
      }
      ok.set(false);
      return active;
    });
    return ok.get();
  }

  static boolean releaseHold(String scheduleDetailId, String clientSessionId, long now) {
    if (scheduleDetailId == null || scheduleDetailId.isBlank()) return false;
    if (clientSessionId == null || clientSessionId.isBlank()) return false;

    AtomicBoolean ok = new AtomicBoolean(false);
    SEAT_HOLDS.compute(scheduleDetailId, (k, existing) -> {
      SeatHold active = existing != null && existing.expiresAtEpochMillis() > now ? existing : null;
      if (active == null) {
        ok.set(true);
        return null;
      }
      if (clientSessionId.equals(active.clientSessionId())) {
        ok.set(true);
        return null;
      }
      ok.set(false);
      return active;
    });
    return ok.get();
  }

  static void releaseAll(List<String> scheduleDetailIds, String clientSessionId) {
    if (scheduleDetailIds == null || scheduleDetailIds.isEmpty()) return;
    if (clientSessionId == null || clientSessionId.isBlank()) return;
    long now = System.currentTimeMillis();
    for (String sdId : scheduleDetailIds) {
      if (sdId == null || sdId.isBlank()) continue;
      releaseHold(sdId, clientSessionId, now);
    }
  }

  private static void cleanupExpiredHolds() {
    long now = System.currentTimeMillis();
    SEAT_HOLDS.entrySet().removeIf(e -> e.getValue() == null || e.getValue().expiresAtEpochMillis() <= now);
  }

  record SeatHold(String clientSessionId, long expiresAtEpochMillis) {
  }
}

