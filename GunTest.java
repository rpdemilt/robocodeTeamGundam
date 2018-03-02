package robots;

import java.awt.Color;
import robocode.*;

public class GunTest extends RateControlRobot{
	private int turnSwitch = 1;
	private int clock = 0;
	public void run() {
		setBodyColor(Color.RED);
		setGunColor(Color.BLUE);
		setRadarColor(Color.WHITE);
		
		setAdjustRadarForRobotTurn(false);
		setAdjustGunForRobotTurn(false);
		
		setRadarRotationRateRadians(Math.PI / 4.0);
		setGunRotationRateRadians(Math.PI / 4.0);
		execute();
		while(true) {	
			if(clock < 8) {
				setTurnRate(turnSwitch * 10);
				setVelocityRate(50);
			} else if(clock > 8) {
				setTurnRate(turnSwitch * -10);
				setVelocityRate(50);
			}
			execute();
		}
	}
	public void onScannedRobot(ScannedRobotEvent e) {
		if(e.getBearing() >= 0) {
			turnSwitch = 1;
		} else {
			turnSwitch = -1;
		}
		
		if(getEnergy() > 50) {
			if(getGunHeat() == 0) {
				fire(1);
			}
			ahead(e.getDistance() + 5);
			scan();
		} else {
			back(e.getDistance());
			if(getGunHeat() == 0) {
				fire(.5);
			}
			turnRight(-turnSwitch * 5);
		}
	}
	public void onHitRobot(HitRobotEvent e) {
		if(e.getBearing() >= 0) {
			turnSwitch = 1;
		} else {
			turnSwitch = -1;
		}
		if(getGunHeat() == 0){
			fire(3);
		}
		ahead(10);
	}
	public void onHitWall(HitWallEvent e) {
		setVelocityRate(-1 * getVelocityRate());
	}
}
