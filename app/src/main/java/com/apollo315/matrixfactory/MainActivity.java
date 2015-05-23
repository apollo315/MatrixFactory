package com.apollo315.matrixfactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.Toast;
//这是从图片特效包导入的
import com.apollo315.imageprocess.ProcessActivity;



public class MainActivity extends Activity {

    private GridView gridView1;                 //网格显示缩略图
    private Button buttonPublish;              //发布按钮
    private final int IMAGE_OPEN = 1;      //打开图片标记
    private final int GET_DATA = 2;           //获取处理后图片标记
    private final int TAKE_PHOTO = 3;       //拍照标记
    private String pathImage;                     //选择图片路径
    private Bitmap bmp;                             //导入临时图片
    private Uri imageUri;                            //拍照Uri
    private String pathTakePhoto;              //拍照路径
    private ProgressDialog mpDialog;         //进度对话框
    private int count = 0;                           //计算上传图片个数 线程调用
    private EditText editText;                      //内容
    private int flagThread = 0;                    //线程循环标记变量 否则会上个线程没执行完就进行下面的
    private int flagThreadUpload = 0;         //上传图片控制变量
    private int flagThreadDialog = 0;          //对话框标记变量

    //获取图片上传URL路径 文件夹名+时间命名图片
    private String[] urlPicture;
    //存储Bmp图像
    private ArrayList<HashMap<String, Object>> imageItem;
    //适配器
    private SimpleAdapter simpleAdapter;
    //插入PublishId通过Json解析
    private String publishIdByJson;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         * 防止键盘挡住输入框
         * 不希望遮挡设置activity属性 android:windowSoftInputMode="adjustPan"
         * 希望动态调整高度 android:windowSoftInputMode="adjustResize"
         */
        getWindow().setSoftInputMode(WindowManager.LayoutParams.
                SOFT_INPUT_ADJUST_PAN);
        //锁定屏幕
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        //获取控件对象
        gridView1 = (GridView) findViewById(R.id.gridView1);
        buttonPublish = (Button) findViewById(R.id.button1);
        editText = (EditText) findViewById(R.id.editText1);

        //发布内容
        buttonPublish.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

        		/*
        		 * 上传图片 进度条显示
        		 * String path = "/storage/emulated/0/DCIM/Camera/lennaFromSystem.jpg";
        		 * upload_SSP_Pic(path,"ranmei");
        		 * Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
        		 */
                //判断是否添加图片
                if(imageItem.size()==1) {
                    Toast.makeText(MainActivity.this, "没有图片需要上传", Toast.LENGTH_SHORT).show();
                    return;
                }

