package beans.airdection;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.UUID;

//LineChart
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import android.content.Context;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //view content
    private Button button_paired;
    private Button button_find;
    private TextView show_data;
    private ListView event_listView;
    private TextView DangerText;
    private ImageView image;
    private Activity myActivity;
    //bluetooth setting
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> deviceName;
    private ArrayAdapter<String> deviceID;
    private Set<BluetoothDevice> pairedDevices;
    private String choseID;
    private BluetoothDevice bleDevice;
    private BluetoothSocket bluesoccket;
    private InputStream mmInputStream;
    private OutputStream mmOuputStream;
    Thread workerThread;//宣告多執行續名為 wokerThread
    volatile boolean stopWorker; // 宣告布林值 stopWorker, 在主記憶體工作(資料同步)

    private int readBufferPosition;
    private byte[] readBuffer;
    private String uid;
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102; // android 6.0 之後定位設備授權請求代碼

    public int PMvalue=0;
    public Resources res;
    public Bitmap a;

    private LineChart lineChart;
    private ArrayList<Entry> entries; // 存儲 PM2.5 數據
    private int timeIndex = 0; // 時間指標
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize the Chart
        lineChart = findViewById(R.id.lineChart); // 找到 LineChart
        setupYAxis(); // 設置 Y 軸範圍
        beginListenForData(); // 開始接收數據

        //圖表等到藍芽連接後再出現
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);
        lineChart.setNoDataText("");
        lineChart.setVisibility(View.VISIBLE);

        myActivity = MainActivity.this;
        res = getResources();
        a= BitmapFactory.decodeResource(res, R.drawable.a000);
        getView();
        setListener();
        deviceName = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
        deviceID = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
        requestLocationPermission();
        DangerText.setText(" ");
        image.setImageResource(R.drawable.a000);
    }

    private void requestLocationPermission() {
        // 如果裝置版本是6.0（包含）以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 取得授權狀態，參數是請求授權的名稱
            int hasPermission = checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION);

            // 如果未授權
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                // 請求授權
                //     第一個參數是請求授權的名稱
                //     第二個參數是請求代碼
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
                return;
            }
        }
    }

    private void getView() {
        button_paired = (Button) findViewById(R.id.btn_paired);
        show_data = (TextView) findViewById(R.id.txtShow);
        event_listView = (ListView) findViewById(R.id.Show_B_List);
//        button_find = (Button) findViewById(R.id.btn_conn);
        DangerText=  (TextView) findViewById(R.id.DangerText);
        image= (ImageView) findViewById(R.id.pmimage);
        TextView theinfo = (TextView) findViewById(R.id.textView3);
        theinfo.setText("Particle pollution from fine particulates (PM2.5) is a concern when levels in air are unhealthy. Breathing in unhealthy levels of PM2.5 can increase the risk of health problems like heart disease, asthma, and low birth weight.");

    }

    private void setListener() {
        button_paired.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                findPBT();

            }
        });
