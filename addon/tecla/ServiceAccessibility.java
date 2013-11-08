package com.android.tecla;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import com.android.tecla.ServiceSwitchEventProvider.SwitchEventProviderBinder;
import com.android.tecla.hud.ManagerAutoScan;
import com.android.tecla.hud.OverlayHUD;
import com.android.tecla.hud.OverlayHighlighter;
import com.android.tecla.utils.TeclaStatic;

import ca.idi.tecla.sdk.SwitchEvent;
import ca.idi.tecla.sdk.SEPManager;
import ca.idrc.tecla.R;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class ServiceAccessibility extends AccessibilityService {

	private final static String CLASS_TAG = "TeclaAccessibilityService";
	private final static int DEBUG_SCAN_DELAY = 1000;

	private final static String EDITTEXT_CLASSNAME = "android.widget.EditText";
	
	public final static int DIRECTION_UP = 0;
	public final static int DIRECTION_LEFT = 1;
	public final static int DIRECTION_RIGHT = 2;
	public final static int DIRECTION_DOWN = 3;
	private final static int DIRECTION_ANY = 8;

	private static ServiceAccessibility sInstance;

	private Boolean register_receiver_called;

	private ArrayList<AccessibilityNodeInfo> mActiveLeafs;
	private AccessibilityNodeInfo mCurrentLeaf;
	private int mLeafIndex;

	private Handler mHandler;
	
	private AccessibilityNodeInfo mLastNode;
	
	private AccessibilityNodeInfo mOriginalNode, mPreviousOriginalNode;
	protected AccessibilityNodeInfo mSelectedNode;

	private OverlayHUD mHUD;
	private OverlayHighlighter mHighlighter;
	private OverlaySwitch mSwitch;

	protected static ReentrantLock mActionLock;
	
	private final static String MAP_VIEW = "android.view.View";
	// For later use for custom actions 
	//private final static String WEB_VIEW = "Web View";
	
	@Override
	public void onCreate() {
		TeclaStatic.logD(CLASS_TAG, "Service created");

		init();
	}

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();

		TeclaStatic.logD(CLASS_TAG, "Service connected");

	}

	private void init() {
		sInstance = this;
		register_receiver_called = false;
		
		mDebugScanHandler = new Handler();
		mHandler = new Handler();

		mActiveLeafs = new ArrayList<AccessibilityNodeInfo>();
		//mActionLock = new ReentrantLock();

		mHighlighter = new OverlayHighlighter(this);
		//mHUD = new OverlayHUD(this);
		
		//if (mSwitch == null) {
			//mSwitch = new OverlaySwitch(this);
			//TeclaApp.setFullscreenSwitch(mFullscreenSwitch);		
		//}

		// Bind to SwitchEventProvider
		//Intent intent = new Intent(this, ServiceSwitchEventProvider.class);
		//bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		//registerReceiver(mReceiver, new IntentFilter(SwitchEvent.ACTION_SWITCH_EVENT_RECEIVED));
		//register_receiver_called = true;
		
		//SEPManager.start(this);

		updateActiveLeafs(null);
		mDebugScanHandler.post(mDebugScanRunnable);
		//mOverlayHUD.show();
		//mFullscreenSwitch.show();
		//mTeclaOverlay.hide();
		//mFullscreenSwitch.hide();
		
		//TeclaApp.setA11yserviceInstance(sInstance);
	}
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		int event_type = event.getEventType();
		TeclaStatic.logD(CLASS_TAG, AccessibilityEvent.eventTypeToString(event_type) + ": " + event.getText());
		AccessibilityNodeInfo node = event.getSource();
		if (node != null) {
			switch (event_type) {
			case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
			case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
			case AccessibilityEvent.TYPE_VIEW_FOCUSED:
			case AccessibilityEvent.TYPE_VIEW_SELECTED:
			case AccessibilityEvent.TYPE_VIEW_SCROLLED:
			case AccessibilityEvent.TYPE_VIEW_CLICKED:
				updateActiveLeafs(node);
			}
		}
		//mHighlighter.setNode(getFirstActiveLeaf(node));
		//showHighlighter();
