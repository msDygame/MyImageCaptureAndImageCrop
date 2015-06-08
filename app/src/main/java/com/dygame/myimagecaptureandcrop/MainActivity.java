package com.dygame.myimagecaptureandcrop;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Sample to use ACTION_IMAGE_CAPTURE(use camera take a capture) and com.android.camera.action.CROP(camera image crop).
 */
public class MainActivity extends ActionBarActivity
{
    protected String TAG = "" ;
    Button pImageCapture ;
    Button pImageLocal ;
    Button pQuit ;
    ImageView ivMainIcon ;
    //define for onActivityResult requestCode
    public static final int REQUEST_CAMERA_CAPTURE = 100 ;
    public static final int REQUEST_CAMERA_RESULT = 101 ;
    public static final int REQUEST_GET_CONTENT_RESULT = 102 ;
    //temp
    File mCurrentPhotoFile ;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Uncaught Exception Handler(Crash Exception)
        MyCrashHandler pCrashHandler = MyCrashHandler.getInstance();
        pCrashHandler.init(getApplicationContext());
        TAG = pCrashHandler.getTag() ;
        //find  resource
        pImageCapture = (Button)findViewById(R.id.button1) ;
        pImageLocal = (Button)findViewById(R.id.button2) ;
        pQuit = (Button)findViewById(R.id.button3) ;
        ivMainIcon = (ImageView)findViewById(R.id.imageView1) ;
        //OnClickListener
        pImageCapture.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //DEBUG
                Log.i(TAG, MyMemoryTool.getLowMemoryThreshold(MainActivity.this)) ;
                try {
                    // 自拍頭像->啟動照相機拍照，並將相片存在指定的檔案中
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);//相當於"android.media.action.IMAGE_CAPTURE"
                    mCurrentPhotoFile = new File("mnt/sdcard/DCIM/Camera/",getPhotoFileName());
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCurrentPhotoFile));
                    startActivityForResult(intent,REQUEST_CAMERA_CAPTURE);
                }
                catch(ActivityNotFoundException e)
                {
                    //不支持攝像頭的拍攝功能
                    e.printStackTrace();
                }
            }
        });
        pImageLocal.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //本地圖庫
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");//選取檔案
                try
                {
                    startActivityForResult(intent, REQUEST_GET_CONTENT_RESULT);
                }
                catch (ActivityNotFoundException e)
                {
                    e.printStackTrace();
                }
            }
        });
        pQuit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        }) ;
        //set defult image
        getDefaultImage() ;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result)
    {
        //本地圖片.
        if (requestCode == 1)
        {

        }
        else if (requestCode == REQUEST_CAMERA_CAPTURE)
        {
            //自拍頭像. 調用相機拍照之後,啟動裁剪照片
            //DEBUG
            Log.i(TAG, MyMemoryTool.getLowMemoryThreshold(MainActivity.this)) ;
            //Add the Photo to a Gallery
            Uri imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver cr = getContentResolver();
            Uri fileUri = Uri.fromFile(mCurrentPhotoFile);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,fileUri));//發送廣播掃瞄文件，來通知系統更新ContentProvider(?)，由於掃瞄工作是在MediaScanner服務中進行的，因此不會阻塞當前程序進程。
            try
            {
                Thread.sleep(3000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            Cursor cursor = cr.query(imgUri, null,MediaStore.Images.Media.DISPLAY_NAME + "='"+ mCurrentPhotoFile.getName() + "'",null, null);
            Uri uri = null;
            if (cursor != null && cursor.getCount() > 0)
            {
                cursor.moveToLast();
                long id = cursor.getLong(0);
                uri = ContentUris.withAppendedId(imgUri, id);
            }
            final Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(uri, "image/*");
            intent.putExtra("crop", "true");//裁剪功能
            //裁剪後輸出圖片的尺寸大小
           //intent.putExtra("outputX", 380);//這會限定圖片為380x500
           //intent.putExtra("outputY", 500);
            //切大照片,有可能因為超過傳回內存的容量16MB,會有問題,(Bitmap預設是ARGB_8888，1920x1080x4=8294400=8MB)
            //原因是因為Android允許你使用return-data拿資料回來,再用(Bitmap)extras.getParcelable("data")拿到圖片
            //檔案太大的解決辦法:不要讓Intent帶檔案回來,自創建檔案,使用uri方法去連結它
            intent.putExtra("return-data", true);//要帶檔案回來
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            this.startActivityForResult(intent, REQUEST_CAMERA_RESULT);
        }
        // 自拍頭像. 裁剪照片之後, 取得裁剪後的照片
        else if (requestCode == REQUEST_CAMERA_RESULT)
        {
            if (result != null)//要檢查,因為裁剪時按cancel回來也會來此,但result == null
            {
                //DEBUG
                Log.i(TAG, MyMemoryTool.getLowMemoryThreshold(MainActivity.this)) ;
                //取得裁剪後的照片
                Bitmap cameraBitmap = (Bitmap) result.getExtras().get("data");
                //將裁剪後的照片 存檔,並返回uri,預設存為/data/data/com.dygame.mobile3/cache/cropped.jpg
                //原始的圖片並沒有被裁剪
                Uri uri = getImageUri(this,cameraBitmap) ;
                //自拍頭像->修改頭像
                ivMainIcon.setImageBitmap(cameraBitmap);
/*
                //20150608@, 沒效果?
                //如果是一般的URI
                final String scheme = result.getData().getScheme();
                String path = ".";
                if ("content".equals(scheme))
                {
                    ///如果是內容URI
                    Cursor cursor = null;
                    final String[] projection = { MediaStore.MediaColumns.DATA };
                    cursor = this.getContentResolver().query(result.getData(), projection, null, null, null);
                    if (cursor != null && cursor.moveToFirst())
                    {
                        final int index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        path = cursor.getString(index) ;
                    }
                }
                else if ("file".equals(scheme))
                {
                    ///如果是檔案URI
                    path = result.getData().getPath();
                }
                //DEBUG
                Log.i(TAG,"original uri="+path) ;
*/
            Log.i(TAG,"Bmp uri="+uri.toString()) ;
            }
        }
        //本地圖片選擇後
        else if (requestCode == REQUEST_GET_CONTENT_RESULT)
        {
            if (result != null)//沒有選擇圖片的話會null
            {
                Uri uri = result.getData();
                final Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(uri, "image/*");
                intent.putExtra("crop", "true");//裁剪功能
                intent.putExtra("return-data", true);//要帶檔案回來
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                this.startActivityForResult(intent, REQUEST_CAMERA_RESULT);
            }
        }
        super.onActivityResult(requestCode, resultCode, result) ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * 自拍頭像檔名
     */
    private String getPhotoFileName()
    {
        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss");
        Date date = new Date(System.currentTimeMillis());
        String time = "IMG-" + formatter.format(date) + ".jpg" ;
        return time ;
    }
    /**
     *  transform a Bitmap into a Uri
     * @throws FileNotFoundException
     */
    public Uri getImageUri(Context inContext, Bitmap inImage)
    {
        String cachePath = null;
        cachePath = getCachePath(inContext) ;
        File sImage = new File(cachePath, "cropped.jpg");
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(sImage);
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);//100 to keep full quality of the image
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //DEBUG
        Log.i(TAG, Uri.fromFile(sImage).toString()) ;
        return Uri.fromFile(sImage);
    }
    static public String getCachePath(Context inContext)
    {
		String cachePath = null;
		 //SD卡存在或者SD卡不可被移除(外部儲存體內建在系統中，無法被移除)
		 if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
			 // /mnt/sdcard/Android/data/com.dygame.mobile3/cache/
		 	cachePath = inContext.getExternalCacheDir().getPath();
		 }
		 else
         {
             //SD卡不存在調用內部儲存空間
			 // /data/data/com.dygame.mobile3/cache/
			 cachePath = inContext.getCacheDir().getPath();
		 }
		 return cachePath ;
    }
    /**
     * 預設頭像
     */
    public void getDefaultImage()
    {
        String cachePath = getCachePath(MainActivity.this) ;
        File sImage = new File(cachePath, "cropped.jpg");
        //DEBUG
        Log.i(TAG,"DefaultImagePath="+sImage.toString()) ;
        if (sImage.exists())
        {
            Uri destination = Uri.fromFile(sImage) ;
            //DEBUG
            Log.i(TAG,destination.toString()) ;
            ivMainIcon.setImageURI(destination);
        }
    }
}
