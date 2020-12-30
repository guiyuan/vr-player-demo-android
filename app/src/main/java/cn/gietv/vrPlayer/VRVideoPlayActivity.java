package cn.gietv.vrPlayer;

import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.threed.jpct.Config;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.GLSLShader;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;
import com.threed.jpct.util.Overlay;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;

import cn.gietv.vrPlayer.renderer2.AGLFont;
import cn.gietv.vrPlayer.renderer2.Button;
import cn.gietv.vrPlayer.renderer2.CardboardOverlayView;
import cn.gietv.vrPlayer.renderer2.GazePointer;
import cn.gietv.vrPlayer.renderer2.ObjectPicker;
import cn.gietv.vrPlayer.renderer2.TextureRenderer;
import cn.gietv.vrPlayer.renderer2.Util;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


/**
 * Created by houde on 2016/4/19.
 */
public class VRVideoPlayActivity extends CardboardActivity implements View.OnClickListener, CardboardView.StereoRenderer{

    public static final String VIDEO_PATH = "video_path";
    public static final String MOVIE_TYPE = "movie_type";
    private static final String TAG = "VRVideoPlayActivity";
    public static final int MOVIE_TYPE_NORMAL = 1;
    public static final int MOVIE_TYPE_TOP_BOTTOM = 2;
    final String numberFormat = "({0,number,0.00}, {1,number,0.00}, {2,number,0.00})";
    private CardboardOverlayView mOverlayView;
    private FrameBuffer frameBuffer;
    private float[] mHeadView = new float[16];
    private final Matrix tempTransform = new Matrix();
    private int fps = 0;
    private World world;
    private AGLFont glFont;
    private Object3D panorama;
    private ObjectPicker objectPicker;
    private Button button;
    private GazePointer gazePointer;
    private Overlay star;
    private SurfaceTexture surfaceTexture;
    //private int videoTextId = -1;
    private boolean frameAvailable = false;
    private Texture externalTexture;
    private TextureRenderer textureRenderer = new TextureRenderer();
    public IjkMediaPlayer mediaPlayer;
        private CardboardView cardboardView;
    public ImageView mExitImage, mVRType ,mPlayStatus;
    private int mMovieType;
    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_ui);
        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        mExitImage = (ImageView) findViewById(R.id.exit_image);
        mVRType = (ImageView) findViewById(R.id.vr_image);
        mTopParent = (LinearLayout) findViewById(R.id.top_parent);
        mPlayStatus = (ImageView) findViewById(R.id.mediacontroller_play_pause);
        mPlayStatus.setOnClickListener(this);
        mVRType.setOnClickListener(this);
        mExitImage.setOnClickListener(this);
        cardboardView.setRenderer(this);
        cardboardView.setSettingsButtonEnabled(false);
        setCardboardView(cardboardView);
        mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);
//        mOverlayView.show3DToast("Pull the magnet when you find an object.");
        Config.glTransparencyOffset = 0;
        Config.glTransparencyMul = 1f/100;
        setVolumeKeysMode(VolumeKeys.NOT_DISABLED);
        cardboardView.setOnClickListener(this);
        mediaPlayer = new IjkMediaPlayer();
//        setOnPreparedListener();
//        setOnInfoListener();
        mediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mp.start();
                handler.postDelayed(showTopView, 3);
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
//        String videopath = getIntent().getStringExtra(VIDEO_PATH);
        String videopath = "rtmp://123.58.99.243:1935/live/vr";

        Log.e("ceshi","videopath="+Uri.parse(videopath).toString());
        if(videopath == null){
            //
            Log.w(TAG, "video path is empty!");
        }
        try {
            if(videopath.startsWith("http")) {
                System.out.println("path     " + videopath);
                mediaPlayer.setDataSource(videopath);
            }else{
                mediaPlayer.setDataSource(this, Uri.parse(videopath));
            }
//            mediaPlayer.setDataSource(Uri.parse(videopath).toString());
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

        Texture star = new Texture(getResources().openRawResource(R.raw.star));
        TextureManager.getInstance().addTexture("SelectedStar", star);

//        mMovieType = getIntent().getIntExtra(MOVIE_TYPE, MOVIE_TYPE_NORMAL);

        mMovieType = MOVIE_TYPE_NORMAL;
    }
