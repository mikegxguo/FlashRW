package com.mitac.flashrw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.List;
import java.lang.reflect.Method;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Environment;
import android.os.StatFs;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.content.Context;
import android.net.Uri;

import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import com.mitac.common.EmmcProperties;
import com.mitac.common.ServiceStateCallback;
import com.mitac.common.SystemApiClient;
import com.mitac.common.exceptions.EmmcPropertiesException;
import com.mitac.common.exceptions.SystemApiClientException;


public class MainActivity extends Activity {

	protected static final int FlashPATH_RESULT_CODE = 5;

	private String ReliabilityPath;
    private String BasicPath;
    private String PerformancePath;

	private Button FlashPathButton;

	private EditText FlashPathEdit;
    private TextView mTextPath;

	private EditText CyclenumEdit;

	private EditText FlashtesttimeEdit;
    private CheckBox box1_basic,box1_performance,box1_reliability;
    private boolean mBasicTest = false;
    private boolean mPerformanceTest = false;
    private boolean mReliabilityTest = false;
    private boolean mState = false, bCheckFile = false;
    private String strSelectFilePath = "";
    private long mLFSize = 0;
    private int length = 1024 * 1024, iBufSize = 4*1024*1024, iCy = 0;
    private int mFileSize = 0, mSYFileSize = 0, iCurSize = 0;
    private static String external_sdcard_path = "";
    private StorageManager mStorageManager;
    private Context mContext;
    private boolean bStop = false;

	private TextView mTextView01;
	private ScrollView myScrollView;

	private String TestResult = "";
	private Handler handler;
	public String LogfilePath;
	public String strSystemTime;
	private Button m_btnstop = null;
  private Button m_btnflash = null;
  private Button m_btnsd = null;
  private Button m_btnud = null;
	private char bufW[];
	private char bufR[];

	private int RunningTest = -1;
	private boolean MyResum = false;
	private long Time1 = 0;
	private int cyclenum = 1;

	private long Flashtesttimenum = 1;

	public Timer timer = null;
	private RunAlltestThead mAllTestThread;
	private int Currentcycle = 0;

    private static String TAG = "Storage";
    private SystemApiClient mSystemApiClient;
    private boolean mServiceReady = false;
    private int emmc_health = -1;
    private boolean mSDTest = false;
    private static final String TIME_STAMP_NAME = "_yyyyMMdd_HHmmss_";

    private void initSystemApiClient() {
        if (mSystemApiClient == null) {
            Log.d(TAG, "init System api client...");
            mSystemApiClient = new SystemApiClient(getApplicationContext(), new ServiceStateCallback() {
                @Override
                public void serviceReady() {
                    mServiceReady = true;
                    //mStartAllTestButton.setEnabled(mServiceReady);
                }
            });

            if (!mSystemApiClient.isServiceReady()) {
                Log.d(TAG, "try to connect system api service...");
                mSystemApiClient.connect();
            } else {
                Log.d(TAG, "system api service ready...");
            }
        } else {
            Log.d(TAG, "no need to init system api client...");
        }
    }

    private void removeSystemApiClient() {
        if (mSystemApiClient != null) {
            Log.d(TAG, "try to disconnect from system client service");
            mSystemApiClient.disconnect();
            mSystemApiClient = null;
        }
    }

    private int testGetEmmcHealthStatusApi() {
        //boolean isSuccess = false;
        int value = -1;

//        try {
//            EmmcProperties.init(mSystemApiClient);
//            value = EmmcProperties.getEmmcHealthStatus();
//            //isSuccess = true;
//        } catch (EmmcPropertiesException e) {
//            e.printStackTrace();
//        } catch (SystemApiClientException e) {
//            e.printStackTrace();
//        }
        //updateTexViewUiResult(mEmmcHealthTextView,
        //        ResUtils.getResString(this, R.string.emmc_health),
        //        Integer.toString(value));
        return value;
    }

