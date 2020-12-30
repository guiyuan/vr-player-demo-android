package cn.gietv.vrPlayer.renderer2;

/**
 * Created by Guiyuan on 2016/6/21.
 */
public class ObjectPicker {


    private Button pickedObject = null;

    public void pickObject(GazePointer gazePointer, Button button)
    {
        if (button == null)
        {
            if (pickedObject != null)
            {
                gazePointer.targetOut(pickedObject);
                pickedObject.onGazeExit();
                pickedObject = null;
            }
        }
        else
        {
            if (pickedObject != null)
            {
                if (pickedObject == button)
                {
                    pickedObject.onGazeStay();
                }
                else
                {
                    pickedObject.onGazeExit();
                    pickedObject = button;
                    pickedObject.onGazeIn();
                    gazePointer.targetIn(pickedObject);
                }
            }
            else
            {
                pickedObject = button;
                pickedObject.onGazeIn();
                gazePointer.targetIn(pickedObject);
            }

        }
    }
}
