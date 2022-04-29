package com.example.klines.objects;


import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import util.Colors;
import util.Geometry;

public class GameBoard {
    //keep in memory the state of our board, i.e. the filled position and provide methods to
    // convert from index view to the array view

    private static final String TAG = "GameBoard";
    private final int numLines;
    private final float step;

    private final boolean[] filledPosition;
    private final int align;

    public GameBoard(int size) {
        filledPosition = new boolean[size*size];
        for (int i = 0; i < size * size; i++) {
            filledPosition[i]=false;
        }
        numLines=size;
        step=1f/size;
        if (size<8)
            align=4;
        else
            align=5;

    }

    public Integer getRandomPosition() {
        int index = (int)(Math.random()*(numLines*numLines));
        while(filledPosition[index]){
            index = (int)(Math.random()*(numLines*numLines));
        }
        filledPosition[index] = true;
        return index;
    }

    /*
    return the point from the provided index according to the dimension of the grid
     */
    public Geometry.Point pointFromIndex(int index) {

        return new Geometry.Point(index%numLines*step+step/2.0f-0.5f,0f, (index/numLines) *step+step/2.0f-0.5f);
    }


    // if position filled return true
    public boolean filled(int i) {
        return filledPosition[i];
    }


    public void moveFromTo(int indexToMove, int i) {
        filledPosition[indexToMove]=false;
        filledPosition[i]=true;
    }

    //check the board and return the position of the pieces that must be deleted because they are aligned
    public List<Integer> checkBoard(SimplePiece[] board){
        //check lines
        Log.d(TAG, "checkBoard: checkLine");
        Colors previousColor = null;
        int index;
        List<Integer> indexToDestroy = new ArrayList<>();
        List<Integer> indexWeMightDestroy = new ArrayList<>();
        for (int i = 0; i < numLines; i++) {
            for (int j = 0; j < numLines; j++) {
                index = i*numLines+j;
                if(filledPosition[index] && board[index].getColor()==previousColor){
                    indexWeMightDestroy.add(index);
                }else {
                    if(indexWeMightDestroy.size()>=align){
                        indexToDestroy.addAll(indexWeMightDestroy);
                    }
                    indexWeMightDestroy.clear();
                    if(filledPosition[index]) {
                        previousColor = board[index].getColor();
                        indexWeMightDestroy.add(index);
                    }
                    else
                        previousColor=null;
                }
            }
            if(indexWeMightDestroy.size()>=align){
                indexToDestroy.addAll(indexWeMightDestroy);
            }
            indexWeMightDestroy.clear();
            previousColor=null;

        }
        Log.d(TAG, "checkBoard: check columns");
        //check columns
        for (int i = 0; i < numLines; i++) {
            //StringBuilder line = new StringBuilder();
            //StringBuilder lineColor = new StringBuilder();
            for (int j = 0; j < numLines; j++) {

                index = j*numLines+i;
                /*line.append(j * numLines + i);
                line.append("\t");
                if(board[index]!=null)
                lineColor.append(board[index].getColor()).append("\t");
                else
                    lineColor.append("nul").append("\t");

                 */
                if(filledPosition[index] && board[index].getColor()==previousColor){
                    indexWeMightDestroy.add(index);
                }else {
                    if(indexWeMightDestroy.size()>=align){
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
            if(indexWeMightDestroy.size()>=align){
                indexToDestroy.addAll(indexWeMightDestroy);
            }
            indexWeMightDestroy.clear();
            previousColor=null;
            /*Log.d(TAG, "checkBoard: "+line);
            Log.d(TAG, "checkBoard: "+lineColor);*/

        }
        for (Integer i:
             indexToDestroy) {
            filledPosition[i]=false;
        }
        return indexToDestroy;
    }

    /*
    create the graph based on the grid and apply a BFS on it, then we try to find a path between our two points
     */
    public List<Integer> movementAllowed(int fromIndex, int toIndex){
        LinkedList<Integer>[] adj = new LinkedList[numLines*numLines];
        Queue<Integer> queue = new LinkedList<>();
        Integer[] parent= new Integer[numLines*numLines];
        parent[toIndex] = -1;
        queue.add(toIndex);
        Integer[] indexes={-numLines,1,numLines,-1};
        boolean[] explored = new boolean[numLines*numLines];
        for (int i = 0; i < numLines * numLines; i++) {
            adj[i]=new LinkedList<>();
            parent[i]=-1;
            explored[i]=false;
            for (Integer tab :
                    indexes) {
                if(i+tab>=0 && i+tab < numLines*numLines &&     //not out of the bonds of the array
                        (!filled(i+tab) || i+tab==fromIndex) && //not a filled case or the case where we will move from
                        ((tab!=1 || (i+tab)%numLines!=0) &&           //if on edge we mustn't add the over edge as neigbhour
                        (tab!=-1 || (i+tab)%numLines!=numLines-1))    //idem
                ){
                    adj[i].add(i+tab);
                }
            }

        }
        explored[toIndex]=true;
        int node;
        int neighbour;
        while(!queue.isEmpty()){
            node = queue.poll();
            for (int i = 0; i <adj[node].size(); i++) {
                neighbour = adj[node].get(i);
                if(!explored[neighbour]){
                    queue.add(neighbour);
                    explored[neighbour]=true;
                    parent[neighbour]=node;
                }
            }
        }
        //get back path
        List<Integer> path = new ArrayList<>();
        path.add(fromIndex);
        int rollback = parent[fromIndex];
        while(rollback!=-1){
            path.add(rollback);
            rollback = parent[rollback];
        }
        printBoard(parent);
        return path;

    }
    public void printBoard(Integer[] path) {
        for (int i = 0; i < numLines; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < numLines; j++) {
                line.append(path[i * numLines + j]).append("\t");
            }
            Log.d(TAG, "printBoard: "+line);
        }
    }

    public boolean[] getFilledPosition() {
        return filledPosition;
    }

    //check if the board is filled
    public boolean gameOver(){
        for (Boolean test :
                filledPosition) {
            if (!test)
                return false;
        }
        return true;
    }
}


