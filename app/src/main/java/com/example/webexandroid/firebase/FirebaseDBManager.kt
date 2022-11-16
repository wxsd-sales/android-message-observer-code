package com.example.webexandroid.firebase

import android.util.Log
import android.widget.Toast
import com.ciscowebex.androidsdk.utils.SettingsStore.getContext
import com.example.webexandroid.utils.SharedPrefUtils
import com.google.firebase.database.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Comment


object FirebaseDBManager {
    private val database = FirebaseDatabase.getInstance()
    val db = Firebase.firestore
    var count=0
    var myRef = database.getReference().child("Users")
    var upRef= database.getReference("help-topic-list-data")
    var dataMap: HashMap<String, String> = HashMap()
    var jsonString:String=""
    var url :String?=null
    var listener : UrlLoadedListener? =null

    var childEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            Log.e("FirebaseDB", "onChildAdded:" + dataSnapshot.key!!)

            // A new comment has been added, add it to the displayed list
            val comment = dataSnapshot.getValue()

            // ...
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            Log.e("FirebaseDB", "onChildChanged: ${dataSnapshot.key}")

            // A comment has changed, use the key to determine if we are displaying this
            // comment and if so displayed the changed comment.
            val newComment = dataSnapshot.getValue()
            val commentKey = dataSnapshot.key

            // ...
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            Log.e("FirebaseDB", "onChildRemoved:" + dataSnapshot.value!!)

            // A comment has changed, use the key to determine if we are displaying this
            // comment and if so remove it.
            val commentKey = dataSnapshot.key

            // ...
        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            Log.e("FirebaseDB", "onChildMoved:" + dataSnapshot.key!!)

            // A comment has changed position, use the key to determine if we are
            // displaying this comment and if so move it.
            val movedComment = dataSnapshot.getValue()
            val commentKey = dataSnapshot.key

            // ...
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.e("FirebaseDB", "postComments:onCancelled", databaseError.toException())
        }
    }

    init {
        fetchUrl("" + getContext()?.let { SharedPrefUtils.getEmailPref(it) })
    }
    fun getData(email:String) {
        Log.e("emailgot",email)
        var emailnew=email.replace('.','*')
        var isDataFound = false
        if(emailnew!="default") {
            myRef.child(emailnew).child("help-topic-list-data")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            var agent_email_list: String = ""
                            var option_title_list: String = ""
                            count = 0
                            val children = snapshot!!.children
                            children.forEach {
                                count++
                                Log.e("children", it.toString())
                                agent_email_list = it.child("agent_email").value!! as String
                                option_title_list = it.child("option_title").value!! as String
                                dataMap.set(option_title_list, agent_email_list)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {

                    }
                })
        }
        else {
            upRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    var agent_email_list: String = ""
                    var option_title_list: String = ""
                    count = 0
                    val children = snapshot!!.children
                    children.forEach {
                        count++
                        Log.e("children", it.toString())
                        agent_email_list = it.child("agent_email").value!! as String
                        option_title_list = it.child("option_title").value!! as String
                        dataMap.set(option_title_list, agent_email_list)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
        }
    }

    fun writeData(option_title: String, agent_email: String, email:String) {
        var emailnew=email.replace('.','*')
        val myclass=MyClass(option_title,agent_email)
//        myRef.child("4").child("option_title").setValue(option_title)
//        myRef.child("4").child("agent_email").setValue(agent_email)
//        val newData= myRef.push()
//        Log.e("newDataKey",newData.key.toString())
        getData("" + getContext()?.let { SharedPrefUtils.getEmailPref(it) })
        Log.e("count",count.toString())
        myRef.child(emailnew).child("help-topic-list-data").child(count.toString()).setValue(myclass)
    }

    fun removeData(email: String) {
        var emailnew=email.replace('.','*')
        val urlRef = database.getReference("Users").child(emailnew).child("help-topic-list-data")
        urlRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                url = snapshot.value.toString()
                if(url!=null){
                    snapshot.getRef().removeValue()
                    count=0
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }



    fun fetchUrl(email: String){
        var emailnew=email.replace('.','*')
        if(email!="default"){
            myRef.child(emailnew).child("background-url").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    url = snapshot.value.toString()
                    listener?.onLoad()
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }
        else {
            val urlRef = database.getReference("main-screen-background-image")
            urlRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    url = snapshot.value.toString()
                    listener?.onLoad()
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }
    }

    fun setLoadListener(listener:UrlLoadedListener){
        this.listener = listener
        if(url!=null){
            listener?.onLoad()
        }
    }

    fun setURL(backgroundURL : String,email:String) {
        var emailnew=email.replace('.','*')
        myRef.child(emailnew).child("background-url").setValue(backgroundURL)
        listener?.onLoad()
        fetchUrl(email)
    }


    interface  UrlLoadedListener {
        fun onLoad()
    }
}