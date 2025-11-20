package de.mhus.nimbus.generated.types;

public interface Block {
    Vector3 getPosition();
    double getBlockTypeId();
    Offsets getOffsets();
    java.util.List<Object> getCornerHeights();
    FaceVisibility getFaceVisibility();
    java.lang.Double getStatus();
    java.util.Map<java.lang.Double, BlockModifier> getModifiers();
    BlockMetadata getMetadata();
}
