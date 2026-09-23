package shilploka.ai;

import org.joml.Vector3i;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkManager;

import java.util.*;

/**
 * WHY:
 * Provides 3D voxel terrain navigation for entities across natural Indian terrain,
 * hills, rivers, and caves.
 *
 * WHAT:
 * Performs A* search taking into account gravity, step-height (1 block step up),
 * fall safety (avoids cliffs > 3 blocks), and water avoidance.
 *
 * USED FOR:
 * Consulted by LivingEntity navigation goals to pathfind toward targets or waypoints.
 */
public class Pathfinder {

    public static final int MAX_STEP_UP = 1;
    public static final int MAX_DROP_DOWN = 3;

    private static class Node implements Comparable<Node> {
        final Vector3i pos;
        final Node parent;
        final float gCost;
        final float hCost;
        final float fCost;

        Node(Vector3i pos, Node parent, float gCost, float hCost) {
            this.pos = pos;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }

        @Override
        public int compareTo(Node o) {
            return Float.compare(this.fCost, o.fCost);
        }
    }

    public static List<Vector3i> findPath(Vector3i start, Vector3i target, ChunkManager chunkManager, int maxNodes) {
        if (start.equals(target)) {
            return Collections.emptyList();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Vector3i, Float> gScores = new HashMap<>();
        Set<Vector3i> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0.0f, heuristic(start, target));
        openSet.add(startNode);
        gScores.put(start, 0.0f);

        int nodesEvaluated = 0;
        Node closestNode = startNode;
        float closestDistance = heuristic(start, target);

        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};

        while (!openSet.isEmpty() && nodesEvaluated < maxNodes) {
            Node current = openSet.poll();
            nodesEvaluated++;

            float distToTarget = heuristic(current.pos, target);
            if (distToTarget < closestDistance) {
                closestDistance = distToTarget;
                closestNode = current;
            }

            if (current.pos.x == target.x && current.pos.z == target.z && Math.abs(current.pos.y - target.y) <= 1) {
                return reconstructPath(current);
            }

            closedSet.add(current.pos);

            for (int i = 0; i < 4; i++) {
                int nx = current.pos.x + dx[i];
                int nz = current.pos.z + dz[i];

                // Check possible elevation transitions (-MAX_DROP_DOWN to +MAX_STEP_UP)
                for (int dy = MAX_STEP_UP; dy >= -MAX_DROP_DOWN; dy--) {
                    int ny = current.pos.y + dy;
                    Vector3i neighborPos = new Vector3i(nx, ny, nz);

                    if (closedSet.contains(neighborPos)) continue;

                    if (isWalkable(nx, ny, nz, chunkManager)) {
                        float tentativeG = current.gCost + ((dy != 0) ? 1.5f : 1.0f);

                        if (tentativeG < gScores.getOrDefault(neighborPos, Float.MAX_VALUE)) {
                            gScores.put(neighborPos, tentativeG);
                            Node neighborNode = new Node(neighborPos, current, tentativeG, heuristic(neighborPos, target));
                            openSet.add(neighborNode);
                        }
                        break; // Only pick the highest valid landing surface at (nx, nz)
                    }
                }
            }
        }

        // If exact target unreachable within budget, return path to closest explored node
        return reconstructPath(closestNode);
    }

    private static boolean isWalkable(int x, int y, int z, ChunkManager chunkManager) {
        // Block below must be solid (not air or fluid)
        int blockBelow = chunkManager.getBlock(x, y - 1, z);
        if (blockBelow == BlockIds.AIR || blockBelow == BlockIds.WATER) {
            return false;
        }

        // Body voxel and head voxel must be non-colliding (air)
        int body = chunkManager.getBlock(x, y, z);
        int head = chunkManager.getBlock(x, y + 1, z);
        return body == BlockIds.AIR && head == BlockIds.AIR;
    }

    private static float heuristic(Vector3i a, Vector3i b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        int dz = a.z - b.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static List<Vector3i> reconstructPath(Node node) {
        List<Vector3i> path = new ArrayList<>();
        Node curr = node;
        while (curr != null && curr.parent != null) {
            path.add(0, curr.pos);
            curr = curr.parent;
        }
        return path;
    }
}
