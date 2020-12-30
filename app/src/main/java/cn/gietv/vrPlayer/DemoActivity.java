package cn.gietv.vrPlayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;

import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.GLSLShader;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cn.gietv.vrPlayer.renderer2.AGLFont;
import cn.gietv.vrPlayer.renderer2.Button;
import cn.gietv.vrPlayer.renderer2.GazePointer;
import cn.gietv.vrPlayer.renderer2.ObjectPicker;
import cn.gietv.vrPlayer.renderer2.TextureRenderer;
import cn.gietv.vrPlayer.renderer2.Util;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @time: 2020-12-22
 * @author: jiguiyuan
 * @description:
 */
public class DemoActivity extends Activity implements GLSurfaceView.Renderer {

    public static final String TAG = "DemoActivity";

    private FrameBuffer frameBuffer;

    private Button button;
    private World world;
    private AGLFont glFont;
    private Object3D panorama;
    private SurfaceTexture surfaceTexture;
    private boolean frameAvailable = false;
    private Texture externalTexture;
    private TextureRenderer textureRenderer = new TextureRenderer();
    public IjkMediaPlayer mediaPlayer;
    private GLSurfaceView mGLSurfaceView;

    private int width;
    private int height;

    private float posX;
    private float posY;
    private float rotateX;
    private float rotateY;

    private SensorManager sensorManager;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new GLSurfaceView(this);
        // use opengl es 2.0
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setPreserveEGLContextOnPause(true);
        mGLSurfaceView.setRenderer(this);
        setContentView(mGLSurfaceView);

        Config.glTransparencyOffset = 0;
        Config.glTransparencyMul = 1f/100;

//        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
//        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
//        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);

        mediaPlayer = new IjkMediaPlayer();
        mediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mp.start();
//                handler.postDelayed(showTopView, 3);
            }
        });

        mediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                System.out.println(what);
                if(what == IMediaPlayer.MEDIA_INFO_BUFFERING_START){
                    mediaPlayer.pause();
                }else if(what == IMediaPlayer.MEDIA_INFO_BUFFERING_END){
                    mediaPlayer.start();
                }
                return true;
            }
        });


        if (mediaPlayer instanceof IjkMediaPlayer) {
            IjkMediaPlayer player = (IjkMediaPlayer) mediaPlayer;
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 60);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 0);
            player.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        }

        String videopath = "rtmp://superscene.sportsmedia.com.cn:1935/live/vr2";
