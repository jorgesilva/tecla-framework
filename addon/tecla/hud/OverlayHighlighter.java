package com.android.tecla.hud;

import com.android.tecla.utils.HighlightBoundsView;
import com.android.tecla.utils.SimpleOverlay;
import com.android.tecla.utils.TeclaStatic;

import ca.idrc.tecla.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityNodeInfo;

public class OverlayHighlighter extends SimpleOverlay {

	public static final String CLASS_TAG = "Highlighter";
	private static final int DEFAULT_COLOR = Color.rgb(0x6F, 0xBF, 0xF5);
	private static final int FRAME_COLOR = Color.rgb(0x38, 0x38, 0x38);

    private final HighlightBoundsView mInnerBoundsView;
    private final HighlightBoundsView mOuterBoundsView;
    
	public OverlayHighlighter(Context context) {
		super(context);
		final WindowManager.LayoutParams params = getParams();
		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
		params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		//params.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
		params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
		setParams(params);
		
		setContentView(R.layout.tecla_highlighter);

		mInnerBoundsView = (HighlightBoundsView) findViewById(R.id.announce_bounds);
		mInnerBoundsView.setHighlightColor(DEFAULT_COLOR);
		
		
		mOuterBoundsView = (HighlightBoundsView) findViewById(R.id.bounds);
		mOuterBoundsView.setHighlightColor(FRAME_COLOR);
	}

	@Override
	protected void onShow() {
		TeclaStatic.logD(CLASS_TAG, "Showing Highlighter");
	}

	@Override
	protected void onHide() {
		TeclaStatic.logD(CLASS_TAG, "Hiding Highlighter");
//        mOuterBounds.clear();
//        mInnerBounds.clear();
	}
	

//	public void clearHighlight() {
//        mInnerBounds.clear();
//        mInnerBounds.postInvalidate();
//        mOuterBounds.clear();
//        mOuterBounds.postInvalidate();
//	}
	
//    public void removeInvalidNodes() {
//
//        mOuterBounds.removeInvalidNodes();
//        mOuterBounds.postInvalidate();
//
//        mInnerBounds.removeInvalidNodes();
//        mInnerBounds.postInvalidate();
//    }

	public void setNode(AccessibilityNodeInfo node) {
	
		//clearHighlight();
		if(node != null) {
		    Rect node_bounds = new Rect();
		    node.getBoundsInScreen(node_bounds);
		    mOuterBoundsView.setLeft(node_bounds.left);
		    mOuterBoundsView.setTop(node_bounds.top);
		    mOuterBoundsView.setRight(node_bounds.right);
		    mOuterBoundsView.setBottom(node_bounds.bottom);
		    mInnerBoundsView.setLeft(node_bounds.left);
		    mInnerBoundsView.setTop(node_bounds.top);
		    mInnerBoundsView.setRight(node_bounds.right);
		    mInnerBoundsView.setBottom(node_bounds.bottom);
		    //mOuterBoundsView.setBounds(node_bounds);
		    //mInnerBoundsView.setBounds(node_bounds);
		    mOuterBoundsView.setStrokeWidth(20);
		    mInnerBoundsView.setStrokeWidth(6);
		    mOuterBoundsView.postInvalidate();        	
		    mInnerBoundsView.postInvalidate();
			
		}
	}
    
}
