package controllers.depthfirst;

import java.awt.Graphics2D;
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

/**
 * Created with IntelliJ IDEA. User: ssamot Date: 14/11/13 Time: 21:45 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {

    /**
     * Random generator for the agent.
     */
    protected Random randomGenerator;

    /**
     * Observation grid.
     */
    protected ArrayList<Observation> grid[][];

    /**
     * block size
     */
    protected int block_size;

    // 用于判断是否形成回路
    private Set<StateObservation> stateObservationsSet = new HashSet<>();

    // 用于保存搜索到的解答, 根据局面获取步骤
    private Map<StateObservation, Types.ACTIONS> stObs2Actions = new HashMap<>();

    // 一个类似 Stack 的 List, 通过 head 判断时间不足时走哪一步, 尾部用于存放当前搜索的局面
    private int head = 0;
    private List<Node> fringe = new ArrayList<>();
    // 当前节点的父节点, 用于判断是否所有子节点都失败了
    private int currentParentIndex = -1;

    /**
     * Public constructor with state observation and time due.
     * 
     * @param so           state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        randomGenerator = new Random();
        grid = so.getObservationGrid();
        block_size = so.getBlockSize();
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

        // 如果已经有了可以走的路径, 获取保存的路径并返回就好
        if (stObs2Actions.containsKey(stateObs)) {
            return stObs2Actions.get(stateObs);
        }

        // 初始化, 第一次就加入这个局面, 否则继续进行上一次的搜索
        if (fringe.size() - head == 0) {
            System.out.println("Initiate stObsStackList.");
            // 清空并初始化
            fringe.clear();
            head = 0;
            currentParentIndex = -1;
            // -1 代表是首节点, 没有上一步行动与父节点
            fringe.add(new Node(stateObs, null, -1));
        } else {
            System.out.println("Continue last search.");
        }

        // 计时相关的自带代码
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        int remainingLimit = 5;
        while (remaining > 2 * avgTimeTaken && remaining > remainingLimit) {
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            // 失败了, 找不到可行方案, 随便返回一个吧
            if (fringe.size() - head == 0) {
                System.out.println("Fail to search the path.");
                ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
                int index = randomGenerator.nextInt(actions.size());
                return actions.get(index);
            }

            int index = fringe.size() - 1;
            Node node = fringe.get(index);

            // 胜利的相关的处理
            if (node.stObs.isGameOver() && node.stObs.getGameWinner() != Types.WINNER.NO_WINNER) {
                Node current = node;
                while (current.parent != -1) {
                    // 循环加入可行的行动
                    stObs2Actions.put(fringe.get(current.parent).stObs, current.action);
                    current = fringe.get(current.parent);
                }
                // 返回当前的步骤
                return stObs2Actions.get(stateObs);
            }

            // 如果当前父节点索引与当前索引相同, 说明这个节点的子节点都失败了, 即这个节点也失败了
            if (currentParentIndex == index) {
                currentParentIndex = node.parent;
                fringe.remove(index);
                continue;
            }
            currentParentIndex = node.parent;

            // 获取当前可行的行动
            ArrayList<Types.ACTIONS> actions = node.stObs.getAvailableActions();

            // 游戏结束或形成回路, 说明失败了, 移除栈尾并继续执行下一个判断
            if (node.stObs.isGameOver() || stateObservationsSet.contains(node.stObs) || actions.size() == 0) {
                fringe.remove(index);
                continue;
            }

            // 加入已扫描局面的集合, 用于下一次判断是否形成回路
            stateObservationsSet.add(node.stObs);

            // 可以继续扩展, 那就扩展
            actions.forEach((action) -> {
                StateObservation stSaved = node.stObs.copy();
                stSaved.advance(action);
                fringe.add(new Node(stSaved, action, index));
            });

            // 依然是自带的时间处理代码, 无视就好
            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
            // System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " +
            // acumTimeTaken + " (" + remaining + ")");
            avgTimeTaken = acumTimeTaken / numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }

        // 时间不够, 没能够搜索完, 那就直接返回头部吧
        System.out.println("Time running out.");
        head++;
        return fringe.get(head).action;
    }

    /**
     * Prints the number of different types of sprites available in the "positions"
     * array. Between brackets, the number of observations of each type.
     * 
     * @param positions array with observations.
     * @param str       identifier to print
     */
    public void printDebug(ArrayList<Observation>[] positions, String str) {
        if (positions != null) {
            System.out.print(str + ":" + positions.length + "(");
            for (int i = 0; i < positions.length; i++) {
                System.out.print(positions[i].size() + ",");
            }
            System.out.print("); ");
        } else
            System.out.print(str + ": 0; ");
    }

    /**
     * Gets the player the control to draw something on the screen. It can be used
     * for debug purposes.
     * 
     * @param g Graphics device to draw to.
     */
    public void draw(Graphics2D g) {
        int half_block = (int) (block_size * 0.5);
        for (int j = 0; j < grid[0].length; ++j) {
            for (int i = 0; i < grid.length; ++i) {
                if (grid[i][j].size() > 0) {
                    Observation firstObs = grid[i][j].get(0); // grid[i][j].size()-1
                    // Three interesting options:
                    int print = firstObs.category; // firstObs.itype; //firstObs.obsID;
                    g.drawString(print + "", i * block_size + half_block, j * block_size + half_block);
                }
            }
        }
    }
}
