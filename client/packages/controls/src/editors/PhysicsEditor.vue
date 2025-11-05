<template>
  <div class="space-y-3 pt-2">
    <div class="form-control">
      <label class="label cursor-pointer justify-start gap-2">
        <input
          v-model="localValue.solid"
          type="checkbox"
          class="checkbox checkbox-sm"
        />
        <span class="label-text">Solid (has collision)</span>
      </label>
    </div>

    <div class="form-control">
      <label class="label cursor-pointer justify-start gap-2">
        <input
          v-model="localValue.interactive"
          type="checkbox"
          class="checkbox checkbox-sm"
        />
        <span class="label-text">Interactive</span>
      </label>
    </div>

    <div class="form-control">
      <label class="label">
        <span class="label-text">Resistance</span>
      </label>
      <input
        v-model.number="localValue.resistance"
        type="number"
        step="0.1"
        class="input input-bordered input-sm"
        placeholder="Movement resistance"
      />
    </div>

    <div class="form-control">
      <label class="label">
        <span class="label-text">Climbable (climb speed)</span>
      </label>
      <input
        v-model.number="localValue.climbable"
        type="number"
        step="0.1"
        min="0"
        class="input input-bordered input-sm"
        placeholder="0 = not climbable"
      />
      <label class="label">
        <span class="label-text-alt">Climb speed when moving forward (e.g., 0.5 = ladder, 1.0 = fast)</span>
      </label>
    </div>

    <div class="form-control">
      <label class="label cursor-pointer justify-start gap-2">
        <input
          v-model="localValue.autoClimbable"
          type="checkbox"
          class="checkbox checkbox-sm"
        />
        <span class="label-text">Auto Climbable (auto-climb 1 block)</span>
      </label>
    </div>

    <div class="form-control">
      <label class="label cursor-pointer justify-start gap-2">
        <input
          v-model="localValue.autoJump"
          type="checkbox"
          class="checkbox checkbox-sm"
        />
        <span class="label-text">Auto Jump (trigger jump automatically)</span>
      </label>
    </div>

    <div class="form-control">
      <label class="label">
        <span class="label-text">Auto Orientation Y (rotation angle)</span>
      </label>
      <input
        v-model.number="localValue.autoOrientationY"
        type="number"
        step="0.1"
        class="input input-bordered input-sm"
        placeholder="Angle in radians (e.g., 0, 1.57, 3.14)"
      />
      <label class="label">
        <span class="label-text-alt">Rotation in radians (0° = 0, 90° = 1.57, 180° = 3.14, 270° = 4.71)</span>
      </label>
    </div>

    <div class="form-control">
      <label class="label">
        <span class="label-text">Auto Move (velocity when on/in block)</span>
      </label>
      <div class="flex gap-2">
        <input
          v-model.number="autoMoveX"
          type="number"
          step="0.1"
          class="input input-bordered input-sm flex-1"
          placeholder="X"
        />
        <input
          v-model.number="autoMoveY"
          type="number"
          step="0.1"
          class="input input-bordered input-sm flex-1"
          placeholder="Y"
        />
        <input
          v-model.number="autoMoveZ"
          type="number"
          step="0.1"
          class="input input-bordered input-sm flex-1"
          placeholder="Z"
        />
      </div>
      <label class="label">
        <span class="label-text-alt">Blocks per second (e.g., 2 0 0 = conveyor belt)</span>
      </label>
    </div>

    <div class="form-control">
      <label class="label">
        <span class="label-text">Passable From (one-way/wall directions)</span>
      </label>
      <div class="flex flex-wrap gap-3 p-2 bg-base-200 rounded">
        <label class="label cursor-pointer gap-2">
          <input
            v-model="passableFromNorth"
            type="checkbox"
            class="checkbox checkbox-xs"
          />
          <span class="label-text text-xs">North</span>
        </label>
        <label class="label cursor-pointer gap-2">
          <input
            v-model="passableFromSouth"
            type="checkbox"
            class="checkbox checkbox-xs"
          />
          <span class="label-text text-xs">South</span>
        </label>
        <label class="label cursor-pointer gap-2">
          <input
            v-model="passableFromEast"
            type="checkbox"
            class="checkbox checkbox-xs"
          />
          <span class="label-text text-xs">East</span>
        </label>
        <label class="label cursor-pointer gap-2">
          <input
            v-model="passableFromWest"
            type="checkbox"
            class="checkbox checkbox-xs"
          />
          <span class="label-text text-xs">West</span>
        </label>
        <label class="label cursor-pointer gap-2">
          <input
            v-model="passableFromUp"
            type="checkbox"
            class="checkbox checkbox-xs"
          />
          <span class="label-text text-xs">Up</span>
        </label>
        <label class="label cursor-pointer gap-2">
          <input
            v-model="passableFromDown"
            type="checkbox"
            class="checkbox checkbox-xs"
          />
          <span class="label-text text-xs">Down</span>
        </label>
        <button
          @click="resetPassableFrom"
          class="btn btn-xs btn-ghost"
        >
          Reset
        </button>
      </div>
      <label class="label">
        <span class="label-text-alt">Solid: one-way block. Non-solid: wall at edges.</span>
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { PhysicsModifier } from '@nimbus/shared';
import { Direction, DirectionHelper } from '@nimbus/shared';

