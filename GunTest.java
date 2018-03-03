package robots;

import java.awt.Color;
import java.awt.geom.Point2D;

import robocode.*;
import static robocode.util.Utils.*;

public class GunTest extends RateControlRobot{
	private int turnSwitch = 1;
	private int gunTurnSwitch = 1;
	private int turnRate = 5;
	private String pattern;
	private int clock = 0;
	
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
			
			switch(pattern) {
				case "HUNT":
					setVelocityRate(10);
					setTurnRate(turnSwitch * turnRate);
					break;

				default:
					setGunRotationRate(10 * gunTurnSwitch);
					setVelocityRate(10);
					setTurnRate(turnSwitch * turnRate);
					break;
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
		double targetAbsBearing = getHeading() + e.getBearing();
		double targetVelocity = e.getVelocity();
		double bearingFromGun = normalRelativeAngleDegrees(targetAbsBearing - getGunHeading());
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
			if(canFire()) {
				fire(getFirePower(e.getDistance()));
			}
		}
		switch(pattern) {
			case "HUNT":
				setVelocityRate(30);
				turnRight(e.getBearing());
				scan();
				break;
			case "COWER":
				setVelocityRate(50);
				turnRight(-e.getBearing());
			default:
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
			pattern = "COWER";
		} else if(e.getCondition().getName().equals("Clock")) {
			
		} else if(e.getCondition().getName().equals("HighEnergy")) {
			pattern = "HUNT";
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
		double targetHeading = e.getHeadingRadians();
		double targetAbsBearing = getHeadingRadians() + e.getBearingRadians();
		long time = (long) (distance / projectileSpeed);
		
		double nextX = (x + distance * Math.sin(targetAbsBearing) + targetVelocity * Math.sin(targetHeading) * time);
		double nextY = (y + distance * Math.cos(targetAbsBearing) + targetVelocity * Math.cos(targetHeading) * time);
		double futureAbsBearing = getAbsoluteBearing(x,y,nextX,nextY);
		double bearingFromGun = normalRelativeAngle(futureAbsBearing - getGunHeadingRadians());
		turnGunRightRadians(bearingFromGun);
	}
	private double getProjectileSpeed(double distance) {
		return 20 - getFirePower(distance) * 3 ;
	}
	private double getFirePower(double distance) {
		return Math.min(400 / distance,3);
	}
	/* adapted from 
	 * http://mark.random-article.com/weber/java/robocode/lesson4.html
	 * absoluteBearing method
	 */
	private double getAbsoluteBearing(double x,double y,double x1,double y1) {
		double deltaX = x1 - x;
		double deltaY = y1 - y;
		double distance = Point2D.distance(x, y, x1, y1);
		double arcSin = Math.asin(deltaX/distance);
		double bearing = 0;
		if (deltaX > 0 && deltaY > 0) { // both pos: lower-Left
			bearing = arcSin;
		} else if (deltaX < 0 && deltaY > 0) { // x neg, y pos: lower-right
			bearing = 2 * Math.PI + arcSin; // arcsin is negative here, actuall 360 - ang
		} else if (deltaX > 0 && deltaY < 0) { // x pos, y neg: upper-left
			bearing = Math.PI - arcSin;
		} else if (deltaX < 0 && deltaY < 0) { // both neg: upper-right
			bearing = Math.PI - arcSin; // arcsin is negative here, actually 180 + ang
		}
		return bearing;
	}
}