	public void onCreate(Bundle savedInstanceState) {
		// StrictMode.setThreadPolicy(new
		// StrictMode.ThreadPolicy.Builder().detectDiskReads()
		// .detectDiskWrites().detectNetwork().penaltyLog().build());

		// StrictMode.setVmPolicy(new
		// StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects()
		// .detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());

		super.onCreate(savedInstanceState);
        mContext = this;

		setContentView(R.layout.activity_main);
        //EmmcProperties.init();
		mySelectPath();// select folder
		strSystemTime = mGetSystemTime();
        (new File("/mnt/sdcard/StorageTest")).mkdirs();
		LogfilePath = "/mnt/sdcard/StorageTest/Auto_RW_log" + strSystemTime + ".txt";
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

          m_btnflash.setEnabled(true);
          m_btnsd.setEnabled(true);
          m_btnud.setEnabled(true);
					m_btnstop.setEnabled(false);
          bStop = false;

				}
			}
		};

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		m_btnstop = (Button) findViewById(R.id.btn_stop);
    m_btnflash = (Button) findViewById(R.id.btn_flash);
    m_btnsd = (Button) findViewById(R.id.btn_sd);
    m_btnud = (Button) findViewById(R.id.btn_ud);
    m_btnflash.setEnabled(true);
    m_btnsd.setEnabled(true);
    m_btnud.setEnabled(true);
		m_btnstop.setEnabled(false);

		mTextView01 = (TextView) findViewById(R.id.myTextView01);
		myScrollView = (ScrollView) findViewById(R.id.scrollView1);

		CyclenumEdit = (EditText) findViewById(R.id.cyclenumber);
		FlashtesttimeEdit = (EditText) findViewById(R.id.testtimenumber);

    m_btnflash.setOnClickListener(new FlashTest());
    m_btnsd.setOnClickListener(new SDTest());
    m_btnud.setOnClickListener(new UsbDiskTest());
		m_btnstop.setOnClickListener(new StopTest());

        box1_basic = (CheckBox) findViewById(R.id.box1_basic);
        box1_performance = (CheckBox) findViewById(R.id.box1_performance);
        box1_reliability = (CheckBox) findViewById(R.id.box1_reliability);
        box1_basic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mBasicTest = true;
                }else{
                    mBasicTest = false;
                }
            }
        });
        box1_performance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mPerformanceTest = true;
                }else{
                    mPerformanceTest = false;
                }
            }
        });
        box1_reliability.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mReliabilityTest = true;
                }else{
                    mReliabilityTest = false;
                }
            }
        });
	}


  private void showOpenDocumentTree() {
      Intent intent = null;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
          mStorageManager = StorageManager.from(mContext);
          StorageVolume volume = mStorageManager.getStorageVolume(new File(external_sdcard_path));
          if (volume != null) {
              intent = volume.createAccessIntent(null);
          }
      }
      if (intent == null) {
          intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
      }
      startActivityForResult(intent, DocumentsUtils.OPEN_DOCUMENT_TREE_CODE);
  }



    @Override
    protected void onResume() {
        super.onResume();
        initSystemApiClient();
    }

    @Override
	protected void onDestroy() {
		RunningTest = -1;
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		super.onDestroy();
        removeSystemApiClient();
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
            mBasicTest = box1_basic.isChecked();
            mPerformanceTest = box1_performance.isChecked();
            mReliabilityTest = box1_reliability.isChecked();


			if (RunningTest == -1 && mAllTestThread == null) {
        m_btnflash.setEnabled(false);
        m_btnsd.setEnabled(false);
        m_btnud.setEnabled(false);
				m_btnstop.setEnabled(true);
				RunningTest = 1;
				mAllTestThread = new RunAlltestThead();
				mAllTestThread.start();

				if (timer != null) {
					timer.cancel();
					timer = null;
				}

			}

		}
	}


    void SelectPath(String strSelectPath) {
            String strTempPath = strSelectPath + "/StorageTest/";
            (new File(strTempPath)).mkdirs();

            ReliabilityPath = strSelectPath + "/StorageTest/RWReliability/";
            (new File(ReliabilityPath)).mkdirs();

            BasicPath = strSelectPath + "/StorageTest/RWBasic/";
            (new File(BasicPath)).mkdirs();

            PerformancePath = strSelectPath + "/StorageTest/RWPerformance/";
            (new File(PerformancePath)).mkdirs();
    }

    public class FlashTest implements OnClickListener {
        public void onClick(View v) {
            if (CyclenumEdit.getText().toString().equals("") == true) {
                Toast.makeText(MainActivity.this,
                        "Please input cycle number!!!", Toast.LENGTH_LONG)
                        .show();
                return;
            }

            SelectPath("/mnt/sdcard");
            mSDTest = false;

            cyclenum = Integer.parseInt(CyclenumEdit.getText().toString());
            mBasicTest = box1_basic.isChecked();
            mPerformanceTest = box1_performance.isChecked();
            mReliabilityTest = box1_reliability.isChecked();

            if (RunningTest == -1 && mAllTestThread == null) {
                m_btnflash.setEnabled(false);
                m_btnsd.setEnabled(false);
                m_btnud.setEnabled(false);
                m_btnstop.setEnabled(true);
                RunningTest = 1;
                mAllTestThread = new RunAlltestThead();
                mAllTestThread.start();
            }
        }
    }

    void GetExternalSDPath() {
        File path;
        mStorageManager = StorageManager.from(mContext);
        StorageVolume[] volumes = mStorageManager.getVolumeList();
        Log.d("Storage", "volume length: "+volumes.length);
        for (int ivolumes = 0; ivolumes < volumes.length; ivolumes++) {
            path = new File(volumes[ivolumes].getPath());
            Log.d("Storage","Trying to create file at - "+path+":: isRemovable="+ volumes[ivolumes].isRemovable()+
                ", getDescription="+volumes[ivolumes].getDescription(mContext)+", isEmulated="+
                volumes[ivolumes].isEmulated()+", isPrimary="+volumes[ivolumes].isPrimary());

            //if(volumes[ivolumes].isPrimary() == true && volumes[ivolumes].isEmulated() == true)
            //    default_sdcard_path = path.toString();

            if(volumes[ivolumes].isRemovable() == true)
                external_sdcard_path = path.toString();

        }
    }

    void GetUsbDiskPath() {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> volumes = mStorageManager.getStorageVolumes();
        try {
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Method getUserLabel = storageVolumeClazz.getMethod("getUserLabel");
            for (int i = 0; i < volumes.size(); i++) {
                StorageVolume storageVolume = volumes.get(i);
                String storagePath = (String) getPath.invoke(storageVolume);
                boolean isRemovableResult = (boolean) isRemovable.invoke(storageVolume);
                String label = (String) getUserLabel.invoke(storageVolume);
                //String description = storageVolume.getDescription(mContext);
                //if ("USB".equals(label)){
                if(storagePath.contains("media_rw")) { //FIXME
                    external_sdcard_path = storagePath;
                    break;
                }
                Log.d("Storage","GetUsbDiskPath--"+ " i=" + i + " ,storagePath=" + storagePath +  " ,description=" + label);
            }
            } catch (Exception e) {
                Log.d("Storage","GetUsbDiskPath--" + " e:" + e);
            }
        }

    public class SDTest implements OnClickListener {
        public void onClick(View v) {
            if (CyclenumEdit.getText().toString().equals("") == true) {
                Toast.makeText(MainActivity.this,
                        "Please input cycle number!!!", Toast.LENGTH_LONG)
                        .show();
                return;
            }

            external_sdcard_path = "";
            GetExternalSDPath();
            if(external_sdcard_path.equals("") == true) {
                Toast.makeText(MainActivity.this,
                        "Please insert SD card!!!", Toast.LENGTH_LONG)
                        .show();
                return;
            }
            SelectPath(external_sdcard_path);
            mSDTest = true;

            if (DocumentsUtils.checkWritableRootPath(MainActivity.this, external_sdcard_path)) {
                showOpenDocumentTree();
                return;
            }

//            try {
//                writeDataToSD();
//            } catch (IOException e) {
//                //do nothing
//            }

            cyclenum = Integer.parseInt(CyclenumEdit.getText().toString());
            mBasicTest = box1_basic.isChecked();
            mPerformanceTest = box1_performance.isChecked();
            mReliabilityTest = box1_reliability.isChecked();

            if (RunningTest == -1 && mAllTestThread == null) {
                m_btnflash.setEnabled(false);
                m_btnsd.setEnabled(false);
                m_btnud.setEnabled(false);
                m_btnstop.setEnabled(true);
                RunningTest = 1;
                mAllTestThread = new RunAlltestThead();
                mAllTestThread.start();
            }
        }
    }

    public class UsbDiskTest implements OnClickListener {
        public void onClick(View v) {
            if (CyclenumEdit.getText().toString().equals("") == true) {
                Toast.makeText(MainActivity.this,
                        "Please input cycle number!!!", Toast.LENGTH_LONG)
                    .show();
                return;
            }

            external_sdcard_path = "";
            GetUsbDiskPath();
            if(external_sdcard_path.equals("") == true) {
                Toast.makeText(MainActivity.this,
                        "Please insert USB disk!!!", Toast.LENGTH_LONG)
                    .show();
                return;
            }
            SelectPath(external_sdcard_path);
            mSDTest = true;

            if (DocumentsUtils.checkWritableRootPath(MainActivity.this, external_sdcard_path)) {
                showOpenDocumentTree();
                return;
            }

            cyclenum = Integer.parseInt(CyclenumEdit.getText().toString());
            mBasicTest = box1_basic.isChecked();
            mPerformanceTest = box1_performance.isChecked();
            mReliabilityTest = box1_reliability.isChecked();

            if (RunningTest == -1 && mAllTestThread == null) {
                m_btnflash.setEnabled(false);
                m_btnsd.setEnabled(false);
                m_btnud.setEnabled(false);
                m_btnstop.setEnabled(true);
                RunningTest = 1;
                mAllTestThread = new RunAlltestThead();
                mAllTestThread.start();
            }
        }
    }

    public  void writeDataToSD() throws IOException {
        String  str = "just a test\n";
        String strRead = "";
        String  sdkOut = external_sdcard_path;//getStoragePath(this,true);  //get root path of external SD

        String  filePath = sdkOut + "/test";
        Log.i(TAG,"lum_ sdkOut: " + filePath);
        File file = new File(filePath);
        if (!file.exists()){
            file.mkdirs();
            Log.i(TAG,"create folder: " + filePath);
        }

        String  fileWritePath = filePath + "/test.txt";
        File fileWrite = new File(fileWritePath);
        Log.i(TAG,"lum: prepare to write" );
        try {
            OutputStream outputStream = DocumentsUtils.getOutputStream(this,fileWrite);
            //  OutputStream outputStream = new FileOutputStream(fileWrite);
            outputStream.write(str.getBytes());
            outputStream.close();
            Log.i(TAG,"lum write successfully" );
            Toast.makeText(this,"path: " + fileWritePath + " successfully ",Toast.LENGTH_SHORT ).show();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"lum fail to write" );
            Toast.makeText(this,"path: " + fileWritePath + "falure",Toast.LENGTH_SHORT ).show();
        }

        try {
            InputStream is = DocumentsUtils.getInputStream(this,fileWrite);
            InputStreamReader input = new InputStreamReader(is, "UTF-8");
            BufferedReader reader = new BufferedReader(input);
            while ((str = reader.readLine()) != null) {
                strRead  +=  str;
            }
            Log.i(TAG,"lum: " +  strRead);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

            if(mBasicTest) {
                mState = true;
                SendMyMessage(handler, 2, " ");
                RunStep1(BasicPath);
                SendMyMessage(handler, 2, " ");
            }

            if(mPerformanceTest) {
                while (RunningTest != -1) {
                    if (cyclenum == Currentcycle) {
                        Currentcycle = 0;
                        break;
                    }
                    Currentcycle++;
                    SendMyMessage(handler, 2, "\n#############Cycle:"+ Currentcycle + "#############");
                    SendMyMessage(handler, 2, " ");

                    RunStep2(PerformancePath);

                    SystemClock.sleep(10);
                }
            }

            if(mReliabilityTest) {
                SendMyMessage(handler, 2, " ");
                RunStep3();
            }

            SendMyMessage(handler, 2, " ");
			RunningTest = -1;
			mAllTestThread = null;
			Message MSG = handler.obtainMessage();
			MSG.arg1 = 3;
			MSG.obj = "Test End!";
			handler.sendMessage(MSG);
		}
	}


