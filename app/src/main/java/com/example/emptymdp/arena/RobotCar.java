package com.example.emptymdp.arena;

import java.util.HashMap;

public class RobotCar {
    private float x, y;
    private int direction, row, col; // row col of top left 2x2 square
    private final HashMap<Integer, int[]> rowColMap;

    public interface RoboMapArea {
        int TOP_LEFT = 0;
        int TOP_RIGHT = 1;
        int BOTTOM_LEFT = 2;
        int BOTTOM_RIGHT = 3;
    }

    public RobotCar(float x, float y, int row, int col, int direction){
        this.x = x;
        this.y = y;
        this.row = row;
        this.col = col;
        this.direction = direction;
        rowColMap = new HashMap<>();
        rowColMap.put(RoboMapArea.TOP_LEFT, new int[]{row,col});
        rowColMap.put(RoboMapArea.TOP_RIGHT, new int[]{row,col+1});
        rowColMap.put(RoboMapArea.BOTTOM_LEFT, new int[]{row+1,col});
        rowColMap.put(RoboMapArea.BOTTOM_RIGHT, new int[]{row+1,col+1});

    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public HashMap<Integer, int[]> getRowColMap() {
        return rowColMap;
    }
}
