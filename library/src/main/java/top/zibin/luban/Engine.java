package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Responsible for starting compress and managing active and cached resources.
 */
class Engine {
  private InputStreamProvider srcImg;
  private File tagImg;
  private int srcWidth;
  private int srcHeight;
  private boolean focusAlpha;

    // add 2019/12/5
  private int quality;
  private final int LONG_SIZE;//最小长边尺寸（分辨率）
  private final int MAX_SIZE;//最大 size (kb)
  private final int MIN_QUALITY;//最小 quality （0 ~ 100）

  Engine(InputStreamProvider srcImg, File tagImg, boolean focusAlpha,int maxSize, int minQuality, int longSize) throws IOException {
    this.tagImg = tagImg;
    this.srcImg = srcImg;
    this.focusAlpha = focusAlpha;
    this.MAX_SIZE = maxSize;
    this.MIN_QUALITY = minQuality;
    this.LONG_SIZE = longSize;

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 1;

    BitmapFactory.decodeStream(srcImg.open(), null, options);
    this.srcWidth = options.outWidth;
    this.srcHeight = options.outHeight;
  }

  @Deprecated
  private int computeSize() {
    srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
    srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

    int longSide = Math.max(srcWidth, srcHeight);
    int shortSide = Math.min(srcWidth, srcHeight);

    float scale = ((float) shortSide / longSide);
    if (scale <= 1 && scale > 0.5625) {
      if (longSide < 1664) {
        return 1;
      } else if (longSide < 4990) {
        return 2;
      } else if (longSide > 4990 && longSide < 10240) {
        return 4;
      } else {
        return longSide / 1280;
      }
    } else if (scale <= 0.5625 && scale > 0.5) {
      return longSide / 1280 == 0 ? 1 : (longSide / 1280);
    } else {
      return (int) Math.ceil(longSide / (1280.0 / scale));
    }
  }

  /**
   * new
   */
  private int computeSize2() {
    srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
    srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

    int longSide = Math.max(srcWidth, srcHeight);
    int shortSide = Math.min(srcWidth, srcHeight);

    int inSampleSize = 1;
    if (longSide > LONG_SIZE) {
      int halfLong = longSide / 2;
      while ((halfLong / inSampleSize) >= LONG_SIZE) {
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }

  private Bitmap rotatingImage(Bitmap bitmap, int angle) {
    Matrix matrix = new Matrix();

    matrix.postRotate(angle);

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
  }

  File compress() throws IOException {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = computeSize2();

    Bitmap tagBitmap = BitmapFactory.decodeStream(srcImg.open(), null, options);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    if (Checker.SINGLE.isJPG(srcImg.open())) {
      tagBitmap = rotatingImage(tagBitmap, Checker.SINGLE.getOrientation(srcImg.open()));
    }
    Bitmap.CompressFormat format = focusAlpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
//    tagBitmap.compress(format, quality, stream);

    //计算压缩质量 从90开始 每次递减10
    int quality = 90;
    tagBitmap.compress(format, quality, stream);
    Log.d("Engine", "compress: quality=" + quality + " ,size=" + stream.toByteArray().length/1024 + "kb");

    while (stream.toByteArray().length >> 10 > MAX_SIZE && quality >= MIN_QUALITY) {
      quality -= 10;
      stream.reset();
      tagBitmap.compress(format, quality, stream);
      Log.d("Engine", "compress: quality=" + quality + " ,size=" + stream.toByteArray().length/1024 + "kb");
    }


    tagBitmap.recycle();

    FileOutputStream fos = new FileOutputStream(tagImg);
    fos.write(stream.toByteArray());
    fos.flush();
    fos.close();
    stream.close();

    return tagImg;
  }

  File compress2() throws IOException {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = computeSize2();

    Bitmap tagBitmap = BitmapFactory.decodeStream(srcImg.open(), null, options);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    if (Checker.SINGLE.isJPG(srcImg.open())) {
      tagBitmap = rotatingImage(tagBitmap, Checker.SINGLE.getOrientation(srcImg.open()));
    }
    tagBitmap.compress(focusAlpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, quality, stream);
    tagBitmap.recycle();

    FileOutputStream fos = new FileOutputStream(tagImg);
    fos.write(stream.toByteArray());
    fos.flush();
    fos.close();
    stream.close();

    return tagImg;
  }
}