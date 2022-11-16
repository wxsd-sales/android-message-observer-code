package com.example.webexandroid

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.webexandroid.auth.JWTLoginActivity
import com.example.webexandroid.auth.OAuthWebLoginActivity
import com.example.webexandroid.firebase.FirebaseDBManager
import com.example.webexandroid.utils.Constants
import com.example.webexandroid.utils.SharedPrefUtils
import com.example.webexandroid.utils.extensions.hideKeyboard
import com.google.android.material.textfield.TextInputEditText
import org.koin.android.viewmodel.ext.android.viewModel
import androidx.lifecycle.Observer
import com.example.webexandroid.databinding.ActivityAlpianMainBinding
import com.example.webexandroid.utils.SharedPrefUtils.clearLoginTypePref
import java.util.regex.Matcher
import java.util.regex.Pattern


class AlpianMainActivity : AppCompatActivity() {

    companion object {
        fun getIntent(context: Context, type: String): Intent {
            val intent = Intent(context, AlpianMainActivity::class.java)
            intent.putExtra(Constants.Intent.TYPE, type)
            return intent
        }

    }

    enum class LoginType(var value: String) {
        OAuth("OAuth"),
        JWT("JWT")
    }

    val webexViewModel: WebexViewModel by viewModel()
    private lateinit var type: String
    private var loginTypeCalled = LoginType.JWT

    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this,R.anim.rotate_open_anim)}
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this,R.anim.rotate_close_anim)}
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this,R.anim.from_bottom_anim)}
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this,R.anim.to_bottom_anim)}
    private var clicked= false
    var isAllFieldsChecked = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alpian_main)
        val editFab :View = findViewById(R.id.edit_fab)
        val logout: View = findViewById(R.id.logout)
        val settings: View = findViewById(R.id.settings_fab)
        val fab: View = findViewById(R.id.add_fab)
        type = intent.getStringExtra(Constants.Intent.TYPE) ?: ""
        if(type=="oauth") {
            webexViewModel.signOutListenerLiveData.observe(this@AlpianMainActivity, Observer {
                it?.let {
                    if (it) {
                        clearLoginTypePref(this)
                        (application as WebexAndroidApp).unloadKoinModules()
                        KitchenSinkForegroundService.stopForegroundService(this)
                        finish()
                    }
                }
            })
        }


                if (type != "oauth") {
                    editFab.visibility = View.GONE
                }
                settings.setOnClickListener {
                    if (type != "oauth") {
                        if (clicked) {
                            editFab.visibility = View.GONE
                            logout.visibility = View.VISIBLE
//                            editFab.startAnimation(toBottom)
                            logout.startAnimation(toBottom)
                            settings.startAnimation(rotateOpen)
                        } else {
                            editFab.visibility = View.GONE
                            logout.visibility = View.INVISIBLE
//                            editFab.startAnimation(fromBottom)
                            logout.startAnimation(fromBottom)
                            settings.startAnimation(rotateClose)
                        }
                    }
                    else{
                        if (clicked) {
                            editFab.visibility = View.VISIBLE
                            logout.visibility = View.VISIBLE
                            editFab.startAnimation(toBottom)
                            logout.startAnimation(toBottom)
                            settings.startAnimation(rotateOpen)
                        } else {
                            editFab.visibility = View.INVISIBLE
                            logout.visibility = View.INVISIBLE
                            editFab.startAnimation(fromBottom)
                            logout.startAnimation(fromBottom)
                            settings.startAnimation(rotateClose)
                        }
                    }
                    clicked = !clicked
                }
                fab.setOnClickListener { view ->
                    showCreateCategoryDialog()
                }
                editFab.setOnClickListener { view ->
                    //showCreateCategoryEditDialog()
                    startActivity(
                        Intent(
                            this@AlpianMainActivity,
                            EditPageDetailsActivity::class.java
                        )
                    )
                }
                logout.setOnClickListener {
                    webexViewModel.signOut()
                }
                val root: ImageView = findViewById(R.id.rootRL)
                var listener = object : FirebaseDBManager.UrlLoadedListener {
                    override fun onLoad() {
                        Glide.with(this@AlpianMainActivity).load(FirebaseDBManager.url)
                            .error(ResourcesCompat.getDrawable(resources, R.drawable.alpian, theme))
                            .into(root)
                    }
                }
                FirebaseDBManager.setLoadListener(listener)

    }

    private fun startJWTActivity(name: String, selection: String, selectionName: String) {
        (application as WebexAndroidApp).JWTloadKoinModules(loginTypeCalled)
        ContextCompat.startActivity(
            this,
            JWTLoginActivity.getIntent(this, name, selection, selectionName),
            null
        )
        finish()
    }



    private fun startOAuthActivity() {
        (application as WebexAndroidApp).JWTloadKoinModules(loginTypeCalled)
        startActivity(Intent(this@AlpianMainActivity, OAuthWebLoginActivity::class.java))
        finish()
    }

    fun showCreateCategoryDialog() {
        var selection: String = "other"
        var spaceID: String? = null
        val context = this
        val messageTo: String? = null
        val builder = AlertDialog.Builder(context)
        // alert = builder.create()
        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.another_view1, null)

        val nameText = view.findViewById(R.id.textName) as TextInputEditText
        val buttonPopup = view.findViewById<ImageButton>(R.id.button_popup)
        val textEmail = view.findViewById<TextInputEditText>(R.id.textEmail)
        val textPhone = view.findViewById<TextInputEditText>(R.id.textPhone)

        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.show()
        val window: Window? = alertDialog.getWindow()
        window?.setGravity(Gravity.AXIS_Y_SHIFT)

        val info = FirebaseDBManager.dataMap.keys.map { it }
        // create an array adapter and pass the required parameter
        // in our case pass the context, drop down layout , and array.
        val autoTextView: AutoCompleteTextView = view.findViewById(R.id.autoTextView)
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line, info
        )
        autoTextView.threshold = 1
        autoTextView.setAdapter(adapter)
        autoTextView.setOnFocusChangeListener(View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                hideKeyboard(view);
            }
        })
        autoTextView.setOnItemClickListener(OnItemClickListener { parent, view, position, rowId ->
            selection = parent.getItemAtPosition(position) as String
            spaceID = FirebaseDBManager.dataMap[selection]
        })

        buttonPopup.setOnClickListener {
            // Dismiss the popup window
            isAllFieldsChecked =
                CheckAllFields(nameText, selection, autoTextView, textPhone, textEmail);
            if (isAllFieldsChecked) {
                Log.e("selectionName", "in alpian main" + selection)
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
                startJWTActivity(nameText.getText().toString(), spaceID!!, selection)
            }
        }


    }

    @SuppressLint("ResourceType")
    fun showCreateCategoryEditDialog() {
        var selection: String = "other"
        var spaceID: String? = null
        val context = this
        val messageTo: String? = null
        val builder = AlertDialog.Builder(context)
        // alert = builder.create()
        // https://stackoverflow.com/questions/10695103/creating-custom-alertdialog-what-is-the-root-view
        // Seems ok to inflate view with null rootView
        val view = layoutInflater.inflate(R.layout.edit_view, null)
        val addButton = view.findViewById<Button>(R.id.addButton)
        val languageLV = view.findViewById<ListView>(R.id.idLVLanguages)
        val itemTitle = view.findViewById<TextInputEditText>(R.id.titleName)
        val agentEmail=view.findViewById<TextInputEditText>(R.id.agentEmail)
        val lngList: ArrayList<String> = ArrayList()

        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.show()
        val window: Window? = alertDialog.getWindow()
        window?.setGravity(Gravity.AXIS_Y_SHIFT)

        val info = FirebaseDBManager.dataMap.map { it }
        // create an array adapter and pass the required parameter

        // Creating an ArrayList of Entry objects

        // Creating an ArrayList of Entry objects
        val itr = info.listIterator()    // or, use `iterator()`
        while (itr.hasNext()) {
            lngList.add(itr.next().toString())
        }

        Log.e("firebaseInfo",info.joinToString(" "))
        // on below line we are adding items to our list

        // on below line we are initializing adapter for our list view.
        val adapter: ArrayAdapter<String?> = ArrayAdapter<String?>(
            this@AlpianMainActivity,
            android.R.layout.simple_list_item_1,
            lngList as List<String?>
        )

        // on below line we are setting adapter for our list view.
        languageLV.adapter = adapter

        // on below line we are adding click listener for our button.
        addButton.setOnClickListener {
            var item:String="default"
            // on below line we are getting text from edit text
            if(itemTitle.text.toString().isNotEmpty() && agentEmail.text.toString().isNotEmpty()) {
                item = itemTitle.text.toString()+'='+agentEmail.text.toString()
            }
            Log.e("item value",item)
            // on below line we are checking if item is not empty
            if (item!="default") {
                // on below line we are adding item to our list.
                lngList.add(item)

                // on below line we are notifying adapter
                // that data in list is updated to update our list view.
                adapter.notifyDataSetChanged()
            }
        }


    }

    private fun CheckAllFields(
        nameText: TextInputEditText,
        selection: String,
        autoTextView: AutoCompleteTextView,
        textPhone: TextInputEditText,
        textEmail: TextInputEditText
    ): Boolean {
        if (nameText.getText().toString().length == 0) {
            nameText.setError("Name is required!")
            return false
        }
        if (selection == "other") {
            autoTextView.setError("Regarding what you are looking for is required!")
            return false
        }
        if (!PhoneNumberUtils.isGlobalPhoneNumber(
                textPhone.getText().toString()
            ) && textPhone.getText().toString().length < 10
        ) {
            textPhone.setError("Please enter a valid phone number!")
            return false
        }
        val pattern: Pattern
        val matcher: Matcher
        val EMAIL_PATTERN =
            "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
        pattern = Pattern.compile(EMAIL_PATTERN)
        matcher = pattern.matcher(textEmail.getText())
        Log.e("emailMatcher", matcher.matches().toString())
        if (!matcher.matches()) {
            textEmail.setError("Please enter a valid email!")
            return false
        }

        // after all validation return true.
        return true
    }
}