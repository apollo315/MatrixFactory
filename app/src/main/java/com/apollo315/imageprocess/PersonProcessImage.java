package com.apollo315.imageprocess;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;

//自定义类实现图像交互 包括:上传、保存、取消 
//原来是Person美白,但后来感觉处理就是多几个效果,所幸做成保存
public class PersonProcessImage
{
    private Bitmap mBitmap;
    public String pathPicture;    //图片路径 传递给ProcessActivity和MainActivity

    //构造方法
    public PersonProcessImage(Bitmap bmp)
    {
        mBitmap = bmp;
    }

    /*
     * 1.图像保存到SD卡
     * 参数:Bitmap filePath 默认为时间命名
     * 如果保存失败返回错误,保存成功为null
     */
    public Uri saveBitmapToSD(Bitmap bmp) throws IOException
    {
        //图片时间命名
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String filename = format.format(date);
        //存储至DCIM文件夹下
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        //File path = new File(Environment.getExternalStorageDirectory(), "suishoupai");
        if (!path.exists()) {
            path.mkdir();
        }
        File imagePath = new File(path, filename +".jpg");
        imagePath.createNewFile();
        FileOutputStream fos = new FileOutputStream(imagePath);
        //质量压缩方法 格式JPG 100表示不压缩 压缩后保存至bos
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
		/*
		 * 注意:不是不显示图片 而是相册缓冲较慢 手机重新启动后才能显示图片
		 */
        return Uri.fromFile(imagePath);
    }

    /*
     * 2.图像上传 先保存后上传
     * 参数:Bitmap filePath 默认为时间命名
     * 如果保存失败返回错误,保存成功为null
     */
    public Uri loadBitmap(Bitmap bmp) throws IOException
    {
        //图片时间命名
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String filename = format.format(date);
        //存储至DCIM文件夹下
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        //File path = new File(Environment.getExternalStorageDirectory(), "suishoupai");
        if (!path.exists()) {
            path.mkdir();
        }
        File imagePath = new File(path, filename +".jpg");
        imagePath.createNewFile();
        FileOutputStream fos = new FileOutputStream(imagePath);
        //质量压缩方法 格式JPG 100表示不压缩 压缩后保存至bos
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
        //图片路径
        pathPicture = imagePath.toString();
		/*
		 * 注意:不是不显示图片 而是相册缓冲较慢 手机重新启动后才能显示图片
		 */
        return Uri.fromFile(imagePath);
    }


}
