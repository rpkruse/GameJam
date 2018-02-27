package com.jam.game.levels;

import java.util.Arrays;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Queue;
import com.jam.game.b2d.Box2dPlatformBuilder;
import com.jam.game.powerup.Powerup;
import com.jam.game.screens.GameScreen;
import com.jam.game.utils.Rando;
import com.jam.game.utils.enums.PlatformType;
import com.jam.game.utils.enums.PowerupType;

public class Level {
	
	private static final float SCALE = 1.0f;
	
	public static int NUB_COUNT = 0;
	
	public static final float MIN_WIDTH = 3.50f / SCALE;
	public static final float MAX_WIDTH = 4.75f / SCALE;
	
	public static final float MIN_HEIGHT = 0.40f / SCALE;
	public static final float MAX_HEIGHT = 0.41f / SCALE;
	
	public static final float MIN_Y_INC = 7.0f / SCALE;
	public static final float MAX_Y_INC = 7.1f / SCALE;
	
//	public static final float MIN_Y_INC = 7.0f / SCALE;
//	public static final float MAX_Y_INC = 10.1f / SCALE;
	
	public static final float MIN_X = 5.0f;
	
	public Entity[] walls;
	
	public float wallHeight = 10.0f;
	private float wallWidth = 2.0f;
	
	private float nubSize = 0.5f;
	
	private float slickPlatformChance = 0.75f; // 25% chance
	
//	private float chanceToSpawnDoublePlatform = 0.75f;

	private int[] lastRow;
	
	private final float SCREEN_SEG = GameScreen.VIRTUAL_WIDTH/3;
	
	private StateMachine sm;
	private Queue<Platform> queue;
	private World world;
	
	public Level(World world) {
		this.queue = new Queue<Platform>();
		this.world = world;
		this.sm = new StateMachine();
	
		this.walls = new Entity[2];
	}
	
	public Platform[] spawnNextWithRow(PooledEngine engine){
		Platform[] platforms = new Platform[3];
		
		float yPos, xPos, width;
		float height = 0.5f;
		
		yPos = 10.0f;
		
		int[] nextRowToSpawn;
		
		boolean firstRow = this.lastRow == null;
		
		if(firstRow){ //We must spawn the first row
			this.lastRow = this.getPlatRowFromLast(true);
			nextRowToSpawn = this.getPlatRowFromLast(false);
		}else{
			nextRowToSpawn = this.getPlatRowFromLast(false);
		}
		
		if (queue.size > 0 && !firstRow) {
			yPos = queue.first().y + randomFloatInRange(Level.MIN_Y_INC, Level.MAX_Y_INC);
		}
		
		for(int i=0; i<this.lastRow.length; i++){
			int val = this.lastRow[i];
			
			if(val == 0) continue;
			
			
			//Nub:
			if(val == 2){
				platforms[i] = new Platform();
				xPos = getNubPosBasedOnCurrentSeg(i);
				platforms[i].set(xPos, yPos + MAX_Y_INC - MAX_Y_INC/3, nubSize, nubSize, PlatformType.NUB);
				
				Body nubbody = Box2dPlatformBuilder.DEFAULT(platforms[i]).build(world);
				platforms[i].setBody(nubbody);
			}else{
				platforms[i] = new Platform();

				width = randomFloatInRange(Level.MIN_WIDTH, Level.MAX_WIDTH);
				
				
				xPos = getXPosBasedOnCurrentSeg(i, width);
				
				if(Rando.getRandomNumber() <= this.slickPlatformChance){
					platforms[i].set(xPos, yPos, width, height, PlatformType.OIL);
				}else{
					platforms[i].set(xPos, yPos, width, height); //Default Platform
				}
				
				platforms[i].setSegment(this.getScreenSeg(xPos));
				
				Body body = Box2dPlatformBuilder.DEFAULT(platforms[i]).buildAndDispose(world); // add body to world and retrieve it
				
				platforms[i].setBody(body);
				
				queue.addFirst(platforms[i]);
			}
		}
		
		this.lastRow = nextRowToSpawn;
		
		return platforms;
	}
	
