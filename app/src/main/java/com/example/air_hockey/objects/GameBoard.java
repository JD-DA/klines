package com.example.air_hockey.objects;


import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import util.Colors;
import util.Geometry;

public class GameBoard {

    private static final String TAG = "GameBoard";
    private int numLines;
    private float step;

    private boolean[] filledPosition;

    public GameBoard(int size) {
        filledPosition = new boolean[size*size];
        numLines=size;
        step=1f/size;

    }

    public Integer getRandomPosition() {
        int index = (int)(Math.random()*(numLines*numLines));
        while(filledPosition[index]){
            index = (int)(Math.random()*(numLines*numLines));
        }
        filledPosition[index] = true;
        return index;
    }

    // sur le plan x/z x=0 milieu de la table
    public Geometry.Point pointFromIndex(int index) {
        Log.d(TAG, "pointFromIndex: "+index);
        Log.d(TAG, "pointFromIndex: "+numLines);
        Log.d(TAG, "pointFromIndex: "+step);
        return new Geometry.Point(index%numLines*step+step/2.0f-0.5f,0f,((int)(index/numLines))*step+step/2.0f-0.5f);
    }


    public boolean filled(int i) {
        return filledPosition[i];
    }

    public void moveFromTo(int indexToMove, int i) {
        filledPosition[indexToMove]=false;
        filledPosition[i]=true;
    }

    public List<Integer> checkBoard(SimplePiece[] board){
        //check lines
        Colors previousColor = null;
        int score=0;
        int index;
        List<Integer> indexToDestroy = new ArrayList<>();
        List<Integer> indexWeMightDestroy = new ArrayList<>();
        for (int i = 0; i < numLines; i++) {
            for (int j = 0; j < numLines; j++) {
                index = i*numLines+j;
                if(filledPosition[index] && board[index].getColor()==previousColor){
                    indexWeMightDestroy.add(index);
                }else {
                    if(indexWeMightDestroy.size()>=numLines-2){
                        indexToDestroy.addAll(indexWeMightDestroy);
                    }
                    indexWeMightDestroy.clear();
                    if(filledPosition[index])
                        previousColor = board[index].getColor();
                    else
                        previousColor=null;
                }
            }

        }
        //check columns
        for (int i = 0; i < numLines; i++) {
            for (int j = 0; j < numLines; j++) {
                index = j*numLines+i;
                if(filledPosition[index] && board[index].getColor()==previousColor){
                    indexWeMightDestroy.add(index);
                }else {
                    if(indexWeMightDestroy.size()>=numLines-2){
                        indexToDestroy.addAll(indexWeMightDestroy);
                    }
                    indexWeMightDestroy.clear();
                    if(filledPosition[index]) {
                        previousColor = board[index].getColor();
                        indexWeMightDestroy.add(index);
                    }else
                        previousColor=null;
                }
            }

        }
        for (Integer i:
             indexToDestroy) {
            filledPosition[i]=false;
        }
        return indexToDestroy;
    }
}