//    public abstract void  setOnPreparedListener();
//    public abstract void  setOnInfoListener();
   // public abstract void  setDataSource(String videopath);


    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.pause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
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
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged " + width + "x" + height);

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
                switch (mMovieType)
                {
                    case MOVIE_TYPE_NORMAL:
                        vertexShader = Util.readContents(getClass().getResourceAsStream("/defaultVertexShaderTex0.src"));
                        break;
                    case MOVIE_TYPE_TOP_BOTTOM:
                        vertexShader = Util.readContents(getResources().openRawResource(R.raw.top_bottom_vertex_shader));
                        break;
                }
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


            button = Button.Create(4, 1f);
            button.setNormalTexture("ButtonNormal");
            button.setSelectedTexture("ButtonSelected");
            button.setActivity(this);

            world.addObject(button.getPlane());

            int x = width / 2 + 50;
            int y = height / 2 + 50;


            gazePointer = new GazePointer(world, x, y, x + 50, y + 50, "SelectedStar");
            gazePointer.setTransparency(100);

            gazePointer.setVisibility(false);
//            button.setVisibility(false);

            objectPicker = new ObjectPicker();
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
                synchronized (VRVideoPlayActivity.this) {
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

    /**
     * Creates the buffers we use to store information about the 3D world.
     *
     * OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getHeadView(mHeadView, 0);
        synchronized(this) {
            if (frameAvailable) {
                int error = GLES20.glGetError();
                if (error != 0) {
                    Log.w("ceshi","gl error before updateTexImage" + error + ": " + GLU.gluErrorString(error));
                }
                surfaceTexture.updateTexImage();
                frameAvailable = false;
            }
        }
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    @Override
    public void onDrawEye(Eye eye) {
        if (frameBuffer == null) {
            frameBuffer = new FrameBuffer(eye.getViewport().width, eye.getViewport().height); // OpenGL ES 2.0 constructor
        }
        Matrix camBack = world.getCamera().getBack();
        camBack.setIdentity();
        tempTransform.setDump(eye.getEyeView());
        tempTransform.transformToGL();
        camBack.matMul(tempTransform);
        world.getCamera().setBack(camBack);
        frameBuffer.clear(RGBColor.BLACK);
        world.renderScene(frameBuffer);
        world.draw(frameBuffer);
        frameBuffer.display();
    }
//    public abstract  void playControl();

    @Override
    public void onFinishFrame(Viewport viewport) {
        int x = frameBuffer.getWidth() / 2;
        int y = frameBuffer.getHeight() / 2;
        SimpleVector dir = Interact2D.reproject2D3DWS(world.getCamera(), frameBuffer, x, y).normalize();

        SimpleVector targetDir = SimpleVector.create(0, -1, 0);
        float angle = dir.calcAngle(targetDir.normalize());
        if (angle < Math.PI / 6)
        {
            gazePointer.setVisibility(true);
            //button.setVisibility(true);

            gazePointer.update();
        }
        else
        {
            gazePointer.setVisibility(false);
            //button.setVisibility(false);
        }
        Object[] res = world.calcMinDistanceAndObject3D(world.getCamera().getPosition(), dir, 10000);
        if (res[1] != null) {
            Object3D obj = (Object3D) res[1];

            Object o = obj.getUserObject();
            if (o != null && o instanceof Button)
            {
                objectPicker.pickObject(gazePointer, (Button)o);
                return;
            }
        }
        objectPicker.pickObject(gazePointer, null);
    }
    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.exit_image){
            finish();
        }else if(v.getId() == R.id.vr_image){
            if (cardboardView.getVRMode()) {
                cardboardView.setVRModeEnabled(false);
                mVRType.setImageResource(R.mipmap.white_vr);
            } else {
                cardboardView.setVRModeEnabled(true);
                mVRType.setImageResource(R.mipmap.green_vr);
            }
        }else if(v.getId() == R.id.mediacontroller_play_pause){
//            playControl();

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                mPlayStatus.setImageResource(R.mipmap.mediacontroller_play);
            } else {
                mediaPlayer.start();
                mPlayStatus.setImageResource(R.mipmap.mediacontroller_pause);
            }
        }else if(v.getId() == R.id.cardboard_view){
            if (mTopParent.getVisibility() == View.VISIBLE) {
                handler.removeCallbacks(showTopView);
                mTopParent.setVisibility(View.GONE);
                mPlayStatus.setVisibility(View.GONE);
            } else if (mTopParent.getVisibility() == View.GONE) {
                mTopParent.setVisibility(View.VISIBLE);
                mPlayStatus.setVisibility(View.VISIBLE);
                handler.removeCallbacks(showTopView);
                handler.postDelayed(showTopView, 3000);
            }
        }
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
    private LinearLayout mTopParent;
    private Handler handler = new Handler();
    private Runnable showTopView = new Runnable() {
        @Override
        public void run() {
            if (!VRVideoPlayActivity.this.isFinishing()) {
                mTopParent.setVisibility(View.GONE);
                mPlayStatus.setVisibility(View.GONE);
            }
        }
    };
}