//		if (TeclaApp.getInstance().isSupportedIMERunning()) {
//			if (isFeedbackVisible()) {
//				int event_type = event.getEventType();
//				TeclaStatic.logD(CLASS_TAG, AccessibilityEvent.eventTypeToString(event_type) + ": " + event.getText());
//
//				AccessibilityNodeInfo node = event.getSource();
//				if (node != null) {
//					if (event_type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
//						mPreviousOriginalNode = mOriginalNode;
//						mOriginalNode = node;				
//						mNodeIndex = 0;
//						searchAndUpdateNodes();
//					} else if (event_type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
//						mPreviousOriginalNode = mOriginalNode;
//						mOriginalNode = node;				
//						mNodeIndex = 0;
//						searchAndUpdateNodes();
//						AccessibilityNodeInfo selectednode = findSelectedNode();
//						if(selectednode != null && selectednode.getParent().isScrollable()) {
//							mSelectedNode = selectednode;
//							mHighlighter.highlightNode(mSelectedNode);
//						}
////						mVisualOverlay.checkAndUpdateHUDHeight();
//					} else if (event_type == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
//						mSelectedNode = node;
//						mHighlighter.highlightNode(mSelectedNode);
//						if(mSelectedNode.getClassName().toString().contains(EDITTEXT_CLASSNAME))
//								TeclaApp.ime.showWindow(true);
//					} else if (event_type == AccessibilityEvent.TYPE_VIEW_SELECTED) {
//						//searchAndUpdateNodes();
//					} else if(event_type == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
//						mPreviousOriginalNode = mOriginalNode;
//						mOriginalNode = node;				
//						mNodeIndex = 0;
//						searchAndUpdateNodes();
//					} else if (event_type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
//						//searchAndUpdateNodes();
//					}
//				} else {
//					mSelectedNode=sInstance.getRootInActiveWindow();
//					TeclaStatic.logD(CLASS_TAG, "Node is null!");
//				}
//			}
//		}
	}

	private Runnable mUpdateActiveLeafsRunnable = new Runnable() {

		@Override
		public void run() {
			AccessibilityNodeInfo thisnode = mLastNode;
			ArrayList<AccessibilityNodeInfo> active_leafs = new ArrayList<AccessibilityNodeInfo>();
			Queue<AccessibilityNodeInfo> q = new LinkedList<AccessibilityNodeInfo>();
			q.add(thisnode);
			while (!q.isEmpty()) {
				thisnode = q.poll();
				if (isActive(thisnode)) {
					active_leafs.add(thisnode);
				}
				for (int i=0; i<thisnode.getChildCount(); ++i) {
					AccessibilityNodeInfo n = thisnode.getChild(i);
					if (n != null) q.add(n); // Don't add if null!
				}
			};
			Collections.sort(active_leafs, new Comparator<AccessibilityNodeInfo>(){
	
				@Override
				public int compare(AccessibilityNodeInfo lhs,
						AccessibilityNodeInfo rhs) {
					Rect outBoundsL = new Rect();
					Rect outBoundsR = new Rect();
					lhs.getBoundsInScreen(outBoundsL);
					rhs.getBoundsInScreen(outBoundsR);
					int swidth = mHighlighter.getRootView().getWidth();
					int sheight = mHighlighter.getRootView().getHeight();
					int smax;
					if (swidth <= sheight) { // Portrait
						smax = sheight;
					} else { // Landscape
						smax = swidth;
					}
	
					if ((outBoundsL.centerX() == outBoundsR.centerX())
							&& (outBoundsL.centerY() == outBoundsR.centerY())) {
						return 0;
					} else {
						return (smax * (outBoundsL.centerY() - outBoundsR.top)) + (outBoundsL.left - outBoundsR.right);
					}
				}
				
			});
			boolean is_same = false;
			if ((mActiveLeafs.size() == active_leafs.size()) && (mActiveLeafs.size() > 0)) {
				Rect cBounds = new Rect();
				Rect nBounds = new Rect();
				int i = 0;
				is_same = true;
				do {
					mActiveLeafs.get(i).getBoundsInScreen(cBounds);
					active_leafs.get(i).getBoundsInScreen(nBounds);
					if (!cBounds.equals(nBounds)) is_same = false;
					i++;
				} while (i < mActiveLeafs.size() && is_same);
			} else {
				mDebugScanHandler.removeCallbacks(mDebugScanRunnable);
				hideHighlighter();
				mLeafIndex = 0;
			}
			if (!is_same) {
				mDebugScanHandler.removeCallbacks(mDebugScanRunnable);
				mActiveLeafs = active_leafs;
				mDebugScanHandler.post(mDebugScanRunnable);
			}
			TeclaStatic.logD(CLASS_TAG, active_leafs.size() + " leafs in the node!");
		}
		
	};
	
	private void updateActiveLeafs(AccessibilityNodeInfo node) {
		if (node == null) {
			TeclaStatic.logW(CLASS_TAG, "Node is null, nothing to do!");
		} else {
			mHandler.removeCallbacks(mUpdateActiveLeafsRunnable);
			AccessibilityNodeInfo parent = node.getParent();
			while (parent != null) {
				node = parent;
				parent = node.getParent();
			}
			mLastNode = node;
			mHandler.post(mUpdateActiveLeafsRunnable);				
		}
	}
	
