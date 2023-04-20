package com.example.filetransfer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.namespace.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    static final int PORT = 8082;
    static final String INCOME_MESSAGE = "I am coming";
    static final String CONFIRM_MESSAGE = "Welcome";
    static final String QUIT_MESSAGE = "Bye bye";
    static final String QUERY_MESSAGE = "Send you some files?";
    static final String ANSWER_YES = "Agree!";
    static final String ANSWER_NO = "Not agree!";

    WifiManager wManager;
    String BroadcastIp;
    DatagramSocket receiver;
    Button btnFlush, btnSend;
    Spinner spinner_target;
    ProgressDialog pDialog;

    List<String> idList; //ip address list
    ArrayAdapter<String> sAdapter; //ip
    List<FileItem> idList2; // file list
    localFileAdapter lAdapter;
    StringBuffer strBuff;   // sending file list

    NotificationManager nManager;
    NotificationChannel channel1;
    Notification notice1;

    RemoteViews rmvs;
    long llast,lTotal, lFileSize; // startTcpSocket
    Timer  timerShowNotice;
    TimerTask task1;
    boolean bShowNotice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int permit1,permit2;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//////////////////////////////////////////////////////////////////////////////
        wManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wManager.isWifiEnabled())
        {
            BroadcastIp = intToString(wManager.getConnectionInfo().getIpAddress());
            int iPos = BroadcastIp.lastIndexOf('.') + 1;
            BroadcastIp =BroadcastIp.substring(0, iPos) + "255";  //broadcast address.
            StartReceiver();
        }


//////////////////////////////////////////////////////////////////////////////////////////////
        btnFlush = findViewById(R.id.button3);
        btnSend = findViewById(R.id.button_send);
        ListView listView_fileList = findViewById(R.id.file_list);
        spinner_target = findViewById(R.id.target_list);
        strBuff = new StringBuffer();
///////////////////////////////////////////////////////////////////////////////////////////////////////
        nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            channel1 = new NotificationChannel("send", "sending files", NotificationManager.IMPORTANCE_LOW);
            nManager.createNotificationChannel(channel1);
        }

////////////////////////////////////////////////////////////////////////////////////////////////////////
        permit1 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        permit2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permit1 != PackageManager.PERMISSION_GRANTED || permit2 != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        idList = new LinkedList<>();
        sAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, idList);
        spinner_target.setAdapter(sAdapter);
///////////////////

        idList2 = new ArrayList<>();
        lAdapter = new localFileAdapter(this, R.layout.file_item_layout, idList2);
        listView_fileList.setAdapter(lAdapter);
        listView_fileList.setOnItemClickListener((parent, view, position, id) -> {
            if (idList2.get(position).getFile().isDirectory())
            {
                listSort(idList2.get(position).getFile());
                listView_fileList.setSelection(0);
            }
        });
        listView_fileList.setOnItemLongClickListener((parent, view, position, id) -> {
            if(idList2.get(position).getFile().isFile())
            {
                CheckBox cb = view.findViewById(R.id.item_checkbox);
                cb.setVisibility(View.VISIBLE);
                cb.setChecked(true);
                idList2.get(position).setChecked(true);
                return true;
            }
           return false;
        });
/////////////////////////////////////

        btnFlush.setOnClickListener(v -> {
            idList.clear();
            SendConfirmation(BroadcastIp,INCOME_MESSAGE + " " + Build.BRAND + " " + Build.MODEL);
        });
////////////////////////////////////////////

        btnSend.setOnClickListener(v -> {
            strBuff.delete(0,strBuff.length());
            for(FileItem value: idList2)
            {
                if(value.getChecked())
                {
                    String temp = value.getFile().getName();
                    temp = temp.replace("--","");
                    temp = temp.replace("\n","");
                    strBuff.append(temp);
                    strBuff.append("--");
                    strBuff.append(value.getFile().length());
                    strBuff.append("\n");
                }
            }
            if(strBuff.length() >0)
            {
                String strTemp = spinner_target.getSelectedItem().toString();
                int kk = strTemp.indexOf("  ");
                strTemp = strTemp.substring(0,kk);
                SendConfirmation(strTemp,QUERY_MESSAGE + strBuff.toString());

            }
            else
                Toast.makeText(this, "You did not select any files! ", Toast.LENGTH_SHORT).show();

        });
