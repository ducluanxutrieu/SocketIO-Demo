package com.smartworld.socketiodemo

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class MessageAdapter(context: Context, resource: Int, objects: List<MessageFormat>) :
    ArrayAdapter<MessageFormat>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var mConvertView = convertView
        Log.i(MainActivity.TAG, "getView:")

        val message = getItem(position)

        when {
            TextUtils.isEmpty(message!!.message) -> {

                mConvertView = (context as Activity).layoutInflater.inflate(R.layout.user_connected, parent, false)

                val messageText = mConvertView.findViewById<TextView>(R.id.message_body)

                Log.i(MainActivity.TAG, "getView: is empty ")
                val userConnected = message.username
                messageText.text = userConnected

            }
            message.uniqueId == MainActivity.uniqueId -> {
                Log.i(MainActivity.TAG, "getView: " + message.uniqueId + " " + MainActivity.uniqueId)


                mConvertView = (context as Activity).layoutInflater.inflate(R.layout.my_message, parent, false)
                val messageText = mConvertView.findViewById<TextView>(R.id.message_body)
                messageText.text = message.message

            }
            else -> {
                Log.i(MainActivity.TAG, "getView: is not empty")

                mConvertView = (context as Activity).layoutInflater.inflate(R.layout.their_message, parent, false)

                val messageText = mConvertView.findViewById<TextView>(R.id.message_body)
                val usernameText = mConvertView.findViewById<View>(R.id.name) as TextView

                messageText.visibility = View.VISIBLE
                usernameText.visibility = View.VISIBLE

                messageText.text = message.message
                usernameText.text = message.username
            }
        }

        return mConvertView
    }
}
