package com.example.emptymdp.arena;

public class Obstacle {
    private float x, y;
    private int direction, row, col;

    public Obstacle(float x, float y, int row, int col){
        this.x = x;
        this.y = y;
        this.row = row;
        this.col = col;
        direction = PixelGridView.Direction.NORTH;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
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
}
