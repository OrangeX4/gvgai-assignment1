
import core.ArcadeMachine;
import java.util.Random;
import core.competition.CompetitionParameters;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yuy
 */
public class Assignment1 {
 
    public static void main(String[] args)
    {
        //Available controllers:
    	String depthfirstController = "controllers.depthfirst.Agent";
    	// String limitdepthfirstController = "controllers.limitdepthfirst.Agent";
        // String AstarController = "controllers.Astar.Agent";
        // String sampleMCTSController = "controllers.sampleMCTS.Agent";

        boolean visuals = true; // set to false if you don't want to see the game
        int seed = new Random().nextInt(); // seed for random
        
        /****** Task 1 ******/
        CompetitionParameters.ACTION_TIME = 50; // set to the time that allow you to do the depth first search
        ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl0.txt", visuals, depthfirstController, null, seed, false);
        ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl1.txt", visuals, depthfirstController, null, seed, false);
        
        
        // /****** Task 2 ******/
        // CompetitionParameters.ACTION_TIME = 100; // no time for finding the whole path
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl0.txt", visuals, limitdepthfirstController, null, seed, false);
        
        
        // /****** Task 3 ******/
        // CompetitionParameters.ACTION_TIME = 100; // no time for finding the whole path
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl0.txt", visuals, AstarController, null, seed, false);
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl1.txt", visuals, AstarController, null, seed, false);
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl2.txt", visuals, AstarController, null, seed, false);
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl3.txt", visuals, AstarController, null, seed, false);
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl4.txt", visuals, AstarController, null, seed, false);
        
        
        // /****** Task 4 ******/
        // CompetitionParameters.ACTION_TIME = 100; // no time for finding the whole path
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl0.txt", visuals, sampleMCTSController, null, seed, false);
        // ArcadeMachine.runOneGame("examples/gridphysics/bait.txt", "examples/gridphysics/bait_lvl1.txt", visuals, sampleMCTSController, null, seed, false);
        
    }   
}
