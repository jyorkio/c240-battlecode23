package thebettercompbot;

import battlecode.common.*;
import thebettercompbot.Pathing;
import thebettercompbot.RobotPlayer;

public class LauncherStrategy {

    /**
     * Run a single turn for a Launcher.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static MapLocation islandLoc;
    static void runLauncher(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        int lowestHealth = 1000;
        int smallestDistance = 100;
        RobotInfo target = null;
        if (RobotPlayer.turnCount == 2) {
            thebettercompbot.Communication.updateHeadquarterInfo(rc);
        }
        //Communication.clearObsoleteEnemies(rc);
        if (enemies.length > 0) {
            for (RobotInfo enemy : enemies) {
                Communication.reportEnemy(rc, enemy.location);
                int enemyHealth = enemy.getHealth();
                int enemyDistance = enemy.getLocation().distanceSquaredTo(rc.getLocation());
                if (enemyHealth < lowestHealth) {
                    target = enemy;
                    lowestHealth = enemyHealth;
                    smallestDistance = enemyDistance;
                } else if (enemyHealth == lowestHealth) {
                    if (enemyDistance < smallestDistance) {
                        target = enemy;
                        smallestDistance = enemyDistance;
                    }
                }
            }
        }
        //Communication.tryWriteMessages(rc);
        if (target != null) {
            if (rc.canAttack(target.getLocation()))
                rc.attack(target.getLocation());
            Pathing.moveTowards(rc, target.getLocation());
        } else {
            RobotInfo[] allies = rc.senseNearbyRobots(9, rc.getTeam());
            int lowestID = rc.getID();
            MapLocation leaderPos = null;
            for (RobotInfo ally : allies) {
                if (ally.getType() != RobotType.LAUNCHER)
                    continue;
                if (ally.getID() < lowestID) {
                    lowestID = ally.getID();
                    leaderPos = ally.getLocation();
                }
            }
            if (leaderPos != null) {
                Pathing.moveTowards(rc, leaderPos);
                rc.setIndicatorString("Following " + lowestID);
            } else {
                MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
                Pathing.moveTowards(rc, center);
                rc.setIndicatorString("I'm the leader!");
            }
        }


        // Also try to move randomly.
        Direction dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }

    }

    static void scanIslands(RobotController rc) throws GameActionException {
        int[] ids = rc.senseNearbyIslands();
        rc.setIndicatorString("scanIslands");
        for (int id : ids) {
            if (rc.senseTeamOccupyingIsland(id) == Team.NEUTRAL) {
                MapLocation[] locs = rc.senseNearbyIslandLocations(id);
                if (locs.length > 0) {
                    islandLoc = locs[0];
                    break;
                }
            }
            Communication.updateIslandInfo(rc, id);
        }

    }
    private static final int HEALTH_BITS = 3;
    static final int STARTING_ISLAND_IDX = GameConstants.MAX_STARTING_HEADQUARTERS;
    static Team readTeamHoldingIsland(RobotController rc, int islandId) {
        try {
            islandId = islandId + STARTING_ISLAND_IDX;
            int islandInt = rc.readSharedArray(islandId);
            int healthMask = 0b111;
            int health = islandInt & healthMask;
            int team = (islandInt >> HEALTH_BITS) % 0b1;
            if (health > 0) {
                return Team.values()[team];
            }
        } catch (GameActionException e) {}
        return Team.NEUTRAL;
    }
}