//	private boolean hasActiveDescendants(AccessibilityNodeInfo node) {
//		boolean has_active_descendants = false;
//		AccessibilityNodeInfo thisnode = node;
//		if (thisnode == null) thisnode = AccessibilityNodeInfo.obtain();  // If node is null, try obtaining a new one
//		if (thisnode != null) {
//			Queue<AccessibilityNodeInfo> q = new LinkedList<AccessibilityNodeInfo>();
//			do {
//				for (int i=0; i<thisnode.getChildCount(); ++i) {
//					AccessibilityNodeInfo n = thisnode.getChild(i);
//					if (n != null) q.add(n); // Don't add if null!
//				}
//				if (!q.isEmpty()) {
//					thisnode = q.poll();
//					if (isActive(thisnode)) {
//						has_active_descendants = true;
//					}
//				}
//			} while(!q.isEmpty() && !has_active_descendants);
//		}
//		return has_active_descendants;
//	}
	
	private void searchActiveNodesBFS(AccessibilityNodeInfo node) {
		mActiveLeafs.clear();
		mFocusedNode = null;
		Queue<AccessibilityNodeInfo> q = new LinkedList<AccessibilityNodeInfo>();
		q.add(node);
		while (!q.isEmpty()) {
			AccessibilityNodeInfo thisnode = q.poll();
			if(thisnode == null) continue;
			if(isActive(thisnode) && !thisnode.isScrollable()) {
				mActiveLeafs.add(thisnode);
				if(thisnode.isFocused())
					mFocusedNode = thisnode;
			}
			for (int i=0; i<thisnode.getChildCount(); ++i) q.add(thisnode.getChild(i));
		}
		//removeActiveParents();
	}
	
	public OverlayHUD getHUD() {
		return mHUD;
	}
	
	public OverlayHighlighter getHighlighter() {
		return mHighlighter;
	}
	
	public void setFullscreenSwitchLongClick(boolean enabled) {
		if(mSwitch != null)
			mSwitch.setLongClick(enabled);
	}
	
	private void showHighlighter() {
		if (mHighlighter != null) {
			if (!mHighlighter.isVisible()) {
				mHighlighter.show();
			}
		}
	}
	
	private void hideHighlighter() {
		if (mHighlighter != null) {
			if (mHighlighter.isVisible()) {
				mHighlighter.hide();
			}
		}
	}
	
	private void showHUD() {
		if (mHUD != null) {
			if (!mHUD.isVisible()) {
				mHUD.show();
			}
		}
	}
	
	private void hideHUD() {
		if (mHUD != null) {
			if (mHUD.isVisible()) {
				mHUD.hide();
			}
		}
	}
	
	public void showSwitch() {
		if (mSwitch != null) {
			if (!mSwitch.isVisible()) {
				mSwitch.show();
			}
		}
	}
	
	public void hideSwitch() {
		if (mSwitch != null) {
			if (mSwitch.isVisible()) {
				mSwitch.hide();
			}
		}
	}
	
	private void showAll() {
		showFeedback();
		showSwitch();
	}
	
	public boolean isFeedbackVisible() {
		return (mHighlighter.isVisible() && mHUD.isVisible());
	}
	
	public void enableScreenSwitch() {
		TeclaApp.persistence.setSelfScanningSelected(true);
		if(!TeclaApp.persistence.isInverseScanningSelected())
			ManagerAutoScan.start();
		showAll();
		sendGlobalHomeAction();
		TeclaApp.persistence.setScreenSwitchSelected(true);
	}

	public void disableScreenSwitch() {
		mSwitch.getRootView().setBackgroundResource(R.drawable.screen_switch_background_normal);
		mSwitch.getRootView().invalidate();
		if (TeclaApp.ime != null) TeclaApp.ime.hideWindow();
		TeclaApp.persistence.setScreenSwitchSelected(false);
		mSwitch.hide();
		hideFeedback();
	}

	public void showFeedback() {
		showHighlighter();
		showHUD();
	}
	
	public void hideFeedback() {
		ManagerAutoScan.stop();
		hideHighlighter();
		hideHUD();
	}
	
