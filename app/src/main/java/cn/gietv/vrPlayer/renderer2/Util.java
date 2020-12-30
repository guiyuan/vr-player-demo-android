package cn.gietv.vrPlayer.renderer2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.List;
import java.util.NoSuchElementException;

public class Util {

	@SuppressWarnings("unchecked")
	public static <T extends View> T findView(View view, int id) {
		View v = view.findViewById(id);
		if (v == null)
			throw new NoSuchElementException("view with id " + id + " not found");
		return (T) v;
	}

	@SuppressWarnings("unchecked")
	public static <T extends View> T findView(Activity activity, int id) {
		View v = activity.findViewById(id);
		if (v == null)
			throw new NoSuchElementException("view with id " + id + " not found");
		return (T) v;
	}

	public static void writeToFile(File file, String data) throws IOException {
		FileOutputStream stream = new FileOutputStream(file);
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
			outputStreamWriter.write(data);
			outputStreamWriter.close();
		} finally {
			stream.close();
		}
	}

	public static SimpleVector interpolate(SimpleVector source, SimpleVector dest, float weight, SimpleVector fill) {
		if (fill == null)
			fill = new SimpleVector();

		fill.x = (1-weight) * source.x + weight * dest.x;
		fill.y = (1-weight) * source.y + weight * dest.y;
		fill.z = (1-weight) * source.z + weight * dest.z;

		return fill;
	}

	public static float[] average(List<float[]> list, float[] fill) {
		final int size = list.get(0).length;

		if (fill == null)
			fill = new float[size];

		for (float[] array : list) {
			for (int i = 0; i < size; i++) {
				fill[i] += array[i];
			}
		}
		for (int i = 0; i < size; i++) {
			fill[i] /= list.size();
		}
		return fill;
	}

	/** returns the minimum 2^n size greater than or equal to given size. */
	public static int get2NSize(int imageSize) {
		int size = 2;
		while (size < imageSize) {
			size = size << 1; // x2
		}
		return size;
	}

	public static String readContents(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		String contents = "";
		String line = null;
		while ((line = reader.readLine()) != null) {
			contents += line + "\n";
		}
		return contents;
	}

	public static Object3D load3DS(Context context, int rawResourceId, float scale) {
		return load3DS(context.getResources().openRawResource(rawResourceId), scale);
	}

	public static Object3D load3DS(InputStream in, float scale) {
		Object3D[] array = Loader.load3DS(in, scale);
		if (array.length != 1)
			throw new IllegalArgumentException("array length:" + array.length);

		return prepare3DS(array[0]);
	}

	public static Object3D[] load3DSArray(Context context, int rawResourceId, float scale) {
		return load3DSArray(context.getResources().openRawResource(rawResourceId), scale);
	}

	public static Object3D[] load3DSArray(InputStream in, float scale) {
		Object3D[] array = Loader.load3DS(in, scale);
		for (Object3D object : array) {
			prepare3DS(object);
		}
		return array;
	}

	private static Object3D prepare3DS(Object3D object) {
		object.rotateX( (float) -Math.PI / 2 );
		//object.rotateZ( (float) Math.PI );
		object.rotateMesh();
		object.getRotationMatrix().setIdentity();
//		object.build();
		return object;
	}

	public static Object3D loadOBJ(Context context, int rawResourceId, float scale) {
		return loadOBJ(context.getResources().openRawResource(rawResourceId), scale);
	}

	public static Object3D loadOBJ(InputStream in, float scale) {
		Object3D[] array = Loader.loadOBJ(in, null, scale);
		if (array.length != 1)
			throw new IllegalArgumentException("array length:" + array.length);

		Object3D object = array[0];
		object.rotateX((float)Math.PI);
		object.rotateMesh();
		object.getRotationMatrix().setIdentity();
//		object.build();

		return object;
	}

//	public static void scaleObject(Object3D object, float xScale, float yScale, float zScale) {
//		object.getMesh().setVertexController(new MeshScaler(xScale, yScale, zScale),
//				IVertexController.PRESERVE_SOURCE_MESH);
//
//		object.getMesh().applyVertexController();
//		object.getMesh().removeVertexController();
//	}

	public static void showInfoMessage(Context context, String message) {
		showMessage(context, "Info", message);
	}

	public static void showErrorMessage(Context context, String message) {
		showMessage(context, "Error", message);
	}

	public static void showMessage(Context context, String title, String message) {
		new AlertDialog.Builder(context)
				.setMessage(message)
				.setTitle(title)
				.setPositiveButton("Ok", null)
				.create().show();
	}
	public final static String MD5(byte[] buffer) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		try {
			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(buffer);
			byte[] md = mdTemp.digest();
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
			return null;
		}
	}
}
