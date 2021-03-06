package com.jam.game;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.graphics.GL20;
import com.jam.game.screens.ScreenHandler;
import com.jam.game.utils.enums.PlatformType;
import com.jam.game.utils.enums.ScreenType;

public class Game implements ApplicationListener {
	
	private static ScreenHandler screenHandler;
	
	public void moveToNextScreen(ScreenType screen){
		screenHandler.moveToScreenAndDispose(screen);
		screenHandler.showCurrentScreen();
	}
	
	@Override
	public void create () {
		screenHandler = new ScreenHandler(this);
	}
	
	@Override
	public void render () {
		if(Gdx.input.isKeyJustPressed(Keys.ESCAPE))
//			screenHandler.pauseCurrentScreen();
			Gdx.app.exit();
		
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		GdxAI.getTimepiece().update(Gdx.graphics.getDeltaTime());
		screenHandler.getCurrentScreen().render(GdxAI.getTimepiece().getDeltaTime());
		
		screenHandler.tryShowPauseScreen(GdxAI.getTimepiece().getDeltaTime());
	}
	
	@Override
	public void dispose () {
		screenHandler.dispose();
	}

	@Override
	public void resize(int width, int height) {
		screenHandler.getCurrentScreen().resize(width, height);
	}

	@Override
	public void pause() {
		screenHandler.getCurrentScreen().pause();
	}

	@Override
	public void resume() {
		screenHandler.getCurrentScreen().resume();
	}
	
	public static ScreenHandler getScreenHandler(){
		return screenHandler;
	}
}