//        button_find.setOnClickListener(new Button.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                findPBT();
//            }
//        });
        event_listView.setAdapter(deviceName);
        event_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> partent, View view, int position, long id) {
                choseID = deviceID.getItem(position);
                try {
                    openBT(choseID);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(myActivity, "選擇了:" + choseID, Toast.LENGTH_SHORT).show();
                deviceName.clear();

            }
        });
    }

    private void findPBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//儲存已配對藍芽設備名稱
        if (mBluetoothAdapter != null) { // 取得是否有藍芽裝置
            //show_data.setText("No bluetooth adapter available"); //若沒有，則顯示找不到藍芽裝置
        }
        if (!mBluetoothAdapter.isEnabled()) { //檢查手機藍芽是否開啟，沒有的話，則跳到android設定藍芽畫面
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1); //回傳找到幾個藍芽周邊
        }
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String str = "已配對完成的裝置有 " + device.getName() + " " + device.getAddress() + "\n";
                //String
                uid = device.getAddress();

                bleDevice = device;
                deviceName.add(str);//將以配對的裝置名稱儲存，並顯示於LIST清單中
                deviceID.add(uid); //好像沒用到
            }
            event_listView.setAdapter(deviceName);
        }
    }

    private void openBT(String choseID) throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //藍芽模組UUID好像都是這樣
        if(pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                bleDevice = device;
                if (device.getAddress().equals(choseID))
                    break;
            }
        }
        if (bleDevice != null) {//DeviceID != null // 如果有找到設備
            bluesoccket = bleDevice.createRfcommSocketToServiceRecord(uuid); //使用被選擇的設備UUID建立連線
            try {
                bluesoccket.connect();
            } catch (IOException e) {
                Log.e("BluetoothError", "Failed to connect to the device: " + e.getMessage());
                // 在主執行緒顯示錯誤訊息
                runOnUiThread(() -> Toast.makeText(myActivity, "無法連接到藍牙設備", Toast.LENGTH_SHORT).show());
                return;  // 中止連接
            }
            mmOuputStream = bluesoccket.getOutputStream();
            mmInputStream = bluesoccket.getInputStream();
            if (mmInputStream == null) {
                Log.e("BluetoothError", "InputStream is null.");
            }
            beginListenForData();
//            View b1 = findViewById(R.id.btn_conn);
            View b2 = findViewById(R.id.btn_paired);
            View b3 = findViewById(R.id.Show_B_List);
//            b1.setVisibility(View.INVISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.INVISIBLE);
        }
    }

    private void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; // 將十進制，轉換成ASCII code LF(line feed-換行)

        stopWorker = false; // 監控多執行續的運作(false為開啟)
        readBufferPosition = 0; // readBuffer 陣列位置 ， 預設為0
        readBuffer = new byte[4096]; // 宣告一個為byte資料型別的陣列


        workerThread = new Thread(new Runnable() { //建立Thread是否運作，是的話進入迴圈，不是就跳出迴圈
            @Override
            public void run() { // Thread物件會調用Runnable物件的run()方法，來控制。
                while (!Thread.currentThread().isInterrupted() && !stopWorker) // 監控Thread是否運作，是的話進入迴圈，不是就跳出迴圈
                {
                    try {
                        if(mmInputStream != null) {
                            int bytesAvailable = mmInputStream.available(); // 宣告接收資料變數
                            //                        Log.d("value","before bytes is available");
                            if (bytesAvailable > 0) //如果有資料進來
                            {
                                Log.d("value", "bytes is available");
                                byte[] packetBytes = new byte[bytesAvailable]; // 宣告byte陣列，數量由bytesAvailable決定
                                mmInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];   // 將資料一個個從packetBytes取出直到b的變數
                                    if (b == delimiter) // 如果b等於換行指令
                                    {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length); //把readBuffer陣列的值複製到encodeBytes陣列裡
                                        final String data = new String(encodedBytes); // 轉成文字，無法被更改
                                        char[] tmp = new char[readBufferPosition];

                                        PMvalue = 0;
                                        for (int j = 0; j < encodedBytes.length - 1; j++) {
                                            if (encodedBytes[j] >= '0' && encodedBytes[j] <= '9') {
                                                PMvalue = PMvalue * 10 + encodedBytes[j] - '0';
                                            } else {
                                                // 無效數字，可能需要處理錯誤情況
                                                Log.e("DataError", "Invalid character in PM2.5 data: " + encodedBytes[j]);
                                                PMvalue = -1; // 表示資料錯誤
                                                break;
                                            }
                                        }

                                        String tmp1 = String.valueOf(PMvalue);
                                        Log.d("value", tmp1);
                                        readBufferPosition = 0; //readBuffer 陣列位置，歸零。

                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                long date = System.currentTimeMillis();
                                                TextView tvDisplayDate = (TextView) findViewById(R.id.DATE);
                                                TextView theinfo = (TextView) findViewById(R.id.textView3);
                                                theinfo.setText("Fine particles in the air (measured as PM2.5) are so small that they can travel deeply into the respiratory tract, reaching the lungs, causing short-term health effects such as eye, nose, throat and lung irritation, coughing, sneezing, runny nose, and shortness of breath. Exposure can also affect heart and lung function, worsening medical conditions like heart disease and asthma, and increase the risk for heart attacks.");
                                                SimpleDateFormat sdf = new SimpleDateFormat("MMM MM dd, yyyy h:mm a");
                                                String dateString = sdf.format(date);
                                                tvDisplayDate.setText("Update Time : " + dateString);

                                                if (PMvalue < 36) {

                                                    if (PMvalue > 23)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a03);
                                                    else if (PMvalue > 11)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a02);
                                                    else if (PMvalue > 0)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a01);
                                                    image.setImageBitmap(a);
                                                    DangerText.setText("良好 ");
                                                    show_data.setText(data);
                                                    addEntry(PMvalue);
                                                } else if (PMvalue < 54) {
                                                    if (PMvalue > 47)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a04);
                                                    else if (PMvalue > 41)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a05);
                                                    else if (PMvalue > 35)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a06);
                                                    image.setImageBitmap(a);
                                                    DangerText.setText("警戒");
                                                    show_data.setText(data);
                                                    addEntry(PMvalue);
                                                } else if (PMvalue < 71) {
                                                    if (PMvalue > 64)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a09);
                                                    else if (PMvalue > 58)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a08);
                                                    else if (PMvalue > 53)
                                                        a = BitmapFactory.decodeResource(res, R.drawable.a07);
                                                    image.setImageBitmap(a);
                                                    DangerText.setText("過量");
                                                    show_data.setText(data);
                                                    addEntry(PMvalue);
                                                } else if (PMvalue > 70) {
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a10);
                                                    image.setImageBitmap(a);
                                                    DangerText.setText("危險");
                                                    show_data.setText(data);
                                                    addEntry(PMvalue);
                                                }
                                            }
                                        });
                                    } else // 若沒有換行，一直存進來
                                    {
                                        readBuffer[readBufferPosition++] = b; //將接受到資料放入陣列裡面
                                    }
                                }
                            }
                        }else{
                            Log.e("MainActivity", "InputStream is null.");
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });
        workerThread.start();
    }

    private double lastValidPMvalue = -1;

    private void addEntry(double PMvalue) {
        // 檢查接收到的PMvalue是否大於0
        if (PMvalue > 0) {
            lastValidPMvalue = PMvalue; // 更新最近一次有效的PMvalue
        } else if (lastValidPMvalue > 0) {
            PMvalue = lastValidPMvalue; // 如果新PMvalue無效，則使用最近一次有效的PMvalue
        }

        // 在折線圖中添加新的數據點
        if (entries == null) {
            entries = new ArrayList<>(); // 初始化 entries
        }

        entries.add(new Entry(timeIndex++, (float) PMvalue)); // timeIndex 作為 x 軸，PMvalue 作為 y 軸

        LineDataSet dataSet = new LineDataSet(entries, "PM2.5 Concentration");
        dataSet.setColor(Color.BLUE); // 設定折線顏色
        dataSet.setValueTextColor(Color.BLACK); // 設定數值顏色
        dataSet.setValueTextSize(10f); // 設置值的字體大小
        dataSet.setDrawCircles(true); // 是否顯示圓圈
        dataSet.setDrawValues(true); // 是否顯示數值

        // 設置X軸顯示最大範圍為20
        lineChart.setVisibleXRangeMaximum(5);

        // 自動滾動至最新的數據點
        lineChart.moveViewToX(timeIndex - 5); // 保證圖表顯示最新的20個點

        // 隱藏X軸的數值
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawLabels(false); // 不顯示 X 軸數值

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // 更新折線圖
    }

    private void setupYAxis() {
        // 獲取左側的 Y 軸
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // 設置 Y 軸最小值為 0
        leftAxis.setAxisMaximum(100f); // 設置 Y 軸最大值為 100

        // 如果不需要右側的 Y 軸，可以禁用它
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false); // 禁用右側的 Y 軸
    }
}
