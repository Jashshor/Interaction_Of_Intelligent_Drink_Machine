package trash_can.jashshor.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.friendlyarm.FriendlyThings.HardwareControler;

import java.util.Timer;
import java.util.TimerTask;

public class DebugActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SerialPort"; //串口通信

    private TextView fromTextView = null;
    private EditText toEditor = null; //初始化收、发栏

    private TextView dianji = null;
    private TextView yanse = null;
    private TextView juli = null;

    private final int MAXLINES = 200; //收发极限
    private StringBuilder remoteData = new StringBuilder(256 * MAXLINES); //初始化收发变量，规定字节数

    private String devName = "/dev/ttyAMA3";             //用哪个串口，需要改成对应串口文件
    private int speed = 9600;		//波特率
    private int dataBits = 8;		//数据位
    private int stopBits = 1;		//停止位
    private int devfd = -1;         //devfd表示串口打开（成功）与否，初始关闭

    @Override
    public void onDestroy() {       //周期末收尾工作
        timer.cancel();
        if (devfd != -1) {
            HardwareControler.close(devfd);
            devfd = -1;
        }
        super.onDestroy();      //父类继承
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        String winTitle = devName + "," + speed + "," + dataBits + "," + stopBits;
        setTitle(winTitle);   														//改变标题 暂定

        ((Button)findViewById(R.id.sendButton)).setOnClickListener(this);	//发送监听
        ((Button)findViewById(R.id.back)).setOnClickListener(this);
        ((Button)findViewById(R.id.test)).setOnClickListener(this);
        fromTextView = (TextView)findViewById(R.id.fromTextView);		//接收栏
        toEditor = (EditText)findViewById(R.id.toEditor);		//发送栏
        dianji = (TextView)findViewById(R.id.dianji);
        yanse = (TextView)findViewById(R.id.yanse);
        juli = (TextView)findViewById(R.id.juli);
        /* no focus when begin */
        toEditor.clearFocus();
        toEditor.setFocusable(false);
        toEditor.setFocusableInTouchMode(true);
        devfd = HardwareControler.openSerialPort( devName, speed, dataBits, stopBits );		//重写 打开串口 方法，设定传输参数，获取串口打开状态
        if (devfd >= 0) {
            timer.schedule(task, 0, 500);  //设置 timer ：0延时，500周期——重复task
        } else {
            devfd = -1;
            fromTextView.append("Fail to open " + devName + "!");
        }
    }

    private final int BUFSIZE = 512;
    private byte[] buf = new byte[BUFSIZE];
    private Timer timer = new Timer();
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (HardwareControler.select(devfd, 0, 0) == 1) {       //以下：timer 通过调用 select 接口轮询串口设备是否有数据到来
                        int retSize = HardwareControler.read(devfd, buf, BUFSIZE);      //读取串口状态，传入数据，数据位数
                        if (retSize > 0) {
                            String str = new String(buf, 0, retSize); //buf二进制数 offset解码偏移量 retSize位数
                            remoteData.append(str);             //最终传入数据增添，                 //TODO str为获取数据
                            switch(str)                         //处理传入数据                       //TODO 测试传输数据，约定数据内容
                            {
                                case "orange": yanse.setText("传感器1：橘黄色");break;
                                case "black": yanse.setText("传感器1：黑色");break;
                                case "dianjigongzuo": dianji.setText("伺服电机：工作");break;
                                case "dianjibugongzuo": dianji.setText("伺服电机：未工作");break;
                                default: juli.setText("距离传感器：" + str + "cm");
                            }
                            //Log.d(TAG, "#### LineCount: " + fromTextView.getLineCount() + ", remoteData.length()=" + remoteData.length());
                            if (fromTextView.getLineCount() > MAXLINES) {       //输入栏数据行数判断大于最大值
                                int nLineCount = fromTextView.getLineCount();
                                int i = 0;
                                for (i = 0; i < remoteData.length(); i++) {     //i从0位到总数据最后一位，可能是截断溢出值 //TODOing
                                    if (remoteData.charAt(i) == '\n') {
                                        nLineCount--;                       //识别 '\n'
                                        if (nLineCount <= MAXLINES) {
                                            break;
                                        }
                                    }
                                }
                                remoteData.delete(0, i);
                                //Log.d(TAG, "#### remoteData.delete(0, " + i + ")");
                                fromTextView.setText(remoteData.toString());
                            } else {
                                fromTextView.append(str);       //行数正常就直接加到输出框就得了 //TODO EditText增加string
                            }
                        }
                    }
                break;          //标准格式 跳出switch case语句
            }
            super.handleMessage(msg);       //父类继承
        }
    };
    private TimerTask task = new TimerTask() {
        public void run() {
            Message message = new Message();
            message.what = 1;				//接收标志位
            handler.sendMessage(message);			//触发信息接收handler ，转至上一段handler处理
        }
    };

    public void onClick(View v)         //发送信息监听 线程
    {
        switch (v.getId()) {					//识别点击按钮点击按钮ID
            case R.id.sendButton:
                String str = toEditor.getText().toString();         //获取发送文
                if (str.length() > 0) {
                    if (str.charAt(str.length()-1) != '\n') {
                        str = str + "\n";           //增加换行符
                    }
                    int ret = HardwareControler.write(devfd, str.getBytes());       //转化二进制调用write接口发送 //TODO 发送数据方式
                    if (ret > 0) {              //依据发送成功与否分类
                        toEditor.setText("");           //TODO setText("") 清空EditText
                        str = ">>> " + str;
                        if (remoteData.length() > 0) {
                            if (remoteData.charAt(remoteData.length()-1) != '\n') {
                                remoteData.append('\n');
                                fromTextView.append("\n");
                            }
                        }
                        remoteData.append(str);
                        fromTextView.append(str);       //在显示上和数据存储上按'“>>>”+?'格式增加发送值（实际上也没必要区分成两个量）
                    } else {
                        Toast.makeText(this,"Fail to send!",Toast.LENGTH_SHORT).show();
                    }
                }break;
            case R.id.back:
                finish();
                break;
            case R.id.test:
                int ret = HardwareControler.write(devfd, "go".getBytes());        //TODO 测试传输数据，约定数据内容
                if (ret > 0) {              //提示发送成功与否
                    Toast.makeText(this,"Succeed in sending!",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,"Fail to send!",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}

/**
    *   发送：
*       Go命令
 *
 *      接收：
 *
* */