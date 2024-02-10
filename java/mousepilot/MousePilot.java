package mousepilot;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JFrame;

import clam.shell.Clam;
import clam.shell.gui.ClamSystemTray;
import clam.shell.joypad.Joypad;
import clam.shell.joypad.JoypadListener;
import clam.shell.joypad.JoypadStream;
import clam.shell.joypad.events.AxisEventX;
import clam.shell.joypad.events.AxisEventY;
import clam.shell.joypad.events.ButtonPressedEvent;
import clam.shell.joypad.events.ButtonReleasedEvent;
import clam.shell.joypad.events.DpadPressedEvent;
import clam.shell.joypad.events.JoypadEvent;

/*
 * Copyright 2024 Christopher Sloetjes
 * http://sloetjes.github.io
 */
public final class MousePilot {	
	public static void main(String[] args) {
		new MousePilot ();
	}
	protected double speed = 8;
	protected Robot robot = null;
	protected JoypadStream controllerStream = null;		
	public MousePilot () {
		try {
			new ClamSystemTray("Mouse Pilot", "./resources/joypad.png");
			robot = new Robot();
			controllerStream = new JoypadStream();						
			controllerStream.addControllerListener(new MousePilotJoypadListener(this));
			new MousePilotPolling (this).start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}		
}
class BlackoutFrame extends JFrame {
	/*
	 * This class is meant to hide the screen
	 */
	public BlackoutFrame(int x, int y, int w, int h) throws Exception {
		super("Blackout!");
		setAlwaysOnTop(true);
		setUndecorated(true);
		setExtendedState(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(new Rectangle(x, y, w, h));
		validate();		
	}

	public void paint(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
class MousePilotPolling extends Thread {
	/*
	 * This moves the mouse according to the X-Axis and Y-Axis input
	 */
	MousePilot mp = null;
	public MousePilotPolling (MousePilot mp) {
		this.mp=mp;
		this.setDaemon(false);
	}
	public void run() {
		try {
			while (true) {
				for (Joypad c : mp.controllerStream.controllers) {
					if (c.x != 0 || c.y != 0) {
						PointerInfo pi = MouseInfo.getPointerInfo();
						Point p = pi.getLocation();
						mp.robot.mouseMove(p.x + (int) (c.x * mp.speed), p.y - (int) (c.y * mp.speed));
					}
				}
				Clam.nap();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}
class MousePilotJoypadListener extends JoypadListener {	
	/*
	 * This is an event-oriented (non-polling) listener for "right now" commands:
	 * 
	 * mouse button: right now
	 * mouse wheel: right now
	 * mouse axis: save data for polling 
	 */
	BlackoutFrame[] blackout = null;	
	MousePilot mp = null;
	public MousePilotJoypadListener(MousePilot mp) throws Exception {		
		blackout = new BlackoutFrame[] { new BlackoutFrame(0, 0, 2560, 1440), new BlackoutFrame(-1920, 360, 1920, 1080) };
		this.mp=mp;
	}

	static HashMap<Integer, Integer> mouseMap = new HashMap<Integer, Integer>();
	static HashMap<Integer, Integer> keyMap = new HashMap<Integer, Integer>();
	static HashMap<Integer, Integer> wheelMap = new HashMap<Integer, Integer>();
	static {
		mouseMap.put(JoypadEvent.VBUTTON_Y, MouseEvent.BUTTON1_DOWN_MASK);
		mouseMap.put(JoypadEvent.VBUTTON_X, MouseEvent.BUTTON3_DOWN_MASK);	
		
		keyMap.put(JoypadEvent.LEFT_TRIGGER_1, KeyEvent.VK_CONTROL);
		keyMap.put(JoypadEvent.LEFT_TRIGGER_2, KeyEvent.VK_ALT);
		keyMap.put(JoypadEvent.RIGHT_TRIGGER_1, KeyEvent.VK_TAB);
		keyMap.put(JoypadEvent.RIGHT_TRIGGER_2, KeyEvent.VK_ESCAPE);
		keyMap.put(JoypadEvent.VBUTTON_GO, KeyEvent.VK_ENTER);
		keyMap.put(JoypadEvent.VBUTTON_MODE, KeyEvent.VK_BACK_SPACE);
		keyMap.put(JoypadEvent.VBUTTON_A, KeyEvent.VK_SPACE);
		keyMap.put(JoypadEvent.VBUTTON_B, KeyEvent.VK_PRINTSCREEN);
		
		wheelMap.put(JoypadEvent.VBUTTON_LEFT, -2);
		wheelMap.put(JoypadEvent.VBUTTON_RIGHT, 2);
		wheelMap.put(JoypadEvent.VBUTTON_UP, -1);
		wheelMap.put(JoypadEvent.VBUTTON_DOWN, 1);
	}
	/*
	 * an event has happened, process it!
	 */
	public void action(JoypadEvent e) {
		try {
			if (e instanceof DpadPressedEvent) {
				mp.robot.mouseWheel(wheelMap.get(e.getButtonID()));
			} else if (e instanceof ButtonPressedEvent) {
				if (e.getButtonID() == JoypadEvent.VBUTTON_MINUS) {
					mp.speed = mp.speed - 1 <= 0 ? 1 : mp.speed - 1;
				} else if (e.getButtonID() == JoypadEvent.VBUTTON_PLUS) {
					mp.speed = mp.speed + 1 >= 32 ? 32 : mp.speed + 1;
				} else if (e.getButtonID() == JoypadEvent.RIGHT_THUMB) {
					toggleBlackout(0);
				} else if (e.getButtonID() == JoypadEvent.LEFT_THUMB) {
					toggleBlackout(1);
				} else if (mouseMap.get(e.getButtonID()) != null) {
					mp.robot.mousePress(mouseMap.get(e.getButtonID()));
				} else if (keyMap.get(e.getButtonID()) != null) {
					mp.robot.keyPress(keyMap.get(e.getButtonID()));
				}
			} else if (e instanceof ButtonReleasedEvent) {
				if (mouseMap.get(e.getButtonID()) != null) {
					mp.robot.mouseRelease(mouseMap.get(e.getButtonID()));
				} else if (keyMap.get(e.getButtonID()) != null) {
					mp.robot.keyRelease(keyMap.get(e.getButtonID()));
				}
			} else if (e instanceof AxisEventX) {
				mp.controllerStream.controllers[e.getControllerID()].x = e.getValue();
			} else if (e instanceof AxisEventY) {
				mp.controllerStream.controllers[e.getControllerID()].y = e.getValue();
			}
		} catch (Exception x) {
			x.printStackTrace();
			System.exit(0);
		}
	}

	public void toggleBlackout(int i) throws IOException {
		blackout[i].setVisible(!blackout[i].isVisible());
		new ProcessBuilder("./native/muteVolume.exe").start();
	}
}