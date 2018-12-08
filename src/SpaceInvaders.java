import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import org.jbox2d.dynamics.BodyType;

import com.almasb.fxgl.GameApplication;
import com.almasb.fxgl.GameSettings;
import com.almasb.fxgl.asset.Assets;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityType;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.PhysicsEntity;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class SpaceInvaders extends GameApplication{
		
	private enum Type implements EntityType{
		SCREEN, SCREEN_LEFT, SCREEN_RIGHT, ALIEN, ALIEN_SHOT1, ALIEN_SHIP, PLAYER_SHIP, PLAYER_SHOT, PROTECTOR,  BONUS1, BONUS_LIFE, BONUS_DEATH, BONUS_SLOW, BONUS_SHIELD, BONUS_TNT;
	}
	
	private Assets assets;
	
	private PhysicsEntity player, shot, alienShot, shield;
	
	private HashMap<PhysicsEntity, String[]> alienMap;
	
	private SoundFactory soundFactory;
	
	private Button pauseButton;

	private double alienSpeed = 1;
	private double playerSpeed = 7;
	
	private boolean isShot = false;
	private boolean bonusShot = false;
	private boolean rapidFire = false;
	private boolean paused = false;
	private boolean spacePressed = false;
	
	private IntegerProperty score = new SimpleIntegerProperty();
	private IntegerProperty level = new SimpleIntegerProperty();
	private IntegerProperty lives = new SimpleIntegerProperty();
	private DoubleProperty speed = new SimpleDoubleProperty();
	private IntegerProperty amountBullets = new SimpleIntegerProperty();

	@Override
	protected void initSettings(GameSettings settings) {
		
		settings.setWidth(800);
		settings.setHeight(800);
		settings.setIntroEnabled(false);
		settings.setTitle("SpaceInvaders");
		settings.setVersion("0.1");

	}

	@Override
	protected void initAssets() throws Exception {
		assets = assetManager.cache();
		assets.logCached();
	}

	@Override
	protected void initGame() {
		
		physicsManager.setGravity(0, 0);
		
		initScreenBounds();
		initPlayer();
		initAliens();
		
		level.set(1);
		lives.set(3);
		speed.set(alienSpeed);
		amountBullets.set(1);
		
		soundFactory = new SoundFactory();
		soundFactory.initMedia();
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.ALIEN, Type.SCREEN_RIGHT) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				moveAliensDown(true);
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.ALIEN, Type.SCREEN_LEFT) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				moveAliensDown(false);
				down = true;
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.SCREEN) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				isShot = false;
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.ALIEN_SHOT1, Type.PLAYER_SHIP) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				
				soundFactory.playExplosionSound();
				
				removeEntity(a);
				
				ExploadableImageView expl = new ExploadableImageView(explosionImage, NUM_CELLS_PER_EXPLOSION,b.getPosition().getX(), b.getPosition().getY());
				
				root.getChildren().add(expl);
				
				expl.explode();
				
				lives.set(lives.get()-1);
				
				if(lives.get() == 0){
					displayGameOver();
					pause();						
				}
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.ALIEN_SHOT1, Type.SCREEN) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				
				removeEntity(a);
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});

		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.ALIEN) {
	
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				
				PhysicsEntity alien = (PhysicsEntity)b;
				
				alienMap.remove(alien);
				
				displayPoints("+10", alien.getPosition().getX(), alien.getPosition().getY());
				
				ExploadableImageView expl = new ExploadableImageView(explosionImage, NUM_CELLS_PER_EXPLOSION,alien.getPosition().getX(), alien.getPosition().getY());
				
				root.getChildren().add(expl);
				
				expl.explode();
				
				soundFactory.playKillInvSound();
				
				isShot = false;
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
				
				score.set(score.get() + 10);
				
				createPresent(alien.getPosition().getX(), alien.getPosition().getY());
				
				if(alienMap.size() <= 0){
//					displayGameOver();
//					pause();
					
					level.set(level.get()+1);
					alienSpeed += 0.5;
					alienSpeed = new BigDecimal(alienSpeed).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
					speed.set(alienSpeed);
					initAliens();
				}
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.ALIEN_SHOT1) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				
				displayPoints("+1", b.getPosition().getX(), b.getPosition().getY());
				
				score.set(score.get() + 1);
				isShot = false;
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHIP, Type.BONUS1) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(b);
				rapidFire = true;
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.BONUS1) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				isShot = false;
				amountBullets.set(30);					
				
				displayPoints("RAPID FIRE", b.getPosition().getX(), b.getPosition().getY());
				
				rapidFire = true;
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHIP, Type.BONUS_LIFE) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(b);
				
				displayPoints("+1 LIFE", b.getPosition().getX(), b.getPosition().getY());
				
				lives.set(lives.get()+1);
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.BONUS_LIFE) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				isShot = false;
				
				displayPoints("+1 LIFE", b.getPosition().getX(), b.getPosition().getY());
				
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
				lives.set(lives.get()+1);
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHIP, Type.BONUS_DEATH) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(b);
				
				displayPoints("DEATH", b.getPosition().getX(), b.getPosition().getY());
				
				ExploadableImageView expl = new ExploadableImageView(explosionImage, NUM_CELLS_PER_EXPLOSION,player.getPosition().getX(), player.getPosition().getY());
				
				root.getChildren().add(expl);
				
				expl.explode();
				
				displayGameOver();
				pause();
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.BONUS_DEATH) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				isShot = false;
				
				lives.set(lives.get()-1);
				
				displayPoints("DEATH", b.getPosition().getX(), b.getPosition().getY());
				
				ExploadableImageView expl = new ExploadableImageView(explosionImage, NUM_CELLS_PER_EXPLOSION,player.getPosition().getX(), player.getPosition().getY());
				
				root.getChildren().add(expl);
				
				expl.explode();
				
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
				
				displayGameOver();
				pause();
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHIP, Type.BONUS_SHIELD) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(b);
				
				displayPoints("not available", b.getPosition().getX(), b.getPosition().getY());
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.BONUS_SHIELD) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				isShot = false;
				
				displayPoints("not available", b.getPosition().getX(), b.getPosition().getY());
				
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHIP, Type.BONUS_SLOW) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(b);
				
				if(playerSpeed == 7){
					displayPoints("SLOW", b.getPosition().getX(), b.getPosition().getY());
					displayTimer(30);
					playerSpeed = 3;					
				}else{
					displayPoints("ALREADY SLOW", b.getPosition().getX(), b.getPosition().getY());
				}
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.BONUS_SLOW) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				isShot = false;
				
				if(playerSpeed == 7){
					displayPoints("SLOW", b.getPosition().getX(), b.getPosition().getY());
					displayTimer(30);
					playerSpeed = 3;					
				}else{
					displayPoints("ALREADY SLOW", b.getPosition().getX(), b.getPosition().getY());
				}
				
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHIP, Type.BONUS_TNT) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(b);
				
				letRandomAliensExplode();
				
				displayPoints("+EXPLOSION", b.getPosition().getX(), b.getPosition().getY());
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		physicsManager.addCollisionHandler(new CollisionHandler(Type.PLAYER_SHOT, Type.BONUS_TNT) {
			
			@Override
			public void onCollisionBegin(Entity a, Entity b) {
				removeEntity(a);
				removeEntity(b);
				isShot = false;
				
				displayPoints("+EXPLOSION", b.getPosition().getX(), b.getPosition().getY());
				
				letRandomAliensExplode();
				
				if(amountBullets.get() <= 0){
					amountBullets.set(1);					
				}
				
				
			}
			@Override
			public void onCollisionEnd(Entity a, Entity b) {}
			@Override
			public void onCollision(Entity a, Entity b) {}
		});
		
		
		/* change alien images */
		Timeline timeline = new Timeline(new KeyFrame(
		        Duration.millis(500),
		        ae -> changeImages()));
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();
		
		/* alienShots*/
		Timeline timelineShots = new Timeline(new KeyFrame(
		        Duration.millis(700),
		        ae -> alienShoot()));
		timelineShots.setCycleCount(Animation.INDEFINITE);
		timelineShots.play();
	}
	
	public void letRandomAliensExplode(){
		Random rand = new Random();
		ArrayList<PhysicsEntity> toRemove = new ArrayList<PhysicsEntity>();
		for(Entry<PhysicsEntity, String[]> entry : alienMap.entrySet()){
			int randomInt = rand.nextInt((10 - 0) + 1) + 0;
			if(randomInt == 10){
				PhysicsEntity currentAlien = entry.getKey();
				removeEntity(currentAlien);
				ExploadableImageView expl = new ExploadableImageView(explosionImage, NUM_CELLS_PER_EXPLOSION, currentAlien.getPosition().getX(), currentAlien.getPosition().getY());
				root.getChildren().add(expl);
				expl.explode();
				toRemove.add(currentAlien);
			}
		}
		for(PhysicsEntity al : toRemove){
			alienMap.remove(al);
		}
	}
	
	private boolean blueNum = true;
	
	public void changeImages(){
		
		for(Entry<PhysicsEntity, String[]> entry : alienMap.entrySet()){
			if(blueNum){
				entry.getKey().setGraphics(assets.getTexture(entry.getValue()[1]));				
				soundFactory.playFastInv1Sound();
			}else{
				entry.getKey().setGraphics(assets.getTexture(entry.getValue()[0]));
				soundFactory.playFastInv2Sound();
			}
		}
		
		blueNum = !blueNum;
	}
	
	private void initPlayer(){
		player = new PhysicsEntity(Type.PLAYER_SHIP);
		player.setPosition(getWidth()/2 - (104/2), getHeight()-65);
		player.setGraphics(assets.getTexture("ship_52x32.png"));
		player.setCollidable(true);
		player.setBodyType(BodyType.DYNAMIC);
		
		addEntities(player);
	}
	
	private void initAliens(){
		
		alienMap = new HashMap<PhysicsEntity, String[]>();
		
		String[] valuesAlienBlue = new String[]{"alien_blau_1_60x44.png","alien_blau_2_60x44.png","alien_shot_20x40.png"};
		String[] valuesAlienYellow = new String[]{"alien_gelb_1_60x44.png","alien_gelb_2_60x44.png","alien_shot2_20x40.png"};
		String[] valuesAlienRed = new String[]{"alien_rot_1_42x42.png","alien_rot_2_42x42.png","alien_shot3_20x40.png"};
		
		for (int i = 0; i < 8; i++) {
			
			PhysicsEntity alien = new PhysicsEntity(Type.ALIEN);
			alien.setPosition((104/2)+(i*70), 120);
			alien.setGraphics(assets.getTexture(valuesAlienBlue[0]));
			alien.setCollidable(true);
			alien.setBodyType(BodyType.KINEMATIC);
			alienMap.put(alien, new String[]{valuesAlienBlue[0],valuesAlienBlue[1],valuesAlienBlue[2]});
			
			addEntities(alien);
			
			alien.setLinearVelocity(alienSpeed, 0);
			
			PhysicsEntity alien2 = new PhysicsEntity(Type.ALIEN);
			alien2.setPosition((104/2)+(i*70), 170);
			alien2.setGraphics(assets.getTexture(valuesAlienYellow[0]));
			alien2.setCollidable(true);
			alien2.setBodyType(BodyType.KINEMATIC);
			alienMap.put(alien2, new String[]{valuesAlienYellow[0],valuesAlienYellow[1],valuesAlienYellow[2]});
			
			addEntities(alien2);
			
			alien2.setLinearVelocity(alienSpeed, 0);
			
			PhysicsEntity alien4 = new PhysicsEntity(Type.ALIEN);
			alien4.setPosition((104/2)+(i*70), 220);
			alien4.setGraphics(assets.getTexture(valuesAlienYellow[0]));
			alien4.setCollidable(true);
			alien4.setBodyType(BodyType.KINEMATIC);
			alienMap.put(alien4, new String[]{valuesAlienYellow[0],valuesAlienYellow[1],valuesAlienYellow[2]});
			
			addEntities(alien4);
			
			alien4.setLinearVelocity(alienSpeed, 0);
			
			PhysicsEntity alien3 = new PhysicsEntity(Type.ALIEN);
			alien3.setPosition((124/2)+(i*70), 270);
			alien3.setGraphics(assets.getTexture(valuesAlienRed[0]));
			alien3.setCollidable(true);
			alien3.setBodyType(BodyType.KINEMATIC);
			alienMap.put(alien3, new String[]{valuesAlienRed[0],valuesAlienRed[1],valuesAlienRed[2]});
			
			addEntities(alien3);
			
			alien3.setLinearVelocity(alienSpeed, 0);
		}
		
	}
	
	private boolean down = true;
	
	public void moveAliensDown(boolean left){
		
		Timeline timeline = new Timeline(new KeyFrame(
		        Duration.millis(10),
		        ae -> changeVeloDown()));
		timeline.setCycleCount(2);
		
		if(down && left){
			timeline.play();	
			down = false;
		}
		
		for(Entry<PhysicsEntity, String[]> entry : alienMap.entrySet()){
			if(left){
				entry.getKey().setLinearVelocity(Math.abs(entry.getKey().getLinearVelocity().getX())*-1,0);				
			}else{
				entry.getKey().setLinearVelocity(Math.abs(entry.getKey().getLinearVelocity().getX()),0);
			}
		}
	}
	
	private boolean movedDown = false;
	
	public void changeVeloDown(){
		
		for(Entry<PhysicsEntity, String[]> entry : alienMap.entrySet()){
			if(!movedDown){
				entry.getKey().setLinearVelocity(entry.getKey().getLinearVelocity().getX(), 10);	
			}else{
				entry.getKey().setLinearVelocity(entry.getKey().getLinearVelocity().getX(), 0);
			}
		}
		
		movedDown = !movedDown;
		
	}
	
	public void shoot(){
		
		shot = new PhysicsEntity(Type.PLAYER_SHOT);
		shot.setPosition(player.getPosition().getX()+(52/2)-(2), player.getPosition().getY()-5);
		shot.setGraphics(assets.getTexture("player_shot_4x14.png"));
		shot.setBodyType(BodyType.DYNAMIC);
		shot.setCollidable(true);
		
		addEntities(shot);
		shot.setLinearVelocity(0, -12);
		
//		shot = new PhysicsEntity(Type.PLAYER_SHOT);
//		shot.setPosition(player.getPosition().getX()+(52/2)-(15), player.getPosition().getY()-120);
//		shot.setGraphics(assets.getTexture("missiles_124x30.gif"));
//		shot.setBodyType(BodyType.DYNAMIC);
//		shot.setCollidable(true);
//		
//		addEntities(shot);
//		shot.setLinearVelocity(0, -7);
	}
	
	public void alienShoot(){
		Random rand = new Random();
		
		for(Entry<PhysicsEntity, String[]> alien : alienMap.entrySet()){
			int randomNum = rand.nextInt((20 - 0) + 1) + 0;
			
			if(randomNum == 1){
				alienShot = new PhysicsEntity(Type.ALIEN_SHOT1);
				alienShot.setGraphics(assets.getTexture(alien.getValue()[2]));
				alienShot.setBodyType(BodyType.KINEMATIC);
				alienShot.setCollidable(true);
				Point2D pos = alien.getKey().getPosition();
				alienShot.setPosition(pos.getX()+30-10, pos.getY());
				
				addEntities(alienShot);
				alienShot.setLinearVelocity(0,3);
				
			}
		}
	}
	
	/**
	 * Create a bonus when a alien is hit with a chance of 1/40.
	 * @param posX
	 * @param posY
	 */
	public void createPresent(double posX, double posY){
		
		Random rand = new Random();
		
		int whichNum = rand.nextInt((5 - 0) + 1) + 0;
		
		/* 1/40 chance */
		int randomNum = rand.nextInt((40 - 0) + 1) + 0;
		
//		whichNum = 0;
//		randomNum = 14;
		
		if(randomNum == 14){
			switch (whichNum) {
			case 1:
				PhysicsEntity bonusLife = new PhysicsEntity(Type.BONUS_LIFE);
				bonusLife.setGraphics(assets.getTexture("heart_20x18.png"));
				bonusLife.setCollidable(true);
				bonusLife.setBodyType(BodyType.KINEMATIC);
				bonusLife.setPosition(posX+30-10, posY);	
				addEntities(bonusLife);
				bonusLife.setLinearVelocity(0,1);
				break;
				
			case 2:
				PhysicsEntity bonusDeath = new PhysicsEntity(Type.BONUS_DEATH);
				bonusDeath.setGraphics(assets.getTexture("skull_20x23.png"));
				bonusDeath.setCollidable(true);
				bonusDeath.setBodyType(BodyType.KINEMATIC);
				bonusDeath.setPosition(posX+30-10, posY);	
				addEntities(bonusDeath);
				bonusDeath.setLinearVelocity(0,1);
				break;
				
			case 3:
				PhysicsEntity bonusTNT = new PhysicsEntity(Type.BONUS_TNT);
				bonusTNT.setGraphics(assets.getTexture("tnt_20x20.png"));
				bonusTNT.setCollidable(true);
				bonusTNT.setBodyType(BodyType.KINEMATIC);
				bonusTNT.setPosition(posX+30-10, posY);	
				addEntities(bonusTNT);
				bonusTNT.setLinearVelocity(0,1);
				break;
				
			case 4:
				PhysicsEntity bonusSlow = new PhysicsEntity(Type.BONUS_SLOW);
				bonusSlow.setGraphics(assets.getTexture("snail_20x19.png"));
				bonusSlow.setCollidable(true);
				bonusSlow.setBodyType(BodyType.KINEMATIC);
				bonusSlow.setPosition(posX+30-10, posY);	
				addEntities(bonusSlow);
				bonusSlow.setLinearVelocity(0,1);
				break;
				
			case 5:
				PhysicsEntity bonusShield = new PhysicsEntity(Type.BONUS_SHIELD);
				bonusShield.setGraphics(assets.getTexture("shield_20x24.png"));
				bonusShield.setCollidable(true);
				bonusShield.setBodyType(BodyType.KINEMATIC);
				bonusShield.setPosition(posX+30-10, posY);	
				addEntities(bonusShield);
				bonusShield.setLinearVelocity(0,1);
				break;

			default:
				PhysicsEntity present = new PhysicsEntity(Type.BONUS1);
				present.setGraphics(assets.getTexture("bullets_20x18.png"));
				present.setCollidable(true);
				present.setBodyType(BodyType.KINEMATIC);
				present.setPosition(posX+30-10, posY);	
				addEntities(present);
				present.setLinearVelocity(0,1);
				break;
			}
		}
	}
	
	private void initScreenBounds(){
		PhysicsEntity top = new PhysicsEntity(Type.SCREEN);
		top.setPosition(0, -10);
		top.setGraphics(new Rectangle(getWidth(), 10));
		top.setCollidable(true);
		
		PhysicsEntity bottom = new PhysicsEntity(Type.SCREEN);
		bottom.setPosition(0, getHeight());
		bottom.setGraphics(new Rectangle(getWidth(), 10));
		bottom.setCollidable(true);
		
		PhysicsEntity left = new PhysicsEntity(Type.SCREEN_LEFT);
		left.setPosition(-10, 0);
		left.setGraphics(new Rectangle(10, getHeight()));
		left.setCollidable(true);
		
		PhysicsEntity right = new PhysicsEntity(Type.SCREEN_RIGHT);
		right.setPosition(getWidth(), 0);
		right.setGraphics(new Rectangle(10, getHeight()));
		right.setCollidable(true);
		
		addEntities(top, bottom, left, right);
	}
	
	private Pane root;

	@Override
	protected void initUI(Pane uiRoot) {
		
		root = uiRoot;
		
//		uiRoot.setStyle("-fx-background-color: #242424;");
		
		pauseButton = new Button();
		pauseButton.setMinHeight(100);
		pauseButton.setMinWidth(300);
		pauseButton.setText("PAUSE");
		pauseButton.setFont(Font.font(null, FontWeight.BOLD, 25));
		pauseButton.setStyle("-fx-font-family: 'Press Start 2P';");
		pauseButton.setTranslateX(getWidth()/2-50);
		pauseButton.setTranslateY(getHeight()/2-150);
		
		pauseButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				pauseButton.setVisible(false);
				System.out.println("resume Button");
				root.toBack();
				paused = false;
				resume();
				
			}
		});
		
		root.getChildren().add(pauseButton);
		
		pauseButton.setVisible(false);
		
		String image = SpaceInvaders.class.getResource("/resources/space-bg.jpg").toExternalForm();
		uiRoot.setStyle("-fx-background-image: url('" + image + "'); " +
		           "-fx-background-position: center center; " +
		           "-fx-background-repeat: stretch;");
		
		uiRoot.setPrefSize(getWidth(),getHeight());
		uiRoot.toBack();
		
		uiRoot.getStylesheets().add("https://fonts.googleapis.com/css?family=Press+Start+2P");

		Text scoreText = new Text("SCORE:");
		scoreText.setX(10);
		scoreText.setY(35);
		scoreText.setCache(true);
		scoreText.setFill(Color.WHITE);
		scoreText.setFont(Font.font(null, FontWeight.BOLD, 25));
		scoreText.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(scoreText);
		
		Text scoreTextNum = new Text();
		scoreTextNum.textProperty().bind(score.asString());
		scoreTextNum.setX(160);
		scoreTextNum.setY(35);
		scoreTextNum.setCache(true);
		scoreTextNum.setFill(Color.WHITE);
		scoreTextNum.setFont(Font.font(null, FontWeight.BOLD, 25));
		scoreTextNum.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(scoreTextNum);
		
		Text levelText = new Text("LEVEL:");
		levelText.setX(10);
		levelText.setY(65);
		levelText.setCache(true);
		levelText.setFill(Color.WHITE);
		levelText.setFont(Font.font(null, FontWeight.BOLD, 25));
		levelText.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(levelText);
		
		Text levelTextNum = new Text();
		levelTextNum.textProperty().bind(level.asString());
		levelTextNum.setX(160);
		levelTextNum.setY(65);
		levelTextNum.setCache(true);
		levelTextNum.setFill(Color.WHITE);
		levelTextNum.setFont(Font.font(null, FontWeight.BOLD, 25));
		levelTextNum.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(levelTextNum);
		
		Text speedText = new Text("SPEED:");
		speedText.setX(500);
		speedText.setY(35);
		speedText.setCache(true);
		speedText.setFill(Color.WHITE);
		speedText.setFont(Font.font(null, FontWeight.BOLD, 25));
		speedText.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(speedText);
		
		Text speedTextNum = new Text();
		speedTextNum.textProperty().bind(speed.asString());
		speedTextNum.setX(650);
		speedTextNum.setY(35);
		speedTextNum.setCache(true);
		speedTextNum.setFill(Color.WHITE);
		speedTextNum.setFont(Font.font(null, FontWeight.BOLD, 25));
		speedTextNum.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(speedTextNum);
		
		Text livesText = new Text("LIVES:");
		livesText.setX(500);
		livesText.setY(65);
		livesText.setCache(true);
		livesText.setFill(Color.WHITE);
		livesText.setFont(Font.font(null, FontWeight.BOLD, 25));
		livesText.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(livesText);
		
		Text livesTextNum = new Text();
		livesTextNum.textProperty().bind(lives.asString());
		livesTextNum.setX(650);
		livesTextNum.setY(65);
		livesTextNum.setCache(true);
		livesTextNum.setFill(Color.WHITE);
		livesTextNum.setFont(Font.font(null, FontWeight.BOLD, 25));
		livesTextNum.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(livesTextNum);
		
		Text ammoText = new Text("AMMO:");
		ammoText.setX(10);
		ammoText.setY(95);
		ammoText.setCache(true);
		ammoText.setFill(Color.WHITE);
		ammoText.setFont(Font.font(null, FontWeight.BOLD, 25));
		ammoText.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(ammoText);
		
		Text ammoTextNum = new Text();
		ammoTextNum.textProperty().bind(amountBullets.asString());
		ammoTextNum.setX(160);
		ammoTextNum.setY(95);
		ammoTextNum.setCache(true);
		ammoTextNum.setFill(Color.WHITE);
		ammoTextNum.setFont(Font.font(null, FontWeight.BOLD, 25));
		ammoTextNum.setStyle("-fx-font-family: 'Press Start 2P';");
		
		uiRoot.getChildren().add(ammoTextNum);
	}
	
	public void displayGameOver(){
		
		String content = "GAME OVER";
		Text gameOverText = new Text();
		gameOverText.setFill(Color.FIREBRICK);
		gameOverText.setFont(Font.font(80));
		gameOverText.setStyle("-fx-font-family: 'Press Start 2P';"+
		"-fx-stroke: black;"+
		"-fx-stroke-width: 3;");
		gameOverText.setX(50);
		gameOverText.setY(500);
		root.getChildren().add(gameOverText);
		
		final Animation animation = new Transition() {
	        {
	            setCycleDuration(Duration.millis(2000));
	        }
	    
	        protected void interpolate(double frac) {
	            final int length = content.length();
	            final int n = Math.round(length * (float) frac);
	            gameOverText.setY(gameOverText.getY()+1);
	            gameOverText.setText(content.substring(0, n));
	        }
	    
	    };
	    
	    animation.play();
		
	}
	
	public void displayPoints(String points, double posX, double posY){
		
		
		Text pointText = new Text();
		pointText.setText(points);
		pointText.setFill(Color.DARKORANGE);
		pointText.setFont(Font.font(15));
		pointText.setStyle("-fx-font-family: 'Press Start 2P';");
		pointText.setX(posX);
		pointText.setY(posY);
		
		pointText.toFront();
		
		root.getChildren().add(pointText);
		
		final Animation animation = new Transition() {
	        {
	            setCycleDuration(Duration.millis(1000));
	        }
	        
	        int counter = 0;
	    
	        protected void interpolate(double frac) {
	        	pointText.setY(pointText.getY()-1);
	            counter++;
	            
	            if(counter >= 40){
	            	root.getChildren().remove(pointText);
	            }
	        }
	    
	    };
	    
	    animation.play();
		
	}
	
	public void displayTimer(int time){
		
		Integer STARTTIME = time;
	    Timeline timeline;
	    IntegerProperty timeSeconds = new SimpleIntegerProperty(STARTTIME);
	    
        
        timeSeconds.set(STARTTIME);
        
        timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(STARTTIME+1),
                new KeyValue(timeSeconds, 0)));
        timeline.playFromStart();
        
        Text timerText = new Text();
        timerText.setText("SLOW:");
        timerText.setX(500);
        timerText.setY(95);
        timerText.setCache(true);
        timerText.setFill(Color.WHITE);
        timerText.setFont(Font.font(null, FontWeight.BOLD, 25));
        timerText.setStyle("-fx-font-family: 'Press Start 2P';");
        
        root.getChildren().add(timerText);
        
        Text timerNum = new Text();
        timerNum.textProperty().bind(timeSeconds.asString());
        timerNum.setX(650);
        timerNum.setY(95);
        timerNum.setCache(true);
        timerNum.setFill(Color.WHITE);
        timerNum.setFont(Font.font(null, FontWeight.BOLD, 25));
        timerNum.setStyle("-fx-font-family: 'Press Start 2P';");
        
        root.getChildren().add(timerNum);

        timeline.setOnFinished(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
        		root.getChildren().remove(timerNum);
        		root.getChildren().remove(timerText);
        		playerSpeed = 7;
        	}
        });
	}

	@Override
	protected void initInput() {
		
		inputManager.addKeyPressBinding(KeyCode.LEFT, () -> {
			
			player.setLinearVelocity(playerSpeed*-1, 0);
		});
		
		inputManager.addKeyPressBinding(KeyCode.RIGHT, () -> {
			
			player.setLinearVelocity(playerSpeed, 0);
		});
		
		inputManager.addKeyPressBinding(KeyCode.UP, () -> {
			
			if(rapidFire){
				
				shoot();
				soundFactory.playShootSound();
				amountBullets.set(amountBullets.get()-1);
				
				if(amountBullets.get() <= 0){
					rapidFire = false;
				}
				return;
			}
			
			if(!isShot){
				
				shoot();
				soundFactory.playShootSound();
				isShot = true;
				amountBullets.set(amountBullets.get()-1);
			}
		});
		
		
		inputManager.addKeyPressBinding(KeyCode.SPACE, () -> {
			System.out.println("-> SPACE PRESSED");
//			
//			if(!spacePressed){
//				
//				if(!paused){		
//					paused = true;
//					pauseButton.setVisible(true);
//					pause();
//					root.toFront();
//					System.out.println("pause");
//				}
//			}
//			
//			spacePressed = true;
		});
		
	}
	
	@Override
	protected void onUpdate() {
		
		player.setLinearVelocity(0,0);
		
		if(shield != null){
			shield.setLinearVelocity(0, 0);				
		}
		
		
	}
	
	/**
	 * This class handles sound effects
	 */
	public class SoundFactory {
		
		MediaPlayer shootPlayer, deathPlayer, ufoPlayer, killInvPlayer, fastInvPlayer1, fastInvPlayer2;
		
		public void initMedia(){
			
			URL shootLink = SpaceInvaders.class.getResource("/sounds/shoot.wav");
			Media sound = new Media(new File(shootLink.toString()).toString());
			shootPlayer = new MediaPlayer(sound);
			
			URL killInvLink = SpaceInvaders.class.getResource("/sounds/invaderkilled.wav");
			Media killInvSound = new Media(new File(killInvLink.toString()).toString());
			killInvPlayer = new MediaPlayer(killInvSound);
			
			URL fastInv1Link = SpaceInvaders.class.getResource("/sounds/fastinvader1.wav");
			Media fastInv1Sound = new Media(new File(fastInv1Link.toString()).toString());
			fastInvPlayer1 = new MediaPlayer(fastInv1Sound);
			
			URL fastInv2Link = SpaceInvaders.class.getResource("/sounds/fastinvader2.wav");
			Media fastInv2Sound = new Media(new File(fastInv2Link.toString()).toString());
			fastInvPlayer2 = new MediaPlayer(fastInv2Sound);
			
			URL deathPlayerLink = SpaceInvaders.class.getResource("/sounds/explosion.wav");
			Media deathPlayerSound = new Media(new File(deathPlayerLink.toString()).toString());
			deathPlayer = new MediaPlayer(deathPlayerSound);
		}
		
		public void playShootSound(){
			shootPlayer.stop();
			shootPlayer.play();
		}
		
		public void playKillInvSound(){
			killInvPlayer.stop();
			killInvPlayer.play();
		}
		
		public void playFastInv1Sound(){
			fastInvPlayer1.stop();
			fastInvPlayer1.play();
		}
		
		public void playFastInv2Sound(){
			fastInvPlayer2.stop();
			fastInvPlayer2.play();
		}
		
		public void playExplosionSound(){
			deathPlayer.stop();
			deathPlayer.play();
		}
		
	}
	
	
	public static void main(String[] args) {
		launch(args);
	}
	
	/*
	 * Explosion .png
	 */
	private static final int NUM_CELLS_PER_EXPLOSION = 48;
	
	String explosionPath = SpaceInvaders.class.getResource("/resources/explosion.png").toExternalForm();

	Image explosionImage = new Image(explosionPath);
	
	class ExploadableImageView extends ImageView {
		
	    private final Rectangle2D[] cellClips;
	    private int numCells;
	    private final Duration FRAME_TIME = Duration.seconds(.01);

	    public ExploadableImageView(Image explosionImage, int numCells, double posX, double posY) {

	    	setFitHeight(100);
	    	setFitWidth(100);
	    	setPreserveRatio(true);
	    	
	        this.numCells = numCells;

	        double cellWidth  = explosionImage.getWidth() / numCells;
	        double cellHeight = explosionImage.getHeight();

	        cellClips = new Rectangle2D[numCells];
	        for (int i = 0; i < numCells; i++) {
	            cellClips[i] = new Rectangle2D( i * cellWidth, 0, cellWidth, cellHeight);
	        }
	        
	        setX(posX-30);
	        setY(posY-20);
	        

	        setImage(explosionImage);
	        setViewport(cellClips[0]);
	    }

	    public void explode() {
	    	
	        final IntegerProperty frameCounter = new SimpleIntegerProperty(0);
	        
	        Timeline kaboom = new Timeline(
	                new KeyFrame(FRAME_TIME, event -> {
	                    frameCounter.set((frameCounter.get() + 1) % numCells);
	                    setViewport(cellClips[frameCounter.get()]);
	                    
	                    if(frameCounter.get() >= 47){
	                    	setVisible(false);
	                    }
	                })
	        );
	        kaboom.setCycleCount(numCells);
	        kaboom.play();
	    }
	}

}