/////////////////////////////////////////////////////////////////////////////////////////
        File file = Environment.getExternalStorageDirectory();
        listSort(file);
////////////////////////////////////////////////////////////////////////////////////
        timerShowNotice = new Timer();
        llast = 0;
        bShowNotice = false;
        task1 = new TimerTask() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                if(bShowNotice) {
                    float speed = (float) (lTotal - llast) / (1024 * 1024);
                    int mm = (int) (lTotal * 100 / lFileSize);
                    llast = lTotal;
                    rmvs.setProgressBar(R.id.rmvs_progressBar, 100, mm, false);
                    String str = String.format("%3.2f MB/S   %d", speed, mm);
                    rmvs.setTextViewText(R.id.rmvs_percent, str + "%");
                    nManager.notify(100, notice1);
                }
            }
        };
        timerShowNotice.schedule(task1,0,1000);
//////////////////////////////////////////////////////////////////////////////////////
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiver.close();
        SendConfirmation(BroadcastIp, QUIT_MESSAGE);
    }
    //UDP server always keep on....
    private void StartReceiver() {
        new Thread(() -> {
            byte[] inBuff = new byte[1024];
            String strRecv, strIP;
            DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length);
            try {
                receiver = new DatagramSocket(PORT);  // server is ready
                SendConfirmation(BroadcastIp,INCOME_MESSAGE + " " + Build.BRAND + " " + Build.MODEL); //send yourself ID with INCOM_MESSAGE
               // you get message contains usful infomations and address which send you theses messages!
                while (true) {
                    receiver.receive(inPacket);
                    strRecv = new String(inBuff, 0, inPacket.getLength()); //message you received.

                    if (strRecv.contains(INCOME_MESSAGE)) {
                        UpdateAddrList(inPacket.getAddress().toString() + strRecv); //update the addr list.
                        strIP = intToString(wManager.getConnectionInfo().getIpAddress()); //local computer IP
                        if (!inPacket.getAddress().toString().contains(strIP))
                            SendConfirmation(inPacket.getAddress().getHostAddress(), CONFIRM_MESSAGE + Build.BRAND + " " + Build.MODEL);//you get

                    }
                    if (strRecv.contains(CONFIRM_MESSAGE) || strRecv.contains(QUIT_MESSAGE))
                    {
                        UpdateAddrList(inPacket.getAddress().toString() + strRecv); //update the addr list.

                    }
                    if (strRecv.contains(QUERY_MESSAGE))
                    {
                        ShowQueryDialog(inPacket.getAddress().getHostAddress(),strRecv);

                    }
                    if (strRecv.contains(ANSWER_YES))
                    {
                        startTcpSocket(inPacket.getAddress().getHostAddress());   //with strBuff sending file list.

                    }
                    if (strRecv.contains(ANSWER_NO))
                       UpdateFileList();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    //sending files, will be finished when sending completed.
    private void startTcpSocket(String address) {
      new Thread(() -> {
          try {
              byte[]  bBuff = new byte[1024];
              int hasRead;

              Thread.sleep( Math.round(Math.random() * 2000));
              for(FileItem value: idList2)
              {
                  lTotal = lFileSize = 0;
                  if(value.getChecked())
                  {
                      //sending files......
                      Thread.sleep(500);
                      lFileSize = value.getFile().length();
                      rmvs = new RemoteViews(getPackageName(), R.layout.rmvs_layout);
                      showNotice(true,value.getFile().getName(),100,0);
                      Socket sender = new Socket(address,8082);
                      OutputStream os = sender.getOutputStream();
                      FileInputStream fis = new FileInputStream(value.getFile());
                     // timerShowNotice.schedule(task1,0,1000);
                      bShowNotice = true;
                      while((hasRead = fis.read(bBuff)) > 0)
                      {
                          os.write(bBuff,0,hasRead);
                          os.flush();
                          lTotal += hasRead;
                      }
                      fis.close();
                      os.close();
                      sender.close();
                      bShowNotice = false;
                    //  timerShowNotice.schedule(task1,0,0);
                    showNotice(false,value.getFile().getName(),100,100);
                  }
              }
              UpdateFileList();
          } catch (IOException | InterruptedException e) {
              e.printStackTrace();
          }

      }).start();
    }

    private void showNotice(boolean bNew, String fileName, int lMax, int lProgress) {
        runOnUiThread(() -> {
            int kk;
            if(bNew)
            {
                if (Build.VERSION.SDK_INT >= 26) {
                    rmvs.setTextViewText(R.id.rmvs_title, fileName + " is sending...");
                    rmvs.setProgressBar(R.id.rmvs_progressBar,lMax, lProgress, false);
                    rmvs.setTextViewText(R.id.rmvs_percent, "0%");
                    notice1 = new NotificationCompat.Builder(MainActivity.this, "send")
                            .setSmallIcon(R.mipmap.ic_jump)
                            .setWhen(System.currentTimeMillis())
                            .setContent(rmvs)
                            .setAutoCancel(false)
                            .build();
                }
                else
                    notice1 = new NotificationCompat.Builder(MainActivity.this,"send")
                            .setSmallIcon(R.mipmap.ic_jump)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(false)
                            .build();
                nManager.notify(100,notice1);
            }
            if(lMax == lProgress)
                nManager.cancel(100);
        });
    }


    //Receiving files,  will be finished when receiving completed.
    private void startTcpServerSocket(String fileList) {
        //prepare sending files delay for 2 seconds
        // remember to store half files when stops in middle way.
        new Thread(() -> {
            try {
                String strName = fileList;
                byte[] bBuff = new byte[1024];
                int iStart,iStop,hasread;
                long lTotal,lFileSize;
                ServerSocket server = new ServerSocket(8082);
                while (strName.length() > 0)
                {
                    Socket client = server.accept();
                    InputStream in = client.getInputStream();
                    iStart = strName.indexOf("--");
                    iStop = strName.indexOf("\n");
                    String str = Environment.getExternalStorageDirectory().getPath() + "/Download/" + strName.substring(0,iStart);
                    lFileSize = Long.parseLong(strName.substring(iStart+2,iStop));
                    showProgressDialog(true,strName.substring(0,iStart),lFileSize,0);
                    strName = strName.substring(iStop + 1);
                    FileOutputStream fos = new FileOutputStream(str);
                    lTotal = 0;
                    while ((hasread = in.read(bBuff)) > 0)
                    {
                        fos.write(bBuff,0,hasread);
                        fos.flush();
                        lTotal += hasread;
                        showProgressDialog(false,null,0,lTotal);
                    }
                    in.close();
                    fos.close();
                    client.close();
                    showProgressDialog(false,null,lTotal,lTotal);
                }
                server.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void UpdateFileList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(FileItem value: idList2)
                    if(value.getChecked())
                        value.setChecked(false);
                lAdapter.notifyDataSetChanged();
            }
        });

    }
    //receive query from other phones, you will make your decisions by this dialog
    private void ShowQueryDialog(String address,String message) {
       runOnUiThread(new Runnable() {
           @Override
           public void run() {
               String strTemp = message.replace(QUERY_MESSAGE,"");
               AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
               builder.setTitle("New Files From " + address)
                       .setMessage( strTemp)
                       .setCancelable(false)
                       .setPositiveButton("yes", (dialog, which) ->
                       {
                          //prepare to receiving files immed
                           startTcpServerSocket(strTemp);
                           SendConfirmation(address,ANSWER_YES);
                       })
                       .setNegativeButton("no", (dialog, which) -> SendConfirmation(address,ANSWER_NO))
                       .create()
                       .show();
           }
       });

   }

    private void showProgressDialog(boolean bNew, String fileName, long lMax, long lProgress) {
        runOnUiThread(() -> {
            if(bNew)
            {
                pDialog = new ProgressDialog(MainActivity.this);
                pDialog.setTitle(fileName +" is copying");
                pDialog.setMax((int)lMax);
                pDialog.setCancelable(false);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.show();
            }
            pDialog.setProgress((int)lProgress);
            if(lMax == lProgress)
                pDialog.cancel();

        });

    }

    //short information sending program.
    private void SendConfirmation( String address,String message) {
        new Thread(() -> {
            DatagramPacket outPacket;
            byte[] out;
            try {
                DatagramSocket sender = new DatagramSocket();
                if(message.contains(CONFIRM_MESSAGE))
                    Thread.sleep(Math.round(Math.random() * 2000));
                outPacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName(address), PORT);
                out = message.getBytes(StandardCharsets.UTF_8);
                outPacket.setData(out);
                sender.send(outPacket);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void UpdateAddrList(final String address) {
        runOnUiThread(() -> {
            int i, iSize = idList.size();
            String strTemp,strAdd;
            if (address.contains(QUIT_MESSAGE))
            {
               i = address.indexOf(QUIT_MESSAGE);
               strTemp = address.substring(1,i);
               for (i = 0; i < iSize; i++)
               {
                 if (idList.get(i).contains(strTemp))
                 {
                     idList.remove(i);
                     break;
                 }
               }
            }
            else
            {
                strAdd = address.replace(CONFIRM_MESSAGE, "  ");
                strAdd = strAdd.replace(INCOME_MESSAGE, "  ");
                strAdd = strAdd.substring(1);
                i = strAdd.indexOf("  ");
                strTemp = address.substring(0,i);
                for (i = 0; i < iSize; i++)
                {
                    if (idList.get(i).contains(strTemp))
                        strTemp = null;
                }
                if(strTemp.length() > 0)
                    idList.add(strAdd);
               //get rid of repeating item.
            }
            sAdapter.notifyDataSetChanged();

        });
    }

    private String intToString(int ip) {
        return (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff) + "."
                + ((ip >> 24) & 0xff);
    }

    private void listSort(File dir) {
        List<File> listFolder = new ArrayList<>();
        List<File> listFiles = new ArrayList<>();
        int j,iMin,iCount;

        File[] tempList = dir.listFiles();
        assert tempList != null;
        for(File value: tempList)
        {
            if(value.isDirectory())
                listFolder.add(value);
            else
                listFiles.add(value);
        }
        idList2.clear();
        if(!dir.getPath().equals("/storage/emulated/0"))
        {
            FileItem item = new FileItem(dir.getParentFile(),false);
            idList2.add(item);
        }
       //folder sort functions

        iCount = listFolder.size();
       while (iCount > 0)
        {
            iMin = 0;
            String strTempName = listFolder.get(0).getName().toLowerCase();
            for (j = 1; j < iCount; j++) {
                if (listFolder.get(j).getName().toLowerCase().compareTo(strTempName) < 0) {
                    strTempName = listFolder.get(j).getName().toLowerCase();
                    iMin = j;
                }
            }
            FileItem item1 = new FileItem(listFolder.get(iMin), false);
            idList2.add(item1);
            listFolder.remove(iMin);
            iCount--;
        }
       // file sort functions
        iCount = listFiles.size();
        while (iCount > 0)
        {
            iMin = 0;
            String strTempName = listFiles.get(0).getName().toLowerCase();
            for (j = 1; j < iCount; j++) {
                if (listFiles.get(j).getName().toLowerCase().compareTo(strTempName) < 0) {
                    strTempName = listFiles.get(j).getName().toLowerCase();
                    iMin = j;
                }
            }
            FileItem item1 = new FileItem(listFiles.get(iMin), false);
            idList2.add(item1);
            listFiles.remove(iMin);
            iCount--;
        }
        lAdapter.notifyDataSetChanged();






//        for(File value: listFolder)
//        {
//            FileItem item = new FileItem(value,false);
//            idList2.add(item);
//        }
//        for(File value: listFiles)
//        {
//            FileItem item = new FileItem(value,false);
//            idList2.add(item);
//        }

    }
}