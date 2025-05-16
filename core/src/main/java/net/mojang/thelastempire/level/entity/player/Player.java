package net.mojang.thelastempire.level.entity.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import net.mojang.thelastempire.TheLastEmpire;
import net.mojang.thelastempire.engine.Color;
import net.mojang.thelastempire.engine.Graphics;
import net.mojang.thelastempire.engine.Light;
import net.mojang.thelastempire.engine.Resources;
import net.mojang.thelastempire.gui.GuiBoss;
import net.mojang.thelastempire.gui.GuiDialogue;
import net.mojang.thelastempire.gui.GuiGameOver;
import net.mojang.thelastempire.gui.GuiScreen;
import net.mojang.thelastempire.level.Level;
import net.mojang.thelastempire.level.entity.Mob;
import net.mojang.thelastempire.level.entity.boss.DomPedroII;
import net.mojang.thelastempire.level.entity.npc.NPC;
import net.mojang.thelastempire.level.item.Item;
import net.mojang.thelastempire.math.Vec2;

public class Player extends Mob {

	private static Texture player = Resources.getTexture("char");
	
	private Inventory inventory = new Inventory(4);
	private PlayerController controller = new PlayerController(this);

	private boolean justSpawned = true;
	private boolean isChatting = false;

	private static final int UP_ANIMATION = 0;
	private static final int DOWN_ANIMATION = 1;
	private static final int LEFT_ANIMATION = 2;
	private static final int RIGHT_ANIMATION = 3;

	protected Animation<TextureRegion> currentAnimation;
	protected Animation<TextureRegion>[] allAnimations;

	private float animationTimer = 0f;
	private boolean canMove = false;

	private Array<String> dialogues = new Array<String>();
	private String dialogueFinishLevel;
	
	private Light light = null;
	protected boolean isDashing = false;

	public Player(Level level, float xs, float ys, boolean canMove, String direction) {
		super(level, 20);

		setPos(xs, ys);
		xo = x;
		yo = y;

		speed = 0.08f;

		this.canMove = canMove;
		this.direction = direction;

		if (level.getGlobalLight() < 0.4f) {
			light = new Light(x, y, 0.5f, 0.1f, 4f, new Color(1f, 0.9f, 0f, 1f));
			Graphics.instance.setLight(light);
		}
		
		createAnimation(player);
	}

	@SuppressWarnings("unchecked")
	private void createAnimation(Texture texture) {
		TextureRegion[] upFrames = new TextureRegion[] { new TextureRegion(texture, 32, 32, 16, 32),
				new TextureRegion(texture, 48, 32, 16, 32), };
		TextureRegion[] downFrames = new TextureRegion[] { new TextureRegion(texture, 32, 0, 16, 32),
				new TextureRegion(texture, 48, 0, 16, 32), };
		TextureRegion[] leftFrames = new TextureRegion[] { new TextureRegion(texture, 32, 64, 16, 32),
				new TextureRegion(texture, 48, 64, 16, 32), };
		TextureRegion[] rightFrames = new TextureRegion[] { new TextureRegion(texture, 0, 64, 16, 32),
				new TextureRegion(texture, 16, 64, 16, 32), };

		allAnimations = (Animation<TextureRegion>[]) new Animation<?>[4];
		allAnimations[UP_ANIMATION] = new Animation<TextureRegion>(0.3f, upFrames);
		allAnimations[DOWN_ANIMATION] = new Animation<TextureRegion>(0.3f, downFrames);
		allAnimations[LEFT_ANIMATION] = new Animation<TextureRegion>(0.3f, leftFrames);
		allAnimations[RIGHT_ANIMATION] = new Animation<TextureRegion>(0.3f, rightFrames);

		currentAnimation = allAnimations[direction.equals("up") ? UP_ANIMATION
				: direction.equals("down") ? DOWN_ANIMATION
						: direction.equals("left") ? LEFT_ANIMATION : RIGHT_ANIMATION];
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		
		GuiScreen guiScreen = TheLastEmpire.getTheLastEmpire().getSceneManager().getScreen();
		boolean shouldBreak = false;
		if (guiScreen != null) {
			shouldBreak = true;
			if (guiScreen instanceof GuiBoss) {
				shouldBreak = false;
			}
		}
		if (shouldBreak) return;
		
		baseTick(xd, yd);
		xd *= 0.71f;
		yd *= 0.71f;

		switch (direction) {
		case "up":
			currentAnimation = allAnimations[UP_ANIMATION];
			break;
		case "down":
			currentAnimation = allAnimations[DOWN_ANIMATION];
			break;
		case "left":
			currentAnimation = allAnimations[LEFT_ANIMATION];
			break;
		case "right":
			currentAnimation = allAnimations[RIGHT_ANIMATION];
			break;
		}
	
		controller.tick();
	}