interface Props {
  modelValue?: PhysicsModifier;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: PhysicsModifier | undefined): void;
}>();

const localValue = ref<PhysicsModifier>(
  props.modelValue ? JSON.parse(JSON.stringify(props.modelValue)) : {}
);

// Computed properties for autoMove Vector3 components
const autoMoveX = computed({
  get: () => localValue.value.autoMove?.x ?? 0,
  set: (value: number) => {
    if (!localValue.value.autoMove) {
      localValue.value.autoMove = { x: 0, y: 0, z: 0 };
    }
    localValue.value.autoMove.x = value;
  }
});

const autoMoveY = computed({
  get: () => localValue.value.autoMove?.y ?? 0,
  set: (value: number) => {
    if (!localValue.value.autoMove) {
      localValue.value.autoMove = { x: 0, y: 0, z: 0 };
    }
    localValue.value.autoMove.y = value;
  }
});

const autoMoveZ = computed({
  get: () => localValue.value.autoMove?.z ?? 0,
  set: (value: number) => {
    if (!localValue.value.autoMove) {
      localValue.value.autoMove = { x: 0, y: 0, z: 0 };
    }
    localValue.value.autoMove.z = value;
  }
});

// Computed properties for passableFrom direction flags
const passableFromNorth = computed({
  get: () => DirectionHelper.hasDirection(localValue.value.passableFrom ?? 0, Direction.NORTH),
  set: (value: boolean) => {
    const current = localValue.value.passableFrom ?? 0;
    localValue.value.passableFrom = value
      ? DirectionHelper.addDirection(current, Direction.NORTH)
      : DirectionHelper.removeDirection(current, Direction.NORTH);
  }
});

const passableFromSouth = computed({
  get: () => DirectionHelper.hasDirection(localValue.value.passableFrom ?? 0, Direction.SOUTH),
  set: (value: boolean) => {
    const current = localValue.value.passableFrom ?? 0;
    localValue.value.passableFrom = value
      ? DirectionHelper.addDirection(current, Direction.SOUTH)
      : DirectionHelper.removeDirection(current, Direction.SOUTH);
  }
});

const passableFromEast = computed({
  get: () => DirectionHelper.hasDirection(localValue.value.passableFrom ?? 0, Direction.EAST),
  set: (value: boolean) => {
    const current = localValue.value.passableFrom ?? 0;
    localValue.value.passableFrom = value
      ? DirectionHelper.addDirection(current, Direction.EAST)
      : DirectionHelper.removeDirection(current, Direction.EAST);
  }
});

const passableFromWest = computed({
  get: () => DirectionHelper.hasDirection(localValue.value.passableFrom ?? 0, Direction.WEST),
  set: (value: boolean) => {
    const current = localValue.value.passableFrom ?? 0;
    localValue.value.passableFrom = value
      ? DirectionHelper.addDirection(current, Direction.WEST)
      : DirectionHelper.removeDirection(current, Direction.WEST);
  }
});

const passableFromUp = computed({
  get: () => DirectionHelper.hasDirection(localValue.value.passableFrom ?? 0, Direction.UP),
  set: (value: boolean) => {
    const current = localValue.value.passableFrom ?? 0;
    localValue.value.passableFrom = value
      ? DirectionHelper.addDirection(current, Direction.UP)
      : DirectionHelper.removeDirection(current, Direction.UP);
  }
});

const passableFromDown = computed({
  get: () => DirectionHelper.hasDirection(localValue.value.passableFrom ?? 0, Direction.DOWN),
  set: (value: boolean) => {
    const current = localValue.value.passableFrom ?? 0;
    localValue.value.passableFrom = value
      ? DirectionHelper.addDirection(current, Direction.DOWN)
      : DirectionHelper.removeDirection(current, Direction.DOWN);
  }
});

// Reset passableFrom to undefined
const resetPassableFrom = () => {
  localValue.value.passableFrom = undefined;
};

watch(localValue, (newValue) => {
  emit('update:modelValue', newValue);
}, { deep: true });
</script>
