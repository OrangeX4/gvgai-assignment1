package controllers.Astar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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

        public int cost = 0;

        // Cutoff, 其实是 h() heuristic function 的结果
        // -1 是初始值, 0 是成功找到路径, >= 1 是启发式函数的预测
        public int heuristics = -1;

        Node(StateObservation stObs, Types.ACTIONS action, Node parent, int cost) {
            this.stObs = stObs;
            this.action = action;
            this.parent = parent;
            this.cost = cost;
            this.heuristics = Agent.heuristic(stObs);
        }
    }

    // 比较器, 用于选出开销最小的节点
    Comparator<Node> comparator = new Comparator<Node>() {
        @Override
        public int compare(Node a, Node b) {
            return (a.cost + a.heuristics) - (b.cost + b.heuristics);
        }
    };

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

    // 优先级队列, 用于获取下一次搜索的元素
    private PriorityQueue<Node> fringe = new PriorityQueue<>(comparator);

    // 用于保存搜索到的解答, 根据局面获取步骤
    private Map<StateObservation, Types.ACTIONS> stObs2Actions = new HashMap<>();

    public static int getDist(Vector2d a, Vector2d b) {
        return (int) Math.abs(a.x / 50 - b.x / 50) + (int) Math.abs(a.y / 50 - b.y / 50);
    }

    /**
     * heuristic function
     * 
     * @param node The node.
     * @return The heuristic function result.
     */
    public static int heuristic(StateObservation stObs) {
        ArrayList<Observation>[] fixedPositions = stObs.getImmovablePositions();
        ArrayList<Observation>[] movingPositions = stObs.getMovablePositions();
        Vector2d avatarPos = stObs.getAvatarPosition();
        if (fixedPositions != null && fixedPositions.length > 1 && fixedPositions[1].size() > 0) {
            Vector2d goalPos = fixedPositions[1].get(0).position;
            if (movingPositions != null && movingPositions.length > 0 && movingPositions[0].size() > 0) {
                // 没吃到钥匙, 就返回从当前位置到钥匙位置再到目标位置的距离
                Vector2d keyPos = movingPositions[0].get(0).position;
                return getDist(avatarPos, keyPos) + getDist(keyPos, goalPos);
            } else {
                // 吃到钥匙, 就返回从当前位置到目标位置的距离
                return getDist(avatarPos, goalPos);
            }
        } else {
            return 0;
        }
    }

    /**
     * Picks a action greedily.
     * 
     * @param stateObs Observation of the current state.
     * @return An random action for the current state
     */
    private Types.ACTIONS pickupGreedyAction(StateObservation stateObs) {
        System.out.println("Pickup greedy action.");
        int currentHeuristics = -1;
        Types.ACTIONS currentAction = null; 
        for (Types.ACTIONS action : stateObs.getAvailableActions()) {
            StateObservation stateCpy = stateObs.copy();
            stateCpy.advance(action);
            if (stateObs.equalPosition(stateCpy)) {
                continue;
            }
            int result = heuristic(stateCpy);
            if (currentHeuristics == -1 || result < currentHeuristics) {
                currentHeuristics = result;
                currentAction = action;
            } 
        }
        return currentAction;
    }

    private int count = 0;

    /**
     * Picks an action. This function is called every game step to request an action
     * from the player.
     * 
     * @param stateObs     Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        // 如果已经有了可以走的路径, 获取保存的路径并返回就好
        if (stObs2Actions.containsKey(stateObs)) {
            return stObs2Actions.get(stateObs);
        }

        fringe.add(new Node(stateObs.copy(), null, null, 0));

        // 计时相关的自带代码
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;
        int remainingLimit = 5;

        while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // 输出次数, 用于统计
            count++;
            if (count % 100 == 0) {
                System.out.println("Count: " + count);
            }

            if (fringe.size() == 0) {
                System.out.println("Failed to find the path.");
                return pickupGreedyAction(stateObs);
            }

            Node node = fringe.remove();

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
                node.heuristics = 0;
                if (stObs2Actions.containsKey(stateObs)) {
                    return stObs2Actions.get(stateObs);
                } else {
                    return pickupGreedyAction(stateObs);
                }
            } else if (!(node.stObs.isGameOver() || stateObservationsSet.contains(node.stObs)
                    || node.stObs.getAvailableActions().size() == 0)) {
                // Add to closed
                stateObservationsSet.add(node.stObs);

                for (Types.ACTIONS action : node.stObs.getAvailableActions()) {
                    StateObservation stateCpy = node.stObs.copy();
                    stateCpy.advance(action);
                    fringe.add(new Node(stateCpy, action, node, node.cost + 1));
                }
            }

            // 依然是自带的时间处理代码, 无视就好
            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
            // System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " +
            // acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken = acumTimeTaken / numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }

        if (fringe.size() != 0) {
            System.out.println("No time to find the path.");
            return pickupGreedyAction(stateObs);
        } else {
            System.out.println("Failed to find the path.");
            return pickupGreedyAction(stateObs);
        }
    }
}