//        String videopath = "rtmp://123.58.99.243:1935/live/vr";
        try {
            mediaPlayer.setDataSource(this, Uri.parse(videopath));
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.externalTexture = new Texture(32, 32);
        TextureManager.getInstance().flush();
        TextureManager.getInstance().addTexture("video_texture", externalTexture);

                Texture fanhuiHover = new Texture(getResources().openRawResource(R.raw.fanhuihover));
        TextureManager.getInstance().addTexture("ButtonSelected", fanhuiHover);

        Texture fanhuiNormal = new Texture(getResources().openRawResource(R.raw.fanhuinormal));
        TextureManager.getInstance().addTexture("ButtonNormal", fanhuiNormal);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.pause();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();

        if (panorama != null)
        {
            GLSLShader shader = panorama.getShader();
            panorama.clearShader();
            shader.finalize();
        }

        if (world != null)
        {
            world.dispose();
            world = null;
        }
        if (frameBuffer != null) {
            frameBuffer.dispose();

            frameBuffer = null;
        }

//        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.i(TAG, "onSurfaceChanged " + width + "x" + height);
        this.width = width;
        this.height = height;
        if (frameBuffer == null) {
            frameBuffer = new FrameBuffer(width, height); // OpenGL ES 2.0 constructor
        } else {
            frameBuffer.dispose();
        }
        if (world == null) {
            Paint paint = new Paint();
            paint.setSubpixelText(true); // required to render some fonts correctly
            paint.setAntiAlias(true);
            paint.setTextSize(25);
            glFont = new AGLFont(paint, true);
            world = new World();
            world.setAmbientLight(255, 255, 255);

            // TODO for some reason (because sphere is inverted) sphere looks upside down, fix it

            panorama = createSphere(100,64,32);
            try {

                String vertexShader = "";
                // normal
                vertexShader = Util.readContents(getClass().getResourceAsStream("/defaultVertexShaderTex0.src"));
                // top_bottom
//                vertexShader = Util.readContents(getResources().openRawResource(R.raw.top_bottom_vertex_shader));

//                switch (mMovieType)
//                {
//                    case MOVIE_TYPE_NORMAL:
//                        vertexShader = Util.readContents(getClass().getResourceAsStream("/defaultVertexShaderTex0.src"));
//                        break;
//                    case MOVIE_TYPE_TOP_BOTTOM:
//                        vertexShader = Util.readContents(getResources().openRawResource(R.raw.top_bottom_vertex_shader));
//                        break;
//                }
                String fragmentShader = Util.readContents(getResources().openRawResource(R.raw.surface_fragment_shader));

                GLSLShader shader = new GLSLShader(vertexShader, fragmentShader);
                panorama.setShader(shader);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            panorama.setTexture("video_texture");
            panorama.build();
            panorama.invert();
            world.addObject(panorama);
            world.getCamera().rotateZ(135);

//            Object3D cube = Primitives.getCube(4);
//            cube.strip();
//            cube.build();
//            world.addObject(cube);
//
//            SimpleVector vec = world.getCamera().getPosition();
//            SimpleVector dir = world.getCamera().getSideVector();
//            dir.scalarMul(10);
//            vec.add(dir);
//            cube.setCenter(vec);
//
//            world.getCamera().lookAt(cube.getCenter());

            button = Button.Create(4, 1f);
            button.setNormalTexture("ButtonNormal");
            button.setSelectedTexture("ButtonSelected");
            button.setActivity(this);
//
            world.addObject(button.getPlane());

//            int x = width / 2 + 50;
//            int y = height / 2 + 50;
//
//
//            gazePointer = new GazePointer(world, x, y, x + 50, y + 50, "SelectedStar");
//            gazePointer.setTransparency(100);
//
//            gazePointer.setVisibility(false);
////            button.setVisibility(false);
//
//            objectPicker = new ObjectPicker();
        }
        if (surfaceTexture != null)
            surfaceTexture.release();


        textureRenderer.surfaceCreated();

        surfaceTexture = new SurfaceTexture(textureRenderer.getTextureId());
        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);


        externalTexture.setExternalId(textureRenderer.getTextureId(), GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                synchronized (DemoActivity.this) {
                    frameAvailable = true;
                }
            }
        });

        synchronized (this) {
            if (!mediaPlayer.isPlaying()) {
                //mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
            frameAvailable = false;
        }

        MemoryHelper.compact();
    }

    @Override
    public void onDrawFrame(GL10 gl10) {

        if (rotateX != 0) {
//            world.getCamera().rotateCameraX(rotateX);
//            panorama.rotateY(rotateX);
            world.getCamera().rotateCameraY(rotateX);
            rotateX = 0;
        }

        if (rotateY != 0) {
//            world.getCamera().rotateCameraY(rotateY);
//            panorama.rotateX(rotateY);
            world.getCamera().rotateCameraX(rotateY);
            rotateY = 0;
        }

        synchronized(this) {
            if (frameAvailable) {
//                int error = GLES20.glGetError();
//                if (error != 0) {
//                    Log.w("ceshi","gl error before updateTexImage" + error + ": " + GLU.gluErrorString(error));
//                }
                surfaceTexture.updateTexImage();
                frameAvailable = false;
            }
        }

        frameBuffer.clear(RGBColor.BLACK);
        world.renderScene(frameBuffer);
        world.draw(frameBuffer);
        frameBuffer.display();

    }

    public Object3D createSphere(int radius, int segmentsW, int segmentsH) {

        int numVertices = (segmentsW + 1) * (segmentsH + 1);
        int numIndices = 2 * segmentsW * (segmentsH - 1) * 3;

        float[] vertices = new float[numVertices * 3];
        int[] indices = new int[numIndices];

        int i, j;
        int vertIndex = 0, index = 0;

        for (j = 0; j <= segmentsH; ++j) {
            float horAngle = (float) (Math.PI * j / segmentsH);
            float z = radius * (float) Math.cos(horAngle);
            float ringRadius = radius * (float) Math.sin(horAngle);

            for (i = 0; i <= segmentsW; ++i) {
                float verAngle = (float) (2.0f * Math.PI * i / segmentsW);
                float x = ringRadius * (float) Math.cos(verAngle);
                float y = ringRadius * (float) Math.sin(verAngle);

                vertices[vertIndex++] = x;
                vertices[vertIndex++] = z;
                vertices[vertIndex++] = y;

                if (i > 0 && j > 0) {
                    int a = (segmentsW + 1) * j + i;
                    int b = (segmentsW + 1) * j + i - 1;
                    int c = (segmentsW + 1) * (j - 1) + i - 1;
                    int d = (segmentsW + 1) * (j - 1) + i;

                    if (j == segmentsH) {
                        indices[index++] = a;
                        indices[index++] = c;
                        indices[index++] = d;
                    } else if (j == 1) {
                        indices[index++] = a;
                        indices[index++] = b;
                        indices[index++] = c;
                    } else {
                        indices[index++] = a;
                        indices[index++] = b;
                        indices[index++] = c;
                        indices[index++] = a;
                        indices[index++] = c;
                        indices[index++] = d;
                    }
                }
            }
        }

        float[] textureCoords = null;
        int numUvs = (segmentsH + 1) * (segmentsW + 1) * 2;
        textureCoords = new float[numUvs];

        numUvs = 0;
        for (j = 0; j <= segmentsH; ++j) {
            for (i = segmentsW; i >= 0; --i) {
                float u = (float) i / segmentsW;
                //textureCoords[numUvs++] = mMirrorTextureCoords ? 1.0f - u : u;
                textureCoords[numUvs++] = 1.0f - u;
                textureCoords[numUvs++] = (float) j / segmentsH;
            }
        }
        Object3D obj = new Object3D(vertices, textureCoords, indices, -1);
        return obj;
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            posY = event.getY();
            posX = event.getX();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            posY = -1;
            posX = -1;
            rotateX = 0;
            rotateY = 0;
            Object3D obj = pickObj((int)event.getX(), (int)event.getY());

            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float xd = event.getX() - posX;
            float yd = event.getY() - posY;
            posY = event.getY();
            posX = event.getX();

            rotateX = xd / 100f;
            rotateY = yd / 100f;
            return true;
        }
        return super.onTouchEvent(event);
    }

    private Object3D pickObj(int x, int y) {
        SimpleVector ray = Interact2D.reproject2D3DWS(world.getCamera(), frameBuffer, x, y).normalize();
        Object[] objs = world.calcMinDistanceAndObject3D(world.getCamera().getPosition(), ray, 10000f);
        if (objs == null || objs[1] == null || objs[0] == (Object)Object3D.RAY_MISSES_BOX) {
            return null;
        }
        Object3D res = (Object3D)objs[1];

        if (res.getID() == button.getPlane().getID()) {
            finish();
        }
        return res;
    }
}
