package com.example.emptymdp.arena;

import java.util.ArrayList;
import java.util.HashMap;

public class ArenaProperty {
    private int numRows, numColumns;
    private ArrayList<Obstacle> obstacleArrayList;
    private RobotCar robotCar;
    private int[][] matrixBoard;

    public ArenaProperty() {
        obstacleArrayList = new ArrayList<>();
    }

    public void setArena(int numRows, int numColumns){
        this.numRows = numRows;
        this.numColumns = numColumns;
        matrixBoard = new int[numRows][numColumns];
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
    }

    public int getNumRows() {
        return numRows;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    public ArrayList<Obstacle> getObstacleArrayList() {
        return obstacleArrayList;
    }

    public void setObstacleArrayList(ArrayList<Obstacle> obstacleArrayList) {
        this.obstacleArrayList = obstacleArrayList;
    }

    public Obstacle getObstacle(int row, int col, int obstacleId){
        // obstacleId == -1 -> find obstacle with rowcol
        // obstacleId != -1 -> find obstacle with id
        for (Obstacle obstacle:getObstacleArrayList()){
            if (obstacleId==-1 && obstacle.getRow()==row && obstacle.getCol()==col)
                return obstacle;
            else if (obstacleId!=-1 && obstacle.getObstacleId()==obstacleId)
                return obstacle;
        }
        return null;
    }

    public RobotCar getRobotCar() {
        return robotCar;
    }

    public void setRobotCar(RobotCar robotCar) {
        this.robotCar = robotCar;
    }

    public void addRobotCar(RobotCar robotCar){
        this.robotCar = robotCar;

        int[] rowCol;
        HashMap<Integer, int[]> rowColMap = robotCar.getRowColMap();
        for (int i=0;i<rowColMap.size();i++){
            rowCol = rowColMap.get(i);
            matrixBoard[rowCol[0]][rowCol[1]] = PixelGridView.CellValue.CAR;
        }
    }

    public void addObstacle(Obstacle obstacle){
        obstacleArrayList.add(obstacle);
        matrixBoard[obstacle.getRow()][obstacle.getCol()] = PixelGridView.CellValue.OBSTACLE;
    }

    public int[][] getMatrixBoard() {
        return matrixBoard;
    }

    public void setMatrixBoard(int[][] matrixBoard) {
        this.matrixBoard = matrixBoard;
    }

}
