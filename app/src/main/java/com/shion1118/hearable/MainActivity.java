package com.shion1118.hearable;

import jp.ne.docomo.smt.dev.characterrecognition.SceneCharacterRecognition;
import jp.ne.docomo.smt.dev.characterrecognition.constants.ImageContentType;
import jp.ne.docomo.smt.dev.characterrecognition.constants.Lang;
import jp.ne.docomo.smt.dev.characterrecognition.data.CharacterRecognitionMessageData;
import jp.ne.docomo.smt.dev.characterrecognition.data.CharacterRecognitionStatusData;
import jp.ne.docomo.smt.dev.characterrecognition.param.CharacterRecognitionRequestParam;
import com.shion1118.hearable.workflow.RecognitionAsyncTaskParam;
import jp.ne.docomo.smt.dev.common.exception.SdkException;
import jp.ne.docomo.smt.dev.common.exception.ServerException;
import jp.ne.docomo.smt.dev.common.http.AuthApiKey;
import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements OnClickListener  {

    static final String APIKEY = "";

    private RecognitionAsyncTask task;

    int RESULT_PICK_FILENAME = 1;

    // 認識ジョブID
    public static String jobid = "";

    // インテントキー
    static final String INTENT_JOBID_KEY = "jobid";

    //使用言語
    public static Lang lang = null;

    // 結果取得ボタン
    static Button resultBtn = null;

    // リクエスト結果テキスト
    static TextView result;

    // 結果表示
    private class RecognitionAsyncTask extends AsyncTask<RecognitionAsyncTaskParam, Integer, CharacterRecognitionStatusData> {
        private AlertDialog.Builder _dlg;

        private boolean isSdkException = false;
        private String exceptionMessage = null;

        public RecognitionAsyncTask(AlertDialog.Builder dlg) {
            super();
            _dlg = dlg;
        }

        @Override
        protected CharacterRecognitionStatusData doInBackground(RecognitionAsyncTaskParam... params) {
            CharacterRecognitionStatusData statusData = null;
            RecognitionAsyncTaskParam req_param = params[0];
            try {

                // パラメータを設定する
                CharacterRecognitionRequestParam param = new CharacterRecognitionRequestParam();
                param.setLang(req_param.getLang());
                param.setFilePath(req_param.getImagePath());
                param.setImageContentType(req_param.getImageType());

                // 情景画像認識要求のリクエスト送信
                SceneCharacterRecognition Recognition = new SceneCharacterRecognition();
                statusData = Recognition.recognize(param);

            } catch (SdkException ex) {
                isSdkException = true;
                exceptionMessage = "ErrorCode: " + ex.getErrorCode() + "\nMessage: " + ex.getMessage();

            } catch (ServerException ex) {
                exceptionMessage = "ErrorCode: " + ex.getErrorCode() + "\nMessage: " + ex.getMessage();
            }

            return statusData;
        }

        @Override
        protected void onCancelled() {
        }

        @Override
        protected void onPostExecute(CharacterRecognitionStatusData statusData) {

            if(statusData == null){
                if(isSdkException){
                    _dlg.setTitle("SdkException 発生");

                }else{
                    _dlg.setTitle("ServerException 発生");
                }
                _dlg.setMessage(exceptionMessage + " ");
                _dlg.show();
                jobid = "";

            }else{

                // 結果表示
                _dlg.setTitle("処理結果");

                StringBuffer sb = new StringBuffer();
                sb.append("認識ジョブの出力 :\n");
                sb.append("　認識ジョブID : " + statusData.getJob().getId() + "\n");
                sb.append("　進行状況 : " + statusData.getJob().getStatus() + "\n");
                sb.append("　リクエスト受け付け時刻 : " + statusData.getJob().getQueueTime() + "\n");

                // メッセージの出力
                CharacterRecognitionMessageData message = statusData.getMessage();
                if (message != null) {
                    sb.append("メッセージ : " + message.getText() + "\n");
                }

                result.setText(sb.toString());

                // ソフトキーボードを非表示にする
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(result.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                resultBtn.setEnabled(true);
                jobid = statusData.getJob().getId();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main);

        AuthApiKey.initializeAuth(MainActivity.APIKEY);

        result = (TextView)findViewById(R.id.result);

        // ボタンイベントの設定
        Button btn = (Button)findViewById(R.id.button_ocr);
        btn.setOnClickListener(this);
        resultBtn = (Button)findViewById(R.id.button_go);
        resultBtn.setEnabled(false);
        resultBtn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.button_lastphoto);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.button_choosephoto);
        btn.setOnClickListener(this);

        // ソフトキーボードを非表示にする
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(result.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(task != null){
            task.cancel(true);
        }
    }


    // インターフェース実装
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.button_ocr:
                pushExecButton();
                break;
            case R.id.button_go:
                intent = new Intent(this, OCR.class);
                intent.putExtra(INTENT_JOBID_KEY, this.jobid);
                startActivity(intent);
                break;
            case R.id.button_lastphoto:
                EditText editText = (EditText)findViewById(R.id.edit_path);
                editText.setText(LastPhoto());
                break;
            case R.id.button_choosephoto:
                Intent i = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_PICK_FILENAME);
                break;
        }
    }

    private void pushExecButton() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
        RecognitionAsyncTaskParam param = new RecognitionAsyncTaskParam();

        // パラメータ取得
        // イメージパス
        EditText editText = (EditText)findViewById(R.id.edit_path);

        // 2値化
        Mat mat = new Mat();
        Bitmap bmp = BitmapFactory.decodeFile(editText.getText().toString());
        Utils.bitmapToMat(bmp,mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(mat, mat, 0, 255, Imgproc.THRESH_TOZERO | Imgproc.THRESH_OTSU);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGRA,4);
        Utils.matToBitmap(mat, bmp);
        mat.release();
        try{
            //ローカルファイルへ保存
            FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/DCIM/opencv.jpg");
            bmp.compress(Bitmap.CompressFormat.JPEG,100,out);
            out.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        // 2値化後の写真を取得
        param.setImagePath(Environment.getExternalStorageDirectory().getPath() + "/DCIM/opencv.jpg");


        // 画像種別取得
        RadioButton radio = (RadioButton)findViewById(R.id.radio_jpg);
        ImageContentType imageType = ImageContentType.IMAGE_JPEG;
        if (radio.isChecked() == false) {
            imageType = ImageContentType.IMAGE_PNG;
        }
        param.setImageType(imageType);

        // 認識言語取得
        radio = (RadioButton)findViewById(R.id.radio_jpn);
        lang = Lang.CHARACTERS_JP;
        if (radio.isChecked() == false) {
            lang = Lang.CHARACTERS_EN;
        }
        param.setLang(lang);

        // 実行
        task = new RecognitionAsyncTask(dlg);
        task.execute(param);
    }

    // ギャラリーから画像選択
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_PICK_FILENAME
                && resultCode == RESULT_OK
                && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(
                    selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex
                    = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            EditText editText = (EditText)findViewById(R.id.edit_path);
            editText.setText(picturePath);
        }
    }

    // 最後の写真取得
    private String LastPhoto(){
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        Cursor cursor = getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, projection[3] + " DESC");
        if (cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            cursor.close();
            return imageLocation;
        }
        cursor.close();
        return null;
    }

    // Open CV 設定
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("INFO", "OpenCV loaded successfully");
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
    }};

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }



}