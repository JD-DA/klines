package com.example.air_hockey.objects;

import com.example.air_hockey.data.VertexArray;
import com.example.air_hockey.programs.ColorShaderProgram;

import java.util.List;

import util.Colors;
import util.MyColors;

public class GridNextPieces {
    //the 3x1 grid holding the pieces to be placed on the next move
    private static final int POSITION_COMPONENT_COUNT = 2;

    private final VertexArray vertexArray;
    private final List<ObjectBuilder.DrawCommand> drawList;
    private Colors color;

    public GridNextPieces() {
        ObjectBuilder.GeneratedData generatedData = ObjectBuilder.createGridNextPieces();
        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
        color = MyColors.WHITE;
    }

    public void bindData(ColorShaderProgram colorProgram) {
        vertexArray.setVertexAttribPointer(0,
                colorProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT, 0);
    }
    
    public void draw() {
        for (ObjectBuilder.DrawCommand drawCommand : drawList) { drawCommand.draw();
        }
    }

    public float getX(){
        float step = 1.0f/7;
        return -(step*1.5f);
    }
    public float getY(){
        float step = 1.0f/7;
        return 0.5f+step*1.5f;
    }


    public Colors getColor() {
        return color;
    }
    public void turnRed(){
        color = MyColors.RED;
    }
    public void turnGreen(){
        color = MyColors.GREEN;
    }
    public void turnWhite(){
        color = MyColors.WHITE;
    }

    public float getStep() {
        return 1.0f/7;
    }
}
