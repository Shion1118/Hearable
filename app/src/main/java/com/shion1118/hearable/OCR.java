package com.shion1118.hearable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import jp.ne.docomo.smt.dev.characterrecognition.SceneCharacterRecognition;
import jp.ne.docomo.smt.dev.characterrecognition.constants.Lang;
import jp.ne.docomo.smt.dev.characterrecognition.data.CharacterRecognitionMessageData;
import jp.ne.docomo.smt.dev.characterrecognition.data.CharacterRecognitionResultData;
import jp.ne.docomo.smt.dev.characterrecognition.data.CharacterRecognitionWordData;
import jp.ne.docomo.smt.dev.characterrecognition.data.CharacterRecognitionWordsData;
import jp.ne.docomo.smt.dev.characterrecognition.param.CharacterRecognitionJobInfoRequestParam;
import com.shion1118.hearable.workflow.RecognitionAsyncTaskParam;
import jp.ne.docomo.smt.dev.common.exception.SdkException;
import jp.ne.docomo.smt.dev.common.exception.ServerException;

/**
 * Created by shion on 15/03/02.
 */
public class OCR extends Activity {
    private RecognitionResultAsyncTask task;
    private TextView result_get;
    private MediaPlayer mediaPlayer;
    TextView word;
    TextView translate_word;
    ListView listview;
    ArrayAdapter<String> adapter;
    String jobid;
    String item;
    Button speakbtn;

    private class RecognitionResultAsyncTask extends AsyncTask<RecognitionAsyncTaskParam, Integer, CharacterRecognitionResultData> {
        private AlertDialog.Builder _dlg;

        private boolean isSdkException = false;
        private String exceptionMessage = null;

        public RecognitionResultAsyncTask(AlertDialog.Builder dlg) {
            super();
            _dlg = dlg;
        }

        @Override
        protected CharacterRecognitionResultData doInBackground(RecognitionAsyncTaskParam... params) {
            CharacterRecognitionResultData resultData = null;
            RecognitionAsyncTaskParam req_param = params[0];
            try {

                // パラメータを設定する
                CharacterRecognitionJobInfoRequestParam param = new CharacterRecognitionJobInfoRequestParam();
                param.setJobId(req_param.getJobId());

                // 情景画像認識結果取得のリクエスト送信
                SceneCharacterRecognition recognize = new SceneCharacterRecognition();
                resultData = recognize.getResult(param);

            } catch (SdkException ex) {
                isSdkException = true;
                exceptionMessage = "ErrorCode: " + ex.getErrorCode() + "\nMessage: " + ex.getMessage();
            } catch (ServerException ex) {
                exceptionMessage = "ErrorCode: " + ex.getErrorCode() + "\nMessage: " + ex.getMessage();
            }
            return resultData;
        }

        @Override
        protected void onCancelled() {
        }

        @Override
        protected void onPostExecute(CharacterRecognitionResultData resultData) {

            if(resultData == null){
                if(isSdkException){
                    _dlg.setTitle("SdkException 発生");

                }else{
                    _dlg.setTitle("ServerException 発生");
                }
                _dlg.setMessage(exceptionMessage + " ");
                _dlg.show();

            }else{
                // 結果表示
                StringBuffer sb = new StringBuffer();

                //sb.append("認識ジョブの出力 :\n");
                //sb.append("　認識ジョブID : " + resultData.getJob().getId() + "\n");
                //sb.append("　進行状況 : " + resultData.getJob().getStatus() + "\n");
                //sb.append("　リクエスト受け付け時刻 : " + resultData.getJob().getQueueTime() + "\n");

                // 抽出した全ての単語の情報の出力
                CharacterRecognitionWordsData wordsData = resultData.getWords();
                if (wordsData != null) {
                    /*sb.append("単語情報 :\n");
                    sb.append("　単語の情報の数 :" + wordsData.getCount() + "\n");*/
                    ArrayList<CharacterRecognitionWordData> wordList = wordsData.getWord();
                    for (CharacterRecognitionWordData wordData : wordList) {
                        /*sb.append("　認識した単語の情報 :\n");
                        sb.append("　　認識した単語 : " + wordData.getText() + "\n");
                        sb.append("　　認識結果の信頼度 : " + wordData.getScore() + "\n");
                        sb.append("　　抽出した単語のカテゴリ : " + wordData.getCategory() + "\n");

                        sb.append("　　抽出した単語の形状を表す座標情報 :\n");
                        CharacterRecognitionShapeData shapeData = wordData.getShape();
                        ArrayList<CharacterRecognitionPointData> pointList = shapeData.getPoint();
                        if (pointList == null) {
                            continue;
                        }
                        sb.append("　　　頂点情報の数 : " + shapeData.getCount() + "\n");
                        sb.append("　　　頂点情報 : " + shapeData.getCount() + "\n");
                        for (CharacterRecognitionPointData pointData : pointList) {
                            sb.append("　　　　x座標, y座標(ピクセル単位) : " + pointData.getX() + ", " + pointData.getY() + "\n");
                        }*/
                        adapter.add(wordData.getText());
                    }
                }

                // メッセージの出力
                CharacterRecognitionMessageData message = resultData.getMessage();
                if (message != null) {
                    sb.append("メッセージ : " + message.getText() + "\n");

                    //処理中の場合再実行
                    AlertDialog.Builder dlg = new AlertDialog.Builder(OCR.this);
                    RecognitionAsyncTaskParam param = new RecognitionAsyncTaskParam();
                    param.setJobId(jobid);
                    task = new RecognitionResultAsyncTask(dlg);
                    task.execute(param);

                }
                result_get.setText(sb.toString());
                listview.setAdapter(adapter);

        }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        word = (TextView)findViewById(R.id.select_word);
        translate_word=(TextView)findViewById(R.id.translate);

        // ListView
        listview = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                item = (String) listView.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(), item + " selected",
                        Toast.LENGTH_SHORT).show();
                word.setText(item);
                speakbtn.setVisibility(View.VISIBLE);
            }
        });

        //発音ボタン
        speakbtn = (Button)findViewById(R.id.speak);
        speakbtn.setVisibility(View.INVISIBLE);

        result_get = (TextView)findViewById(R.id.result_get);

        // ソフトキーボードを非表示にする
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(result_get.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        // 認識ジョブIDを受取り EditText に設定する
        Intent intent =  this.getIntent();
        jobid = intent.getStringExtra(MainActivity.INTENT_JOBID_KEY);

        AlertDialog.Builder dlg = new AlertDialog.Builder(OCR.this);
        RecognitionAsyncTaskParam param = new RecognitionAsyncTaskParam();

        // パラメータ取得
        // 認識ジョブID
        param.setJobId(jobid);

        // 実行
        task = new RecognitionResultAsyncTask(dlg);
        task.execute(param);
    }

    public void speak(View v) {
        if(MainActivity.lang == Lang.CHARACTERS_EN) {
            String url = "http://translate.google.com/translate_tts?tl=en&q=" + item;
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {

            }
        } else {
            String url = "http://translate.google.com/translate_tts?tl=ja&q=" + item;
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {

            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(task != null){
            task.cancel(true);
        }
    }

    // 戻った時の処理
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            MainActivity.result.setText("");
            MainActivity.resultBtn.setEnabled(false);
            return super.onKeyDown(keyCode, event);
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}