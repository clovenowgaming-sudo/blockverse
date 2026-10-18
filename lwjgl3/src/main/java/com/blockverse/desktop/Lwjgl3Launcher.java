package com.blockverse.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.blockverse.game.BlockVerseGame;

public final class Lwjgl3Launcher {
    private Lwjgl3Launcher() { }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("BlockVerse - Early Sandbox Prototype");
        config.setWindowedMode(1280, 720);
        config.setForegroundFPS(120);
        config.setIdleFPS(30);
        config.useVsync(true);
        config.setResizable(true);
        new Lwjgl3Application(new BlockVerseGame(), config);
    }
}
