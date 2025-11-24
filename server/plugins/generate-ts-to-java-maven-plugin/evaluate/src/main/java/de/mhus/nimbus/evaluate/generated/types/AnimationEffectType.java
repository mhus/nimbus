/*
 * Source TS: AnimationData.ts
 * Original TS: 'enum AnimationEffectType'
 */
package de.mhus.nimbus.evaluate.generated.types;

public enum AnimationEffectType {
    SCALE(1),
    ROTATE(2),
    TRANSLATE(3),
    COLOR_CHANGE(4),
    FADE(5),
    FLASH(6),
    PROJECTILE(7),
    EXPLOSION(8),
    PARTICLES(9),
    SPAWN_ENTITY(10),
    SKY_CHANGE(11),
    LIGHT_CHANGE(12),
    CAMERA_SHAKE(13),
    PLAY_SOUND(14),
    BLOCK_BREAK(15),
    BLOCK_PLACE(16),
    BLOCK_CHANGE(17);

    @lombok.Getter
    private final int tsIndex;
    AnimationEffectType(int tsIndex) { this.tsIndex = tsIndex; }
}
