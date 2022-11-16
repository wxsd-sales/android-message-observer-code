package com.example.webexandroid.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.webexandroid.AlpianMainActivity
import com.example.webexandroid.databinding.ActivityLoginBinding
import com.example.webexandroid.R
import com.example.webexandroid.WebexAndroidApp
import com.example.webexandroid.firebase.FirebaseDBManager
import com.example.webexandroid.utils.SharedPrefUtils
import com.example.webexandroid.utils.SharedPrefUtils.getEmailPref
import com.example.webexandroid.utils.SharedPrefUtils.getLoginTypePref
import com.example.webexandroid.utils.SharedPrefUtils.saveEmailPref
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import kotlin.collections.HashMap

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    private lateinit var userRef: DatabaseReference

    enum class LoginType(var value: String) {
        OAuth("OAuth"),
        JWT("JWT")
    }

    private var loginTypeCalled = LoginType.OAuth

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userRef=FirebaseDatabase.getInstance().getReference().child("Users");

        DataBindingUtil.setContentView<ActivityLoginBinding>(this, R.layout.activity_login)
            .also { binding = it }
            .apply {

                val type = getLoginTypePref(this@LoginActivity)

                textEmailAddress.setText(getEmailPref(this@LoginActivity))

                btnOauthLogin.setOnClickListener {
                    loginTypeCalled = LoginType.OAuth
                    saveEmailPref(this@LoginActivity, textEmailAddress.text.toString())

                    var emailaddr = textEmailAddress.text
                    if (emailaddr.isEmpty() ) {
                        runOnUiThread(Runnable {
                            Toast.makeText(
                                getApplicationContext(),
                                "Please enter your email address",
                                Toast.LENGTH_LONG
                            ).show()
                        })
                        return@setOnClickListener
                    }
                    //var uid= java.util.UUID.randomUUID().toString()
                    var profileMap : HashMap<String, String>
                            = HashMap<String, String> ()
                    //profileMap.put("uid",uid)
                    var email=emailaddr.toString()
                    var url:String=""
                    email=email.replace('.','*')
                    profileMap.put("email",emailaddr.toString())
                    val urlRef = FirebaseDatabase.getInstance().getReference().child("main-screen-background-image")
                    urlRef.addListenerForSingleValueEvent(object :ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            url = snapshot.value.toString()
                            profileMap.put("background-url",url)
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }

                    })


                    val postListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if(dataSnapshot.exists()){

                            }
                            else{
                                userRef.child(email).updateChildren(profileMap as Map<String, Any>).addOnCompleteListener(
                                    OnCompleteListener {
                                        if(it.isSuccessful)
                                        {
                                            Toast.makeText(this@LoginActivity,"info updated", Toast.LENGTH_LONG)
                                        }
                                    })
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                        }
                    }
                    userRef.orderByChild("email").equalTo(emailaddr.toString()).addValueEventListener(postListener)

//                    userRef.child(uid).updateChildren(profileMap as Map<String, Any>).addOnCompleteListener(
//                        OnCompleteListener {
//                            if(it.isSuccessful)
//                            {
//                                Toast.makeText(this@LoginActivity,"info updated", Toast.LENGTH_LONG)
//                            }
//                        })

                    saveEmailPref(this@LoginActivity, textEmailAddress.text.toString())
                    FirebaseDBManager.getData("" + SharedPrefUtils.getEmailPref(this@LoginActivity))
                    FirebaseDBManager.fetchUrl("" + SharedPrefUtils.getEmailPref(this@LoginActivity))
                    startOAuthActivity()
                }

                btnJwtLogin?.setOnClickListener{
                    FirebaseDBManager.getData("default")
                    FirebaseDBManager.fetchUrl("default")
                    ContextCompat.startActivity(
                        this@LoginActivity,
                        AlpianMainActivity.getIntent(this@LoginActivity, "jwt"),
                        null
                    )
                }
            }
    }


    private fun startOAuthActivity() {
        (application as WebexAndroidApp).OAuthloadKoinModules(loginTypeCalled)
        startActivity(Intent(this@LoginActivity, OAuthWebLoginActivity::class.java))
        finish()
    }
//
//    private fun startJWTActivity() {
//        (application as WebexAndroidApp).loadKoinModules(loginTypeCalled)
//        startActivity(Intent(this@LoginActivity, JWTLoginActivity::class.java))
//        finish()
//    }
}
