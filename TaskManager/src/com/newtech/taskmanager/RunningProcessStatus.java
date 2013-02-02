/*
 * CopyRight (C) 2013 NewTech CORP LTD.
 * RunningProcessStatus.java
 */

package com.newtech.taskmanager;

import java.util.ArrayList;
import java.util.List;

import com.newtech.taskmanager.util.TMLog;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Debug.MemoryInfo;
import android.util.SparseArray;

public class RunningProcessStatus {
	private final static String TAG = "RunningProcessStatus";

	private Context mContext;
	private ActivityManager mAm;
	private PackageManager mPm;

	RunningProcessStatus(Context context) {
		mContext = context;
		mAm = (ActivityManager) mContext
				.getSystemService(Activity.ACTIVITY_SERVICE);
		mPm = (PackageManager) mContext.getPackageManager();
	}

	/**
	 * Get the current proccess's information. Notice: this method will spend
	 * long time. Use it in the work thread.
	 *
	 * @return ArrayList<ProcessInfo> the list of all running application's
	 *         information
	 */
	public synchronized ArrayList<ProcessInfo> getRunningApplication() {
		TMLog.begin(TAG);
		ArrayList<ProcessInfo> runningProcess = new ArrayList<ProcessInfo>();
		final SparseArray<ProcessInfo> tmpAppProcesses = new SparseArray<ProcessInfo>();
		List<RunningAppProcessInfo> runnings = mAm.getRunningAppProcesses();

		// initialize the running application process
		for (RunningAppProcessInfo info : runnings) {
			try {
				ApplicationInfo appInfo = mPm.getApplicationInfo(
						info.processName, PackageManager.GET_ACTIVITIES);
				ProcessInfo processInfo = new ProcessInfo(info.processName);
				processInfo.setAppInfo(appInfo);
				processInfo.setRunningInfo(info);

				tmpAppProcesses.put(info.pid, processInfo);
				runningProcess.add(processInfo);
			} catch (NameNotFoundException e) {
				// Just ignore it
			}
		}

		// find the running service for process
		List<RunningServiceInfo> services = mAm
				.getRunningServices(Integer.MAX_VALUE);
		for (RunningServiceInfo service : services) {
			if (service.started && service.clientLabel != 0 && service.pid > 0) {
				ProcessInfo processInfo = tmpAppProcesses.get(service.pid);
				processInfo.addService(service);
			}
		}

		// update the Memory Info
		final int count = runningProcess.size();
		int[] pids = new int[count];
		for (int i = 0; i < count; i++) {
			ProcessInfo processInfo = runningProcess.get(i);
			pids[i] = processInfo.getPid();
		}
		MemoryInfo[] meminfo = mAm.getProcessMemoryInfo(pids);

		for (int i = 0; i < count; i++) {
			ProcessInfo processInfo = runningProcess.get(i);
			processInfo.setMemory(meminfo[i].getTotalPss());
		}
		TMLog.end(TAG);
		return runningProcess;
	}
}
