package com.pisapapiros.phonetfinder;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	EditText editText1;
	TextView textView1;
	ProgressDialog dialog;
	CheckBox audio;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		editText1 = (EditText) findViewById(R.id.editText1);
		textView1 = (TextView) findViewById(R.id.textView1);
		audio = (CheckBox) findViewById(R.id.checkBox1);

		dialog = new ProgressDialog(this);
		dialog.setMessage("Downloading...");
		dialog.setTitle("Progress");
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setCancelable(true);
	}

	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.explanation:
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.addCategory(Intent.CATEGORY_BROWSABLE);
			intent.setData(Uri
					.parse("http://www.wordreference.com/es/pronunciacion-inglesa.aspx"));
			startActivity(intent);
		}
		return true;
	}

	public void go(View view) {
		// hide the keyboard
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText1.getWindowToken(), 0);
		// execute the query in the second thread with the input word as a param
		String inputWord = editText1.getText().toString();
		new SecondaryTask().execute(inputWord);
	}

	/* Background task, invoked by the following class*/
	public Document getCodeFromWeb(String[] words) {
		Document code = null;
		String url = "http://www.wordreference.com/es/translation.asp?tranword="
				+ words[0];
		System.out.println("[getCodeFromWeb] URL: " + url);
		try {
			code = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.println("[getCodeFromWeb] code: " + code);
		return code;
	}

	/* Class for background task */
	private class SecondaryTask extends AsyncTask<String, Integer, Document> {

		protected void onPreExecute() {
			// This method is executed just after calling the execute
			dialog.setProgress(0);
			dialog.setMax(100);
			dialog.show();
		}

		protected Document doInBackground(String... word) {
			// System.out.println("[doInBackgound] Word: " + word.toString());
			Document res = null;
			publishProgress(1);
			res = getCodeFromWeb(word);
			// Progress bar code
			for (int i = 2; i < 10; i++) {
				try {
					publishProgress((Integer) i);
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// Update bar values
				if (isCancelled())
					break;
			}
			publishProgress(10);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return res;
		}

		protected void onProgressUpdate(Integer... valores) {
			// When publishProgress is called
			int p = Math.round(10 * valores[0]); // coming from 0 until 10
			dialog.setProgress(p);
		}

		protected void onPostExecute(Document code) {
			Element phonetic = null;
			// System.out.println("[onPostExecute] Reached with: " +
			// code.text());
			try {
				// First dictionary
				phonetic = code.getElementById("pronWR");
				if (phonetic == null) {
					// System.out.println("[onPostExecute] Second dictionary, class 'EsIPA'");
					phonetic = code.getElementsByClass("EsIPA").first();
				}
				// if (phonetic == null) {
				// System.out.println("[onPostExecute] Third dictionary, class 'phonetics'");
				// Not working. getElementsByClass("phonetics").first(),
				// select("span[class=phonetics]")
				// }
				System.out.println("Writting: " + phonetic.text());
				textView1.setText(Html.fromHtml(phonetic.text()));
			} catch (Exception e) {
				// Word not found, phonetic = null
				textView1.setText("Not found");
			}
			// We have the transcription. Now get the sound
			if (audio.isChecked()) {
				MediaPlayer mplayer = new MediaPlayer();
				try {
					String soundCode = code.getElementById("aud").attr("value");
					//System.out.println("soundCode: " + soundCode);
					playSound(mplayer, "http://wordreference.com/audio/en/us/"
							+ soundCode + "-1.mp3");
				} catch (Exception e) {
					// Sound not found, playing error message
					playSound(mplayer,
							"http://wordreference.com/audio/en/us/11185-1.mp3");
				}
			}
			dialog.dismiss();
		}
	}

	public void playSound(MediaPlayer player, String url) {
		System.out.println(url);
		try {
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setDataSource(url);
			player.prepare();
			player.start();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
