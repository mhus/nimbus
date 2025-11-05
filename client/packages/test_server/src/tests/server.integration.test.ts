/**
 * Integration tests for NimbusServer
 * Tests against test server on localhost:3111 (separate from dev server on 3011)
 */

const BASE_URL = process.env.TEST_SERVER_URL || 'http://localhost:3111';

describe('NimbusServer Integration Tests', () => {
  describe('Health Check', () => {
    it('should respond to health check', async () => {
      const response = await fetch(`${BASE_URL}/health`);
      const data = (await response.json()) as any;

      expect(response.status).toBe(200);
      expect(data.status).toBe('ok');
      expect(data.version).toBe('2.0.0');
    });
  });

  describe('World API', () => {
    it('should list all worlds', async () => {
      const response = await fetch(`${BASE_URL}/api/worlds`);
      const worlds = (await response.json()) as any[];

      expect(response.status).toBe(200);
      expect(Array.isArray(worlds)).toBe(true);
      expect(worlds.length).toBeGreaterThan(0);
      expect(worlds[0]).toHaveProperty('worldId');
      expect(worlds[0]).toHaveProperty('name');
      expect(worlds[0]).toHaveProperty('chunkSize');
    });

    it('should get specific world details', async () => {
      const response = await fetch(`${BASE_URL}/api/worlds/test-world-1`);
      const world = (await response.json()) as any;

      expect(response.status).toBe(200);
      expect(world.worldId).toBe('test-world-1');
      expect(world.name).toBe('Test World 1');
      expect(world.chunkSize).toBe(16);
      expect(world.dimensions).toBeDefined();
    });

    it('should return 404 for non-existent world', async () => {
      const response = await fetch(`${BASE_URL}/api/worlds/non-existent`);

      expect(response.status).toBe(404);
    });
  });

  describe('BlockType API', () => {
    it('should get AIR BlockType (ID: 0)', async () => {
      const response = await fetch(
        `${BASE_URL}/api/worlds/test-world-1/blocktypes/0`
      );
      const blockType = (await response.json()) as any;

      expect(response.status).toBe(200);
      expect(blockType.id).toBe(0);
      expect(blockType.name).toBe('Air');
      expect(blockType.description).toBeDefined();
      expect(blockType.modifiers).toBeDefined();
      expect(blockType.modifiers[0]).toBeDefined();
      expect(blockType.modifiers[0].visibility.shape).toBe(0); // INVISIBLE
    });

    it('should get BlockType with texture (ID: 100)', async () => {
      const response = await fetch(
        `${BASE_URL}/api/worlds/test-world-1/blocktypes/100`
      );
      const blockType = (await response.json()) as any;

      expect(response.status).toBe(200);
      expect(blockType.id).toBe(100);
      expect(blockType.name).toBeDefined();
      expect(blockType.name).not.toBe('');
      expect(blockType.description).toBeDefined();
      expect(blockType.modifiers[0].visibility.textures).toBeDefined();
      expect(blockType.modifiers[0].visibility.textures[0]).toContain('.png');
    });

    it('should get BlockType range (100-105)', async () => {
      const response = await fetch(
        `${BASE_URL}/api/worlds/test-world-1/blocktypes/100/105`
      );
      const blockTypes = (await response.json()) as any[];

      expect(response.status).toBe(200);
      expect(Array.isArray(blockTypes)).toBe(true);
      expect(blockTypes.length).toBeGreaterThan(0);
      expect(blockTypes.length).toBeLessThanOrEqual(6);

      blockTypes.forEach((bt: any) => {
        expect(bt.id).toBeGreaterThanOrEqual(100);
        expect(bt.id).toBeLessThanOrEqual(105);
        expect(bt.name).toBeDefined();
        expect(bt.description).toBeDefined();
      });
    });

    it('should return 404 for non-existent BlockType', async () => {
      const response = await fetch(
        `${BASE_URL}/api/worlds/test-world-1/blocktypes/99999`
      );

      expect(response.status).toBe(404);
    });

    it('should have texture path with .png extension', async () => {
      const response = await fetch(
        `${BASE_URL}/api/worlds/test-world-1/blocktypes/200`
      );
      const blockType = (await response.json()) as any;

      expect(response.status).toBe(200);
      const texturePath = blockType.modifiers[0].visibility.textures[0];
      expect(texturePath).toMatch(/\.png$/);
    });

    it('should load different BlockTypes correctly', async () => {
      const ids = [0, 100, 200, 300, 400, 500, 600];
      const results = await Promise.all(
        ids.map((id) =>
          fetch(`${BASE_URL}/api/worlds/test-world-1/blocktypes/${id}`)
        )
      );

      const blockTypes = await Promise.all(
        results.map((r) => (r.ok ? r.json() : null))
      );

      const validBlockTypes = blockTypes.filter((bt) => bt !== null);
      expect(validBlockTypes.length).toBeGreaterThan(5);

      validBlockTypes.forEach((bt: any) => {
        expect(bt.id).toBeDefined();
        expect(bt.name).toBeDefined();
        expect(bt.description).toBeDefined();
        expect(bt.modifiers).toBeDefined();
      });
    });
  });

  describe('BlockType Lazy Loading', () => {
    it('should load BlockTypes on-demand without caching issues', async () => {
      // Request same BlockType twice
      const response1 = await fetch(
        `${BASE_URL}/api/worlds/test-world-1/blocktypes/150`
      );
      const bt1 = (await response1.json()) as any;

      const response2 = await fetch(
        `${BASE_URL}/api/worlds/test-world-1/blocktypes/150`
      );
      const bt2 = (await response2.json()) as any;

      expect(bt1).toEqual(bt2);
      expect(bt1.id).toBe(150);
    });
  });

  // Note: CORS headers are set by server, but node-fetch doesn't expose them in tests
  // CORS can be verified manually with browser or curl
});
