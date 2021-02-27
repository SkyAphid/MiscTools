package nokori.tools;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class AutoFF14HouseBuyer implements NativeKeyListener {
	
	private boolean running = true;
	private boolean inputStart = false;
	
	private Robot robot;
	
	public static void main(String[] args) {
		try {
			GlobalScreen.registerNativeHook();
		}
		catch (NativeHookException ex) {
			System.err.println("There was a problem registering the native hook.");
			System.err.println(ex.getMessage());

			System.exit(1);
		}
		
		// Clear previous logging configurations.
		LogManager.getLogManager().reset();

		// Get the logger for "org.jnativehook" and set the level to off.
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
		
		AutoFF14HouseBuyer buyer = new AutoFF14HouseBuyer();

		GlobalScreen.addNativeKeyListener(buyer);
		
		System.out.println("Starting program.");
		buyer.run();
	}

	public void run() {
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
		//0, 0, 0, 0, down, 0, left, 0 
		while(running) {
			System.out.println("Running");
			
			Random random = new Random();
			
			if (inputStart) {
				int keyDelay = 100 + (int) (100 * random.nextFloat());
				int serverWait = 500 + (int) (100 * random.nextFloat());
				
				robot.delay(keyDelay);
				robot.keyPress(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				robot.keyRelease(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				System.out.println("1 -> 0");
				
				robot.delay(keyDelay);
				robot.keyPress(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				robot.keyRelease(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				System.out.println("2 -> 0");
				
				robot.delay(serverWait);
				robot.keyPress(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				robot.keyRelease(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				System.out.println("3 -> 0");
				
				robot.delay(keyDelay);
				robot.keyPress(KeyEvent.VK_DOWN);
				robot.delay(keyDelay);
				robot.keyRelease(KeyEvent.VK_DOWN);
				robot.delay(keyDelay);
				System.out.println("4 -> Down");
				
				robot.delay(keyDelay);
				robot.keyPress(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				robot.keyRelease(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				System.out.println("5 -> 0");
				
				robot.delay(keyDelay);
				robot.keyPress(KeyEvent.VK_LEFT);
				robot.delay(keyDelay);
				robot.keyRelease(KeyEvent.VK_LEFT);
				robot.delay(keyDelay);
				System.out.println("6 -> Left");
				
				robot.delay(keyDelay);
				robot.keyPress(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				robot.keyRelease(KeyEvent.VK_NUMPAD0);
				robot.delay(keyDelay);
				System.out.println("7 -> 0");
				
				robot.waitForIdle();
			}
		}
		
	}
	
	@Override
	public void nativeKeyPressed(NativeKeyEvent e) {

	}

	@Override
	public void nativeKeyReleased(NativeKeyEvent e) {
		if (e.getKeyCode() == NativeKeyEvent.VC_CONTROL) {
			inputStart = !inputStart;
			System.out.println("Input Start:" + inputStart);
		}
		
		if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
			running = false;
			System.out.println("Ending program.");
			
			try {
				GlobalScreen.unregisterNativeHook();
			} catch (NativeHookException e1) {
				e1.printStackTrace();
			}
			
			System.exit(0);
		}
	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