                //消息提示
                Toast.makeText(MainActivity.this, "发布成功", Toast.LENGTH_SHORT).show();
            }
        });

        /*
         * 载入默认图片添加图片加号
         * 通过适配器实现
         * SimpleAdapter参数imageItem为数据源 R.layout.griditem_addpic为布局
         */
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.gridview_addpic); //加号
        imageItem = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("itemImage", bmp);
        map.put("pathImage", "add_pic");
        imageItem.add(map);
        simpleAdapter = new SimpleAdapter(this,
                imageItem, R.layout.griditem_addpic,
                new String[] { "itemImage"}, new int[] { R.id.imageView1});
        /*
         * HashMap载入bmp图片在GridView中不显示,但是如果载入资源ID能显示 如
         * map.put("itemImage", R.drawable.img);
         * 解决方法:
         *              1.自定义继承BaseAdapter实现
         *              2.ViewBinder()接口实现
         *  参考 http://blog.csdn.net/admin_/article/details/7257901
         */
        simpleAdapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data,
                                        String textRepresentation) {
                // TODO Auto-generated method stub
                if(view instanceof ImageView && data instanceof Bitmap){
                    ImageView i = (ImageView)view;
                    i.setImageBitmap((Bitmap) data);
                    return true;
                }
                return false;
            }
        });
        gridView1.setAdapter(simpleAdapter);

        /*
         * 监听GridView点击事件
         * 报错:该函数必须抽象方法 故需要手动导入import android.view.View;
         */
        gridView1.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id)
            {
                if( imageItem.size() == 10) { //第一张为默认图片
                    Toast.makeText(MainActivity.this, "图片数9张已满", Toast.LENGTH_SHORT).show();
                }
                else if(position == 0) { //点击图片位置为+ 0对应0张图片
                    //Toast.makeText(MainActivity.this, "添加图片", Toast.LENGTH_SHORT).show();
                    AddImageDialog();
                }
                else {
                    DeleteDialog(position);
                    //Toast.makeText(MainActivity.this, "点击第" + (position + 1) + " 号图片",
                    //		Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    //获取图片路径 响应startActivityForResult
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //打开图片
        if(resultCode==RESULT_OK && requestCode==IMAGE_OPEN) {
            Uri uri = data.getData();
            if (!TextUtils.isEmpty(uri.getAuthority())) {
                //查询选择图片
                Cursor cursor = getContentResolver().query(
                        uri,
                        new String[] { MediaStore.Images.Media.DATA },
                        null,
                        null,
                        null);
                //返回 没找到选择图片
                if (null == cursor) {
                    return;
                }
                //光标移动至开头 获取图片路径
                cursor.moveToFirst();
                String path = cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA));
                //向处理活动传递数据
                //Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, ProcessActivity.class); //主活动->处理活动
                intent.putExtra("path", path);
                //startActivity(intent);
                startActivityForResult(intent, GET_DATA);
            } else {
                Intent intent = new Intent(this, ProcessActivity.class); //主活动->处理活动
                intent.putExtra("path", uri.getPath());
                //startActivity(intent);
                startActivityForResult(intent, GET_DATA);
            }
        }  //end if 打开图片
        //获取图片
        if(resultCode==RESULT_OK && requestCode==GET_DATA) {
            //获取传递的处理图片在onResume中显示
            pathImage = data.getStringExtra("pathProcess");
        }
        //拍照
        if(resultCode==RESULT_OK && requestCode==TAKE_PHOTO) {
            Intent intent = new Intent("com.android.camera.action.CROP"); //剪裁
            intent.setDataAndType(imageUri, "image/*");
            intent.putExtra("scale", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            //广播刷新相册
            Intent intentBc = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intentBc.setData(imageUri);
            this.sendBroadcast(intentBc);
            //向处理活动传递数据
            Intent intentPut = new Intent(this, ProcessActivity.class); //主活动->处理活动
            intentPut.putExtra("path", pathTakePhoto);
            //startActivity(intent);
            startActivityForResult(intentPut, GET_DATA);
        }
    }

    //刷新图片
    @Override
    protected void onResume() {
        super.onResume();
        //获取传递的处理图片在onResume中显示
        //Intent intent = getIntent();
        //pathImage = intent.getStringExtra("pathProcess");
        //适配器动态显示图片
        if(!TextUtils.isEmpty(pathImage)){
            Bitmap addbmp=BitmapFactory.decodeFile(pathImage);
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("itemImage", addbmp);
            map.put("pathImage", pathImage);
            imageItem.add(map);
            simpleAdapter = new SimpleAdapter(this,
                    imageItem, R.layout.griditem_addpic,
                    new String[] { "itemImage"}, new int[] { R.id.imageView1});
            //接口载入图片
            simpleAdapter.setViewBinder(new ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data,
                                            String textRepresentation) {
                    // TODO Auto-generated method stub
                    if (view instanceof ImageView && data instanceof Bitmap) {
                        ImageView i = (ImageView) view;
                        i.setImageBitmap((Bitmap) data);
                        return true;
                    }
                    return false;
                }
            });
            gridView1.setAdapter(simpleAdapter);
            simpleAdapter.notifyDataSetChanged();
            //刷新后释放防止手机休眠后自动添加
            pathImage = null;
        }
    }

    /*
     * Dialog对话框提示用户删除操作
     * position为删除图片位置
     */
    protected void DeleteDialog(final int position) {
        AlertDialog.Builder builder = new Builder(MainActivity.this);
        builder.setMessage("确认移除已添加图片吗？");
        builder.setTitle("提示");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                imageItem.remove(position);
                simpleAdapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    /*
     * 添加图片 可通过本地添加、拍照添加
     */
    protected void AddImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("添加图片");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setCancelable(false); //不响应back按钮
        ////怎么让字符串居中呢？这是个问题！2015-05-19
        //百度上说只能用setAdapter和setView来自定义视图，太麻烦了，不管了，先搞重点
        builder.setItems(new String[] {"本地相册选择","手机相机添加","取消"},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        switch(which) {
                            case 0: //本地相册
                                dialog.dismiss();
                                Intent intent = new Intent(Intent.ACTION_PICK,
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(intent, IMAGE_OPEN);
                                //通过onResume()刷新数据
                                break;
                            case 1: //手机相机
                                dialog.dismiss();
                                File outputImage = new File(Environment.getExternalStorageDirectory(), "suishoupai_image.jpg");
                                pathTakePhoto = outputImage.toString();
                                try {
                                    if(outputImage.exists()) {
                                        outputImage.delete();
                                    }
                                    outputImage.createNewFile();
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                                imageUri = Uri.fromFile(outputImage);
                                Intent intentPhoto = new Intent("android.media.action.IMAGE_CAPTURE"); //拍照
                                intentPhoto.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                startActivityForResult(intentPhoto, TAKE_PHOTO);
                                break;
                            case 2: //取消添加
                                dialog.dismiss();
                                break;
                            default:
                                break;
                        }
                    }
                });
        //显示对话框
        builder.create().show();
    }

    /**退出对话框
     *
     */
    private void exitDialog() {
        AlertDialog.Builder builder = new Builder(MainActivity.this);
        builder.setTitle("FBI WARNING！");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setMessage("\n确认退出矩阵工厂吗？\n");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                MainActivity.this.finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //创建并显示对话框
        builder.create().show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            exitDialog();
            //FL47没有这句，这是我自己加的。
            //我觉得这里应该有这句，这样理解对吗？
            return true;
        }
        return false;
    }

    /*
         * 开启上传图片的线程
         * 第一个参数是文件完整路径（包括文件名）
         * 第二个参数是要放在服务器哪个文件夹下
         */
    private void upload_SSP_Pic(final String path,final String dirname) {}

    /*
     * 插入表 参数SQL语句 Type=1表示插入 2查询
     */
    private void SavePublish(final String type,final String sqlexe) {}

    /*
     * 解析SQL查询数据
     */
    private void jsonjiexi(String jsondata) {}


  	/*
  	 * End
  	 */
}