//======================================================================================
//======================================================================================
//======================================================================================
//======================================================================================
//======================================================================================

		private char[] getChars (byte[] bytes) {
			Charset cs = Charset.forName ("UTF-8");
			ByteBuffer bb = ByteBuffer.allocate (bytes.length);
			bb.put (bytes);
			bb.flip ();
			CharBuffer cb = cs.decode (bb);

			return cb.array();
		}


		public void RunStep2(String TestDirPath) {

            String  testFilePath;
            FileReader fileR;
            FileWriter fileW;
            String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            Random random;

			//handler .removeMessages(0);
			//Message msg = handler.obtainMessage();
			random = new Random();

//			String strFileSize = "";
			String strFileName = "";
			boolean bReturn = true;
			testFilePath = TestDirPath + "RWPerformanceTest.txt";

            SendMyMessage(handler, 2, "====R/W Performance Test start");
			File file = new File(testFilePath);
			file.delete();


			if (bCheckFile)
			{
				strFileName = mySelectFileSize();
			}
			else
			{
				bufW = new char[length];
				SendMyMessage(handler, 2, "Create temp 1M buf.");
				for(int i = 0 ; i < length; ++i){
					int number = random.nextInt(62);//[0,62)
					bufW[i] = str.charAt(number);
				}
				strFileName= "1M file";
			}

			//Stop
			if(bStop)
			{
				SendMyMessage(handler, 2, "Stopped");
				return;
			}

			if (bCheckFile)
			{
//				SendMyMessage(handler, 1, "Being processed, please wait...");
				if (!RWFileTest(testFilePath, strFileName, 64))
				{
					bReturn = false;
				}
			}
			else
			{
				if(!RWTest(bufW, length, testFilePath, strFileName, 64))
				{
					bReturn = false;
				}
			}
			//Stop
			if(bStop)
			{
				SendMyMessage(handler, 2, "Stopped");
				return;
			}

			if (bCheckFile)
			{
//				SendMyMessage(handler, 1, "Being processed, please wait...");
				if (!RWFileTest(testFilePath, strFileName, 128))
				{
					bReturn = false;
				}
			}
			else
			{
				if(!RWTest(bufW, length, testFilePath, strFileName, 128))
				{
					bReturn = false;
				}
			}
			//Stop
			if(bStop)
			{
				SendMyMessage(handler, 2, "Stopped");
				return;
			}

			if (bCheckFile)
			{
//				SendMyMessage(handler, 1, "Being processed, please wait...");
				if (!RWFileTest(testFilePath, strFileName, 256))
				{
					bReturn = false;
				}
			}
			else
			{
				if(!RWTest(bufW, length, testFilePath, strFileName, 256))
				{
					bReturn = false;
				}
			}
			//Stop
			if(bStop)
			{
				SendMyMessage(handler, 2, "Stopped");
				return;
			}

			long iBegin = 0;
			long iEnd = 0;
			int iTime = 0;
			if (!bCheckFile)
			{
				String hundredFileDir = TestDirPath + "100Filestest/";
				delAllFile(hundredFileDir);
				(new File(hundredFileDir)).delete();
				(new File(hundredFileDir)).mkdirs();

				int iHundredFileNum = 100;
				int iHundredFileSize = 0;
//				if (bCheckFile)
//				{
//					iHundredFileSize = length;
//					SendMyMessage(handler, 1, "Create temp " + strFileSize + " buf.");
//				}
//				else
//				{
					iHundredFileSize = 10 * 1024;
					bufW = new char[iHundredFileSize];

					SendMyMessage(handler, 2, "Create temp 10KB buf.");

					//Stop
					if(bStop)
					{
						SendMyMessage(handler, 2, "Stopped");
						return;
					}

					for(int i = 0 ; i < iHundredFileSize; ++i){
						int number = random.nextInt(62);//[0,62)
						bufW[i] = str.charAt(number);
						//Stop
						if(bStop)
						{
							SendMyMessage(handler, 2, "Stopped");
							return;
						}
					}
//				}

				iBegin = System.currentTimeMillis();
				for(int i = 0 ; i < iHundredFileNum; ++i){
					String myTestFilePath = hundredFileDir + "HundredFileTest" + Integer.toString( i + 1) + ".txt";

					if(!WriteFileByBuff(myTestFilePath, bufW, 64)){
						bReturn = false;
						SendMyMessage(handler, 2, "Write HundredFileTest" + Integer.toString( i + 1) + " fail");
					}
					//Stop
					if(bStop)
					{
						SendMyMessage(handler, 2, "Stopped");
						return;
					}
				}
				iEnd = System.currentTimeMillis();
				iTime = (int)(iEnd - iBegin);
//				if (bCheckFile) {
//					SendMyMessage(handler, 1, "Write " + strFileSize + " x 100 files with write buffer size 64K  Time= " + Integer.toString((int)(iEnd - iBegin)) + "ms  Speed=" + ((iHundredFileSize*100)/iTime)*1000/1024 + "KB/s");
//				} else {
					SendMyMessage(handler, 2, "Write " + iHundredFileSize/1024 + "KB x 100 files with write buffer size 64K  Time= "
							+ Integer.toString(iTime) + "ms  Speed=" + ((iHundredFileSize*100)/iTime)*1000/1024 + "KB/s  "+
							myTwoDecimal(""+(((iHundredFileSize*100)/iTime)*1000/1024)/1024+"."+(((iHundredFileSize*100)/iTime)*1000/1024)%1024)+"MB/s");
//				}

				int iReadTime = 0;
				for(int i = 0 ; i < iHundredFileNum; ++i){
					String myTestFilePath = hundredFileDir + "HundredFileTest" + Integer.toString( i + 1) + ".txt";
					char[] bufRead = new char[iHundredFileSize + 10];
					iBegin = System.currentTimeMillis();
					if(ReadFileByBuff(myTestFilePath, bufRead, 64, true) == iHundredFileSize){
						iEnd = System.currentTimeMillis();
						iReadTime += (int)(iEnd - iBegin);
						boolean bSame = true;
						for(int j = 0 ; j < iHundredFileSize; ++j){
							if(bufRead[j] != bufW[j]){
								bSame = false;
							}
						}

						if(!bSame){
							SendMyMessage(handler, 2, "HundredFileTest" + Integer.toString( i + 1) + " isn't correct");

							bReturn = false;
						}
					}
					else
					{
						iEnd = System.currentTimeMillis();
						iReadTime += (int)(iEnd - iBegin);
						bReturn = false;
						SendMyMessage(handler, 2, "The length of HundredFileTest" + Integer.toString( i + 1) + " isn't correct");

						//msg.obj = "The length of HundredFileTest" + Integer.toString( i + 1) + " isn't correct";
						//msg.arg1 = 1;
						//handler.sendMessage(msg);
					}
					//Stop
					if(bStop)
					{
						SendMyMessage(handler, 2, "Stopped");
						return;
					}
				}

//				if (bCheckFile) {
//					SendMyMessage(handler, 1, "Read " + strFileSize + " x 100 files with write buffer size 64K  Time= " + Integer.toString(iReadTime) + "ms  Speed=" + ((iHundredFileSize*100)/iReadTime)*1000/1024 + "KB/s");
//				} else {
					SendMyMessage(handler, 2, "Read " + iHundredFileSize/1024 + "KB x 100 files with write buffer size 64K  Time= " + Integer.toString(iReadTime) + "ms  Speed=" + ((iHundredFileSize*100)/iReadTime)*1000/1024 + "KB/s  "
							+ myTwoDecimal(""+(((iHundredFileSize*100)/iReadTime)*1000/1024)/1024+"."+(((iHundredFileSize*100)/iReadTime)*1000/1024)%1024)+"MB/s");
//				}
			}


			String thousandsFileDir = TestDirPath + "1000Filestest/";
			delAllFile(thousandsFileDir);
			(new File(thousandsFileDir)).delete();
			(new File(thousandsFileDir)).mkdirs();

			int iThousandsFileNum = 1000;


			iBegin = System.currentTimeMillis();
			for(int i = 0; i < iThousandsFileNum; ++i)
			{
				String myTestFilePath = thousandsFileDir + "thousandsFileTest" + Integer.toString( i + 1) + ".txt";
				try {
					if(!(new File(myTestFilePath)).createNewFile())
					{
						bReturn = false;
						SendMyMessage(handler, 2, "Create thousandsFileTest" + Integer.toString( i + 1) + " failed");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//Stop
				if(bStop)
				{
					SendMyMessage(handler, 2, "Stopped");
					return;
				}
			}
			iEnd = System.currentTimeMillis();
			SendMyMessage(handler, 2, "Directory list of 1000 files = " + Integer.toString((int)(iEnd - iBegin)) + "ms");


			SendMyMessage(handler, 2, "Delete temp files");

			delAllFile(thousandsFileDir);
			(new File(thousandsFileDir)).delete();
			(new File(thousandsFileDir)).mkdirs();

			if(bReturn)
			{
				SendMyMessage(handler, 2, "Pass");
			}
			else
			{
				SendMyMessage(handler, 2, "Fail");
			}
			delAllFile(TestDirPath);
			myWriteLog(TestDirPath+"PerformanceTest.txt");

            SendMyMessage(handler, 2, "====R/W Performance Test end");
		}






		public String mySelectFileSize()
		{
			String strFileSize = "";
			strSelectFilePath = FlashPathEdit.getText().toString();
			Log.d("333333333333333", strSelectFilePath);
			File dF = new File(strSelectFilePath);

			mLFSize = dF.length();
			//
			iCy = (int)(mLFSize / iBufSize);
			//
			mSYFileSize = (mFileSize % iBufSize);
			if (mSYFileSize != 0)
			{
				++iCy;
			}
			Log.d("22222222222222222", ""+mLFSize);

			int iSize1 = 0, iSize2 = 0;
			if (mLFSize >= 1024*1024*1024)
			{
				iSize1 = (int) (mLFSize/(1024*1024*1024));
				iSize2 = (int) (mLFSize%(1024*1024*1024));
				strFileSize = "" + myTwoDecimal("" + iSize1 + "." + iSize2) + "GB";
			}
			else if (mLFSize >= 1024*1024)
			{
				iSize1 = (int) (mLFSize/(1024*1024));
				iSize2 = (int) (mLFSize%(1024*1024));
				strFileSize = ""+ myTwoDecimal("" + iSize1 + "." + iSize2) + "MB";
			}
			else if (mLFSize >= 1024)
			{
				iSize1 = (int) (mLFSize/1024);
				iSize2 = (int) (mLFSize%1024);
				strFileSize = ""+ myTwoDecimal("" + iSize1 + "." + iSize2) + "KB";
			}
			else if (mLFSize > 0) {
				strFileSize = ""+ mLFSize + "B";
			}
			SendMyMessage(handler, 1, "Create temp " + strFileSize + " buf.");

			return  " " + strFileSize + " file";
		}

		public boolean WriteFileByBuff(String path, char[] data, int BuffSize)
		{
			boolean bReturn = true;
			try {

				BufferedWriter bufWtr = new BufferedWriter (new FileWriter(path), BuffSize * 1024);
				bufWtr.write(data);
				bufWtr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				bReturn = false;
			}
			return bReturn;
		}

		public int ReadFileByBuff(String path, char[] data, int BuffSize, boolean bRead)
		{
			int iReturn = 0;
			try {

				BufferedReader bufRdr = new BufferedReader (new FileReader(path), BuffSize * 1024);
				iReturn = bufRdr.read(data);
				bufRdr.close();
				if (bRead) {
					File fR = new File(path);
					if (!fR.isDirectory()) {
						fR.delete();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return iReturn;
		}


		public boolean RWFileTest(String strTestFilePath, String strFileName, int iBufType)
		{
			boolean bReturn = true;
			long lBeginWrite = 0, lEndWrite = 0, lBeginRead = 0, lEndRead = 0;
			int iWriteTime = 0, iReadTime = 0;
			for (int i = 0; i < iCy; i++)
			{
				iCurSize = iBufSize;
				if (i == iCy-1)
				{
					if (mSYFileSize != 0)
					{
						iCurSize = mSYFileSize;
					}
				}
				Log.d("111111111111111", ""+iCy+"  shengyu:  "+ mSYFileSize);
				bufW = new char[iCurSize];
				try {
					BufferedReader bufRdr = new BufferedReader (new FileReader(strSelectFilePath), iCurSize);
					bufRdr.read(bufW);
					bufRdr.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lBeginWrite = System.currentTimeMillis();
				if (WriteFileByBuff(strTestFilePath, bufW, iBufType))
				{
					lEndWrite = System.currentTimeMillis();
					iWriteTime += (int)(lEndWrite - lBeginWrite);
					if (i == iCy-1)
					{
						SendMyMessage(handler, 2, "Write "+strFileName+" with " + Integer.toString(iBufType)+
								"KB buf  Time= "+ Integer.toString(iWriteTime)+"ms  Speed="+(mLFSize/iWriteTime)*1000/1024 + "KB/s  "
								+ myTwoDecimal("" + ((mLFSize/iWriteTime)*1000/1024)/1024 +"."+((mLFSize/iWriteTime)*1000/1024)%1024) + "MB/S" );
					}
					bufR = new char[iCurSize];
					lBeginRead = System.currentTimeMillis();
					if (ReadFileByBuff(strTestFilePath, bufR, iBufType, false) == iCurSize)
					{
						lEndRead = System.currentTimeMillis();
						iReadTime += (int)(lEndRead - lBeginRead);
						boolean bSame = true;
						//check Read and Write data whether same
						for (int j = 0; j < iCurSize; j++) {
							if (bufR[j] != bufW[j]) {
								bSame = false;
								break;
							}
						}
						if (bSame)
						{
							if (i == iCy-1)
							{
								SendMyMessage(handler, 2, "Read "+strFileName+" with " + Integer.toString(iBufType)+
										"KB buf  Time= "+ Integer.toString(iReadTime)+"ms  Speed="+(mLFSize/iReadTime)*1000/1024 + "KB/s  "
										+ myTwoDecimal("" + ((mLFSize/iWriteTime)*1000/1024)/1024 +"."+((mLFSize/iWriteTime)*1000/1024)%1024) + "MB/S" );
							}
						}
						else
						{
							SendMyMessage(handler, 2, "The "+ strFileName +" with "+ Integer.toString(iBufType)+"KB buf isn't correct.");
							bReturn = false;
							break;
						}
					}
					else
					{
						SendMyMessage(handler, 2, "The length of "+ strFileName +" with "+ Integer.toString(iBufType)+"KB buf isn't correct.");
						bReturn = false;
						break;
					}
				}
				else
				{
					SendMyMessage(handler, 2, "Write "+ strFileName +" with "+ Integer.toString(iBufType)+"KB buf fail.");
					bReturn = false;
					break;
				}
			}
			return bReturn;
		}

		public boolean RWTest(char[] bufWrt, int bufLength, String filePath,String fileName, int bufType)
		{
			boolean bReturn = true;
			//Message msg = handler.obtainMessage();
			long iBegin = 0;
			long iEnd = 0;
			iBegin = System.currentTimeMillis();
			if(WriteFileByBuff(filePath, bufW, bufType))
			{
				iEnd = System.currentTimeMillis();
				//msg.obj = "Write 1M file with 64K buf pass.";
				//msg.arg1 = 1;
				//handler.sendMessage(msg);
				int iTime =(int)(iEnd - iBegin);
				SendMyMessage(handler, 2, "Write "+ fileName +" with "+ Integer.toString(bufType)+"KB buf  Time= "+
						Integer.toString((int)(iEnd - iBegin)) + "ms  Speed=" + (bufLength/iTime)*1000/1024 + "KB/s  "+
						myTwoDecimal(""+((bufLength/iTime)*1000/1024)/1024+"."+((bufLength/iTime)*1000/1024)%1024)+"MB/s");

				char[] bufRead = new char[bufLength + 10];
				iBegin = System.currentTimeMillis();
				if(ReadFileByBuff(filePath, bufRead, bufType, false) == length)
				{
					iEnd = System.currentTimeMillis();
					boolean bSame = true;
					for(int j = 0 ; j < bufLength; ++j){
						if(bufRead[j] != bufWrt[j]){
							bSame = false;
						}
					}

					if(bSame){
						iTime =(int)(iEnd - iBegin);
						SendMyMessage(handler, 2, "Read "+ fileName +" with "+ Integer.toString(bufType)+"KB buf  Time= "+
								Integer.toString((int)(iEnd - iBegin)) + "ms  Speed=" + (bufLength/iTime)*1000/1024 + "KB/s  "+
								myTwoDecimal(""+((bufLength/iTime)*1000/1024)/1024+"."+((bufLength/iTime)*1000/1024)%1024)+"MB/s");

						//	msg.obj = "The " + fileName + " with " + Integer.toString(bufType) + "K buf is correct.";
						//	msg.arg1 = 1;
						//	handler.sendMessage(msg);
					}
					else
					{
						SendMyMessage(handler, 2, "The "+ fileName +" with "+ Integer.toString(bufType)+"KB buf isn't correct.");

						//msg.obj = "The " + fileName + " with " + Integer.toString(bufType) + "K buf isn't correct.";
						//msg.arg1 = 1;
						//handler.sendMessage(msg);
						bReturn = false;
					}
				}
				else
				{
					SendMyMessage(handler, 2, "The length of "+ fileName +" with "+ Integer.toString(bufType)+"KB buf isn't correct.");

					//msg.obj = "The length of " + fileName + " with " + Integer.toString(bufType) + "K buf isn't correct.";
					//msg.arg1 = 1;
					//handler.sendMessage(msg);
					bReturn = false;
				}

			}
			else
			{
				SendMyMessage(handler, 2, "Write "+ fileName +" with "+ Integer.toString(bufType)+"KB buf fail.");

				//msg.obj = "Write " + fileName + " with " + Integer.toString(bufType) + "K buf fail.";
				//msg.arg1 = 1;
				//handler.sendMessage(msg);
			}

			return bReturn;
		}

		public double myTwoDecimal(String strData)
		{
			double dData = Double.parseDouble(strData);
			int j = (int)Math.round(dData * 100);//
			double dRetData = (double)j/100.00;//

			return dRetData;
		}

		public void myWriteLog(String path)
		{
			File file = new File(path);
			file.delete();
			try {
				BufferedWriter bufWtr = new BufferedWriter (new FileWriter(path));
				bufWtr.write(TestResult);
				bufWtr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
				}
				else {
					temp = new File(path + File.separator + tempList[i]);
				}

        if (temp.isFile()) {
            if(!mSDTest) {
                temp.delete();
            } else {
                boolean ret = DocumentsUtils.delete(MainActivity.this, temp);
                Log.i(TAG, "delete: "+ret);
            }
        } //if(temp.isFile())
			}
		}

    public void delSpecialFiles(String path, int min) {
        Date date = new Date(System.currentTimeMillis() - 1000*60*min);
        File folder = new File(path);
        if (!folder.exists()) {
            return;
        }
        if (!folder.isDirectory()) {
            return;
        }
        File[] files = folder.listFiles();
        for (int i=0; i<files.length; i++){
            File file = files[i];
            if (new Date(file.lastModified()).before(date)){
                if(!mSDTest) {
                    file.delete();
                } else {
                    boolean ret = DocumentsUtils.delete(MainActivity.this, file);
                    Log.i(TAG, "delete: "+ret);
                }
            }
        }
    }
//======================================================================================
//======================================================================================
//======================================================================================
//======================================================================================
//======================================================================================
//======================================================================================


		public void RunStep1(String TestDirPath) {

            String  testFilePath;
            FileReader fileR;
            FileWriter fileW;
            String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            Random random;
            char bufW[];
            char bufR[];
            int length = 1024 * 1024;

            SendMyMessage(handler, 2, "====R/W Basic Test start");
			if (mState) {
				//Looper.prepare();
				//handler.removeMessages(0);
				//	Message msg = handler.obtainMessage();
				random = new Random();
				try
				{
					boolean bReturn = true;
					testFilePath = TestDirPath + "RWTest.txt";

					File file = new File(testFilePath);
					file.delete();

					bufW = new char[length];

					SendMyMessage(handler, 2, "Create temp 1M buf.");

					for(int i = 0 ; i < length; ++i){
						int number = random.nextInt(62);//[0,62)
						bufW[i] = str.charAt(number);
					}

					fileW = new FileWriter(testFilePath);

					Log.d("My App", testFilePath);

					SendMyMessage(handler, 2, "Begin writing.");


					fileW.write(bufW);
					fileW.close();

					//read
					fileR = new FileReader(testFilePath);
					bufR = new char[length + 10];

					SendMyMessage(handler, 2, "Begin reading.");

					int iRead = fileR.read(bufR);
					fileR.close();
					if(iRead != length){
						SendMyMessage(handler, 2, "The length isn't correct.");
						bReturn = false;
					}
					else{
						SendMyMessage(handler, 2, "The length is correct.");
					}

					boolean bSame = true;
					for(int i = 0 ; i < length; ++i){
						if(bufR[i] != bufW[i]){
							bSame = false;
						}
					}

					if(bSame){
						SendMyMessage(handler, 2, "The file is correct.");
					}
					else{
						SendMyMessage(handler, 2, "The file isn't correct.");
						bReturn = false;
					}

					if(bReturn)
					{
						SendMyMessage(handler, 2, "Pass");
					}
					else
					{
						SendMyMessage(handler, 2, "Fail");
					}

				}
				catch(IOException e){
					e.printStackTrace();
				}
			}
            SendMyMessage(handler, 2, "====R/W Basic Test end");
		}

    private boolean hasSpaceForSize(long size) {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return false;
        }

        String path = null;
        if(mSDTest) {
            path = external_sdcard_path;
        } else {
            //path = "/mnt/sdcard"; // path:/storage/emulated/0
            path = Environment.getExternalStorageDirectory().getPath();
        }
        //SendMyMessage(handler, 2, "hasSpaceForSize: "+path);
        try {
            StatFs stat = new StatFs(path);
            long space = stat.getAvailableBlocks() * (long) stat.getBlockSize()/(1024*1024);
            SendMyMessage(handler, 2, "hasSpaceForSize: "+space+"M");
            return space > size;
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return false;
    }


	private void RunStep3() {
		String testFilePath;
		FileReader fileR;
		FileWriter fileW;
		String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random;
		char bufW[];
		char bufR[];
		int length = 3 * 1024 * 1024;//FIXME
		int fileCount = 10;
		boolean bReturn = true;
    int total = 0;

		Flashtesttimenum = Integer.parseInt(FlashtesttimeEdit.getText().toString());
		SendMyMessage(handler, 2, "====R/W Reliability Test start");
		long marktime1 = SystemClock.uptimeMillis();
		long marktime2 = 0;
		random = new Random();
		bufW = new char[length];
    bufR = new char[length+10];
		SendMyMessage(handler, 2, "Create temp 3M buf.");
		for (int i = 0; i < length; ++i) {
			int number = random.nextInt(62);// [0,62)
			bufW[i] = str.charAt(number);
		}
    //Log.i(TAG, "bufW "+bufW[0]+" "+bufW[1]+" "+bufW[2]+" "+bufW[3]);
    Currentcycle = 0;
    if(!mSDTest) {
        emmc_health = testGetEmmcHealthStatusApi();
    }
    while (!bStop && marktime2 - marktime1 <= Flashtesttimenum * 60000) {
        Currentcycle++;
        SendMyMessage(handler, 2, "\n#############Cycle:"+ Currentcycle + "#############");
        SendMyMessage(handler, 2, " ");

			marktime2 = SystemClock.uptimeMillis();
			try {
        String filename = new SimpleDateFormat(TIME_STAMP_NAME).format(new Date(System.currentTimeMillis()));
        // write 10 files, and each file has 3M random data
				for (int i = 0; i < fileCount; i++) {
					String TestFileName = "Reliability" + filename + Integer.toString(i) + ".txt";
					testFilePath = ReliabilityPath + TestFileName;
          SendMyMessage(handler, 2, "Begin writing " + TestFileName);
          if(!mSDTest) {
              fileW = new FileWriter(testFilePath);
              //Log.d("My App", testFilePath);
              fileW.write(bufW);
              fileW.close();
          } else {
              try {
                  File fileWrite = new File(testFilePath);
                  OutputStream outputStream = DocumentsUtils.getOutputStream(MainActivity.this,fileWrite);
                  outputStream.write(new String(bufW).getBytes("UTF-8"));
                  outputStream.close();
                  byte[] temp = new String(bufW).getBytes("UTF-8");
                  //Log.i(TAG, "bufW "+temp[0]+" "+temp[1]+" "+temp[2]+" "+temp[3]);
                  //Log.i(TAG,"write successfully" );
                  //Toast.makeText(this,"path: " + testFilePath + " successfully ",Toast.LENGTH_SHORT ).show();
              } catch (IOException e) {
                  e.printStackTrace();
                  Log.i(TAG,"fail to write" );
                  //Toast.makeText(this,"path: " + testFilePath + "falure",Toast.LENGTH_SHORT ).show();
              }
          }
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

					String TestFileName = "Reliability" + filename + Integer.toString(i)+ ".txt";
					testFilePath = ReliabilityPath + TestFileName;
          //bufR = new char[length + 10];
          SendMyMessage(handler, 2, "Begin reading " + testFilePath);
          int iRead = 0;
          String tmp;
          String strRead = "";
          if(!mSDTest) {
              fileR = new FileReader(testFilePath);
              iRead = fileR.read(bufR);
              fileR.close();
          } else {
              try {
                  File fileRead = new File(testFilePath);
                  InputStream is = DocumentsUtils.getInputStream(MainActivity.this,fileRead);
                  InputStreamReader input = new InputStreamReader(is, "UTF-8");
                  BufferedReader reader = new BufferedReader(input);
                  while ((tmp = reader.readLine()) != null) {
                      strRead  +=  tmp;
                  }
                  //Log.i(TAG, "strRead: "+strRead);
                  char[] chr = strRead.toCharArray();
                  iRead = strRead.length();
                  for(int j = 0; j < length; ++j) {
                      bufR[j] = chr[j];
                  }
                  //Log.i(TAG, "Read: "+iRead);
                  //Log.i(TAG, "bufR "+bufR[0]+" "+bufR[1]+" "+bufR[2]+" "+bufR[3]);
              } catch (FileNotFoundException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
              } catch (IOException e) {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
              }
          }
					if (iRead != length) {
						SendMyMessage(handler, 2, "The length of file "	+ Integer.toString(i) + " isn't correct.");
						bReturn = false;
					} else {
						SendMyMessage(handler, 2, "The length of file "	+ Integer.toString(i) + " is correct.");
					}
					boolean bSame = true;
					for (int j = 0; j < length; ++j) {
						if (bufR[j] != bufW[j]) {
							bSame = false;
						}
					}

					if (bSame) {
						SendMyMessage(handler, 2,	"The file " + Integer.toString(i)	+ " is correct.");
					} else {
						SendMyMessage(handler, 2,	"The file " + Integer.toString(i) + " isn't correct.");
						bReturn = false;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
        bStop = true; //FIXME
			}

      //print the total data written in every test cycle
      total = 30*Currentcycle;
      if(total>1024) {
          total = total/1024;
          if(total>1024) {
              total = total/1024;
              SendMyMessage(handler, 2, "Total data written:"+Integer.toString(total)+"TB");
          } else {
              SendMyMessage(handler, 2, "Total data written:"+Integer.toString(total)+"GB");
          }
      } else {
          SendMyMessage(handler, 2, "Total data written:"+Integer.toString(total)+"MB");
      }

      if(!mSDTest) {
          SendMyMessage(handler, 2, "Emmc health status(before test): "+emmc_health);
          emmc_health = testGetEmmcHealthStatusApi();
          SendMyMessage(handler, 2, "Emmc health status(after test): "+emmc_health);
      }
      if(hasSpaceForSize(500) == false) {
          //If less than 500M, delete some files(1G)
          if(!mSDTest) {
              //FIXME
              //Internal FLash: ~8G
              //There is no enough space to write data for 7 minutes
              delSpecialFiles(ReliabilityPath, 4);
          } else {
              bStop = true;//FIXME
          }
      }
		}

		if (bReturn) {
			SendMyMessage(handler, 2, "Pass");
		} else {
			SendMyMessage(handler, 2, "Fail");
		}
		delAllFile(ReliabilityPath);
		SendMyMessage(handler, 2, "====R/W Reliability Test end");
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
                bStop = true;
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
      if(requestCode == DocumentsUtils.OPEN_DOCUMENT_TREE_CODE) {
          if (data != null && data.getData() != null) {
              Uri uri = data.getData();
              DocumentsUtils.saveTreeUri(MainActivity.this, external_sdcard_path, uri);
          }
          return;
      }

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
			ReliabilityPath = FlashPathEdit.getText().toString()
					+ "/StorageTest/RWReliability/";
			(new File(ReliabilityPath)).mkdirs();

            BasicPath = FlashPathEdit.getText().toString()
                    + "/StorageTest/RWBasic/";
            (new File(BasicPath)).mkdirs();

            PerformancePath = FlashPathEdit.getText().toString()
                    + "/StorageTest/RWPerformance/";
            (new File(PerformancePath)).mkdirs();

		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void mySelectPath() {
        mTextPath = (TextView) findViewById(R.id.textView_path);
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
        mTextPath.setVisibility(View.GONE);
        FlashPathEdit.setVisibility(View.GONE);
        FlashPathButton.setVisibility(View.GONE);
	}

}
