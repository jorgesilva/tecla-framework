package ca.idrc.tecla.framework;

import android.util.Log;

public class TeclaStatic {
	/**
	 * Main debug switch, turns on/off debugging for the whole framework
	 */
	public static final boolean DEBUG = false;
	/**
	 * Tag used for logging in the whole framework
	 */
	public static final String TAG = "TeclaNextFramework";

	public static void logV(String class_tag, String msg) {
		if (DEBUG) Log.v(TAG, class_tag + ": " + msg);
	}
	
	public static void logI(String class_tag, String msg) {
		if (DEBUG) Log.i(TAG, class_tag + ": " + msg);
	}
	
	public static void logD(String class_tag, String msg) {
		if (DEBUG) Log.d(TAG, class_tag + ": " + msg);
	}
	
	public static void logW(String class_tag, String msg) {
		if (DEBUG) Log.w(TAG, class_tag + ": " + msg);
	}

	public static void logE(String class_tag, String msg) {
		if (DEBUG) Log.e(TAG, class_tag + ": " + msg);
	}

}
