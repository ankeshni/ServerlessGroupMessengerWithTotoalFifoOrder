package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static  int max_queue = 6;

    Integer dictator_PORT=11108;
    String myPort = null;
    String delimiter="~`~%#@&%&";
    int seq=0;
    ArrayList<String> messages= new ArrayList<String>(10000);
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private void brodcast(EditText editText,String myPort){
        String msg = editText.getText().toString() + "\n";
        editText.setText(""); // This is one way to reset the input box.
        TextView localTextView = (TextView) findViewById(R.id.textView1);
        localTextView.append("\t" + msg); // This is one way to display a string.
        TextView remoteTextView = (TextView) findViewById(R.id.textView1);
        remoteTextView.append("\n");

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"message" ,msg, myPort);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
                /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);


        /*
         * Register an OnKeyListener for the input box. OnKeyListener is an event handler that
         * processes each key event. The purpose of the following code is to detect an enter key
         * press event, and create a client thread so that the client thread can send the string
         * in the input box over the network.
         */
         /*
         *Make Send button "button4" send data  by pressing enter
         */



        Button sendbutton= (Button) findViewById(R.id.button4);
        sendbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */

                brodcast(editText,myPort);
            }
        });

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER))) {

                    /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                    brodcast(editText,myPort);
                    return true;
                }
                return false;
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while(true){
                    Socket accepted_socket = serverSocket.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(accepted_socket.getInputStream()));
                    String[] received_message = br.readLine().split(delimiter);
                    if(received_message!=null&& received_message.length==2){

                        String type = received_message[0];
                        if(type.equals("message")){
                            messages.add(received_message[1]);
                            //publishProgress(received_message[1]);
                            if(messages.size()>=max_queue){//dead dictator handler
                                max_queue=1;
                                Integer update_dictator=Integer.parseInt(REMOTE_PORT3);
                                if(update_dictator.toString().equals(myPort)){
                                    //publishProgress("I am the new dictator"+myPort);
                                    for(int ji =Integer.parseInt(REMOTE_PORT0);ji<=11124;ji=ji+4){
                                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),ji);
                                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                                        bw.write("update_dictator"+delimiter+update_dictator+"\n");
                                        bw.flush();
                                        //socket.close();
                                    }


                                }
                                else {
                                    messages.clear();
                                }

                            }
                        }
                        else if(type.equals("proposal")){
                            if(dictator_PORT.toString().equals(myPort)){
                                //publishProgress("I am the dictator");
                                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"approval" ,received_message[1], myPort);

                            }
                        }
                        else if(type.equals("approval")){
                            publishProgress(received_message[1]);
                            messages.remove(received_message[1]);
                            if(received_message.length==3){
                                dictator_PORT=Integer.parseInt(received_message[3]);
                            }
                        }
                        else if(type.equals("update_dictator")){
                            //publishProgress("new dictator is"+received_message[1]);
                            dictator_PORT=Integer.parseInt(received_message[1]);
                            while(messages.size()!=0){
                                for(int ji =Integer.parseInt(REMOTE_PORT0);ji<=11124;ji=ji+4){
                                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),ji);
                                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                                    bw.write("approval"+delimiter+messages.get(0)+"\n");
                                    bw.flush();
                                    //socket.close();
                                }messages.remove(0);
                            }

                        }
                    }

                    accepted_socket.close();
                    br.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");
            /*
             *Inserting messages in database             *
             */
            ContentValues cv = new ContentValues();
            cv.put("key",Integer.toString(seq++) );
            cv.put("value",strReceived);
            getContentResolver().insert(mUri, cv);



            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket;
                String msgToSend = msgs[1];
                String msg_type=msgs[0];
                if(msg_type.equals("message")){
                    for(int ji =Integer.parseInt(REMOTE_PORT0);ji<=11124;ji=ji+4){
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),ji);


                    /*
                     * TODO: Fill in your client code that sends out a message.
                     */
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        bw.write(msg_type+delimiter+msgToSend);
                        bw.flush();
                        //socket.close();
                    }
                    try{
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),dictator_PORT);
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        bw.write("proposal"+delimiter+msgToSend);
                        bw.flush();
                    }
                    catch (IOException e) {
                        Log.e(TAG,"dictator is dead");
                    }

                    //socket.close();
                } else if (msg_type.equals("approval")) {
//                    for(int ji =Integer.parseInt(REMOTE_PORT0);ji<=11124;ji=ji+4){
//                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),ji);
//                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//                        bw.write("approval"+delimiter+msgToSend+"\n");
//                        bw.flush();
//                        //socket.close();
//                    }
                    if(dictator_PORT.toString().equals(myPort)){
                        for(int ji =Integer.parseInt(REMOTE_PORT0);ji<=11124;ji=ji+4){
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),ji);
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                            bw.write("approval"+delimiter+msgToSend+"\n");
                            bw.flush();
                            //socket.close();
                        }

                    }
                }
                else if (msg_type.equals("update_dictator")) {
                    for(int ji =Integer.parseInt(msgToSend);ji<=11124;ji=ji+4){
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),ji);
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        bw.write("update_dictator"+delimiter+msgToSend+"\n");
                        bw.flush();
                        //socket.close();
                    }
                }

            }
            catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }

    }



}
