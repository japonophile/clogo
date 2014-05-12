import java.io.DataInputStream;

import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

public class LogoTurtle {
	
	public class LogoCommand {
		public static final int QUITTE = 0;
		public static final int AVANCE = 1;
		public static final int RECULE = 2;
		public static final int GAUCHE = 3;
		public static final int DROITE = 4;
		public static final int LEVECRAYON = 5;
		public static final int BAISSECRAYON = 6;
	}
	
	public static int moveSpeed = 100;
	public static int moveStep = 30;
	public static int turnSpeed = 100;
	public static float turnStep = (float) 4.2;
	public static int pencilSpeed = 100;
	public static int pencilStep = 100;
	
	public static void log(String msg) {
		
		LCD.clear();
		LCD.drawString(msg, 0, 0);
		LCD.refresh();
	}

	public static void main(String[] args) {
		
		boolean stopped = false;
		NXTConnection connection = null;
		DataInputStream dis = null;
		
		while (!stopped) {
		
			try {
					
				log("CONNECTION...");
					
				connection = Bluetooth.waitForConnection();
				dis = connection.openDataInputStream();
				
				while (true) {
					
					int command = dis.readInt();
					if (command == LogoCommand.QUITTE) {
						stopped = true;
						break;
					}
					
					int distance = 0, angle = 0;
					switch (command) {
					case LogoCommand.AVANCE:
						distance = dis.readInt();
						log("AVANCE " + distance);
						Motor.B.setSpeed(moveSpeed);
						Motor.B.rotate(moveStep * distance);
						break;
					case LogoCommand.RECULE:
						distance = dis.readInt();
						log("RECULE " + distance);
						Motor.B.setSpeed(moveSpeed);
						Motor.B.rotate(- moveStep * distance);
						break;
					case LogoCommand.GAUCHE:
						angle = dis.readInt();
						log("GAUCHE " + angle);
						Motor.C.setSpeed(turnSpeed);
						Motor.C.rotate(- Math.round(turnStep * angle));
						break;
					case LogoCommand.DROITE:
						angle = dis.readInt();
						log("DROITE " + angle);
						Motor.C.setSpeed(turnSpeed);
						Motor.C.rotate(Math.round(turnStep * angle));
						break;
					case LogoCommand.LEVECRAYON:
						log("LEVECRAYON");
						Motor.A.setSpeed(pencilSpeed);
						Motor.A.rotate(pencilStep / 2);
						break;
					case LogoCommand.BAISSECRAYON:
						log("BAISSECRAYON");
						Motor.A.setSpeed(pencilSpeed);
						Motor.A.rotate(- pencilStep);
						break;
					}
				}
				
				dis.close();
				connection.close();
			}
			catch (Exception e) {
				
				try {
					dis.close();
					connection.close();
				}
				catch (Exception e2) {}
				
				log("INTERRUPTION!");
				
				try {
					Thread.sleep(1000);
				}
				catch (Exception e3) {}
			}
		}
	}
}
