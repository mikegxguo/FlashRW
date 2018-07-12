package com.example.flashrwreliability;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.Timer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	protected static final int FlashPATH_RESULT_CODE = 5;

	private String Flash_TestDirPath;

	private Button FlashPathButton;

	private EditText FlashPathEdit;

	private EditText CyclenumEdit;

	private EditText FlashtesttimeEdit;

	private TextView mTextView01;
	private ScrollView myScrollView;

	private String TestResult = "";
	private Handler handler;
	public String LogfilePath;
	public String strSystemTime;
	private Button m_btnstart = null;
	private Button m_btnstop = null;
	char bufW[];
	char bufR[];

	private int RunningTest = -1;
	private boolean MyResum = false;
	private long Time1 = 0;
	private int cyclenum = 1;

	private long Flashtesttimenum = 1;

	public Timer timer = null;
	private RunAlltestThead mAllTestThread;
	private int Currentcycle = 0;

	public void onCreate(Bundle savedInstanceState) {
		// StrictMode.setThreadPolicy(new
		// StrictMode.ThreadPolicy.Builder().detectDiskReads()
		// .detectDiskWrites().detectNetwork().penaltyLog().build());

		// StrictMode.setVmPolicy(new
		// StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
		// .detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mySelectPath();// select folder
		strSystemTime = mGetSystemTime();
		LogfilePath = "/mnt/sdcard/Auto_RW_log" + strSystemTime + ".txt";
		handler = new Handler(Looper.getMainLooper()) {// Looper.getMainLooper()

			/*
			 * 20140319 Joshua add: 1.Always move text view scroll to the
			 * bottom. 2.Clean log display when length over 10000. 3.Clean
			 * string TestResult for system halted issue.
			 */
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.arg1 == 2) {
					Log.d("2", msg.toString());
					TestResult += (String) msg.obj + "\n";
					WriteDataLog(LogfilePath, (String) msg.obj);
					mTextView01.setText(TestResult);
					myScrollView.scrollTo(0, TestResult.length());
					if (mTextView01.getText().length() > 10000) {
						TestResult = "";
					}

				} else if (msg.arg1 == 3) {
					TestResult += (String) msg.obj + "\n";
					WriteDataLog(LogfilePath, (String) msg.obj);
					mTextView01.setText(TestResult);
					myScrollView.scrollTo(0, TestResult.length());
					if (mTextView01.getText().length() > 10000) {
						TestResult = "";
					}

					m_btnstart.setEnabled(true);
					m_btnstop.setEnabled(false);

				}
			}
		};

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		m_btnstart = (Button) findViewById(R.id.btn_start);
		m_btnstop = (Button) findViewById(R.id.btn_stop);
		m_btnstart.setEnabled(true);
		m_btnstop.setEnabled(false);

		mTextView01 = (TextView) findViewById(R.id.myTextView01);
		myScrollView = (ScrollView) findViewById(R.id.scrollView1);

		CyclenumEdit = (EditText) findViewById(R.id.cyclenumber);
		FlashtesttimeEdit = (EditText) findViewById(R.id.testtimenumber);

		m_btnstart.setOnClickListener(new StartTest());
		m_btnstop.setOnClickListener(new StopTest());
	}

	protected void onDestroy() {
		RunningTest = -1;
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		super.onDestroy();
	}

	public class StartTest implements OnClickListener {
		public void onClick(View v) {
			if (CyclenumEdit.getText().toString().equals("") == true) {
				Toast.makeText(MainActivity.this,
						"Please input cycle number!!!", Toast.LENGTH_LONG)
						.show();
				return;
			}
			if (FlashPathEdit.getText().toString().equals("") == true) {
				Toast.makeText(MainActivity.this, "Please input the Path!!!",
						Toast.LENGTH_LONG).show();
				return;
			}
			cyclenum = Integer.parseInt(CyclenumEdit.getText().toString());

			if (RunningTest == -1 && mAllTestThread == null) {
				m_btnstart.setEnabled(false);
				m_btnstop.setEnabled(true);
				RunningTest = 1;
				mAllTestThread = new RunAlltestThead(/* Flash_TestDirPath */);
				mAllTestThread.start();

				if (timer != null) {
					timer.cancel();
					timer = null;
				}

			}

		}
	}

	void SendMyMessage(Handler msghandler, int type, String msg) {
		Message MSG = msghandler.obtainMessage();
		MSG.obj = msg;
		MSG.arg1 = type;
		msghandler.sendMessage(MSG);
	}

	public class RunAlltestThead extends Thread {
		// RunStep1()-> Flash Function Test
		// RunStep2()-> Flash Performance Test
		// RunStep3()-> Flash Reliability Test
		// RunStep4()-> SD Function Test
		// RunStep5()-> SD Performance Test
		// RunStep6()-> SD Reliability Test

		// public String TestDirPath;
		//
		// public RunAlltestThead(String TestDirPath) {
		// //this.handler = handler;
		// this.TestDirPath = TestDirPath;
		// }
		public void run() {
			super.run();
			Currentcycle = 0;
			while (RunningTest != -1) {
				if (cyclenum == Currentcycle) {
					Currentcycle = 0;
					break;
				}
				Currentcycle++;
				SendMyMessage(handler, 2, "\n#############Cycle:"
						+ Currentcycle + "#############");
				SendMyMessage(handler, 2, " ");
				RunningTest = 3;
				if (RunningTest == -1) {
					break;
				} else {
					RunningTest = 3;
					RunStep3();
				}
				SystemClock.sleep(10);
			}
			RunningTest = -1;
			mAllTestThread = null;
			Message MSG = handler.obtainMessage();
			MSG.arg1 = 3;
			MSG.obj = "Test End!";
			handler.sendMessage(MSG);
		}
	}

	private void RunStep3() {
		String testFilePath;
		FileReader fileR;
		FileWriter fileW;
		String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random;
		char bufW[];
		char bufR[];
		int length = 3 * 1024 * 1024;
		int fileCount = 10;
		boolean bStop = false;
		boolean bReturn = true;

		Flashtesttimenum = Integer.parseInt(FlashtesttimeEdit.getText()
				.toString());
		SendMyMessage(handler, 2, "====Flash R/W Reliability Test start");
		long marktime1 = SystemClock.uptimeMillis();
		long marktime2 = 0;
		random = new Random();
		bufW = new char[length];
		SendMyMessage(handler, 2, "Create temp 3M buf.");
		for (int i = 0; i < length; ++i) {
			int number = random.nextInt(62);// [0,62)
			bufW[i] = str.charAt(number);
		}
		while (!bStop && marktime2 - marktime1 <= Flashtesttimenum * 60000) {
			marktime2 = SystemClock.uptimeMillis();
			try {
				for (int i = 0; i < fileCount; i++) {
					String TestFileName = "Reliability" + Integer.toString(i)
							+ ".txt";
					testFilePath = Flash_TestDirPath + TestFileName;
					fileW = new FileWriter(testFilePath);

					Log.d("My App", testFilePath);

					SendMyMessage(handler, 2, "Begin writing " + TestFileName);

					fileW.write(bufW);
					fileW.close();
				}

				if (bStop) {
					SendMyMessage(handler, 2, "Stopped");
					return;
				}

				// read
				for (int i = 0; i < fileCount; i++) {
					if (bStop) {
						SendMyMessage(handler, 2, "Stopped");
						return;
					}

					String TestFileName = "Reliability" + Integer.toString(i)
							+ ".txt";
					testFilePath = Flash_TestDirPath + TestFileName;

					fileR = new FileReader(testFilePath);
					bufR = new char[length + 10];

					SendMyMessage(handler, 2, "Begin reading " + testFilePath);

					int iRead = fileR.read(bufR);
					fileR.close();
					if (iRead != length) {
						SendMyMessage(handler, 2, "The length of file "
								+ Integer.toString(i) + " isn't correct.");

						bReturn = false;
					} else {
						SendMyMessage(handler, 2, "The length of file "
								+ Integer.toString(i) + " is correct.");
					}
					boolean bSame = true;
					for (int j = 0; j < length; ++j) {
						if (bufR[j] != bufW[j]) {
							bSame = false;
						}
					}

					if (bSame) {
						SendMyMessage(handler, 2,
								"The file " + Integer.toString(i)
										+ " is correct.");
					} else {
						SendMyMessage(handler, 2,
								"The file " + Integer.toString(i)
										+ " isn't correct.");
						bReturn = false;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (bReturn) {
			SendMyMessage(handler, 2, "Pass");
		} else {
			SendMyMessage(handler, 2, "Fail");
		}
		delAllFile(Flash_TestDirPath);
		SendMyMessage(handler, 2, "====Flash R/W Reliability Test end");
	}

	public void delAllFile(String path) {
		File file = new File(path);
		if (!file.exists()) {
			return;
		}
		if (!file.isDirectory()) {
			return;
		}
		String[] tempList = file.list();
		File temp = null;
		for (int i = 0; i < tempList.length; i++) {
			if (path.endsWith(File.separator)) {
				temp = new File(path + tempList[i]);
			} else {
				temp = new File(path + File.separator + tempList[i]);
			}

			if (temp.isFile()) {
				temp.delete();
			}
		}
	}

	public boolean WriteFileByBuff(String path, char[] data, int BuffSize) {
		boolean bReturn = true;
		try {

			BufferedWriter bufWtr = new BufferedWriter(new FileWriter(path),
					BuffSize * 1024);
			bufWtr.write(data);
			bufWtr.close();
		} catch (IOException e) {
			e.printStackTrace();
			bReturn = false;
		}
		return bReturn;
	}

	public int ReadFileByBuff(String path, char[] data, int BuffSize,
			boolean bRead) {
		int iReturn = 0;
		try {

			BufferedReader bufRdr = new BufferedReader(new FileReader(path),
					BuffSize * 1024);
			iReturn = bufRdr.read(data);
			bufRdr.close();
			if (bRead) {
				File fR = new File(path);
				if (!fR.isDirectory()) {
					fR.delete();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return iReturn;
	}

	public boolean RWFileTest(String strTestFilePath, String strFileName,
			int iBufType) {
		boolean bReturn = true;
		long lBeginWrite = 0, lEndWrite = 0, lBeginRead = 0, lEndRead = 0;
		int iWriteTime = 0, iReadTime = 0, iCy = 0, iBufSize = 4 * 1024 * 1024;
		int mFileSize = 0, mSYFileSize = 0, iCurSize = 0;
		long mLFSize = 0;

		for (int i = 0; i < iCy; i++) {
			iCurSize = iBufSize;
			if (i == iCy - 1) {
				if (mSYFileSize != 0) {
					iCurSize = mSYFileSize;
				}
			}
			Log.d("111111111111111", "" + iCy + "  shengyu:  " + mSYFileSize);
			bufW = new char[iCurSize];
			lBeginWrite = System.currentTimeMillis();
			if (WriteFileByBuff(strTestFilePath, bufW, iBufType)) {
				lEndWrite = System.currentTimeMillis();
				iWriteTime += (int) (lEndWrite - lBeginWrite);
				if (i == iCy - 1) {
					SendMyMessage(handler, 2, "Write "
							+ strFileName
							+ " with "
							+ Integer.toString(iBufType)
							+ "KB buf  Time= "
							+ Integer.toString(iWriteTime)
							+ "ms  Speed="
							+ (mLFSize / iWriteTime)
							* 1000
							/ 1024
							+ "KB/s  "
							+ myTwoDecimal(""
									+ ((mLFSize / iWriteTime) * 1000 / 1024)
									/ 1024 + "."
									+ ((mLFSize / iWriteTime) * 1000 / 1024)
									% 1024) + "MB/S");
				}
				bufR = new char[iCurSize];
				lBeginRead = System.currentTimeMillis();
				if (ReadFileByBuff(strTestFilePath, bufR, iBufType, false) == iCurSize) {
					lEndRead = System.currentTimeMillis();
					iReadTime += (int) (lEndRead - lBeginRead);
					boolean bSame = true;
					for (int j = 0; j < iCurSize; j++) {
						if (bufR[j] != bufW[j]) {
							bSame = false;
							break;
						}
					}
					if (bSame) {
						if (i == iCy - 1) {
							SendMyMessage(
									handler,
									2,
									"Read "
											+ strFileName
											+ " with "
											+ Integer.toString(iBufType)
											+ "KB buf  Time= "
											+ Integer.toString(iReadTime)
											+ "ms  Speed="
											+ (mLFSize / iReadTime)
											* 1000
											/ 1024
											+ "KB/s  "
											+ myTwoDecimal(""
													+ ((mLFSize / iWriteTime) * 1000 / 1024)
													/ 1024
													+ "."
													+ ((mLFSize / iWriteTime) * 1000 / 1024)
													% 1024) + "MB/S");
						}
					} else {
						SendMyMessage(handler, 2, "The " + strFileName
								+ " with " + Integer.toString(iBufType)
								+ "KB buf isn't correct.");
						bReturn = false;
						break;
					}
				} else {
					SendMyMessage(handler, 2, "The length of " + strFileName
							+ " with " + Integer.toString(iBufType)
							+ "KB buf isn't correct.");
					bReturn = false;
					break;
				}
			} else {
				SendMyMessage(handler, 2, "Write " + strFileName + " with "
						+ Integer.toString(iBufType) + "KB buf fail.");
				bReturn = false;
				break;
			}
		}
		return bReturn;
	}

	public boolean RWTest(char[] bufWrt, int bufLength, String filePath,
			String fileName, int bufType) {
		boolean bReturn = true;
		long iBegin = 0;
		long iEnd = 0;
		int length = 1024 * 1024;
		iBegin = System.currentTimeMillis();
		if (WriteFileByBuff(filePath, bufW, bufType)) {
			iEnd = System.currentTimeMillis();
			int iTime = (int) (iEnd - iBegin);
			SendMyMessage(
					handler,
					2,
					"Write "
							+ fileName
							+ " with "
							+ Integer.toString(bufType)
							+ "KB buf  Time= "
							+ Integer.toString((int) (iEnd - iBegin))
							+ "ms  Speed="
							+ (bufLength / iTime)
							* 1000
							/ 1024
							+ "KB/s  "
							+ myTwoDecimal(""
									+ ((bufLength / iTime) * 1000 / 1024)
									/ 1024 + "."
									+ ((bufLength / iTime) * 1000 / 1024)
									% 1024) + "MB/s");

			char[] bufRead = new char[bufLength + 10];
			iBegin = System.currentTimeMillis();
			if (ReadFileByBuff(filePath, bufRead, bufType, false) == length) {
				iEnd = System.currentTimeMillis();
				boolean bSame = true;
				for (int j = 0; j < bufLength; ++j) {
					if (bufRead[j] != bufWrt[j]) {
						bSame = false;
					}
				}

				if (bSame) {
					iTime = (int) (iEnd - iBegin);
					SendMyMessage(handler, 2, "Read "
							+ fileName
							+ " with "
							+ Integer.toString(bufType)
							+ "KB buf  Time= "
							+ Integer.toString((int) (iEnd - iBegin))
							+ "ms  Speed="
							+ (bufLength / iTime)
							* 1000
							/ 1024
							+ "KB/s  "
							+ myTwoDecimal(""
									+ ((bufLength / iTime) * 1000 / 1024)
									/ 1024 + "."
									+ ((bufLength / iTime) * 1000 / 1024)
									% 1024) + "MB/s");
				} else {
					SendMyMessage(handler, 2, "The " + fileName + " with "
							+ Integer.toString(bufType)
							+ "KB buf isn't correct.");

					bReturn = false;
				}
			} else {
				SendMyMessage(handler, 2, "The length of " + fileName
						+ " with " + Integer.toString(bufType)
						+ "KB buf isn't correct.");

				bReturn = false;
			}

		} else {
			SendMyMessage(handler, 2,
					"Write " + fileName + " with " + Integer.toString(bufType)
							+ "KB buf fail.");

		}

		return bReturn;
	}

	public double myTwoDecimal(String strData) {
		double dData = Double.parseDouble(strData);
		int j = (int) Math.round(dData * 100);//
		double dRetData = (double) j / 100.00;//

		return dRetData;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (RunningTest != -1) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/*
	 * 20140319 Joshua add: 1. Disable stop button to resolve system halted
	 * issue after click many times.
	 */

	public class StopTest implements OnClickListener {
		public void onClick(View v) {
			if (RunningTest != -1) {
				RunningTest = -1;
				m_btnstop.setEnabled(false);
				if (timer != null) {
					timer.cancel();
					timer = null;
				}
			}
		}

	}

	public void WriteDataLog(String strFilePath, String strlog) {
		String Filename = strFilePath;

		String strline = strlog + "\n\r";
		FileWriter fw = null;
		try {
			fw = new FileWriter(Filename, true);
			fw.append(strline);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String mGetSystemTime() {
		SimpleDateFormat sDateFormat = new SimpleDateFormat("hhmmss");
		String strDate = sDateFormat.format(new java.util.Date());
		return strDate;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (FlashPATH_RESULT_CODE == resultCode) {
			Bundle bundle = null;
			if (data != null && (bundle = data.getExtras()) != null) {
				if (bundle.getString("path").equals("/") == true) {
					FlashPathEdit.setText("");
					Toast.makeText(MainActivity.this,
							"The target path can't be / .", Toast.LENGTH_LONG)
							.show();
					return;
				}
				FlashPathEdit.setText(bundle.getString("path"));
			}

			if (FlashPathEdit.getText().toString().equals("") == true) {
				Toast.makeText(this, "Please select path!!!", Toast.LENGTH_LONG)
						.show();
				return;
			}
			Flash_TestDirPath = FlashPathEdit.getText().toString()
					+ "/FlashRWReliabilityTest/";
			(new File(Flash_TestDirPath)).mkdirs();

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void mySelectPath() {
		FlashPathEdit = (EditText) findViewById(R.id.path_edit);
		FlashPathButton = (Button) findViewById(R.id.path);
		FlashPathButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						MyFileManager.class);
				intent.putExtra("button", "2000");
				startActivityForResult(intent, FlashPATH_RESULT_CODE);
			}
		});
	}

}
