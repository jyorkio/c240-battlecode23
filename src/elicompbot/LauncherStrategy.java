package elicompbot;

import battlecode.common.*;
import elicompbot.Pathing;
import elicompbot.RobotPlayer;

public class LauncherStrategy {

    private static MapLocation[] islands;

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 1000;
        int smallestDistance = 100;
        RobotInfo target = null;
        if (RobotPlayer.turnCount == 2) {
            Communication.updateHeadquarterInfo(rc);
        }
     //   Communication.clearObsoleteEnemies(rc);
        if (enemies.length > 0) {
            for (RobotInfo enemy: enemies){
         //       Communication.reportEnemy(rc, enemy.location);
                int enemyHealth = enemy.getHealth();
                int enemyDistance = enemy.getLocation().distanceSquaredTo(rc.getLocation());
                if (enemyHealth < lowestHealth){
                    target = enemy;
                    lowestHealth = enemyHealth;
                    smallestDistance = enemyDistance;
                }
                else if (enemyHealth == lowestHealth){
                    if (enemyDistance < smallestDistance){
                        target = enemy;
                        smallestDistance = enemyDistance;
                    }
                }
            }
        }
     //   Communication.tryWriteMessages(rc);
        if (target != null){
            if (rc.canAttack(target.getLocation()))
                rc.attack(target.getLocation());
            Pathing.moveTowards(rc, target.getLocation());
        }
        else {
            RobotInfo[] allies = rc.senseNearbyRobots(9, rc.getTeam());
            int lowestID = rc.getID();
            MapLocation leaderPos = null;
            for (RobotInfo ally : allies){
                if (ally.getType() != RobotType.LAUNCHER)
                    continue;
                if (ally.getID() < lowestID){
                    lowestID = ally.getID();
                    leaderPos = ally.getLocation();
                }
            }
            if (leaderPos != null){
                Pathing.moveTowards(rc, leaderPos);
                rc.setIndicatorString("Following " + lowestID);
            }
            else{
                MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
                Pathing.moveTowards(rc, center);
                rc.setIndicatorString("I'm the leader!");
            }
        }

        if (target == null){
            islands = rc.senseNearbyIslandLocations(10);
            if (islands.length > 0) {
                MapLocation islandsLoc = islands[0];
                Direction dir = rc.getLocation().directionTo(islandsLoc);
                if(rc.canMove(dir))
                    rc.move(dir);
            }
        }

        
        // Also try to move randomly.
        Direction dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }

    static void runAmplifier(RobotController rc) throws GameActionException{
        MapLocation center = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
        Pathing.moveTowards(rc, center);
    }
}
