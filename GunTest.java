package robots;

import java.awt.Color;
import robocode.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import static robocode.util.Utils.normalRelativeAngle;

public class GunTest extends RateControlRobot{
	private int turnSwitch = 1;
	private int gunTurnSwitch = 1;
	private int turnRate = 5;
	private int clock = 0;
	private int pattern = 0;
	
	public void run() {
		setBodyColor(Color.RED);
		setGunColor(Color.BLUE);
		setRadarColor(Color.WHITE);
		setBulletColor(Color.RED);
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(false);
		
		addCustomEvent(new Condition("HighEnergy") {
			public boolean test() {
				return getEnergy() > 80;
			}
		});
		addCustomEvent(new Condition("LowEnergy") {
			public boolean test() {
				return getEnergy() > 30;
			}
		});
		addCustomEvent(new Condition("Clock") {
			public boolean test() {
				return clock >= 8;
			}
		});
		while(true) {
			setGunRotationRate(5 * gunTurnSwitch);
			switch(pattern) {
				case 0:
					setVelocityRate(10);
					setTurnRate(turnSwitch * turnRate);
					break;
				case 1:
				case 2:
				default:
			}
			execute();
		}
	}
	public void onScannedRobot(ScannedRobotEvent e) {
		setGunRotationRate(0);
		execute();
		if(e.getBearing() >= 0) {
			gunTurnSwitch = 1;
		} else {
			gunTurnSwitch = -1;
		}
		double targetBearing = getHeading() + e.getBearing();
		double targetVelocity = e.getVelocity();
		double bearingFromGun = normalRelativeAngleDegrees(targetBearing - getGunHeading());
		if(targetVelocity == 0) {
			if(bearingFromGun <= 10) {
				if(canFire()) {
					fire(3);
				}
			} else {
				setTurnGunRight(getHeading() + bearingFromGun);
			}
		} else {
			leadShot(getProjectileSpeed(e.getDistance()),e);
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
	public void onCustomEvent(CustomEvent e) {
		if(e.getCondition().getName().equals("LowEnergy")) {
			
		} else if(e.getCondition().getName().equals("Clock")) {
			if(pattern == 2) {
				pattern = 0;
			} else {
				pattern++;
			}
		}
	}
	private boolean canFire() {
		return getGunHeat() == 0;
	}
	private void leadShot(double projectileSpeed, ScannedRobotEvent e) {
		double x = getX();
		double y = getY();
		double distance = e.getDistance();
		double targetVelocity = e.getVelocity();
		double targetHeading = e.getHeading();
		double time = projectileSpeed * distance;
		
		double nextX = x + distance * Math.sin(targetHeading) + targetVelocity * Math.cos(targetHeading) * time;
		double nextY = y + distance * Math.cos(targetHeading) + targetVelocity * Math.sin(targetHeading) * time;
		
		double trackAngle = Math.atan(nextX/nextY);
		turnGunRightRadians(normalRelativeAngle(trackAngle - getGunHeadingRadians()));
		if(canFire()) {
			fire(getFirePower(distance));
		}
	}
	private double getProjectileSpeed(double distance) {
		return 20 - getFirePower(distance) * 3 ;
	}
	private double getFirePower(double distance) {
		return Math.min(400 / distance,3);
	}
}