	public Platform[] spawnNextWithStates(PooledEngine engine) {
		Platform[] platforms = new Platform[4];
		
		int[][] currentStateValues = this.sm.moveToNextStateAndReturn();
		
		int onPlat = 0;
		float yPos, xPos, width;
		float height = 0.5f;
		
		yPos = 10.0f;//randomFloatInRange(Level.MIN_Y_INC, Level.MAX_Y_INC);
		
		if (queue.size > 0) {
			yPos = queue.first().y; //+ randomFloatInRange(Level.MIN_Y_INC, Level.MAX_Y_INC);
		}
		
		System.out.println("ON STATE: " + this.sm.current_state);
		
		for(int i=currentStateValues.length-1; i>=0; i--){
			yPos += randomFloatInRange(Level.MIN_Y_INC, Level.MAX_Y_INC) * ((currentStateValues.length-1) - i);
			for(int j=0; j<currentStateValues[i].length; j++){
				int val = currentStateValues[i][j];
				if(val == 0) continue;
				
				if(val == 2){
					platforms[onPlat] = new Platform();
					xPos = getNubPosBasedOnCurrentSeg(j);
					platforms[onPlat].set(xPos, yPos - MAX_Y_INC/3, nubSize, nubSize, PlatformType.NUB);
					
					Body nubbody = Box2dPlatformBuilder.DEFAULT(platforms[onPlat]).build(world);
					platforms[onPlat].setBody(nubbody);
				}else{
					platforms[onPlat] = new Platform();

					width = randomFloatInRange(Level.MIN_WIDTH, Level.MAX_WIDTH);
					
					
					xPos = getXPosBasedOnCurrentSeg(j, width);
					
					if(Rando.getRandomNumber() <= this.slickPlatformChance){
						platforms[onPlat].set(xPos, yPos, width, height, PlatformType.OIL);
					}else{
						platforms[onPlat].set(xPos, yPos, width, height); //Default Platform
					}
					
					platforms[onPlat].setSegment(this.getScreenSeg(xPos));
					
					Body body = Box2dPlatformBuilder.DEFAULT(platforms[onPlat]).buildAndDispose(world); // add body to world and retrieve it
					
					platforms[onPlat].setBody(body);
					
					queue.addFirst(platforms[onPlat]);
				}
				onPlat++;
			}
		}

		return platforms;
	}
	
	public Powerup spawnPowerUp(float platformX, float platformY, PooledEngine engine){
		Powerup p = new Powerup();
		
		float xPos = platformX + MIN_WIDTH/7;
		float yPos = platformY + MAX_HEIGHT*4;
		
		p.set(xPos, yPos, 1f, 1f);
		
		Body body = Box2dPlatformBuilder.DEFAULT(p).build(world);
		p.setBody(body);
		
		PowerupType puT = Rando.getRandomNumber() > 0.0f ? PowerupType.LIGHT : PowerupType.HELMET; 
		p.setType(puT);
		if(p.getType() == PowerupType.LIGHT) p.setLightSystem();
		
		return p;
	}
	
	public Body[] spawnLeftAndRightWalls(float y){
		Platform[] walls = new Platform[2];
		
		//Left Wall:
		walls[0] = new Platform();
		walls[0].set(0, y, this.wallWidth, this.wallHeight);		
		Body lBody = Box2dPlatformBuilder.DEFAULT(walls[0]).buildAndDispose(world); // add body to world and retrieve it
		walls[0].setBody(lBody);
		
		//Right Wall:
		walls[1] = new Platform();
		walls[1].set(GameScreen.VIRTUAL_WIDTH, y, this.wallWidth, this.wallHeight);		
		Body rBody = Box2dPlatformBuilder.DEFAULT(walls[1]).buildAndDispose(world); // add body to world and retrieve it
		walls[1].setBody(rBody);
				
		return new Body[] {walls[0].getBody(), walls[1].getBody()};
	}
	
	private int[] getPlatRowFromLast(boolean isFirstRow){
		int[] newRow = new int[]{0,0,0};
		
		boolean twoPlats = Rando.coinFlip();
		
		int index;
		
		//Step 1: if we are the first row, return one (or two) new platform positions to spawn
		if(isFirstRow){
			index = this.getValidIndex(newRow);
			newRow[index] = 1;
			
			if(twoPlats){
				index = this.getValidIndex(newRow);
				newRow[index] = 1;
			}
			
			return newRow;
		}
		
		//Step 2:
		index = this.getValidIndex(newRow);
		newRow[index] = 1;
		int dist = this.getDistanceBetweenPlatRows(this.lastRow, index);
		
		//If we spawn below, place nub next to the above row and try to place another plat
		if(dist == 0){
			//If we spawn the nub in the middle, make it a random side
			if(index == 1){
				
				//If it has neighbors we dont need to care:
				if(this.lastRow[0] != 0 || this.lastRow[2] != 0) return newRow;
				
				int newNubPos = Rando.coinFlip() ? index - 1 : index + 1;
				newRow[newNubPos] = 1; //(Nub value...change to var later!)
				//Try to spawn other plat
				if(twoPlats){
					index = this.getValidIndex(newRow);
					newRow[index] = 1;
				}
			}else{ //Just spawn the nub to the right or left (depending on placement)
				if(index == 0){
					if(this.lastRow[1] != 0) return newRow;
					newRow[index+1] = 1;
				}
				if(index == 2){
					if(this.lastRow[1] != 0) return newRow;
					newRow[index-1] = 1;
				}
			}
		//If it is next to it, just spawn another platform on the current row randomly
		}else if(dist == 1){
			if(twoPlats){
				index = this.getValidIndex(newRow);
				newRow[index] = 1;
			}
		//If it is very large, we must spawn a new nub between (will always be the center)
		}else if(dist > 1){
			newRow[1] = 2;
		}
		return newRow;
	}
	
