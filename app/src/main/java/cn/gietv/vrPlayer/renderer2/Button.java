package cn.gietv.vrPlayer.renderer2;

import android.app.Activity;
import android.util.Log;

import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;

/**
 * Created by Guiyuan on 2016/6/21.
 */
public class Button extends Object implements GazeEventListener{

    private Object3D mPlane = null;
    private String mNormalTexture = null;
    private String mSelectedTexture = null;

    private Activity mActivity = null;

    public static Button Create(int quads, float scale)
    {
        Object3D obj = Primitives.getPlane(quads, scale);


        Button button = new Button(obj);

        return button;
    }

    private Button(Object3D p)
    {
        mPlane = p;
        mPlane.rotateY(135f);
        mPlane.rotateZ(135f);
        mPlane.rotateX(45f);
        mPlane.translate(0, -20f, -5f);
        mPlane.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
        mPlane.setTransparency(100);

        p.setUserObject(this);
    }

    public void setActivity(Activity activity)
    {
        mActivity = activity;
    }

    public void setName(String name) {
        mPlane.setName(name);
    }

    public void setVisibility(boolean visibility)
    {
        mPlane.setVisibility(visibility);
    }

    public void setNormalTexture(String normalTexture)
    {
        mNormalTexture = normalTexture;
        mPlane.setTexture(mNormalTexture);
    }

    public void setSelectedTexture(String selectedTexture)
    {
        mSelectedTexture = selectedTexture;
    }

    public Object3D getPlane()
    {
        return mPlane;
    }

    @Override
    public void onGazeIn() {
        if (mSelectedTexture != null)
        {
            mPlane.setTexture(mSelectedTexture);
        }
    }

    @Override
    public void onGazeStay() {

    }

    @Override
    public void onGazeExit() {
        if (mNormalTexture != null)
        {
            mPlane.setTexture(mNormalTexture);
        }
    }

    @Override
    public void onGazeSelected() {
        Log.e("Button", "onGazeSelected");
        if (mActivity != null)
        {
            mActivity.finish();
        }
    }
}
