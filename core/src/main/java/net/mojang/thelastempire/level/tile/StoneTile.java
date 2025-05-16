package net.mojang.thelastempire.level.tile;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import net.mojang.thelastempire.engine.Graphics;
import net.mojang.thelastempire.level.Level;
import net.mojang.thelastempire.level.Material;

public class StoneTile extends Tile {

	protected StoneTile(int id, Material material) {
		super(id, material);
	}
	
	@Override
    public TextureRegion getDynamicTexture(Graphics g, int x, int y, Level level) {
        TextureRegion texture = getTexture();
        texture = getConnectedTextures(level, x, y, 0, 12, Tile.grass.id, texture, g);
        return texture;
    }

}
