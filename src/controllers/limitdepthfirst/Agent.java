package controllers.limitdepthfirst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Created with IntelliJ IDEA. User: ssamot Date: 14/11/13 Time: 21:45 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends controllers.sampleRandom.Agent {

    class Node {
        public StateObservation stObs;

        // 生成这个局面的那一步行动
        public Types.ACTIONS action = null;

        // 在 fringe 中的索引
        public Node parent = null;

        // 深度
        public int depth = 0;

        // Cutoff, 其实是 h() heuristic function 的结果
        // -2 是默认初始值, -1 是失败, 0 是成功找到路径, >= 1 是启发式函数的预测
        public int cutoff = -2;

        Node(StateObservation stObs, Types.ACTIONS action, Node parent, int depth) {
            this.stObs = stObs;
            this.action = action;
            this.parent = parent;
            this.depth = depth;
        }
    }

    /**
     * Public constructor with state observation and time due.
     * 
     * @param so           state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        super(so, elapsedTimer);
    }

    // 用于判断是否形成回路
    private Set<StateObservation> stateObservationsSet = new HashSet<>();

    // 用于保存搜索到的解答, 根据局面获取步骤
    private Map<StateObservation, Types.ACTIONS> stObs2Actions = new HashMap<>();

    private int getDist(Vector2d a, Vector2d b) {
        return (int) Math.abs(a.x / 50 - b.x / 50) + (int) Math.abs(a.y / 50 - b.y / 50);
    }

    /**
     * heuristic function
     * 
     * @param node  The node.
     * @return The heuristic function result.
     */
    private int heuristic(Node node) {
        StateObservation stObs = node.stObs;
        ArrayList<Observation>[] fixedPositions = stObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stObs.getMovablePositions();
        Vector2d avatarPos = stObs.getAvatarPosition();
        Vector2d goalPos = fixedPositions[1].get(0).position;
        if (movingPositions[0].size() != 0) {
            // 没吃到钥匙, 就返回从当前位置到钥匙位置再到目标位置的距离
            Vector2d keyPos = movingPositions[0].get(0).position;
            return getDist(avatarPos, keyPos) + getDist(keyPos, goalPos);
        } else {
            // 吃到钥匙, 就返回从当前位置到目标位置的距离
            return getDist(avatarPos, goalPos);
        }
    }


    /**
     * depth limited search recursively
     * 
     * @param node  The current node.
     * @param limit The max depth.
     * @return The final node with h() heuristic function result.
     */
    private Node recursiveDLS(Node node, int limit) {

        // Init cutoffNode
        Node cutoffNode = null;

        // if success
        if (node.stObs.isGameOver() && node.stObs.getGameWinner() == Types.WINNER.PLAYER_WINS) {
            System.out.println("Success to find the path.");
            Node current = node;
            while (current.parent != null) {
                // 循环加入可行的行动
                if (stObs2Actions.containsKey(current.parent.stObs)) {
                    System.out.println("Error: State already exists.");
                } else {
                    stObs2Actions.put(current.parent.stObs, current.action);
                }
                current = current.parent;
            }
            node.cutoff = 0;
            return node;
        } else if (node.stObs.isGameOver() || stateObservationsSet.contains(node.stObs)
                || node.stObs.getAvailableActions().size() == 0) {
            // Return failure
            node.cutoff = -1;
            return node;
        } else if (node.depth == limit) {
            // Add to closed set
            stateObservationsSet.add(node.stObs);
            node.cutoff = heuristic(node);
            return node;
        } else {
            // Add to closed set
            stateObservationsSet.add(node.stObs);
            // Expand
            for (Types.ACTIONS action : node.stObs.getAvailableActions()) {
                StateObservation stateCpy = node.stObs.copy();
                stateCpy.advance(action);
                Node newNode = new Node(stateCpy, action, node, node.depth + 1);
                Node result = recursiveDLS(newNode, limit);
                if (result.cutoff != 0 && result.cutoff != -1) {
                    if (cutoffNode == null || result.cutoff < cutoffNode.cutoff) {
                        cutoffNode = result;
                    }
                } else if (result.cutoff == 0) {
                    return result;
                }
            }
            if (cutoffNode != null) {
                return cutoffNode;
            } else {
                // return failure
                node.cutoff = -1;
                return node;
            }
        }
    }

    /**
     * Picks a random action.
     * 
     * @param stateObs Observation of the current state.
     * @return An random action for the current state
     */
    private Types.ACTIONS pickupRandomAction(StateObservation stateObs) {
        // 找不到了, 随便返回一个吧
        System.out.println("Pickup random action.");
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        int index = randomGenerator.nextInt(actions.size());
        return actions.get(index);
    }

    /**
     * Picks an action. This function is called every game step to request an action
     * from the player.
     * 
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        // 最大深度限制
        int limit = 5;

        // 如果已经有了可以走的路径, 获取保存的路径并返回就好
        if (stObs2Actions.containsKey(stateObs)) {
            return stObs2Actions.get(stateObs);
        }

        // 计时相关的自带代码
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;
        int remainingLimit = 5;

        while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // 递归调用, 主要逻辑
            Node node = new Node(stateObs.copy(), null, null, 0);
            Node result = recursiveDLS(node, limit);
            if (result.cutoff == 0) {
                if (stObs2Actions.containsKey(stateObs)) {
                    return stObs2Actions.get(stateObs);
                }
            } else if (result.cutoff == -1) {
                stateObservationsSet.clear();
                return pickupRandomAction(stateObs);
            } else {
                // 返回可以拿到最高分的一个
                Node current = result;
                while (!current.parent.stObs.equalPosition(stateObs)) {
                    if (current.parent != null) {
                        current = current.parent;
                    } else {
                        return pickupRandomAction(stateObs);
                    }
                }
                stateObservationsSet.clear();
                return current.action;
            }

            // 依然是自带的时间处理代码, 无视就好
            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
            // System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " +
            // acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken = acumTimeTaken / numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }

        return pickupRandomAction(stateObs);
    }
}
