package com.smartworld.socketiodemo

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.IO

import io.socket.client.Socket
import io.socket.emitter.Emitter

import org.json.JSONException
import org.json.JSONObject

import java.net.URISyntaxException
import java.util.ArrayList
import java.util.UUID

open class MainActivity : AppCompatActivity() {

    private var textField: EditText? = null
    private var sendButton: ImageButton? = null

    private var username: String? = null

    private var hasConnection: Boolean? = false

    private var messageListView: ListView? = null
    private var messageAdapter: MessageAdapter? = null

    private var thread2: Thread? = null
    private var startTyping = false
    private var time = 2

    private var mSocket: Socket? = null

    @SuppressLint("HandlerLeak")
    internal var handler2: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.i(TAG, "handleMessage: typing stopped $startTyping")
            if (time == 0) {
                title = "SocketIO"
                Log.i(TAG, "handleMessage: typing stopped time is $time")
                startTyping = false
                time = 2
            }
        }
    }

    private var onNewMessage: Emitter.Listener = Emitter.Listener { args ->
        runOnUiThread(Runnable {
            Log.i(TAG, "run: ")
            Log.i(TAG, "run: " + args.size)
            val data = args[0] as JSONObject
            val username: String
            val message: String
            val id: String
            try {
                username = data.getString("username")
                message = data.getString("message")
                id = data.getString("uniqueId")

                Log.i(TAG, "run: $username$message$id")

                val format = MessageFormat(id, username, message)
                Log.i(TAG, "run:4 ")
                messageAdapter!!.add(format)
                Log.i(TAG, "run:5 ")

            } catch (e: Exception) {
                return@Runnable
            }
        })
    }

    private var onNewUser: Emitter.Listener = Emitter.Listener { args ->
        runOnUiThread(Runnable {
            val length = args.size

            if (length == 0) {
                return@Runnable
            }
            //Here i'm getting weird error..................///////run :1 and run: 0
            Log.i(TAG, "run: ")
            Log.i(TAG, "run: " + args.size)

            try {
                val username = JSONObject(username!!)
                this.username = username.getString("username")
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val format = MessageFormat(null, username, null)
            messageAdapter!!.add(format)
            messageListView!!.smoothScrollToPosition(0)
            messageListView!!.scrollTo(0, messageAdapter!!.count - 1)
            Log.i(TAG, "run: $username")
        })
    }


    private var onTyping: Emitter.Listener = Emitter.Listener { args ->
        runOnUiThread {
            val data = args[0] as JSONObject
            Log.i(TAG, "run: " + args[0])
            try {
                var typingOrNot: Boolean? = data.getBoolean("typing")
                val userName = data.getString("username") + " is Typing......"
                val id = data.getString("uniqueId")

                if (id == uniqueId) {
                    typingOrNot = false
                } else {
                    title = userName
                }

                if (typingOrNot!!) {

                    if (!startTyping) {
                        startTyping = true
                        thread2 = Thread(
                            object : Runnable {
                                override fun run() {
                                    while (time > 0) {
                                        synchronized(this) {
                                            try {
                                                Thread.sleep(1000)
                                                Log.i(TAG, "run: typing $time")
                                            } catch (e: InterruptedException) {
                                                e.printStackTrace()
                                            }

                                            time--
                                        }
                                        handler2.sendEmptyMessage(0)
                                    }

                                }
                            }
                        )
                        thread2!!.start()
                    } else {
                        time = 2
                    }

                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    init {
        try {
            mSocket = IO.socket("https://thawing-shore-92555.herokuapp.com")
        } catch (e: URISyntaxException) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        username = intent.getStringExtra("username")

        uniqueId = UUID.randomUUID().toString()
        Log.i(TAG, "onCreate: $uniqueId")

        if (savedInstanceState != null) {
            hasConnection = savedInstanceState.getBoolean("hasConnection")
        }

        if (!hasConnection!!) {
            mSocket!!.connect()
            mSocket!!.on("connect user", onNewUser)
            mSocket!!.on("chat message", onNewMessage)
            mSocket!!.on("on typing", onTyping)

            val userId = JSONObject()
            try {
                userId.put("username", username!! + " Connected")
                mSocket!!.emit("connect user", userId)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        Log.i(TAG, "onCreate: " + hasConnection!!)
        hasConnection = true


        Log.i(TAG, "onCreate: $username Connected")

        textField = findViewById(R.id.textField)
        sendButton = findViewById(R.id.sendButton)
        messageListView = findViewById(R.id.messageListView)

        val messageFormatList = ArrayList<MessageFormat>()
        messageAdapter = MessageAdapter(this, R.layout.item_message, messageFormatList)
        messageListView!!.adapter = messageAdapter

        onTypeButtonEnable()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasConnection", hasConnection!!)
    }

    private fun onTypeButtonEnable() {
        textField!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {

                val onTyping = JSONObject()
                try {
                    onTyping.put("typing", true)
                    onTyping.put("username", username)
                    onTyping.put("uniqueId", uniqueId)
                    mSocket!!.emit("on typing", onTyping)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                sendButton!!.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })
    }

//    private fun addMessage(username: String, message: String) {
//
//    }

    fun sendMessage(view: View) {
        Log.i(TAG, "sendMessage: ")
        val message = textField!!.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(message)) {
            Log.i(TAG, "sendMessage:2 ")
            return
        }
        textField!!.setText("")
        val jsonObject = JSONObject()
        try {
            jsonObject.put("message", message)
            jsonObject.put("username", username)
            jsonObject.put("uniqueId", uniqueId)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        Log.i(TAG, "sendMessage: 1" + mSocket!!.emit("chat message", jsonObject))
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing) {
            Log.i(TAG, "onDestroy: ")

            val userId = JSONObject()
            try {
                userId.put("username", username!! + " DisConnected")
                mSocket!!.emit("connect user", userId)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            mSocket!!.disconnect()
            mSocket!!.off("chat message", onNewMessage)
            mSocket!!.off("connect user", onNewUser)
            mSocket!!.off("on typing", onTyping)
            username = ""
            messageAdapter!!.clear()
        } else {
            Log.i(TAG, "onDestroy: is rotating.....")
        }

    }

    companion object {
        const val TAG = "MainActivity"
        lateinit var uniqueId: String
    }
}