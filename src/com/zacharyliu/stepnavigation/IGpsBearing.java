package com.zacharyliu.stepnavigation;

public interface IGpsBearing extends ICustomSensor {
	
	public void resume();
	public void pause();
	public void on();
	public void off();
	
}
