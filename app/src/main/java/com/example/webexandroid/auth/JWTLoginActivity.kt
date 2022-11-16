package com.example.webexandroid.auth

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.webexandroid.AlpianMainActivity
import com.example.webexandroid.R
import com.example.webexandroid.WebexAndroidApp
import com.example.webexandroid.WebviewActivity
import com.example.webexandroid.databinding.ActivityLoginWithTokenBinding
import com.example.webexandroid.messaging.spaces.detail.SpaceDetailActivity
import com.example.webexandroid.utils.Constants
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.json.JSONException
import org.koin.android.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class JWTLoginActivity : AppCompatActivity() {

    companion object {
        fun getIntent(context: Context, name: String, spaceId: String, selectionName: String): Intent {
            val intent = Intent(context, JWTLoginActivity::class.java)
            intent.putExtra(Constants.Intent.SPACE_ID, spaceId)
            intent.putExtra(Constants.Intent.NAME, name)
            intent.putExtra(Constants.Intent.SELECTION_NAME, selectionName)
            return intent
        }

    }

    lateinit var binding: ActivityLoginWithTokenBinding
    private val loginViewModel: LoginViewModel by viewModel()
    private lateinit var spaceId: String
    private lateinit var name: String
    private lateinit var selectionName: String
    private var queue: RequestQueue? = null
    var map:MutableMap<String,String>?=null
    var authToken:String ?= null
    var token: String ?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spaceId = intent.getStringExtra(Constants.Intent.SPACE_ID) ?: ""
        name = intent.getStringExtra(Constants.Intent.NAME) ?: ""
        selectionName = intent.getStringExtra(Constants.Intent.SELECTION_NAME) ?: ""
        queue = Volley.newRequestQueue(this);
        DataBindingUtil.setContentView<ActivityLoginWithTokenBinding>(this, R.layout.activity_login_with_token)
                .also { binding = it }
                .apply {
                    progressLayout.visibility = View.VISIBLE
                    val key="mK/6LbgBAGbUIHmqSZa0XNlUN7R9kFEA28qaDYJFeik="
                    val seckey = Keys.hmacShaKeyFor((Decoders.BASE64.decode(key)))
                    val rndm= (0..100000).random()
                    //val sub="guest-"+rndm.toString()
                    val sub="guest-3"
                    val now = Date()
                    Log.e("name",name)
                    val jwt = Jwts.builder()
                        .setHeaderParam("typ", "JWT")
                        .setHeaderParam("alg","HS256")
                        .claim("sub", sub)
                        .claim("name",name)
                        .claim("iss","Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi84NTBjMTA1Yy1lMDAwLTQ4ZDctYmZiZC02Mjg2MTc1NmVmZDI")
                        .setExpiration(Date(now.time + 2 * 1000 * 60 * 60))
                        .signWith(seckey)
                        .compact()
                    token = jwt
                    Log.e("token",token)
                    //token = "YTZhZDg4YzctZjE3Mi00NjkzLWExNTItOWMwYWQxMTk5ZWNkNTM5M2Q5OGItZmYy_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f"
                    loginViewModel.loginWithJWT(token!!)

                    loginViewModel.isAuthorized.observe(this@JWTLoginActivity, Observer { isAuthorized ->
                        progressLayout.visibility = View.GONE
                        isAuthorized?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
                                onLoginFailed()
                            }
                        }
                    })

                    loginViewModel.isAuthorizedCached.observe(this@JWTLoginActivity, Observer { isAuthorizedCached ->
                        progressLayout.visibility = View.GONE
                        isAuthorizedCached?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
//                                jwtTokenText.visibility = View.VISIBLE
//                                loginButton.visibility = View.VISIBLE
//                                loginFailedTextView.visibility = View.GONE
                            }
                        }
                    })

                    loginViewModel.errorData.observe(this@JWTLoginActivity, Observer { errorMessage ->
                        progressLayout.visibility = View.GONE
                        onLoginFailed(errorMessage)
                    })

                    loginViewModel.initialize()
                }
    }

    override fun onBackPressed() {
        (application as WebexAndroidApp).closeApplication()
    }

    private fun onLoggedIn() {
        Log.e("selectionName","in JWT main"+selectionName)
        Log.e("Bottoken",token)
        val url = "https://webexapis.com/v1/jwt/login"

////        val currentTime = Calendar.getInstance().time
////
////        Log.e("Currenttime", currentTime.toString())
//
//        val c:Calendar = Calendar.getInstance();
//        val sdf= SimpleDateFormat("HH:mm:ss");
//        val strDate = sdf.format(c.getTime());
//        Log.d("Date","DATE : " + strDate)
//
////        val builder = AlertDialog.Builder(this)
////        builder.setTitle("Out of Office Hours!")
////        builder.setMessage("Alpian offices are currently closed! Agent will reach out to you in normal business hours."+"\n"+"To continue to leave a message please press OK!")
//////builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))
////
////        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            val request: JsonObjectRequest = object :
                JsonObjectRequest(Request.Method.POST, url, null, Response.Listener { response ->
                    try {
                        val accessToken=response.getString("token")
                        Log.e("accesstoken",accessToken)
                    ContextCompat.startActivity(this, SpaceDetailActivity.getIntent(this, spaceId,
                        SpaceDetailActivity.Companion.ComposerType.POST_SPACE, null, null,false,selectionName,name,accessToken), null)
                        //startActivity(Intent(this@JWTLoginActivity, WebviewActivity::class.java))
                        //ContextCompat.startActivity(this, WebviewActivity.getIntent(this,accessToken,spaceId), null)
                        finish()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }, Response.ErrorListener { error -> error.printStackTrace() }
                ) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["Content-Type"] = "application/json"
                    params["Authorization"] =
                        "Bearer "+token
                    return params
                }
            }
            queue?.add(request)
//        }
//
//        builder.setNegativeButton(android.R.string.cancel) { dialog, which ->
            //startActivity(Intent(this@JWTLoginActivity, WebviewActivity::class.java))
//        }
//        builder.show()


        //Log.e("authToken",map?.get("auth_token"))
//        ContextCompat.startActivity(this, SpaceDetailActivity.getIntent(this, spaceId,
//            SpaceDetailActivity.Companion.ComposerType.POST_SPACE, null, null,false,selectionName,name), null)
//        finish()
    }

    private fun onLoginFailed(failureMessage: String = getString(R.string.jwt_login_failed)) {
//        binding.loginButton.visibility = View.VISIBLE
//        binding.loginFailedTextView.visibility = View.VISIBLE
//        binding.loginFailedTextView.text = failureMessage
    }
}