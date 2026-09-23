package shilploka.ai;

/**
 * WHY:
 * Foundation of the behavioral artificial intelligence system for ShilpLoka fauna and hostile mobs.
 *
 * WHAT:
 * Defines lifecycle callbacks for modular goal-directed behaviors.
 *
 * USED FOR:
 * Implemented by wander, attack, flee, and herd behaviors attached to LivingEntity.
 */
public interface AIGoal {
    boolean shouldExecute();
    default void start() {}
    void tick();
    default boolean shouldContinue() { return shouldExecute(); }
    default void stop() {}
}
