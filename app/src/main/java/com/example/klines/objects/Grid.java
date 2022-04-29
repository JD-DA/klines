package com.example.klines.objects;

import com.example.klines.data.VertexArray;
import com.example.klines.programs.ColorShaderProgram;

import java.util.List;

import util.Colors;
import util.MyColors;

public class Grid {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int BYTES_PER_FLOAT = 4;

    private final VertexArray vertexArray;
    private final List<ObjectBuilder.DrawCommand> drawList;
    private int numLines;
    private Colors color;

    public Grid(int size) {
        //call the builder to create the array of vertexes
        ObjectBuilder.GeneratedData generatedData = ObjectBuilder.createGrid(size);
        vertexArray = new VertexArray(generatedData.vertexData);
        drawList = generatedData.drawList;
        numLines=size;
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

    public int getNumLines() {
        return numLines;
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
}
