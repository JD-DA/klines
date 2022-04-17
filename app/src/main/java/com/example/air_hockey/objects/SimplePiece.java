package com.example.air_hockey.objects;



import android.util.Log;

import com.example.air_hockey.data.VertexArray;
import com.example.air_hockey.programs.ColorShaderProgram;

import java.util.List;

import util.Colors;
import util.Geometry;
import util.MyColors;

public class SimplePiece {
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final String TAG = "SimplePiece";

    public final float radius, height;

    private final VertexArray vertexArray;
    private final List<ObjectBuilder.DrawCommand> drawList;

    private int x;
    private int y;
    private int numPoints;
    private Geometry.Point point;
    private Colors color;


    public SimplePiece(float radius, float height, int numPointsAroundPuck, Geometry.Point point) {
        this.point = point;
        ObjectBuilder.GeneratedData generatedData = ObjectBuilder.createPiece(new Geometry.Cylinder(
            new Geometry.Point(0f,0f,0f), radius, height), numPointsAroundPuck);
        this.radius = radius;
        this.height = height;
        this.numPoints = numPointsAroundPuck;
        color = MyColors.colorRange[(int)(Math.random()*7)];
        //Log.d(TAG, "SimplePiece: "+r+" "+g+" "+b);



        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
    }

    public void bindData(ColorShaderProgram colorProgram) {
        vertexArray.setVertexAttribPointer(0,
            colorProgram.getPositionAttributeLocation(),
            POSITION_COMPONENT_COUNT, 0);
    }
    public void draw() {
        for (ObjectBuilder.DrawCommand drawCommand : drawList) {
            drawCommand.draw();
        }
    }

    public Geometry.Point getPoint() {
        return point;
    }

    public int getNumPoints() {
        return numPoints;
    }

    public float getR() {
        return color.getR();
    }

    public float getG() {
        return color.getG();
    }

    public float getB() {
        return color.getB();
    }

    public void changeColor(){
        color = MyColors.colorRange[(int)(Math.random()*7)];
    }

    public void setPoint(Geometry.Point point) {
        this.point = point;
    }

    public Colors getColor() {
        return color;
    }
}