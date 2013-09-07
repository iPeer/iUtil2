package com.simple.ipeer.iutil2.engine;

public class AnnouncerHelper implements Runnable {

	private AnnouncerHandler announcerHandler;
	private String helperName = "Helper";
	private Main engine;
	private long startDelay = 0L;

	private boolean isRunning = false;
	private Thread thread;

	public AnnouncerHelper(AnnouncerHandler ah, String helperName, Main engine) {
		this(ah, helperName, engine, 0L);
	}

	public AnnouncerHelper(AnnouncerHandler ah, String helperName, Main engine, long startDelay) {
		this.announcerHandler = ah;
		this.helperName = helperName;
		this.engine = engine;
		this.startDelay = startDelay;
		engine.log("AnnouncerHelper \""+helperName+"\" has started.");
	}

	@Override
	public void run() {
		try {
			if (this.startDelay > 0L)
				Thread.sleep(startDelay);

			while (isRunning && !thread.isInterrupted()) {
				if (this.announcerHandler.timeTilUpdate() < 0 || this.announcerHandler.timeTilUpdate() > Long.valueOf(this.announcerHandler.getUpdateDelay())) {
					engine.log("[AnnouncerHelper] Found a thread that was not updating, restarting it.", helperName);
					this.announcerHandler.stopAll();
					this.announcerHandler.startAll();
				}
				Thread.sleep(300000);
			}
		} catch (InterruptedException e) {
			engine.log("[AnnouncerHelper] Helper could not sleep!", helperName);
			e.printStackTrace();
		}
	}

	public void start() {
		if (!this.isRunning) {
			this.isRunning = true;
			(thread = new Thread(this, "AnnouncerHelper for "+helperName)).start();
		}
	}

	public void stop() {
		this.isRunning = false;
		thread.interrupt();
	}

}

