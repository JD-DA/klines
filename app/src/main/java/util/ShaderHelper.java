package util;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;

import android.util.Log;
/*
Class inspired from the book "OpenGL ES 2 for android" by Kevin Brothaler
 */
public class ShaderHelper {
    private static final String TAG = "ShaderHelper";
    public static int compileVertexShader(String shaderCode){
        return compileShader(GL_VERTEX_SHADER,shaderCode);
    }
    public static int compileFragmentShader(String shaderCode){
        return compileShader(GL_FRAGMENT_SHADER,shaderCode);
    }

    private static int compileShader(int type, String shaderCode) {
        final int shaderObjectID = glCreateShader(type);
        if (shaderObjectID == 0){

                Log.w(TAG,"Could not create new Shader");

            return 0;
        }

        glShaderSource(shaderObjectID,shaderCode);
        glCompileShader(shaderObjectID);
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectID,GL_COMPILE_STATUS,compileStatus,0);
            Log.v(TAG,"Results of compiling source :\n"+shaderCode+"\n:"+glGetShaderInfoLog(shaderObjectID));
        if(compileStatus[0]==0){
            glDeleteShader(shaderObjectID);
                Log.w(TAG,"Compilation of shader failed");
            return 0;
        }
        return shaderObjectID;
    }

    public static int linkProgram(int vertexShaderId, int fragmentShaderId){
        final int programObjectID = glCreateProgram();
        if(programObjectID==0){
                Log.w(TAG,"Could not create new program");

            return 0;
        }
        glAttachShader(programObjectID,vertexShaderId);
        glAttachShader(programObjectID,fragmentShaderId);
        glLinkProgram(programObjectID);
        final int[] linkstatus = new int[1];
        glGetProgramiv(programObjectID,GL_LINK_STATUS,linkstatus,0);
            Log.v(TAG,"Result of linking program:\n"+glGetProgramInfoLog(programObjectID));
        if(linkstatus[0]==0){
            glDeleteProgram(programObjectID);
                Log.w(TAG,"Linking of program failed.");
            return 0;
        }
        return programObjectID;
    }

    public static boolean validateProgram(int programObjectId){
        glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId,GL_VALIDATE_STATUS,validateStatus,0);

            Log.v(TAG,"Result of validating program:\n"+validateStatus[0]+"\nLog: "+glGetProgramInfoLog(programObjectId));

        return validateStatus[0]!=0;
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int program;
        // Compile the shaders.
        int vertexShader = compileVertexShader(vertexShaderSource);
        int fragmentShader = compileFragmentShader(fragmentShaderSource);
        // Link them into a shader program.
        program = linkProgram(vertexShader, fragmentShader);
            validateProgram(program);
        return program; }

}
