package controllers.depthfirst;

import core.game.StateObservation;
import ontology.Types;

public class Node {
    public StateObservation stObs;

    // 生成这个局面的那一步行动
    public Types.ACTIONS action = null;

    // 在 fringe 中的索引
    public int parent = -1;
    public int child = -1;

    Node(StateObservation stObs, Types.ACTIONS action, int parent) {
        this.stObs = stObs;
        this.action = action;
        this.parent = parent;
    }
}
