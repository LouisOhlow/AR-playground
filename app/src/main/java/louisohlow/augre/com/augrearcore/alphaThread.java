package louisohlow.augre.com.augrearcore;

import android.util.Log;

public class alphaThread implements Runnable {

    public void run(){
        while(ARNode.alpha < 1.0f) {
            try {
                Thread.sleep(30);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ARNode.alpha += 0.01f;
            ARNode.setRenderableMaterial();
            Log.d("alpha", "is "+ ARNode.alpha);
        }
        ARNode.alpha= 1.0f;
        ARNode.setRenderableMaterial();
}

}
