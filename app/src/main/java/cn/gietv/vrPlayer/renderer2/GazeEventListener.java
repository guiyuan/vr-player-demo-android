package cn.gietv.vrPlayer.renderer2;

/**
 * Created by Guiyuan on 2016/6/21.
 */
public interface GazeEventListener {
    public void onGazeIn();
    public void onGazeStay();
    public void onGazeExit();
    public void onGazeSelected();
}
