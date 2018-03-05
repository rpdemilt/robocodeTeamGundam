package rdemilt_hkifle;

import java.awt.Color;
import java.awt.geom.Point2D;

import robocode.*;
import static robocode.util.Utils.*;

public class MoveTester extends RateControlRobot{
	private int turnSwitch = 1;
	private int strafeSwitch = 1;
	private int gunTurnSwitch = 1;
	private int turnRate = 10;
	private final int WALL_BOUNDARY = 50;
	private int clock = 0;
	private final int MAX_CLOCK = 100;
	private double fireSafety = 1.0;
	private double[] energyTrack = new double[2]; 
	private int tooCloseToWall = 0;
	
	public void run() {
		setBodyColor(Color.RED);
		setGunColor(Color.BLUE);
		setRadarColor(Color.WHITE);
		setBulletColor(Color.RED);
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(false);
		
		addCustomEvent(new Condition("ShotPredicted") {
			public boolean test() {
				return ((energyTrack[0] - energyTrack[1]) > 2);
			}
		});
		addCustomEvent(new Condition("Wall") {
			public boolean test() {
				return (getX() <= WALL_BOUNDARY || getX() >= getBattleFieldWidth() - WALL_BOUNDARY ||
						getY() <= WALL_BOUNDARY || getY() >= getBattleFieldHeight() - WALL_BOUNDARY);
			}
		});
		while(true) {
			setVelocityRate(8);
			setGunRotationRate(10);
			turnRight(turnSwitch * 5);
			execute();
		}
	}
	public void onScannedRobot(ScannedRobotEvent e) {
		setGunRotationRate(0);
		execute();
	
		//energyTrack[0] = e.getEnergy();
		if(e.getBearing() >= 0) {
			turnSwitch = 1;
			gunTurnSwitch = 1;
		} else {
			turnSwitch = -1;
			gunTurnSwitch = -1;
		}
		double targetAbsBearing = getHeading() + e.getBearing();
		double targetVelocity = e.getVelocity();
		double bearingFromGun = normalRelativeAngleDegrees(targetAbsBearing - getGunHeading());
		if(targetVelocity == 0) {
			if(bearingFromGun <= 10) {
				if(canFire()) {
					setFire(3);
				}
			} else {
				setTurnGunRight(getHeading() + bearingFromGun);
			}
		} else {
			//energyTrack[1] = e.getEnergy();
			leadShot(getProjectileSpeed(e.getDistance()),e);
			if(canFire()) {
				setFire(getFirePower(e.getDistance()));
			}
			
		}
		if(getEnergy() > 50) {
			ram(e);
			execute();
		} else {
			strafe(e);
			scan();
		}
		
	}
	public void onHitRobot(HitRobotEvent e) {
		double targetAbsBearing = getHeading() + e.getBearing();
		double bearingFromGun = normalRelativeAngleDegrees(targetAbsBearing - getGunHeading());
		if(e.getBearing() >= 0) {
			turnSwitch = 1;
		} else {
			turnSwitch = -1;
		}
		setTurnGunRight(bearingFromGun);
		if(getGunHeat() == 0){
			setFire(3 * fireSafety);
		}
		setBack(100);
	}
	//public void onHitWall(HitWallEvent e) {
		//setBack(50);
	//}
	public void onCustomEvent(CustomEvent e) {
		if(e.getCondition().getName().equals("ShotPredicted")) {
			if(getEnergy() > 50) {
				dodge();
			} else {
				safeDodge();
			}
			energyTrack = new double[2];
		} else if(e.getCondition().getName().equals("Wall")) {
			
			if (tooCloseToWall <= 0) 
			{
				tooCloseToWall += WALL_BOUNDARY;
				setMaxVelocity(0);
			}
			turnRight(90);
			setMaxVelocity(8);
			ahead(100);
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
		return Math.min(400 / distance,3) * fireSafety;
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
	private void clock() {
		clock++;
		if(clock > MAX_CLOCK) {
			clock = 0;
		}
	}
	private void dodge() {
		setVelocityRate(-getVelocityRate());
		back(30);
	}
	private void safeDodge() {
		setVelocityRate(-getVelocityRate());
		back(10);
	}
	private void strafe(ScannedRobotEvent e) {
		setTurnRight(e.getBearing() + 90 + 5);
		setVelocityRate(4);
		setGunRotationRate(30);
	}
	private void straighten(ScannedRobotEvent e) {
		setTurnRight(e.getBearing());
		setGunRotationRate(30);
	}
	private void ram(ScannedRobotEvent e) {
		turnRight(e.getBearing());
		ahead(e.getDistance() + 7);
	}
}