	@Override
	public void draw(Graphics g) {
		if (!isChatting && canMove && !isDashing) {
			up = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
			down = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
			left = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
			right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
		}

		if (up || down || left || right) {
			animationTimer += Gdx.graphics.getDeltaTime();
		}
		TextureRegion texture = currentAnimation.getKeyFrame(animationTimer, true);

		Vec2 position = getDrawPosition();
		if (light != null) {
			light.setPos(position.x + 0.5f, position.y + 1f);
		}
		
		g.drawTexture(texture, position.x, position.y, 1f, 2f);
		moveCameraToPlayer(g, position.x, position.y);
		
		controller.draw(g);
	}

	private void moveCameraToPlayer(Graphics g, float x, float y) {
		if (justSpawned) {
			g.setOffset(x + 0.5f, y + 1f, 1f);
			justSpawned = false;
			return;
		}

		g.setOffset(x + 0.5f, y + 1f, 2f * Gdx.graphics.getDeltaTime());

		if (level.getWidth() > g.getScreenWidth() && level.getHeight() > g.getScreenHeight()) {
			float halfWidth = g.getScreenWidth() / 2f;
			float halfHeight = g.getScreenHeight() / 3f;
			float x0 = halfWidth + 0.5f;
			float y0 = halfHeight + 2.5f;
			float x1 = level.getWidth() - halfWidth - 0.5f;
			float y1 = level.getHeight() - halfHeight * 2f + 1;
			g.limitOffset(x0, y0, x1, y1);
		}
	}

	@Override
	public String getName() {
		return "Player";
	}

	public boolean shouldRender() {
		return true;
	}

	public void interact(NPC npc) {
		isChatting = true;
		up = false;
		down = false;
		left = false;
		right = false;
		npc.startChatting();
	}

	public void stopChatting() {
		isChatting = false;
	}

	public void setDialogues(Array<String> dialogues, String dialogueFinishLevel) {
		this.dialogues = dialogues;
		this.dialogueFinishLevel = dialogueFinishLevel;
		
		TheLastEmpire.getTheLastEmpire().setGuiScreen(new GuiDialogue(this, this::onDialogueFinish, level), false);
	}
	
	private void onDialogueFinish() {
		level.loadLevel(dialogueFinishLevel, true, false);
	}

	public Array<String> getDialogues() {
		return dialogues;
	}

	public Texture getTexture() {
		return player;
	}
	
	public Inventory getInventory() {
		return inventory;
	}

	public void setItems(Item[] items) {
		inventory.addAll(items);
	}
	
	public Level getLevel() {
		return level;
	}
	
	public PlayerController getController() {
		return controller;
	}
	
	public void stopMoving() {
		up = false;
		down = false;
		left = false;
		right = false;
	}
	
	@Override
	public void remove() {
		super.remove();
		TheLastEmpire tle = TheLastEmpire.getTheLastEmpire();
		tle.playSound("hurt_player1", 1f, 1f);
		tle.setGuiScreen(new GuiGameOver(), true);
		DomPedroII.increaseTries();
	}
	
}