/*	public void turnFullscreenOn() {
		TeclaApp.persistence.setSelfScanningEnabled(true);
		if(!TeclaApp.persistence.isInverseScanningEnabled())
			AutoScanManager.start();
		showAll();
		//showFullscreenSwitch();
		sendGlobalHomeAction();
		//TeclaApp.persistence.setFullscreenEnabled(true);
	}
	
	public void turnFullscreenOff() {
		TeclaApp.a11yservice.hideFullscreenSwitch();
		TeclaApp.persistence.setSelfScanningEnabled(false);
		AutoScanManager.stop();				
		//TeclaApp.overlay.hideAll();
		TeclaApp.persistence.setFullscreenEnabled(false);
//		if(TeclaApp.settingsactivity != null) {
//			TeclaApp.settingsactivity.uncheckFullScreenMode();
//		}
	}
	
*/
	private AccessibilityNodeInfo findSelectedNode() {
		AccessibilityNodeInfo result = null;
		for (AccessibilityNodeInfo node: mActiveLeafs) {
			if(node.isSelected()) {
				if(result == null)
					result = node;
				else {
					Rect node_rect = new Rect();
					Rect result_rect = new Rect();
					node.getBoundsInScreen(node_rect);
					result.getBoundsInScreen(result_rect);
					if(node_rect.contains(result_rect))
						result = node;
				}
			}
		}
		return result;
	}
		
	private AccessibilityNodeInfo mFocusedNode;
	private void searchAndUpdateNodes() {
		//		TeclaHighlighter.clearHighlight();
		searchActiveNodesBFS(mOriginalNode);
		
		if (mActiveLeafs.size() > 0 ) {
			mSelectedNode = findNeighbourNode(mSelectedNode, DIRECTION_ANY);
			if(mSelectedNode == null) mSelectedNode = mActiveLeafs.get(0);
			if(mFocusedNode != null) {
				mSelectedNode = mFocusedNode;
			}
			mHighlighter.setNode(mSelectedNode);
			if(mPreviousOriginalNode != null) 
				mPreviousOriginalNode.recycle();
		}
	}

	private void removeActiveParents() {
		ArrayList<Rect> node_rects = new ArrayList<Rect>();
		AccessibilityNodeInfo node;
		Rect rect;
		int i;
		for(i=0; i<mActiveLeafs.size(); ++i) {
			rect = new Rect();
			node = mActiveLeafs.get(i);
			node.getBoundsInScreen(rect);
			node_rects.add(rect);
		}
		i=0;
		Rect rect2;
		while(i<node_rects.size()) {
			rect = node_rects.get(i);
			boolean removedANode = false;
			for(int j=0; j<node_rects.size(); ++j) {
				if(i==j) continue;
				rect2 = node_rects.get(j);
				if(rect.contains(rect2)) {
					node_rects.remove(i); 
					mActiveLeafs.remove(i);
					removedANode = true;
					break;
				}
			}
			if(!removedANode) ++i;
		}
	}

