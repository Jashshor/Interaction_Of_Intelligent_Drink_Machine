package trash_can.jashshor.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.friendlyarm.FriendlyThings.HardwareControler;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ImageButton arcView1;
    private ImageButton arcView2;
    public int bugs = 0;
    private TextView bug;
    private TextView julidui;
    private Button button5;
    private Button button4;
    MediaPlayer mp=MediaPlayer.create(this,R.raw.meow);
    private TextView zhuangtai;

    private String devName = "/dev/ttyAMA3";             //用哪个串口，需要改成对应串口文件
    private int speed = 9600;		//波特率
    private int dataBits = 8;		//数据位
    private int stopBits = 1;		//停止位
    private int devfd = -1;         //devfd表示串口打开（成功）与否，初始关闭
    private final int BUFSIZE = 512;
    private byte[] buf = new byte[BUFSIZE];
    private Timer timer = new Timer();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) this.findViewById(R.id.button2);   //维护模式按钮
        zhuangtai =(TextView)findViewById(R.id.zhuangtai);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {            //切换activity
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, DebugActivity.class);
                startActivity(intent);
                bugs = 0;
                bug.setText("错误报告："+ bugs);//维护后bugs清零，状态变正常
                zhuangtai.setText("系统状态：正常");
            }});
        Button button3 = (Button) this.findViewById(R.id.button3);      //故障反馈按钮
        bug = (TextView)findViewById(R.id.bug);
        julidui = (TextView)findViewById(R.id.julidui);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bugs = bugs +1;
                bug.setText("错误报告："+ bugs);
                if(bugs ==20 ) zhuangtai.setText("系统状态：可能异常");
            }
        });
        devfd = HardwareControler.openSerialPort( devName, speed, dataBits, stopBits );		//重写 打开串口 方法，设定传输参数，获取串口打开状态
        if (devfd >= 0) {
            zhuangtai.setText("系统状态：正常");
            timer.schedule(task, 0, 500);  //设置 timer ：0延时，500周期——重复task
        } else {
            devfd = -1;
            Toast.makeText(this,"出现错误，已记录",Toast.LENGTH_SHORT).show();
            bugs = bugs+1;
            bug.setText("错误报告："+ bugs);
            zhuangtai.setText("系统状态：异常");           //状态异常为真异常（串口异常），bugs更偏向用户反馈情况
        }
        button5 =(Button)findViewById(R.id.button5);
        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {           //GO命令运转电机
                int ret = HardwareControler.write(devfd, "go".getBytes());

                if (ret > 0) {
                    Toast.makeText(MainActivity.this,"Succeed in sending!",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,"Fail to send!",Toast.LENGTH_SHORT).show();
                }
            }
        });
        arcView1 = (ImageButton)findViewById(R.id.arc1);
        arcView1.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {//确认可乐
               AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
               builder.setTitle("确认");
               builder.setMessage("请确认是否选择可乐？" ) ;
               builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialogInterface, int i) { }}); //取消无指令
               builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialogInterface, int i) {
                       int ret = HardwareControler.write(devfd, "black".getBytes());
                   }
               }).show();
           }});
        arcView2 = (ImageButton)findViewById(R.id.arc2);
        arcView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("确认");
                builder.setMessage("请确认是否选择橙汁？" ) ;
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }}); //取消无指令
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int ret = HardwareControler.write(devfd, "orange".getBytes());
                    }
                }).show();
            }});
    }
private TimerTask task = new TimerTask() {
    public void run() {
        Message message = new Message();
        message.what = 1;				//接收标志位
        handler.sendMessage(message);			//触发信息接收handler ，转至上一段handler处理
    }
};


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (HardwareControler.select(devfd, 0, 0) == 1) {       //以下：timer 通过调用 select 接口轮询串口设备是否有数据到来
                        int retSize = HardwareControler.read(devfd, buf, BUFSIZE);      //读取串口状态，传入数据，数据位数
                        if (retSize > 0) {
                            String str = new String(buf, 0, retSize); //buf二进制数 offset解码偏移量 retSize位数
                            switch(str)                         //处理传入数据                       //TODO 测试传输数据，约定数据内容
                            {
//                                case "orange": yanse.setText("传感器1：橘黄色");break;
//                                case "black": yanse.setText("传感器1：黑色");break;
//                                case "dianjigongzuo": dianji.setText("伺服电机：工作");break;
//                                case "dianjibugongzuo": dianji.setText("伺服电机：未工作");break;
                                default:
                                    int in = Integer.parseInt(str);
                                    if(in >= 10 & in< 80) julidui.setText("用户距离：正常");
                                    else  julidui.setText("用户距离：异常  请站在适当位置");
                                break;
                            }
                        }
                    }
                break;          //标准格式 跳出switch case语句
            }
            super.handleMessage(msg);       //父类继承
        }
    };
}