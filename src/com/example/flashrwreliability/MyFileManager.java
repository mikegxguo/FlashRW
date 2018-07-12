package com.example.flashrwreliability;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MyFileManager extends ListActivity {

	private List<String> items = null;
	private List<String> paths = null;
	private String rootPath = "/mnt/sdcard";
	private String curPath = "/mnt/sdcard";
	private TextView mPath = null;
	private String s_Button = "";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Intent _intent = getIntent();
		s_Button = _intent.getStringExtra("button");
		Log.d("Button", s_Button);

		setContentView(R.layout.fileselect);

		mPath = (TextView) findViewById(R.id.mPath);
		Button buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
		Button buttonCancle = (Button) findViewById(R.id.buttonCancle);

		buttonConfirm.setOnClickListener(new ButtonClickListener1());
		buttonCancle.setOnClickListener(new ButtonClickListener2());

		getFileDir(rootPath);
	}

	private final class ButtonClickListener1 implements OnClickListener {
		public void onClick(View v) {
			Intent data = new Intent(MyFileManager.this, MainActivity.class);
			Bundle bundle = new Bundle();
			if (s_Button.equals("2000")) {
				Log.d(curPath, "12311111111111111");
				bundle.putString("path", curPath);
				data.putExtras(bundle);
				setResult(5, data);
			} else {
				// bundle.putString("path2", curPath);
				// data.putExtras(bundle);
				// setResult(3,data);
			}
			finish();
		}
	}

	private final class ButtonClickListener2 implements OnClickListener {
		public void onClick(View v) {
			finish();
		}
	}

	private void getFileDir(String filePath) {
		mPath.setText(filePath);
		items = new ArrayList<String>();
		paths = new ArrayList<String>();
		File f = new File(filePath);
		File[] files = f.listFiles();

		if (!filePath.equals(rootPath)) {
			items.add("b1");
			paths.add(rootPath);
			items.add("b2");
			paths.add(f.getParent());
		}
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			items.add(file.getName());
			paths.add(file.getPath());
		}

		setListAdapter(new MyAdapter(this, items, paths));
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(paths.get(position));
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files == null) {
				Toast.makeText(MyFileManager.this,
						"This folder is not readable.", Toast.LENGTH_LONG)
						.show();
				return;
			}
			if (paths.get(position).equals("/mnt/asec")) {
				Toast.makeText(MyFileManager.this, "This folder is not right.",
						Toast.LENGTH_LONG).show();
				return;
			}
			curPath = paths.get(position);
			getFileDir(paths.get(position));
		} else {
			// openFile(file);
			Intent data = new Intent(MyFileManager.this, MainActivity.class);
			Bundle bundle = new Bundle();
			bundle.putString("file", paths.get(position));
			bundle.putString("path1", "");
			bundle.putString("path2", "");
			data.putExtras(bundle);
			if (s_Button.equals("1000")) {
				setResult(1, data);
			}
			if (s_Button.equals("1050")) {
				setResult(0, data);
			}

			finish();
		}
	}

}