//	private void sortAccessibilityNodes(ArrayList<AccessibilityNodeInfo> nodes) {
//		ArrayList<AccessibilityNodeInfo> sorted = new ArrayList<AccessibilityNodeInfo>();
//		Rect bounds_unsorted_node = new Rect();
//		Rect bounds_sorted_node = new Rect();
//		boolean inserted = false; 
//		for(AccessibilityNodeInfo node: nodes) {
//			if(sorted.size() == 0) sorted.add(node);
//			else {
//				node.getBoundsInScreen(bounds_unsorted_node);
//				inserted = false; 
//				for (int i=0; i<sorted.size() && !inserted; ++i) {
//					sorted.get(i).getBoundsInScreen(bounds_sorted_node);
//					if(bounds_sorted_node.centerY() > bounds_unsorted_node.centerY()) {
//						sorted.add(i, node);
//						inserted = true;
//					} else if (bounds_sorted_node.centerY() == bounds_unsorted_node.centerY()) {
//						if(bounds_sorted_node.centerX() > bounds_unsorted_node.centerX()) {
//							sorted.add(i, node);
//							inserted = true;
//						}
//					}
//				}
//				if(!inserted) sorted.add(node);
//			}
//		}
//		nodes.clear();
//		nodes = sorted; 
//	}

	public void selectNode(int direction ) {
		selectNode(sInstance.mSelectedNode,  direction );
	}

	public void selectNode(AccessibilityNodeInfo refnode, int direction ) {
		NodeSelectionThread thread = new NodeSelectionThread(refnode, direction);
		thread.start();	
	}

	private static AccessibilityNodeInfo findNeighbourNode(AccessibilityNodeInfo refnode, int direction) {
		int r2_min = Integer.MAX_VALUE;
		int r2 = 0;
		double ratio_min = Double.MAX_VALUE;
		double ratio = 0;
		double K = 2;
		Rect refOutBounds = new Rect();
		if(refnode == null) return null;
		refnode.getBoundsInScreen(refOutBounds);
		int x = refOutBounds.centerX();
		int y = refOutBounds.centerY();
		int dx, dy;
		Rect outBounds = new Rect();
		AccessibilityNodeInfo result = null; 
		for (AccessibilityNodeInfo node: sInstance.mActiveLeafs ) {
			if(refnode.equals(node) && direction != DIRECTION_ANY) continue; 
			node.getBoundsInScreen(outBounds);
			dx = x - outBounds.centerX();
			dy = y - outBounds.centerY();
			r2 = dx*dx + dy*dy;
			switch (direction ) {
			case DIRECTION_UP:
				if(dy <= 0) continue;
				ratio = Math.round(Math.abs(dx/Math.sqrt(r2)*K));
				break;  
			case DIRECTION_DOWN:
				if(dy >= 0) continue;
				ratio = Math.round(Math.abs(dx/Math.sqrt(r2)*K));
				break;  
			case DIRECTION_LEFT:
				if(dx <= 0) continue;
				ratio = Math.round(Math.abs(dy/Math.sqrt(r2)*K));
				break; 
			case DIRECTION_RIGHT:
				if(dx >= 0) continue;
				ratio = Math.round(Math.abs(dy/Math.sqrt(r2)*K));
				break; 
			default: 
				break; 
			}
			if(ratio <= ratio_min) {
				if(ratio < ratio_min) {
					ratio_min = ratio;
					r2_min = r2;
					result = node;					
				} else if(r2 < r2_min) {
					r2_min = r2;
					result = node;						
				}
			}
		}
		if(ratio_min >= 0.95*K) result = null;
		return result;		
	}

	public void clickActiveNode() {
		if(sInstance.mActiveLeafs.size() == 0) return;
		if(sInstance.mSelectedNode == null) sInstance.mSelectedNode = sInstance.mActiveLeafs.get(0);
		
		// Use to find out view type for custom actions
		//Log.i("NODE TO STRING"," " + sInstance.mSelectedNode.toString());
		
		sInstance.mSelectedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
		if(isFeedbackVisible()) 
			mHighlighter.clearHighlight();
	}

	//	public static void selectActiveNode(int index) {
	//		if(sInstance.mActiveNodes.size()==0) return; 
	//		sInstance.mNodeIndex = index;
	//		sInstance.mNodeIndex = Math.min(sInstance.mActiveNodes.size() - 1, sInstance.mNodeIndex + 1);
	//		sInstance.mNodeIndex = Math.max(0, sInstance.mNodeIndex - 1);
	//		TeclaHighlighter.updateNodes(sInstance.mActiveNodes.get(sInstance.mNodeIndex));
	//	}
	//
	//	public static void selectPreviousActiveNode() {
	//		if(sInstance.mActiveNodes.size()==0) return; 
	//		sInstance.mNodeIndex = Math.max(0, sInstance.mNodeIndex - 1);
	//		TeclaHighlighter.updateNodes(sInstance.mOriginalNode, sInstance.mActiveNodes.get(sInstance.mNodeIndex));
	//
	//	}
	//
	//	public static void selectNextActiveNode() {
	//		if(sInstance.mActiveNodes.size()==0) return;
	//		sInstance.mNodeIndex = Math.min(sInstance.mActiveNodes.size() - 1, sInstance.mNodeIndex + 1);
	//		TeclaHighlighter.updateNodes(sInstance.mOriginalNode, sInstance.mActiveNodes.get(sInstance.mNodeIndex));
	//	}
	//

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(SwitchEvent.ACTION_SWITCH_EVENT_RECEIVED)) {
				handleSwitchEvent(intent.getExtras());
			}
		}
	};

	private boolean isSwitchPressed = false;
	private String[] actions = null;
	private void handleSwitchEvent(Bundle extras) {
		TeclaStatic.logD(CLASS_TAG, "Received switch event.");
		SwitchEvent event = new SwitchEvent(extras);
		if (event.isAnyPressed()) {
			isSwitchPressed = true;
			actions = (String[]) extras.get(SwitchEvent.EXTRA_SWITCH_ACTIONS);
			if(TeclaApp.persistence.isInverseScanningSelected()) {
				ManagerAutoScan.start();
			}
		} else if(isSwitchPressed) { // on switch released
			isSwitchPressed = false;
			if(TeclaApp.persistence.isInverseScanningSelected()) {
				if(AdapterInputMethod.isShowingKeyboard()) AdapterInputMethod.selectScanHighlighted();
				else selectHighlighted();
				ManagerAutoScan.stop();
			} else {
				String action_tecla = actions[0];
				int max_node_index = mActiveLeafs.size() - 1;
				switch(Integer.parseInt(action_tecla)) {

				case SwitchEvent.ACTION_NEXT:
					if(AdapterInputMethod.isShowingKeyboard()) AdapterInputMethod.scanNext();
					else mHUD.scanNext();
					break;
				case SwitchEvent.ACTION_PREV:
					if(AdapterInputMethod.isShowingKeyboard()) AdapterInputMethod.scanPrevious();
					else mHUD.scanPrevious();
					break;
				case SwitchEvent.ACTION_SELECT:
					if(AdapterInputMethod.isShowingKeyboard()) AdapterInputMethod.selectScanHighlighted();
					else selectHighlighted();				
					break;
				case SwitchEvent.ACTION_CANCEL:
					//TODO: Programmatic back key?
				default:
					break;
				}
				if(TeclaApp.persistence.isSelfScanningSelected())
					ManagerAutoScan.setExtendedTimer();
			}
			
		}
	}

	@Override
	public void onInterrupt() {

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		shutdownInfrastructure();
	}

	private void selectHighlighted() {
		AccessibilityNodeInfo node = TeclaApp.a11yservice.mSelectedNode;
		AccessibilityNodeInfo parent = null;
		if(node != null) parent = node.getParent();
		int actions = 0;
		if(parent != null) actions = node.getParent().getActions();
		
		if(mHUD.getPage() == 0) {
			switch (mHUD.getIndex()){
			case OverlayHUD.HUD_BTN_TOP:
				if(isActiveScrollNode(node) 
						&& (actions & AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) 
						== AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
					parent.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
				} else
					TeclaApp.a11yservice.selectNode(ServiceAccessibility.DIRECTION_UP);
				break;
			case OverlayHUD.HUD_BTN_BOTTOM:
				if(isActiveScrollNode(node)
						&& (actions & AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) 
						== AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
					node.getParent().performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
				} else 
					TeclaApp.a11yservice.selectNode(ServiceAccessibility.DIRECTION_DOWN);
				break;
			case OverlayHUD.HUD_BTN_LEFT:
				TeclaApp.a11yservice.selectNode(ServiceAccessibility.DIRECTION_LEFT);
				break;
			case OverlayHUD.HUD_BTN_RIGHT:
				TeclaApp.a11yservice.selectNode(ServiceAccessibility.DIRECTION_RIGHT);
				break;
			case OverlayHUD.HUD_BTN_TOPRIGHT:
				TeclaApp.a11yservice.clickActiveNode();
				break;
			case OverlayHUD.HUD_BTN_BOTTOMLEFT:
				TeclaApp.a11yservice.sendGlobalBackAction();
				/*if(Persistence.isDefaultIME(mContext) && TeclaApp.persistence.isIMERunning()) {
					TeclaStatic.logI(CLASS_TAG, "LatinIME is active");
					TeclaApp.ime.pressBackKey();
				} else TeclaStatic.logW(CLASS_TAG, "LatinIME is not active!");*/
				break;
			case OverlayHUD.HUD_BTN_TOPLEFT:
				TeclaApp.a11yservice.sendGlobalNotificationAction();
				/*if(Persistence.isDefaultIME(mContext) && TeclaApp.persistence.isIMERunning()) {
					TeclaStatic.logI(CLASS_TAG, "LatinIME is active");
					TeclaApp.ime.pressHomeKey();
				} else TeclaStatic.logW(CLASS_TAG, "LatinIME is not active!");*/
				break;
			case OverlayHUD.HUD_BTN_BOTTOMRIGHT:
				mHUD.turnPage();
				break;
			}
		} else if(mHUD.getPage() == 1) {
			switch (mHUD.getIndex()){
			case OverlayHUD.HUD_BTN_TOP:
			case OverlayHUD.HUD_BTN_BOTTOM:
			case OverlayHUD.HUD_BTN_LEFT:
			case OverlayHUD.HUD_BTN_RIGHT:
			case OverlayHUD.HUD_BTN_TOPRIGHT:
			case OverlayHUD.HUD_BTN_BOTTOMLEFT:
				break;
			case OverlayHUD.HUD_BTN_TOPLEFT:
				TeclaApp.a11yservice.sendGlobalHomeAction();
				break;
			case OverlayHUD.HUD_BTN_BOTTOMRIGHT:
				mHUD.turnPage();
				break;
			}
		}
		
		if(TeclaApp.persistence.isSelfScanningSelected())
			ManagerAutoScan.resetTimer();

	}
	
	/**
	 * Shuts down the infrastructure in case it has been initialized.
	 */
	public void shutdownInfrastructure() {	
		TeclaStatic.logD(CLASS_TAG, "Shutting down infrastructure...");
		if (mBound) unbindService(mConnection);
		SEPManager.stop(getApplicationContext());
		hideFeedback();
		
		if (mSwitch != null) {
			if(mSwitch.isVisible()) {
				mSwitch.hide();
			}
		}
		if (register_receiver_called) {
			unregisterReceiver(mReceiver);
			register_receiver_called = false;
		}
	}

	protected class NodeSelectionThread extends Thread {
		AccessibilityNodeInfo current_node;
		int direction; 
		public NodeSelectionThread(AccessibilityNodeInfo node, int dir) {
			current_node = node;
			direction = dir;
		}
		public void run() {
			if(hasScrollableParent(current_node)) {
				navigateWithDPad(direction);
				return;
			}
			AccessibilityNodeInfo node;
			mActionLock.lock();
			node = findNeighbourNode(current_node, direction);
			
			if(node != null) {
				if (sInstance.mSelectedNode.toString().contains(MAP_VIEW)){
					navigateWithDPad(direction);
				}else{
					mHighlighter.setNode(node);
				if(node.isFocusable()) 
					node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
				sInstance.mSelectedNode = node;
				}
				
			} else {
				navigateWithDPad(direction);
			}
			mActionLock.unlock(); 
		}
	}

	private static void navigateWithDPad(int direction) {
		switch(direction) {
		case(DIRECTION_UP):
			TeclaApp.ime.sendKey(KeyEvent.KEYCODE_DPAD_UP);
			break;
		case(DIRECTION_DOWN):
			TeclaApp.ime.sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
			break;
		case(DIRECTION_LEFT):
			TeclaApp.ime.sendKey(KeyEvent.KEYCODE_DPAD_LEFT);
			break;
		case(DIRECTION_RIGHT):
			TeclaApp.ime.sendKey(KeyEvent.KEYCODE_DPAD_RIGHT);
			break;					
		}
	}
	
	public static boolean hasScrollableParent(AccessibilityNodeInfo node) {
		if(node == null) return false;
		AccessibilityNodeInfo parent = node.getParent();
		if (parent != null) {
			if(!parent.isScrollable()) return false;
		}
		return true;
	}

