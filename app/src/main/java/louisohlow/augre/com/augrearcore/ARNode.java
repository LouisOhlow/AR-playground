package louisohlow.augre.com.augrearcore;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ARNode extends AnchorNode {
    private AugmentedImage image;
    private static CompletableFuture<ModelRenderable> modelRenderableCompletableFuture;
    private ModelRenderable modelRenderable;

    private ExternalTexture texture;

    // The color to filter out of the video.
    public static float alpha = 0.0f;
    private static final Color CHROMA_KEY_COLOR = new Color(1f, 1f, 0f);

    private String TAG = "ARNode: ";



    public ARNode (Context context, int modelID, ExternalTexture texture){
        this.texture = texture;
        if(modelRenderableCompletableFuture == null)
        {
            modelRenderableCompletableFuture = ModelRenderable.builder()
                    .setRegistryId("my_model")
                    .setSource(context, modelID)
                    .build();
        }
    }

    public void setImage(AugmentedImage image) {
            this.image = image;
            if(!modelRenderableCompletableFuture.isDone())
            {
                CompletableFuture.allOf(modelRenderableCompletableFuture)
                        .thenAccept((Void aVoid) -> {
                            setImage(image);
                        }).exceptionally(throwable -> {
                    return null;
                });
            }
            else{
                try {
                    modelRenderable = modelRenderableCompletableFuture.get();
                    modelRenderable.getMaterial().setExternalTexture("videoTexture", texture);
                    modelRenderable.getMaterial().setFloat4("keyColor", CHROMA_KEY_COLOR);
                    modelRenderable.getMaterial().setFloat("alpha", alpha);
                    modelRenderable.setShadowCaster(false);
                    Log.d(TAG, "renderable assigned");
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        setAnchor(image.createAnchor(image.getCenterPose()));




        Node node = new Node();

        float scale = 0.265f;
        node.setLocalScale(new Vector3(scale + (scale*0.2f) ,scale, scale));
        //Vector3->  1 = links/rechts, 2 = vorne/hinten, 3 = unten+
        node.setLocalPosition(new Vector3(0f, 0f, 0.132f));
        //Pose pose = Pose.makeRotation(0.0f, 0.0f, 0.0f, 0f);
        node.setLocalRotation(Quaternion.axisAngle(new Vector3(1, 0, 0f), 270));

        node.setParent(this);
        node.setRenderable( modelRenderableCompletableFuture.getNow(null));



    }

    public ModelRenderable getRenderable(){
        return modelRenderable;
    }

    public static void setRenderableMaterial() {
        if(!modelRenderableCompletableFuture.isDone())
        {}
        else{
            try {
                modelRenderableCompletableFuture.get().getMaterial().setFloat("alpha", alpha);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
