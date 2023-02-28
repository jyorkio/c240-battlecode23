package thebettercompbot;

import battlecode.common.*;

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
            rc.setIndicatorString("alive");
        }
        //rc.readSharedArray()

        if(hqLoc == null) {
            scanHQ(rc);
            rc.setIndicatorString("scanHQ");
        }
        if(wellLoc == null) {
            scanWells(rc);
            rc.setIndicatorString("scanWells");
        }
        scanIslands(rc);


        //Collect from well if close and inventory not full
        if(wellLoc != null && rc.canCollectResource(wellLoc, -1)) {
            rc.collectResource(wellLoc, -1);
            rc.setIndicatorString("collectingResources");
        }

        //Transfer resource to headquarters
        depositResource(rc, ResourceType.ADAMANTIUM);
        depositResource(rc, ResourceType.MANA);


        if(rc.canTakeAnchor(hqLoc, Anchor.ACCELERATING)){
            rc.takeAnchor(hqLoc, Anchor.ACCELERATING);
            rc.setIndicatorString("acceleratingAnchorMode");
            anchorMode = true;
        }

        if(rc.canTakeAnchor(hqLoc, Anchor.STANDARD)) {
            rc.takeAnchor(hqLoc, Anchor.STANDARD);
            rc.setIndicatorString("AnchorMode");
            anchorMode = true;
        }

        //upgrade Adamantium well if available resources
        int totalAdam = rc.getResourceAmount(ResourceType.ADAMANTIUM);
        if (totalAdam > 1400 & rc.canTransferResource(wellLoc, ResourceType.ADAMANTIUM, 1400)) {
            rc.transferResource(wellLoc, ResourceType.ADAMANTIUM, 1400);
            rc.setIndicatorString("upgradeAdam");
        }

        // upgrade mana well is possible
        int totalMana = rc.getResourceAmount(ResourceType.MANA);
        if (totalMana > 1400 & rc.canTransferResource(wellLoc, ResourceType.MANA, 1400)) {
            rc.transferResource(wellLoc, ResourceType.MANA, 1400);
            rc.setIndicatorString("upgradeMana");
        }
        //no resources -> look for well
        if(anchorMode) {
            if(islandLoc == null) {
                for (int i = thebettercompbot.Communication.STARTING_ISLAND_IDX; i < thebettercompbot.Communication.STARTING_ISLAND_IDX + GameConstants.MIN_NUMBER_ISLANDS; i++) {
                    MapLocation islandNearestLoc = thebettercompbot.Communication.readIslandLocation(rc, i);
                    if (islandNearestLoc != null) {
                        islandLoc = islandNearestLoc;
                        break;
                    }
                }
            }
            else thebettercompbot.RobotPlayer.moveTowards(rc, islandLoc);

            if(rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) == Team.NEUTRAL) {
                rc.placeAnchor();
                anchorMode = false;
            }
        }
        else {
            int total = getTotalResources(rc);
            if(total == 0) {
                //move towards well or search for well
                if(wellLoc == null) thebettercompbot.RobotPlayer.moveRandom(rc);
                else if(!rc.getLocation().isAdjacentTo(wellLoc)) thebettercompbot.RobotPlayer.moveTowards(rc, wellLoc);
            }
            if(total == GameConstants.CARRIER_CAPACITY) {
                //move towards HQ
                thebettercompbot.RobotPlayer.moveTowards(rc, hqLoc);
            }
        }
        thebettercompbot.Communication.tryWriteMessages(rc);

    }
    //    Communication.tryWriteMessages(rc);
    // carrier focused on gathering resources
    static void getResources(RobotController rc) throws GameActionException {
        System.out.println("Resource bot is running");
        if(hqLoc == null) scanHQ(rc);
        if(wellLoc == null) scanWells(rc);
        scanIslands(rc);
        //Collect from well if close and inventory not full
        if(wellLoc != null && rc.canCollectResource(wellLoc, -1)) rc.collectResource(wellLoc, -1);
        //Transfer resource to headquarters
        depositResource(rc, ResourceType.ADAMANTIUM);
        depositResource(rc, ResourceType.MANA);
        int total = getTotalResources(rc);
        if(total == 0) {
            //move towards well or search for well
            if(wellLoc == null) RobotPlayer.moveRandom(rc);
            else if(!rc.getLocation().isAdjacentTo(wellLoc)) RobotPlayer.moveTowards(rc, wellLoc);
        }
        if(total == GameConstants.CARRIER_CAPACITY) {
            //move towards HQ
            RobotPlayer.moveTowards(rc, hqLoc);
        }
    }

    // carrier focused on placing anchors
    static void anchorBot(RobotController rc) throws GameActionException {
        rc.takeAnchor(hqLoc, Anchor.STANDARD);
        anchorMode = true;
        System.out.println("Anchor bot is running");
        if(islandLoc == null) {
            for (int i = Communication.STARTING_ISLAND_IDX; i < Communication.STARTING_ISLAND_IDX + GameConstants.MIN_NUMBER_ISLANDS; i++) {
                MapLocation islandNearestLoc = Communication.readIslandLocation(rc, i);
                if (islandNearestLoc != null) {
                    islandLoc = islandNearestLoc;
                    break;
                }
            }
        }
        else RobotPlayer.moveTowards(rc, islandLoc);
        if(rc.canPlaceAnchor() && rc.senseTeamOccupyingIsland(rc.senseIsland(rc.getLocation())) == Team.NEUTRAL) {
            rc.placeAnchor();
            anchorMode = false;
        }
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
        if(wellLoc == null) {
            if ((RobotPlayer.turnCount % 5) == 0) {
                thebettercompbot.RobotPlayer.moveRandom(rc);
            }
        }
    }

    static void depositResource(RobotController rc, ResourceType type) throws GameActionException {
        int amount = rc.getResourceAmount(type);
        if(amount > 0) {
            if(rc.canTransferResource(hqLoc, type, amount)) rc.transferResource(hqLoc, type, amount);
        }
    }

    static int getTotalResources(RobotController rc) {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) 
            + rc.getResourceAmount(ResourceType.MANA) 
            + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    static void scanIslands(RobotController rc) throws GameActionException {
        int[] ids = rc.senseNearbyIslands();
        rc.setIndicatorString("scanIslands");
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
