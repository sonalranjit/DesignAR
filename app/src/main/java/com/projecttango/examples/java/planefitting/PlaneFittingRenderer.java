/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.projecttango.examples.java.planefitting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.rajawali.ScenePoseCalculator;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.renderer.RajawaliRenderer;

import java.io.File;
import java.util.HashSet;

import javax.microedition.khronos.opengles.GL10;

/**
 * Very simple example augmented reality renderer which displays a cube fixed in place.
 * The position of the cube in the OpenGL world is updated using the {@code updateObjectPose}
 * method.
 */
public class PlaneFittingRenderer extends RajawaliRenderer {
    private static final float CUBE_SIDE_LENGTH = 0.1f;
    private static final String TAG = PlaneFittingRenderer.class.getSimpleName();
    private Material material;
    // Augmented Reality related fields
    private ATexture mTangoCameraTexture;
    private boolean mSceneCameraConfigured;

    private Object3D mObject;
    private Matrix4 mObjectTransform;
    private boolean mObjectPoseUpdated = false;
    private Context mcontext;

    public PlaneFittingRenderer(Context context) {
        super(context);
        this.mcontext = context;
    }

    @Override
    protected void initScene() {
        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        ScreenQuad backgroundQuad = new ScreenQuad();
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            backgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        getCurrentScene().addChildAt(backgroundQuad, 0);

        // Add a directional light in an arbitrary direction.
        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(3, 2, 4);
        getCurrentScene().addLight(light);

        // Set-up a material: green with application of the light and
        // instructions.
        material = new Material();
        material.setColor(0xffffffff);
        String mPath = Environment.getExternalStorageDirectory().toString() + "/the_best_picture_ever.png";
        try {
            Texture t = new Texture("instructions", R.drawable.instructions);
            if((new File(mPath)).exists()) {
                File image = new File(mPath);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
                bitmap = Bitmap.createScaledBitmap(bitmap,480,740,true);
                t = new Texture("instructions", bitmap);
            }
            material.addTexture(t);
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        material.setColorInfluence(0.1f);
        material.enableLighting(true);
        material.setDiffuseMethod(new DiffuseMethod.Lambert());

        //Build an obj and place it initially in the origin: AZX


        // Build a Cube and place it initially in the origin.
        Cube mObject = new Cube(CUBE_SIDE_LENGTH);
        mObject.setMaterial(material);
        mObject.setPosition(0, 0, -3);
        mObject.setRotation(Vector3.Axis.Z, 180);
        getCurrentScene().addChild(mObject);
    }
    HashSet<Cube> list= new HashSet<Cube>();
    @Override
    protected void onRender(long elapsedRealTime, double deltaTime) {
        // Update the AR object if necessary
        // Synchronize against concurrent access with the setter below.
        synchronized (this) {

            if (mObjectPoseUpdated) {
                Vector3 pos  = new Vector3(mObjectTransform.getTranslation());
                Vector3 cam = new Vector3(getCurrentCamera().getPosition());
                Vector3 ori = new Vector3(getCurrentCamera().getRotX()
                ,getCurrentCamera().getRotY()
                ,getCurrentCamera().getRotZ());
                //Vector3 dist = (new Vector3(0.1,0.1,0.1)).rotateBy(getCurrentCamera().getOrientation());
                //dist.rotateZ(Math.PI / 4.0);
                Vector3 dist = (new Vector3(pos).subtract(new Vector3(cam)));
                /*dist.rotateX(getCurrentCamera().getRotX());
                dist.rotateY(getCurrentCamera().getRotY());
                dist.rotateZ(getCurrentCamera().getRotZ());
*/

                boolean isThrough = false;
                Vector3 rightVector = null;
                int height = 0;
                Vector3 highest = new Vector3(0f,0f,0f);
                for(Cube vect : list)
                {
                    //System.out.println("4356345A: " + pos);
                    if((true &&Test.pointToLineDistance(cam,pos,vect.getPosition())<CUBE_SIDE_LENGTH/2.0))//

                    //if(Math.abs(pos.y - vect.getPosition().y)<0.1f && Math.abs(pos.x - vect.getPosition().x)<0.1f)
                    {
                        System.out.println("90934:" + Test.pointToLineDistance(cam,pos,vect.getPosition()));
                        if(vect.getPosition().z>highest.z) {
                        pos = new Vector3(vect.getPosition());
                          highest = new Vector3(vect.getPosition());

                        }
                        height++;
                    }
                }
                if(true||!isThrough) {

                    Cube mObject = new Cube(CUBE_SIDE_LENGTH);
                    mObject.setMaterial(material);
                    // Place the 3D object in the location of the detected plane.
                    mObject.setPosition(new Vector3(pos));
                   // mObject.setOrientation(new Quaternion().fromMatrix(mObjectTransform).conjugate());
                    mObject.setPosition(re(mObject));
                    getCurrentScene().addChild(mObject);
                    // Move it forward by half of the size of the cube to make it
                    // flush with the plane surface.
                    //if(height>0)
                    //mObject.moveUp(CUBE_SIDE_LENGTH * height);

                    list.add(mObject);
                }
                else
                {
/*
                    //Toast toast = Toast.makeText(mcontext, "GO!", Toast.LENGTH_SHORT);
                    //toast.show();
                    Vector3 newTransform = new Vector3(pos);
                    System.out.println("4356345 THIS IS THE VECTOR \n" + newTransform);
                    Cube mObject = new Cube(0.1f);
                    mObject.setMaterial(material);
                    // Place the 3D object in the location of the detected plane.
                    mObject.setPosition(new Vector3(newTransform));
                    getCurrentScene().addChild(mObject);
                    mObject.moveUp(0.1 * height);
                    // Move it forward by half of the size of the cube to make it
                    // flush with the plane surface.
                    //mObject.moveForward(CUBE_SIDE_LENGTH / 2.0f);
                    list.add(newTransform);
                    */
                }
                mObjectPoseUpdated = false;
            }
        }

        super.onRender(elapsedRealTime, deltaTime);
    }
    public Vector3 re(Cube cube0)
    {
        for(Cube cubei : list)
        {
            if(new Vector3(cube0.getPosition()).distanceTo(new Vector3(cubei.getPosition()))
                    <0.1)
            {
                Vector3 pos = cube0.getPosition();
                Cube vect = cubei;
                if(Math.abs(pos.z - vect.getPosition().z)<0.1f && Math.abs(pos.x - vect.getPosition().x)<0.1f)
                {
                    cube0.moveUp(CUBE_SIDE_LENGTH);
                    return re(cube0);
                }
            }
        }
        return cube0.getPosition();
    }
    /**
     * Save the updated plane fit pose to update the AR object on the next render pass.
     * This is synchronized against concurrent access in the render loop above.
     */
    public synchronized void updateObjectPose(float[] planeFitTransform) {
        mObjectTransform = new Matrix4(planeFitTransform);
        mObjectPoseUpdated = true;
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     * <p/>
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public void updateRenderCameraPose(TangoPoseData cameraPose) {
        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is need because Rajawali uses left handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        mSceneCameraConfigured = false;
    }

    public boolean isSceneCameraConfigured() {
        return mSceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scen camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(TangoCameraIntrinsics intrinsics) {
        Matrix4 projectionMatrix = ScenePoseCalculator.calculateProjectionMatrix(
                intrinsics.width, intrinsics.height,
                intrinsics.fx, intrinsics.fy, intrinsics.cx, intrinsics.cy);
        getCurrentCamera().setProjectionMatrix(projectionMatrix);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }
}
