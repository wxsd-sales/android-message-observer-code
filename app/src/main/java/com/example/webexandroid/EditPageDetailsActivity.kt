package com.example.webexandroid

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.example.webexandroid.firebase.FirebaseDBManager
import com.example.webexandroid.utils.SharedPrefUtils
import com.example.webexandroid.utils.extensions.hideKeyboard
import com.google.android.material.textfield.TextInputEditText


class EditPageDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_page_details)
        val addButton = findViewById<ImageButton>(R.id.addButton)
        val languageLV = findViewById<ListView>(R.id.idLVLanguages)
        val backgroundURL = findViewById<TextInputEditText>(R.id.backgroundURL)
        val itemTitle = findViewById<TextInputEditText>(R.id.titleName)
        val agentEmail=findViewById<TextInputEditText>(R.id.agentEmail)
        val lngList: ArrayList<String> = ArrayList()
        val root: ImageView = findViewById(R.id.rootRL)
        val submitButton = findViewById<Button>(R.id.submit_button)
        val deleteButton = findViewById<Button>(R.id.delete_button)
        var listener = object : FirebaseDBManager.UrlLoadedListener{
            override fun onLoad() {
                Glide.with(this@EditPageDetailsActivity).load(FirebaseDBManager.url)
                    .error(ResourcesCompat.getDrawable(resources,R.drawable.alpian,theme))
                    .into(root)
            }
        }
        if(FirebaseDBManager.count>=1)
        {
            deleteButton.visibility= View.VISIBLE
        }
        else{
            deleteButton.visibility=View.GONE
        }
        FirebaseDBManager.setLoadListener(listener)
        FirebaseDBManager.getData("" + SharedPrefUtils.getEmailPref(this))
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
            this@EditPageDetailsActivity,
            android.R.layout.simple_list_item_1,
            lngList as List<String?>
        )

        // on below line we are setting adapter for our list view.
        languageLV.adapter = adapter
        submitButton.setOnClickListener{
            if(backgroundURL.text.toString()!=null) {
                FirebaseDBManager.setURL(
                    backgroundURL.text.toString(),
                    "" + SharedPrefUtils.getEmailPref(this)
                )
            }
            ContextCompat.startActivity(
                this@EditPageDetailsActivity,
                AlpianMainActivity.getIntent(this@EditPageDetailsActivity, "oauth"),
                null
            )
        }
        deleteButton.setOnClickListener{
            FirebaseDBManager.removeData("" + SharedPrefUtils.getEmailPref(this))
            lngList.clear()
            adapter.notifyDataSetChanged()
            deleteButton.visibility=View.GONE
        }
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
            FirebaseDBManager.writeData(itemTitle.text.toString(),agentEmail.text.toString(), "" + SharedPrefUtils.getEmailPref(this))
            itemTitle.setText("")
            agentEmail.setText("")
            deleteButton.visibility=View.VISIBLE
            hideKeyboard(it)
        }
        languageLV.setOnItemLongClickListener(AdapterView.OnItemLongClickListener { a, v, position, id ->
            val adb= AlertDialog.Builder(this@EditPageDetailsActivity)
            adb.setTitle("Delete?")
            adb.setMessage("Are you sure you want to delete $position")
            adb.setNegativeButton("Cancel", null)
            adb.setPositiveButton("Ok") { dialog, which ->
                lngList.removeAt(position)
                adapter.notifyDataSetChanged()
            }
            adb.show()
            true
        })
    }
}