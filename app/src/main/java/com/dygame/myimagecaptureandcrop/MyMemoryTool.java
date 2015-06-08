package com.dygame.myimagecaptureandcrop;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
/**
 * get current memory usage in android
 */
public class MyMemoryTool
{
	//The available memory on the system
	@SuppressLint("NewApi")
	static public String getAvailableMemory(Context context)
	{
		  ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
          ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
          activityManager.getMemoryInfo(mi);
          long availableMegs = mi.availMem / 1048576L;//1MB
          //Percentage can be calculated for API 16+
          long percentAvail = 0 ;
          if (Build.VERSION.SDK_INT >= 16)
          {
        	  percentAvail = 100 * mi.availMem / mi.totalMem;
          }
          //
          String sz = "availableMemMB=" + availableMegs + ",percentAvailableMem=" + percentAvail + "%" ;
          return sz ;
	}
	//The threshold(ªùÂe) of availMem at which we consider memory to be low and start killing background services and other non-extraneous processes.
	static public String getLowMemoryThreshold(Context context)
	{
		  ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
          ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
          activityManager.getMemoryInfo(mi);
          long lowMemoryMegs = mi.threshold / 1048576L;//1MB
          long availableMegs = mi.availMem / 1048576L;//1MB
          //threshold = if the system considers itself to currently be in a low memory situation.
          String sz = "availableMemMB=" + availableMegs + "/lowMemoryThresholdMB=" + lowMemoryMegs ;
          return sz ;
	}
}
