package cn.gietv.vrPlayer.renderer2;

import android.util.Log;

import com.threed.jpct.FrameBuffer;
import com.threed.jpct.World;
import com.threed.jpct.util.Overlay;

/**
 * Created by Guiyuan on 2016/6/21.
 */
public class GazePointer extends Overlay {

    private static final double SELECT_TIME = 3000.0;

    private GazeEventListener mTarget = null;
    private long mStayTime = 0;
    private boolean mStaying = false;

    public GazePointer(World world, FrameBuffer buffer, String textureName) {
        super(world, buffer, textureName);
    }

    public GazePointer(World world, int upperLeftX, int upperLeftY, int lowerRightX, int lowerRightY, String textureName) {
        super(world, upperLeftX, upperLeftY, lowerRightX, lowerRightY, textureName);
    }

    public GazePointer(World world, int upperLeftX, int upperLeftY, int lowerRightX, int lowerRightY, String textureName, boolean modifyUV) {
        super(world, upperLeftX, upperLeftY, lowerRightX, lowerRightY, textureName, modifyUV);
    }

    public void targetIn(GazeEventListener target)
    {
        mTarget = target;
        //mStayTime = 0.0;
        mStayTime = System.currentTimeMillis();
        mStaying = true;

    }

    public void targetOut(GazeEventListener target)
    {
        mStaying = false;
        mTarget = null;
    }

    public void update()
    {
        if (!mStaying)
            return;

        if (mTarget == null)
            return;

        long currentTime = System.currentTimeMillis();

        long passTime = currentTime - mStayTime;
        Log.e("GazePointer", "time is " + passTime);
        if (currentTime - mStayTime >= SELECT_TIME)
        {
            mTarget.onGazeSelected();
            mStayTime = 0;

        }
    }
}
