package elicompbot;

import battlecode.common.*;
import elicompbot.Communication;
import elicompbot.RobotPlayer;

import static battlecode.common.ResourceType.ADAMANTIUM;
import static battlecode.common.ResourceType.MANA;

public class CarrierStrategy {
    
    static MapLocation hqLoc;
    static MapLocation wellLoc;
    static MapLocation islandLoc;

    static boolean anchorMode = false;
    static int numHeadquarters = 0;

    /**
     * Run a single turn for a Carrier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runCarrier(RobotController rc) throws GameActionException {
        if (RobotPlayer.turnCount == 2) {
            Communication.updateHeadquarterInfo(rc);
        }

        if(hqLoc == null) scanHQ(rc); rc.setIndicatorString("scanning hq");
        if(wellLoc == null) scanWells(rc); rc.setIndicatorString("scanning wells");


        scanIslands(rc);

        if(wellLoc == null && RobotPlayer.turnCount < 2000) {
            RobotPlayer.moveRandom(rc);
            rc.setIndicatorString("move randomly to find wells");
        }

        //Collect from well if close and inventory not full
        if(wellLoc != null && rc.canCollectResource(wellLoc, -1) && RobotPlayer.turnCount < 300) {
            rc.collectResource(wellLoc, -1);
            rc.setIndicatorString("Collecting");
        }

            //Transfer resource to headquarters
        depositResource(rc, ADAMANTIUM);
        depositResource(rc, ResourceType.MANA);
        depositResource(rc, ResourceType.ELIXIR);


        if(rc.canTakeAnchor(hqLoc, Anchor.STANDARD)) {
            rc.takeAnchor(hqLoc, Anchor.STANDARD);
            rc.setIndicatorString("Taking Anchor");
            anchorMode = true;
        }

        // Make elixir wells if possible.
        WellInfo[] manawell = rc.senseNearbyWells(MANA);
        WellInfo w = manawell[0];
        if(wellLoc != null && rc.getResourceAmount(ADAMANTIUM) > 30 && rc.canTransferResource(w.getMapLocation(), ADAMANTIUM, 30)) {
            if(manawell.length > 0 && 300 < RobotPlayer.turnCount && RobotPlayer.turnCount < 2000) {
                rc.transferResource(w.getMapLocation(), ADAMANTIUM, 30);
                rc.setIndicatorString("Making Elixir Well!");
            }
        }

        /*if(wellLoc != null && rc.getResourceAmount(ADAMANTIUM) > 30 && rc.getRoundNum() < 300 && rc.canTransferResource(wellLoc, ADAMANTIUM, 30)) {
            //rc.transferResource(wellLoc, ADAMANTIUM, 30);
        }

        if(wellLoc != null && rc.getResourceAmount(MANA) > 30 && rc.getRoundNum() < 300 && rc.canTransferResource(wellLoc, MANA, 30)) {
            rc.transferResource(wellLoc, MANA, 30);
        }*/
        //no resources -> look for well
        if(anchorMode) {
            if(islandLoc == null) {
                for (int i = Communication.STARTING_ISLAND_IDX; i < Communication.STARTING_ISLAND_IDX + GameConstants.MAX_NUMBER_ISLANDS; i++) {
                    MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
                    if (islandNearestLoc != null) {
                        islandLoc = islandNearestLoc;
                        rc.setIndicatorString("setting island location");
                        break;
                    }
                }
            }
            else RobotPlayer.moveTowards(rc, islandLoc);
            rc.setIndicatorString("moving towards an island");

            if(rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) == Team.NEUTRAL && RobotPlayer.turnCount <600)  {
                rc.placeAnchor(); rc.setIndicatorString("placing an anchor");
                anchorMode = false;
            }
        }
        else {
            int total = getTotalResources(rc);
            if(total == 0) {
                //move towards well or search for well
                if(wellLoc == null){ RobotPlayer.moveRandom(rc); rc.setIndicatorString("moving randomly"); }
                else if(!rc.getLocation().isAdjacentTo(wellLoc)) {
                    RobotPlayer.moveTowards(rc, wellLoc);
                    rc.setIndicatorString("moving to well");
                }
            }
            if(total == GameConstants.CARRIER_CAPACITY) {
                //move towards HQ
                RobotPlayer.moveTowards(rc, hqLoc);
                rc.setIndicatorString("moving to hq");
            }
        }
        Communication.tryWriteMessages(rc);
    }

    static void scanHQ(RobotController rc) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo robot : robots) {
            if(robot.getTeam() == rc.getTeam() && robot.getType() == RobotType.HEADQUARTERS) {
                hqLoc = robot.getLocation();
                break;
            }
        }
    }

    static void scanWells(RobotController rc) throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        if(wells.length > 0) wellLoc = wells[0].getMapLocation();
    }

    static void depositResource(RobotController rc, ResourceType type) throws GameActionException {
        int amount = rc.getResourceAmount(type);
        if(amount > 0) {
            if(rc.canTransferResource(hqLoc, type, amount)) rc.transferResource(hqLoc, type, amount);
        }
    }

    static int getTotalResources(RobotController rc) {
        return rc.getResourceAmount(ADAMANTIUM)
            + rc.getResourceAmount(ResourceType.MANA) 
            + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    static void scanIslands(RobotController rc) throws GameActionException {
        int[] ids = rc.senseNearbyIslands();
        for(int id : ids) {
            if(rc.senseTeamOccupyingIsland(id) == Team.NEUTRAL) {
                MapLocation[] locs = rc.senseNearbyIslandLocations(id);
                if(locs.length > 0) {
                    islandLoc = locs[0];
                    break;
                }
            }
            Communication.updateIslandInfo(rc, id);
        }
    }
}
