package utils;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;

import java.awt.*;

public class OglUtils {

	public static void printOGLparameters(GL2 gl) {
		if (gl == null)
			return;
		System.out.println("Init GL is " + gl.getClass().getName());
		System.out.println("GL_VENDOR " + gl.glGetString(GL2.GL_VENDOR));
		System.out.println("GL_RENDERER " + gl.glGetString(GL2.GL_RENDERER));
		System.out.println("GL_VERSION " + gl.glGetString(GL2.GL_VERSION));
		System.out.println("GL_EXTENSIONS " + gl.glGetString(GL2.GL_EXTENSIONS));
	}

	public static void drawStr(GLAutoDrawable glDrawable, float x, float y, float z, String s) {
		if (glDrawable == null)
			return;
		GL2 gl = glDrawable.getGL().getGL2();
		GLUT glut = new GLUT();
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glRasterPos3f(x, y, z);
		glut.glutBitmapString(GLUT.BITMAP_8_BY_13, s);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
	}
	
	public static void drawStr(GLAutoDrawable glDrawable, float x, float y, String s) {
		drawStr(glDrawable, x, y, 0, s) ;
	}
	
	public static void drawStr2D(GLAutoDrawable glDrawable, int x, int y, String s) {
		if (glDrawable == null)
			return;
		GL2 gl = glDrawable.getGL().getGL2();

		//push all parameters
		int shaderProgram = pushAll(glDrawable);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glFrontFace(GL2.GL_CCW);
		gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
		gl.glPolygonMode(GL2.GL_BACK, GL2.GL_LINE);
		
		gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
		gl.glViewport(0, 0, glDrawable.getSurfaceWidth(),
				glDrawable.getSurfaceHeight());
		TextRenderer renderer;
		renderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 12));
		renderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		
		renderer.beginRendering(glDrawable.getSurfaceWidth(),
				glDrawable.getSurfaceHeight());
		renderer.draw(s, x, y);
		renderer.endRendering();
		
		//pop all parameters
		popAll(glDrawable, shaderProgram);
		}
	 
	 private static int pushAll(GLAutoDrawable glDrawable){
		 	if (glDrawable == null)
				return 0;
			GL2 gl = glDrawable.getGL().getGL2();

			//push all parameters
			int[] shaderProgram = new int[1];
			gl.glUseProgram(0);
			gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, shaderProgram, 0);
			gl.glPushAttrib(GL2.GL_ENABLE_BIT);
			gl.glPushAttrib(GL2.GL_DEPTH_BUFFER_BIT);
			gl.glPushAttrib(GL2.GL_VIEWPORT_BIT);
			gl.glPushAttrib(GL2.GL_TEXTURE_BIT);
			gl.glPushAttrib(GL2.GL_POLYGON_BIT);
			gl.glDisable(GL2.GL_DEPTH_TEST);
			gl.glDisableVertexAttribArray(0);
			gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
			gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
			gl.glDepthMask(false);
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			
			return shaderProgram[0];
	 }
	 
	 private static void popAll(GLAutoDrawable glDrawable, int shaderProgram){
		 if (glDrawable == null)
				return;
			GL2 gl = glDrawable.getGL().getGL2();

			//pop all parameters
			gl.glPopMatrix();
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glPopAttrib();
			gl.glPopAttrib();
			gl.glPopAttrib();
			gl.glPopAttrib();
			gl.glPopAttrib();
			gl.glUseProgram(shaderProgram);
	 }
}
