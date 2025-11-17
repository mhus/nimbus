package de.mhus.nimbus.generated;

/**
 * Generated from AnimationData.ts
 * DO NOT EDIT MANUALLY - This file is auto-generated
 */
public enum AnimationEffectType {
    SCALE('scale'),
    ROTATE('rotate'),
    TRANSLATE('translate'),
    COLOR_CHANGE('colorChange'),
    FADE('fade'),
    FLASH('flash'),
    PROJECTILE('projectile'),
    EXPLOSION('explosion'),
    PARTICLES('particles'),
    SPAWN_ENTITY('spawnEntity'),
    SKY_CHANGE('skyChange'),
    LIGHT_CHANGE('lightChange'),
    CAMERA_SHAKE('cameraShake'),
    PLAY_SOUND('playSound'),
    BLOCK_BREAK('blockBreak'),
    BLOCK_PLACE('blockPlace'),
    BLOCK_CHANGE('blockChange');

    private final int value;

    AnimationEffectType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