//	public boolean isFirstActiveScrollNode(AccessibilityNodeInfo node) {
//		if(!hasScrollableParent(node)) return false;
//		AccessibilityNodeInfo parent = node.getParent();
//		AccessibilityNodeInfo  activeNode = null;
//		for(int i=0; i < parent.getChildCount()-1; ++i) {
//			AccessibilityNodeInfo  aNode = parent.getChild(i);
//			if(isActive(aNode)) {
//				activeNode = aNode;
//				break;
//			}
//		}
//
//		return isSameNode(node, activeNode);
//	}

	public boolean isActiveScrollNode(AccessibilityNodeInfo node) {
		if(node == null) return false;
		return (hasScrollableParent(node) && isActive(node))? true:false;
	}

//	public boolean isLastActiveScrollNode(AccessibilityNodeInfo node) {
//		if(!hasScrollableParent(node)) return false;
//		AccessibilityNodeInfo parent = node.getParent();
//		AccessibilityNodeInfo  lastScrollNode = null;
//		for(int i=parent.getChildCount()-1; i>=0; --i) {
//			AccessibilityNodeInfo aNode = parent.getChild(i);
//			if(isActive(aNode)) {
//				lastScrollNode = aNode;
//				break;
//			}
//		}	
//		return isSameNode(node, lastScrollNode);
//	}
	
	public static boolean isSameNode(AccessibilityNodeInfo node1, AccessibilityNodeInfo node2) {
		if(node1 == null || node2 == null) return false;
		Rect node1_rect = new Rect(); 
		node1.getBoundsInScreen(node1_rect);	
		Rect node2_rect = new Rect(); 
		node2.getBoundsInScreen(node2_rect);	
		if(node1_rect.left == node2_rect.left
				&& node1_rect.right == node2_rect.right
				&& node1_rect.top == node2_rect.top
				&& node1_rect.bottom == node2_rect.bottom) 
			return true;
		return false;
	}
	
	public static boolean isInsideParent(AccessibilityNodeInfo node) {
		if(node == null) return false;
		AccessibilityNodeInfo parent = node.getParent();
		if(parent == null) return false;
		Rect node_rect = new Rect();
		Rect parent_rect = new Rect();
		node.getBoundsInScreen(node_rect);
		parent.getBoundsInScreen(parent_rect);
		if(parent_rect.contains(node_rect)) return true;
		return false;
	}

	public void sendGlobalBackAction() {
		sInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
	}

	public void sendGlobalHomeAction() {
		sInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);		
	}	

	public void sendGlobalNotificationAction() {
		sInstance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);		
	}	

	public void injectSwitchEvent(SwitchEvent event) {
		switch_event_provider.injectSwitchEvent(event);
	}

	public void injectSwitchEvent(int switchChanges, int switchStates) {
		switch_event_provider.injectSwitchEvent(switchChanges, switchStates);
	}

	ServiceSwitchEventProvider switch_event_provider;
	boolean mBound = false;

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			SwitchEventProviderBinder binder = (SwitchEventProviderBinder) service;
			switch_event_provider = binder.getService();
			mBound = true;
			TeclaStatic.logD(CLASS_TAG, "Ally service bound to SEP");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;

		}
	};

	private boolean isActive(AccessibilityNodeInfo node) {
		boolean is_active = false;
		AccessibilityNodeInfo parent = node.getParent();
		if (node.isVisibleToUser()
				&& node.isClickable()
				&& (isA11yFocusable(node))
				//&& !(!isInputFocusable(node) && !(node.isScrollable() || parent.isScrollable()))
				&& node.isEnabled())
			is_active = true;
		return is_active;
	}
	
	private boolean isInputFocusable(AccessibilityNodeInfo node) {
		return (node.getActions() & AccessibilityNodeInfo.ACTION_FOCUS) == AccessibilityNodeInfo.ACTION_FOCUS;
	}
	
	private boolean isA11yFocusable(AccessibilityNodeInfo node) {
		return (node.getActions() & AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS;
	}
	
	private Handler mDebugScanHandler;
	private Runnable mDebugScanRunnable = new Runnable() {

		@Override
		public void run() {
			mDebugScanHandler.removeCallbacks(mDebugScanRunnable);
			if (mActiveLeafs.size() > 0) {
				if (mLeafIndex >= mActiveLeafs.size()) {
					mLeafIndex = 0;
				}
				mCurrentLeaf = mActiveLeafs.get(mLeafIndex);
				mHighlighter.setNode(mCurrentLeaf);
				showHighlighter();
				mLeafIndex++;
				//logProperties(mCurrentLeaf);
			}
			mDebugScanHandler.postDelayed(mDebugScanRunnable, DEBUG_SCAN_DELAY);
		}
		
	};
	
	private void logProperties(AccessibilityNodeInfo node) {
		AccessibilityNodeInfo parent = node.getParent();
		TeclaStatic.logW(CLASS_TAG, "Node properties");
		TeclaStatic.logW(CLASS_TAG, "isA11yFocusable? " + Boolean.toString(isA11yFocusable(node)));
		TeclaStatic.logW(CLASS_TAG, "isInputFocusable? " + Boolean.toString(isInputFocusable(node)));
		//TeclaStatic.logD(CLASS_TAG, "isVisible? " + Boolean.toString(node.isVisibleToUser()));
		//TeclaStatic.logD(CLASS_TAG, "isClickable? " + Boolean.toString(node.isClickable()));
		//TeclaStatic.logD(CLASS_TAG, "isEnabled? " + Boolean.toString(node.isEnabled()));
		//TeclaStatic.logD(CLASS_TAG, "isScrollable? " + Boolean.toString(node.isScrollable()));
		//TeclaStatic.logD(CLASS_TAG, "isSelected? " + Boolean.toString(node.isSelected()));
		//TeclaStatic.logW(CLASS_TAG, "Parent properties");
		//TeclaStatic.logW(CLASS_TAG, "isVisible? " + Boolean.toString(parent.isVisibleToUser()));
		//TeclaStatic.logW(CLASS_TAG, "isClickable? " + Boolean.toString(parent.isClickable()));
		//TeclaStatic.logW(CLASS_TAG, "isEnabled? " + Boolean.toString(parent.isEnabled()));
		//TeclaStatic.logW(CLASS_TAG, "isA11yFocusable? " + Boolean.toString((parent.getActions() & AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS));
		//TeclaStatic.logW(CLASS_TAG, "isInputFocusable? " + Boolean.toString((parent.getActions() & AccessibilityNodeInfo.ACTION_FOCUS) == AccessibilityNodeInfo.ACTION_FOCUS));
		//TeclaStatic.logW(CLASS_TAG, "isScrollable? " + Boolean.toString(parent.isScrollable()));
		//TeclaStatic.logW(CLASS_TAG, "isSelected? " + Boolean.toString(parent.isSelected()));
	}
	
}
