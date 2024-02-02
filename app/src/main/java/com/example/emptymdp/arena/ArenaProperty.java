package com.example.emptymdp.arena;

import java.util.ArrayList;

public class ArenaProperty {
    private int numRows, numColumns;
    private ArrayList<Obstacle> obstacleArrayList;
    private RoboCar roboCar;
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

    public RoboCar getRoboCar() {
        return roboCar;
    }

    public void setRoboCar(RoboCar roboCar) {
        this.roboCar = roboCar;
    }

    public void addRoboCar(RoboCar roboCar){
        this.roboCar = roboCar;
        matrixBoard[roboCar.getRow()][roboCar.getCol()] = PixelGridView.CellValue.CAR;
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
