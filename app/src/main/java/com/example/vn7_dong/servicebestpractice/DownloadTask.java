package com.example.vn7_dong.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;

import javax.xml.transform.OutputKeys;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer> {

    private static final  int TYPE_SUCCESS = 0;
    private static final  int TYPE_FAILED = 1;
    private static final  int TYPE_PAUSED = 2;
    private static final  int TYPE_CANCELED = 3;

    private DownloadListener downloadListener ;
    private  boolean isCanceled = false;
    private  boolean isPaused = false;

    private int lastProgress;

    public void DownloadTask(DownloadListener downloadListener){
        this.downloadListener = downloadListener;
    }


    @Override//...是变长变量,可以传一个或多个变量进来;如果只传一个参数的话就用params[0]获取到那一个参数,同理取得可能的更多的参数.
    protected  Integer doInBackground(String... param){
        InputStream in = null;
        RandomAccessFile randomAccessFile = null;
        File file = null;
        try {
            long downloadLength = 0;//记录已下载的文件长度；
            String downloadUrl = param[0];
            String filName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            //lastIndexOf()获取要搜索的字符、字符串最后次出现的位置
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            //DIRECTORY_DOWNLOADS //下载文件保存的位置
            //getExternalStoragePublicDirectory()--往sdcard中保存特定类型的内容
            file = new File(directory+filName);
            if(file.exists()){
                downloadLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);//自定义方法
            if(contentLength == 0){
                return TYPE_FAILED;
            }else if(contentLength == downloadLength){
                return TYPE_SUCCESS;
            }
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    //断电下载，指定从哪个字节开始下载
                    .addHeader("RANGE","byte="+downloadLength+"-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response !=null){
                in = response.body().byteStream();
                randomAccessFile = new RandomAccessFile(file,"rw");
                randomAccessFile.seek(downloadLength);//跳过已经下载的字节
                byte[] b = new byte[1024];
                int total = 0;
                int len;
                while((len = in.read(b))!=1){

                    if(isCanceled){
                        return  TYPE_CANCELED;
                    }else if (isPaused){
                        return TYPE_PAUSED;
                    }else {
                        total +=len;
                        randomAccessFile.write(b,0,len);
                        //计算已下载的百分比
                        int progress = (int)((total+downloadLength)*100/contentLength);
                        publishProgress(progress);
                    }
                    response.body().close();
                    return  TYPE_SUCCESS;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(in!=null){
                    in.close();
                }
                if (randomAccessFile !=null){
                    randomAccessFile.close();
                }
                if (isCanceled&&file!=null){
                    file.delete();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_SUCCESS;
    }

    @Override
    protected void  onProgressUpdate(Integer... value){
        int progress = value[0];
        if(progress>lastProgress){
            downloadListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status){
        switch (status){
            case TYPE_SUCCESS:
                downloadListener.onSuccess();
                break;
            case TYPE_FAILED:
                downloadListener.onFailed();
                break;
            case TYPE_PAUSED:
                downloadListener.onPaused();
                break;
            case TYPE_CANCELED:
                downloadListener.onCanceled();
                break;
            default:
                break;
        }
    }

    public  void pauseDownload(){
        isPaused = true;
    }
    public  void cancelDownload(){
        isCanceled = true;
    }
    private long getContentLength(String downloadUrl)throws IOException{
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if(  response != null && response.isSuccessful()){
            long contentLength = response.body().contentLength();
            response.body().close();
        }
        return 0;
    }
}
