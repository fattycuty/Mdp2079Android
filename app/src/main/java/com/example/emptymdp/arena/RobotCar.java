package com.example.emptymdp.arena;

import java.util.HashMap;

public class RobotCar {
    private int direction;
    private final HashMap<Integer, int[]> rowColMap;
    public interface RobotMapArea {
        int TOP_LEFT = 0;
        int TOP_MIDDLE = 1;
        int TOP_RIGHT = 2;
        int MIDDLE_LEFT = 3;
        int MIDDLE_MIDDLE = 4;
        int MIDDLE_RIGHT = 5;
        int BOTTOM_LEFT = 6;
        int BOTTOM_MIDDLE = 7;
        int BOTTOM_RIGHT = 8;
    }

    public RobotCar(int row, int col, int direction){
        this.direction = direction;
        rowColMap = new HashMap<>();
        rowColMap.put(RobotMapArea.TOP_LEFT, new int[]{row-1,col-1});
        rowColMap.put(RobotMapArea.TOP_MIDDLE, new int[]{row-1,col});
        rowColMap.put(RobotMapArea.TOP_RIGHT, new int[]{row-1,col+1});
        rowColMap.put(RobotMapArea.MIDDLE_LEFT, new int[]{row,col-1});
        rowColMap.put(RobotMapArea.MIDDLE_MIDDLE, new int[]{row,col});
        rowColMap.put(RobotMapArea.MIDDLE_RIGHT, new int[]{row,col+1});
        rowColMap.put(RobotMapArea.BOTTOM_LEFT, new int[]{row+1,col-1});
        rowColMap.put(RobotMapArea.BOTTOM_MIDDLE, new int[]{row+1,col});
        rowColMap.put(RobotMapArea.BOTTOM_RIGHT, new int[]{row+1,col+1});
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public HashMap<Integer, int[]> getRowColMap() {
        return rowColMap;
    }
}
