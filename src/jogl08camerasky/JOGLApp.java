package jogl08camerasky;


import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class JOGLApp {
	private static final int FPS = 60; // animator's target frames per second
	private static final int WIDTH =800;
	private static final int HEIGHT=600;
	/**
	 * TODO:
	 * KOLIZE
	 * Pohled nahoru
	 * textury
	 * mlha
	 * */


	public void start(){
		try {
			Frame testFrame = new Frame("Bludiste");
			testFrame.setSize(WIDTH, HEIGHT);

			// setup OpenGL Version 2
	    	GLProfile profile = GLProfile.get(GLProfile.GL2);
	    	GLCapabilities capabilities = new GLCapabilities(profile);
	    	capabilities.setRedBits(8);
			capabilities.setBlueBits(8);
			capabilities.setGreenBits(8);
			capabilities.setAlphaBits(8);
			capabilities.setDepthBits(24);

	    	// The canvas is the widget that's drawn in the JFrame
	    	GLCanvas canvas = new GLCanvas(capabilities);
	    	TestRenderer ren = new TestRenderer();
			canvas.addGLEventListener(ren);
			canvas.addMouseListener(ren);
			canvas.addMouseMotionListener(ren);
			canvas.addKeyListener(ren);
	    	canvas.setSize( WIDTH, HEIGHT );
	    	
	    	testFrame.add(canvas);
			
	        final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
	    	 
	    	testFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					new Thread() {
	                     @Override
	                     public void run() {
	                        if (animator.isStarted()) animator.stop();
	                        System.exit(0);
	                     }
	                  }.start();
				}
			});
			Toolkit t = Toolkit.getDefaultToolkit();
			Image i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			Cursor noCursor = t.createCustomCursor(i, new Point(0, 0), "none");

			testFrame.setCursor(noCursor);
			testFrame.setTitle("BLUDISTE");
	    	testFrame.pack();
	    	testFrame.setVisible(true);
            animator.start(); // start the animation loop

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new JOGLApp().start());
	}
}