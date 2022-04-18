package com.example.air_hockey.objects;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glDrawArrays;

import com.example.air_hockey.data.VertexArray;
import com.example.air_hockey.programs.ColorShaderProgram;
import com.example.air_hockey.programs.TextureShaderProgram;

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