	private int getDistanceBetweenPlatRows(int[] lastRow, int currentPlatPos){
		int dist = -1;
		
		if(lastRow[currentPlatPos] == 1){
			dist = 0;
		}else if(currentPlatPos == 1){ //If we are in the middle, then we know that the dist is one (b/c one is not above)
			dist = 1;
		}else if((currentPlatPos > 1 && lastRow[currentPlatPos - 1] == 1) || (currentPlatPos == 0 && lastRow[currentPlatPos + 1] == 1)){
			dist = 1;
		}else if((currentPlatPos == 2 && this.lastRow[0] == 1) || (currentPlatPos == 0 && this.lastRow[2] == 1)){
			dist = 2;
		}
		
		return dist;
	}
	private int getValidIndex(int[] row){
		int index = Rando.getRandomBetweenInt(row.length);
		
		while(row[index] != 0){
			index = Rando.getRandomBetweenInt(row.length);
		}
		
		return index;
	}
	
	private float getXPosBasedOnLastSeg(int lastSeg, float width){
		if(lastSeg < 0){
			return randomFloatInRange(MIN_X, GameScreen.VIRTUAL_WIDTH - MIN_X - width);
		}else if(lastSeg == 0){ //Left => M || R
			return randomFloatInRange(this.SCREEN_SEG + MIN_X, GameScreen.VIRTUAL_WIDTH - MIN_X - width);
		}else if(lastSeg == 1){//Middle ==> L || R
			return Rando.getRandomNumber() > 0.5f ? 
					randomFloatInRange(MIN_X, this.SCREEN_SEG - MIN_X - width) : //Left
					randomFloatInRange(this.SCREEN_SEG * 2, GameScreen.VIRTUAL_WIDTH - MIN_X - width); //Right
		}else{//Right => L || M
			return randomFloatInRange(MIN_X, this.SCREEN_SEG*2 - MIN_X - width);
		}
	}
	
	private float getNubPosBasedOnCurrentSeg(int seg){
		if(seg == 0){
			return this.SCREEN_SEG;
		}else if(seg == 1){
			return randomFloatInRange(this.SCREEN_SEG, this.SCREEN_SEG*2);
		}else{
			return this.SCREEN_SEG*2;
		}
	}
	
	private float getXPosBasedOnCurrentSeg(int seg, float width){
		if(seg == 0){ //Left
			return randomFloatInRange(MIN_X, this.SCREEN_SEG - MIN_X - width);
		}else if(seg == 1){//Middle
			return randomFloatInRange(this.SCREEN_SEG, this.SCREEN_SEG*2 - MIN_X - width);
		}else{//Right
			return randomFloatInRange(this.SCREEN_SEG*2, GameScreen.VIRTUAL_WIDTH - MIN_X - width);
		}
	}
	
	private int getScreenSeg(float x){
		int seg = 0;
		
		for(int i=1; i<4; i++){
			float testSeg = this.SCREEN_SEG * i;
			
			if(x <= testSeg){
				seg = i;
				return seg;
			}
		}
		
		return seg;
	}

	public Platform getHead() {
		
		if (queue.size == 0) {
			return null;
		}
		
		return queue.first();
	}
	
	public Platform getTail() {
		
		if (queue.size == 0) {
			return null;
		}
		
		return queue.last();
	}
	
	public Platform getAndRemoveHead() {
		
		if (queue.size == 0) {
			return null;
		}
		
		world.destroyBody(queue.first().getBody());
		return queue.removeFirst();
	}
	
	public Platform getAndRemoveTail() {
		
		if (queue.size == 0) {
			return null;
		}
		
		world.destroyBody(queue.last().getBody());
		return queue.removeLast();
	}

	
	private float randomFloatInRange(float start, float end) {
		return (Rando.getRandomNumber() * (end - start))+start;
	}
	
	public Queue<Platform> getPlatformQueue() {
		return queue;
	}
	
}
