package com.example.webexandroid.messaging.spaces.detail

//import com.example.webexandroid.databinding.DialogPostMessageHandlerBinding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ciscowebex.androidsdk.message.LocalFile
import com.ciscowebex.androidsdk.message.Message
import com.ciscowebex.androidsdk.utils.EmailAddress
import com.example.webexandroid.BaseActivity
import com.example.webexandroid.R
import com.example.webexandroid.calling.CallActivity
import com.example.webexandroid.databinding.ActivitySpaceDetailBinding
import com.example.webexandroid.databinding.SentMessageBinding
import com.example.webexandroid.messaging.composer.MessageComposerActivity
import com.example.webexandroid.messaging.composer.MessageComposerViewModel
import com.example.webexandroid.messaging.spaces.ReplyMessageModel
import com.example.webexandroid.messaging.spaces.SpaceMessageModel
import com.example.webexandroid.person.PersonViewModel
import com.example.webexandroid.utils.Constants
import com.example.webexandroid.utils.extensions.hideKeyboard
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_space_detail.*
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.ext.android.inject


class SpaceDetailActivity : BaseActivity(), MessageReceiveListener {

    companion object {
        enum class ComposerType {
            POST_SPACE,
            POST_PERSON_ID,
            POST_PERSON_EMAIL
        }

        fun getIntent(
            context: Context,
            spaceId: String,
            type: ComposerType,
            messageId: String? = null,
            replyParentMessage: ReplyMessageModel?,
            guestUser: Boolean,
            selectionName: String,
            name: String,
            accessToken: String
        ): Intent {
            val intent = Intent(context, SpaceDetailActivity::class.java)
            intent.putExtra(Constants.Intent.SPACE_ID, spaceId)
            intent.putExtra(Constants.Intent.COMPOSER_TYPE, type)
            intent.putExtra(Constants.Intent.MESSAGE_ID, messageId)
            intent.putExtra(Constants.Intent.COMPOSER_REPLY_PARENT_MESSAGE, replyParentMessage)
            intent.putExtra(Constants.Intent.GUEST_USER, guestUser)
            intent.putExtra(Constants.Intent.SELECTION_NAME, selectionName)
            intent.putExtra(Constants.Intent.NAME, name)
            intent.putExtra(Constants.Intent.ACCESS_TOKEN, accessToken)
            return intent
        }

    }

    lateinit var messageClientAdapter: MessageClientAdapter
    lateinit var binding: ActivitySpaceDetailBinding

    //var accessToken: String="ZTFhODRkOTgtODViMy00ZDExLWExOWYtYTFhMWU4NTY3MWNhYzdiYmM3OWEtZTdl_PF84_d80e63ce-4911-40b3-9880-6c9c7a654430"
    private val spaceDetailViewModel: SpaceDetailViewModel by inject()
    private val personViewModel: PersonViewModel by inject()
    private val messageComposerViewModel: MessageComposerViewModel by inject()
    private lateinit var composerType: ComposerType
    private lateinit var spaceId: String
    private lateinit var selectionName: String
    private lateinit var name: String
    private lateinit var accessToken: String
    var guestUser: Boolean = false

    private var messageId: String? = null
    private var id: String? = null
    private var replyParentMessage: ReplyMessageModel? = null
    private var queue: RequestQueue? = null
    var handler: Handler = Handler()
    var runnable: Runnable? = null
    var delay = 5000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tag = "SpaceDetailActivity"

        spaceId = intent.getStringExtra(Constants.Intent.SPACE_ID) ?: ""
        selectionName = intent.getStringExtra(Constants.Intent.SELECTION_NAME) ?: ""
        name = intent.getStringExtra(Constants.Intent.NAME) ?: ""
        accessToken = intent.getStringExtra(Constants.Intent.ACCESS_TOKEN) ?: ""
        Log.e("accessToken",accessToken)
        composerType =
            intent.getSerializableExtra(Constants.Intent.COMPOSER_TYPE) as Companion.ComposerType
        guestUser = intent.getBooleanExtra(Constants.Intent.GUEST_USER, false)

        queue = Volley.newRequestQueue(this);

        messageId = intent.getStringExtra(Constants.Intent.MESSAGE_ID)
        replyParentMessage =
            intent.getParcelableExtra(Constants.Intent.COMPOSER_REPLY_PARENT_MESSAGE)
        spaceDetailViewModel.spaceId = spaceId
        id = spaceDetailViewModel.spaceId
        //spaceMessageRecyclerView.scrollToPosition(messageClientAdapter.itemCount-1)
        DataBindingUtil.setContentView<ActivitySpaceDetailBinding>(
            this,
            R.layout.activity_space_detail
        )
            .also { binding = it }
            .apply {
                val messageActionBottomSheetFragment = MessageActionBottomSheetFragment({ message ->
                    spaceDetailViewModel.deleteMessage(message)
                },
                    { message -> spaceDetailViewModel.markMessageAsRead(message) },
                    { message -> replyMessageListener(message) },
                    { message -> editMessage(message) })

                messageClientAdapter =
                    MessageClientAdapter(messageActionBottomSheetFragment, supportFragmentManager)
                spaceMessageRecyclerView.layoutManager=LinearLayoutManager(this@SpaceDetailActivity)
                spaceMessageRecyclerView.adapter = messageClientAdapter
                webexViewModel.setMessageObserver(this@SpaceDetailActivity)
                //spaceMessageRecyclerView.scrollToPosition(spaceDetailViewModel.getMessages().get)

                binding.phoneImage.setOnClickListener {
                    it.context.startActivity(
                        CallActivity.getOutgoingIntent(
                            it.context,
                            spaceId,
                            guestUser
                        )
                    )
                }
                Log.e("selectionName","in space detail"+selectionName)
                binding.heading.text=selectionName
                binding.toolbar.title=selectionName
                setUpObservers()
//                handler.postDelayed(Runnable {
//                    handler.postDelayed(runnable, delay.toLong())
//                    getRequest()
////                    Toast.makeText(
////                        this@SpaceDetailActivity, "This method is run every 10 seconds",
////                        Toast.LENGTH_SHORT
////                    ).show()
//                }.also { runnable = it }, delay.toLong())

                //spaceDetailViewModel.observeMessages()
                //super.onResume()

                    // spaceMessageRecyclerView.smoothScrollToPosition(spaceMessageRecyclerView.getAdapter()?.itemCount!!)

                //spaceMessageRecyclerView.scrollToPosition(50)
                //Log.i(tag,"message"+messageClientAdapter.itemCount)
                //spaceMessageRecyclerView.scrollToPosition(messageClientAdapter.itemCount-1)
//                    postMessageFAB.setOnClickListener {
//                        ContextCompat.startActivity(this@SpaceDetailActivity,
//                                MessageComposerActivity.getIntent(this@SpaceDetailActivity, MessageComposerActivity.Companion.ComposerType.POST_SPACE, spaceDetailViewModel.spaceId, null), null)
//                    }
                getRequest()
                postMessageFAB.setOnClickListener {
                    postRequest()
                }
                setUpObservers()


            }


    }

    private fun postRequest() {
        val url = "https://webexapis.com/v1/messages"
        val jsonBody: JSONObject =
            JSONObject("{\"toPersonEmail\":\""+spaceId+"\",\"text\":\""+binding.editGchatMessage.text+"\"}");
        val request: JsonObjectRequest = object :
            JsonObjectRequest(Request.Method.POST, url, jsonBody, Response.Listener { response ->
                try {
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
                    "Bearer "+accessToken
                return params
            }
        }
        queue?.add(request)
        resetView()
    }

    private fun getRequest() {
        val url = "https://webexapis.com/v1/messages/direct?personEmail="+spaceId
        //messageClientAdapter.notifyDataSetChanged()
        messageClientAdapter.messages.clear()
        //messageClientAdapter.personName.clear()

        val request: JsonObjectRequest = object :
            JsonObjectRequest(Request.Method.GET, url, null, Response.Listener { response ->
                try {
                    //Log.e("response", response.toString())
                    val jsonArray = response.getJSONArray("items")
                    var messages= mutableListOf<com.example.webexandroid.messaging.spaces.detail.Message>()
                    for (i in 0 until jsonArray.length()) {
                        val employee = jsonArray.getJSONObject(i)
                        //if(employee.getString("text")!=null) {
                        var text:String?=null
                        if(employee.has("text"))
                        {
                            text = employee.getString("text")
                            //Log.e("texmessage",text)
                        }
                        else {
                            val arrjson = employee.getJSONArray("files")
                            for (i in 0 until arrjson.length())
                                text= arrjson.getString(i)
                            //Log.e("texmessage",text)
                        }
                        val personId=employee.getString("personId")
                        val personEmail=employee.getString("personEmail")
                        //val personName=getPeopleInfoRequest(personId)
                        //val map=mutableMapOf("text" to text,"personId" to personId,"personEmail" to personEmail,"userName" to name)
                        val message=com.example.webexandroid.messaging.spaces.detail.Message(messageId,personEmail,text,personId,name)
                        messages.add(message)
                        //messageClientAdapter.notifyDataSetChanged()


                    }
                    runOnUiThread(object:Runnable{
                        override fun run() {
                            messages.reverse()
                            messageClientAdapter.update(messages)
                        }

                    })

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
                    "Bearer "+accessToken
                return params
            }
        }
        queue?.add(request)
        if(swipeContainer.isRefreshing()){
            swipeContainer.setRefreshing(false);
        }
    }

    private fun getpersonDetails(personId: String){
        val url = "https://webexapis.com/v1/people/"+personId
        var personName:String ?= null
        val request: JsonObjectRequest = object :

            JsonObjectRequest(Request.Method.GET, url, null, Response.Listener { response ->

                try {

                    //Log.e("response", response.getString("displayName"))
                    personName=response.getString("displayName")
                    //Log.e("personName", personName)
                    val mapperson= mutableMapOf("personName" to personName)
                    //messageClientAdapter.personName.add(mapperson as MutableMap<String, String>)
                    messageClientAdapter.notifyDataSetChanged()

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
                    "Bearer "+accessToken
                return params
            }
        }
        queue?.add(request)
    }

    private fun sendButtonClicked() {
        if (binding.editGchatMessage.text.isEmpty()) {
            //showDialogWithMessage(this@SpaceDetailActivity, R.string.post_message_error, getString(R.string.post_message_empty_error))
        } else {
            messageId?.let {
                // Edit message flow
                editMessage(it)
            }
                ?: composerType.let { type ->
                    id?.let {
                        when (type) {
                            Companion.ComposerType.POST_SPACE -> {
                                postToSpace(it, null)
                            }
                            Companion.ComposerType.POST_PERSON_ID -> {
                                Log.e("personid", it)
                                postPerson(it)
                            }
                            Companion.ComposerType.POST_PERSON_EMAIL -> {
                                Log.e("email", it)
                                //postPersonByEmail(it, null)
                            }
                        }
                    }
                }
        }
    }

    private fun editMessage(messageId: String) {
        val str = binding.editGchatMessage.text.toString()
        val messageContent = binding.editGchatMessage.getMessageContent()
        val text: Message.Text = Message.Text.plain(str)


        messageComposerViewModel.editMessage(messageId, text, messageContent.messageInputMentions)
    }

//    private fun postPersonByEmail(email: String, files: ArrayList<LocalFile>?) {
//        val emailAddress = EmailAddress.fromString(email)
//        emailAddress?.let {
//            messageComposerViewModel.postToPerson(emailAddress, binding.editGchatMessage.text.toString(), true, files)
//            showProgress()
//        } ?: run {
//            //showDialogWithMessage(this@SpaceDetailActivity, R.string.post_message_error, getString(R.string.post_message_email_empty))
//        }
//    }

//    private fun postPersonByEmail(email: String) {
//        val API: String = "&api_key=ZmEyYmUzOWYtZTg1YS00YzUzLWJjMDYtYTkwNmM4MGE0MzM3ZDcxZDYyZjYtOTAy_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f";
//        val email: String = "&toPersonEmail=rajithabhavani.kantheti@gmail.com";
//        val text: String= "&text=hello";
//        val URL_PREFIX: String = "https://webexapis.com/v1/messages";
//        var url: String=URL_PREFIX + API + email+ text
//
//        return new StringRequest(Request.Method.GET, url,
//        new Response.Listener<String>() {
//            // 3rd param - method onResponse lays the code procedure of success return
//            // SUCCESS
//            @Override
//            public void onResponse(String response) {
//                // try/catch block for returned JSON data
//                // see API's documentation for returned format
//                try {
//                    JSONObject result = new JSONObject(response).getJSONObject("list");
//                    int maxItems = result.getInt("end");
//                    JSONArray resultList = result.getJSONArray("item");
//
//                    ...
//
//                    // catch for the JSON parsing error
//                } catch (JSONException e) {
//                    Toast.makeText(AddFoodItems.this, e.getMessage(), Toast.LENGTH_LONG).show();
//                }
//            } // public void onResponse(String response)
//        }, // Response.Listener<String>()
//        new Response.ErrorListener() {
//            // 4th param - method onErrorResponse lays the code procedure of error return
//            // ERROR
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                // display a simple message on the screen
//                Toast.makeText(AddFoodItems.this, "Food source is not responding (USDA API)", Toast.LENGTH_LONG).show();
//            }
//        });
//    }

    private fun postPerson(email: String) {
        val emailAddress = EmailAddress.fromString(email)
        emailAddress?.let {
            //Log.e("spaceActivity", "inside")
            messageComposerViewModel.postMessageDraftNew(
                emailAddress.toString(),
                binding.editGchatMessage.text.toString()
            )
            showProgress()
        } ?: run {
            //showDialogWithMessage(this@SpaceDetailActivity, R.string.post_message_error, getString(R.string.post_message_email_empty))
        }
    }

    private fun postPersonById(personId: String, files: ArrayList<LocalFile>?) {
        messageComposerViewModel.postToPerson(
            personId,
            binding.editGchatMessage.text.toString(),
            true,
            files
        )
        showProgress()
    }

    private fun showProgress() {
        //binding.progressLayout.visibility = View.VISIBLE
    }

    private fun postToSpace(spaceId: String, files: ArrayList<LocalFile>?) {
        val messageContent = binding.editGchatMessage.getMessageContent()

        var progress = true

        replyParentMessage?.let { replyMessage ->
            val str = binding.editGchatMessage.text.toString()

            val text: Message.Text? = Message.Text.plain(str)


            text?.let { msgTxt ->
                val draft = Message.draft(msgTxt)

                messageContent.messageInputMentions?.let { mentionsArray ->
                    for (item in mentionsArray) {
                        draft.addMentions(item)
                    }
                }

                files?.let { filesArray ->
                    for (item in filesArray) {
                        draft.addAttachments(item)
                    }
                }

                draft.setParent(replyMessage.getMessage())

                //messageComposerViewModel.postMessageDraft(spaceId, draft)
            } ?: run {
                progress = false
                // showDialogWithMessage(this@SpaceDetailActivity, R.string.post_message_error, getString(R.string.post_message_invalid_message))
            }
        } ?: run {
            messageComposerViewModel.postToSpace(
                spaceId,
                binding.editGchatMessage.text.toString(),
                true,
                messageContent.messageInputMentions,
                files
            )
        }

        if (progress) {
            //showProgress()
        }
    }


    private fun replyMessageListener(message: SpaceMessageModel) {
        val model = ReplyMessageModel(
            message.spaceId,
            message.messageId,
            message.created,
            message.isSelfMentioned,
            message.parentId,
            message.isReply,
            message.personId,
            message.personEmail,
            message.toPersonId,
            message.toPersonEmail
        )
        ContextCompat.startActivity(
            this@SpaceDetailActivity,
            MessageComposerActivity.getIntent(
                this@SpaceDetailActivity,
                MessageComposerActivity.Companion.ComposerType.POST_SPACE,
                spaceDetailViewModel.spaceId,
                model
            ), null
        )
    }

    private fun editMessage(message: SpaceMessageModel) {
        startActivity(
            MessageComposerActivity.getIntent(
                this@SpaceDetailActivity, MessageComposerActivity.Companion.ComposerType.POST_SPACE,
                spaceDetailViewModel.spaceId, null, message.messageId
            )
        )
    }

    override fun onResume() {
        super.onResume()
        spaceDetailViewModel.getSpaceById()
        //getRequest()
        webexViewModel.setMessageObserver(this@SpaceDetailActivity)
        //spaceDetailViewModel.observeMessages()
        spaceMessageRecyclerView.smoothScrollToPosition(spaceMessageRecyclerView.getAdapter()?.itemCount!!)
    }

    private fun getMessages() {
        binding.noMessagesLabel.visibility = View.GONE
        //binding.progressLayout.visibility = View.VISIBLE
        spaceDetailViewModel.getMessages()
    }

    private fun displayPostMessageHandler(message: Message) {
        val builder: androidx.appcompat.app.AlertDialog.Builder =
            androidx.appcompat.app.AlertDialog.Builder(this)

        builder.setTitle(R.string.message_details)

//        DialogPostMessageHandlerBinding.inflate(layoutInflater)
//            .apply {
//                messageData = message
//                val msg = message.getTextAsObject()
//
//                msg.getMarkdown()?.let {
//                    messageBodyTextView.text = Html.fromHtml(msg.getMarkdown(), Html.FROM_HTML_MODE_LEGACY)
//                } ?: run {
//                    msg.getPlain()?.let {
//                        messageBodyTextView.text = Html.fromHtml(msg.getPlain(), Html.FROM_HTML_MODE_LEGACY)
//                    }
//                }
//                builder.setView(this.root)
//                builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
//                    dialog.dismiss()
//                }
//
//                builder.show()
//            }
    }

    private fun resetView() {
        binding.editGchatMessage.text.clear()
        hideKeyboard(binding.editGchatMessage)
        //hideProgress()
        //swipeContainer.setRefreshing(true)
        //val intent = intent
        //getRequest()
        //finish()
        //startActivity(intent)
    }

    private fun hideProgress() {
        //binding.progressLayout.visibility = View.GONE
    }

    private fun setUpObservers() {
        messageComposerViewModel.postMessages.observe(this@SpaceDetailActivity, Observer {
            it?.let {
                //displayPostMessageHandler(it)
            } ?: run {
                //showDialogWithMessage(this@SpaceDetailActivity, R.string.post_message_internal_error, "")
            }
            resetView()
        })

        messageComposerViewModel.postMessageError.observe(this@SpaceDetailActivity, Observer {
            it?.let {
                // showDialogWithMessage(this@SpaceDetailActivity, R.string.post_message_internal_error, it)
            } ?: run {
                //showDialogWithMessage(this@SpaceDetailActivity, R.string.post_message_internal_error, "")
            }
            resetView()
        })

        messageComposerViewModel.editMessage.observe(this@SpaceDetailActivity, Observer {
            it?.let {
                // showDialogWithMessage(this@SpaceDetailActivity, null, getString(R.string.message_edit_successful))
            } ?: run {
                // showDialogWithMessage(this@SpaceDetailActivity, null, getString(R.string.edit_message_internal_error))
            }
            resetView()
        })

        spaceDetailViewModel.space.observe(this@SpaceDetailActivity, Observer {
            binding.space = it
        })

//        personViewModel.person.observe(this@SpaceDetailActivity,Observer{
//            var personName=it.displayName
//            Log.e("PersonName",personName)
//        })

//        spaceDetailViewModel.spaceMessages.observe(this@SpaceDetailActivity, Observer { list ->
//            list?.let {
//                //binding.progressLayout.visibility = View.GONE
//                binding.swipeContainer.isRefreshing = false
//
//                if (it.isEmpty()) {
//                    binding.noMessagesLabel.visibility = View.VISIBLE
//                } else {
//                    binding.noMessagesLabel.visibility = View.GONE
//                }
//
//                messageClientAdapter.messages.clear()
//                messageClientAdapter.messages.addAll(it)
////                Toast.makeText(this, messageClientAdapter.itemCount.toString(), Toast.LENGTH_LONG)
////                    .show();
////                spaceMessageRecyclerView.scrollToPosition(messageClientAdapter.itemCount)
//                messageClientAdapter.notifyDataSetChanged()
//
//
//            }
//        })

//        spaceDetailViewModel.deleteMessage.observe(this@SpaceDetailActivity, Observer { model ->
//            model?.let {
//                val position = messageClientAdapter.messages.indexOf(it)
//                messageClientAdapter.messages.removeAt(position)
//                messageClientAdapter.notifyItemRemoved(position)
//            }
//        })

        spaceDetailViewModel.messageError.observe(
            this@SpaceDetailActivity,
            Observer { errorMessage ->
                errorMessage?.let {
                    showErrorDialog(it)
                }
            })

        spaceDetailViewModel.markMessageAsReadStatus.observe(
            this@SpaceDetailActivity,
            Observer { model ->
                model?.let {
                    //showDialogWithMessage(this@SpaceDetailActivity, R.string.success, "Message with id ${it.messageId} marked as read")
                }
            })

        spaceDetailViewModel.getMeData.observe(this@SpaceDetailActivity, Observer { model ->
            model?.let {
                MessageActionBottomSheetFragment.selfPersonId = it.personId
            }
        })

//        spaceDetailViewModel.messageEventLiveData.observe(this@SpaceDetailActivity, Observer { pair ->
//            if(pair != null) {
//                when (pair.first) {
//                    WebexRepository.MessageEvent.Received -> {
//                       // Log.d(tag, "Message Received event fired!")
//                        if(pair.second is Message) {
//                            val message = pair.second as Message
//                            // For replies, find parent and add to replies list at bottom.
//                            if(message.isReply()){
//                                val parentMessagePosition = messageClientAdapter.getPositionById(message.getParentId()?: "")
//                                // Ignore case when parent is not found, as parent might not be present in the list
//                                if(parentMessagePosition != -1) {
//                                    if(parentMessagePosition == messageClientAdapter.messages.size - 1 ){
//                                        messageClientAdapter.messages.add(SpaceMessageModel.convertToSpaceMessageModel(message))
//                                        messageClientAdapter.notifyItemInserted(messageClientAdapter.messages.size - 1)
//                                    }else {
//                                        var positionToInsert = parentMessagePosition + 1
//                                        for(i in (parentMessagePosition + 1) until messageClientAdapter.messages.size - 1) {
//                                            if (!messageClientAdapter.messages[i].isReply){
//                                                positionToInsert = i;
//                                                break;
//                                            }
//                                        }
//                                        messageClientAdapter.messages.add(positionToInsert, SpaceMessageModel.convertToSpaceMessageModel(message))
//                                        messageClientAdapter.notifyItemInserted(positionToInsert)
//                                    }
//                                }
//                            }else {
//                                messageClientAdapter.messages.add(SpaceMessageModel.convertToSpaceMessageModel(message))
//                                messageClientAdapter.notifyItemInserted(messageClientAdapter.messages.size - 1)
//                            }
//                        }
//                    }
//                    WebexRepository.MessageEvent.Deleted -> {
//                        if (pair.second is String?) {
//                            //Log.d(tag, "Message Deleted event fired!")
//                            val position = messageClientAdapter.getPositionById(pair.second as String? ?: "")
//                            if (!messageClientAdapter.messages.isNullOrEmpty() && position != -1) {
//                                messageClientAdapter.messages.removeAt(position)
//                                messageClientAdapter.notifyItemRemoved(position)
//                            }
//                        }
//                    }
//                    WebexRepository.MessageEvent.MessageThumbnailUpdated -> {
//                        //Log.d(tag, "Message ThumbnailUpdated event fired!")
//                        val fileList: List<RemoteFile>? = pair.second as? List<RemoteFile>
//                        if(!fileList.isNullOrEmpty()){
//                            for( thumbnail in fileList){
//                                Log.d(tag, "Message Updated thumbnail : ${thumbnail.getDisplayName()}")
//                            }
//                        }
//
//                    }
////                    WebexRepository.MessageEvent.Edited -> {
////                        if (pair.second is Message) {
////                            val message = pair.second as Message
////                            val position = messageClientAdapter.getPositionById(message.getId() ?: "")
////                            if (!messageClientAdapter.messages.isNullOrEmpty() && position != -1) {
////                                messageClientAdapter.messages[position] = SpaceMessageModel.convertToSpaceMessageModel(message)
////                                messageClientAdapter.notifyItemChanged(position)
////                            }
////                        }
////                    }
//                }
//            }
//        })
//    }
//
    }

    class WrapContentLayoutManager{

    }

    class MessageClientAdapter(
        private val messageActionBottomSheetFragment: MessageActionBottomSheetFragment,
        private val fragmentManager: FragmentManager
    ) : RecyclerView.Adapter<MessageClientViewHolder>() {
        var messages: MutableList<com.example.webexandroid.messaging.spaces.detail.Message> = mutableListOf()
        //var personName: MutableList<MutableMap<String, String>> = mutableListOf()
//    fun getPositionById(messageId: String): Int {
//        return messages.indexOfFirst { it.messageId == messageId }
//    }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageClientViewHolder {
            return MessageClientViewHolder(
                SentMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                messageActionBottomSheetFragment, fragmentManager
            )
        }

        override fun getItemCount(): Int = messages.size


        override fun onBindViewHolder(holder: MessageClientViewHolder, position: Int) {
            holder.bind(messages[position])
            //Log.e("messages",messages[position].toString())
        }

        fun update(messages: MutableList<com.example.webexandroid.messaging.spaces.detail.Message>) {
            val callback:MessageDiffUtil= MessageDiffUtil(this.messages,messages)
            val diffUtil :DiffUtil.DiffResult=DiffUtil.calculateDiff(callback)
            this.messages.clear()
            this.messages.addAll(messages)
            diffUtil.dispatchUpdatesTo(this)
        }

        fun addItem(message: com.example.webexandroid.messaging.spaces.detail.Message) {
            this.messages.add(message)
            this.notifyItemInserted(this.messages.size-1)
        }

    }

    class MessageClientViewHolder(
        private val binding: SentMessageBinding,
        private val messageActionBottomSheetFragment: MessageActionBottomSheetFragment,
        private val fragmentManager: FragmentManager
    ) : RecyclerView.ViewHolder(binding.root) {
        var messageItem: com.example.webexandroid.messaging.spaces.detail.Message ?= null
        val tag = "MessageClientViewHolder"
        private lateinit var userRef: DatabaseReference
        var anonymousname: String? = null

    init {
        binding.messageContainer.setOnClickListener {
            messageItem?.let { message ->
                MessageDetailsDialogFragment.newInstance(message.message!!).show(fragmentManager, "MessageDetailsDialogFragment")
            }
        }
    }

        fun bind(message: com.example.webexandroid.messaging.spaces.detail.Message) {
            val spaceDetailActivity= SpaceDetailActivity()
            var personName:String?=null
            //binding.message = message.get("text")
            messageItem = message
//        binding.messageContainer.setOnLongClickListener { view ->
//            messageActionBottomSheetFragment.message = message
//            messageActionBottomSheetFragment.show(fragmentManager, MessageActionBottomSheetFragment.TAG)
//            true
//        }

            //binding.messagerName?.text = messages.get("personName")
            var personEmail = message.personEmail
            if(personEmail=="rkanthet@cisco.com")
            {
                personName="Rajitha Kantheti"
            }
            else
                if(personEmail=="hachawla@cisco.com")
                {
                    personName="Harish Chawla"
                }
            else
                if(personEmail=="ashessin@cisco.com")
                {
                    personName="Ashesh Singh"
                }
            else
                if(personEmail=="hca-helen@webex.bot")
                {
                    personName="Helen"
                }

//        when {
//            message.messageBody.getMarkdown() != null -> {
//                if(personEmail.contains("appid.ciscospark.com")) {
//                    binding.cardGchatMessageMe?.visibility=View.VISIBLE
//                    binding.cardGchatMessageYou?.visibility=View.GONE
//                    binding.messagerName?.visibility=View.GONE
//                    binding.imageGchatProfileOther?.visibility=View.GONE
//                    binding.myName?.text=message.name
//                    binding.textGchatMessageMe.text =
//                        Html.fromHtml(message.messageBody.getMarkdown(), Html.FROM_HTML_MODE_LEGACY)
//                }
//                else
//                {
//                    binding.cardGchatMessageYou?.visibility=View.VISIBLE
//                    binding.cardGchatMessageMe?.visibility=View.GONE
//                    binding.messagerName?.visibility=View.VISIBLE
//                    binding.imageGchatProfileOther?.visibility=View.VISIBLE
//                    binding.myName?.visibility=View.GONE
//                    binding.textGchatMessageYou?.text =
//                        Html.fromHtml(message.messageBody.getMarkdown(), Html.FROM_HTML_MODE_LEGACY)
//                }
//            }
//            message.messageBody.getPlain() != null -> {
            if (personEmail?.contains("appid.ciscospark.com") == true) {
                binding.cardGchatMessageMe?.visibility = View.VISIBLE
                binding.cardGchatMessageYou?.visibility = View.GONE
                binding.myName?.visibility = View.VISIBLE
                binding.messagerName?.visibility = View.GONE
                binding.imageGchatProfileOther?.visibility = View.GONE
               binding.myName?.text = message.userName
                binding.textGchatMessageMe.text = message.message
            } else {
                binding.cardGchatMessageYou?.visibility = View.VISIBLE
                binding.cardGchatMessageMe?.visibility = View.GONE
                binding.myName?.visibility = View.GONE
                binding.messagerName?.visibility = View.VISIBLE
                binding.messagerName?.text = personName
                binding.imageGchatProfileOther?.visibility = View.VISIBLE
                binding.textGchatMessageYou?.text = message.message
            }
//            }
//            else -> {
//                binding.textGchatMessageMe.text = ""
//            }
//        }

            binding.executePendingBindings()
        }
    }

    override fun onMessageReceived(message: Message) {

        message?.let {
            val message=com.example.webexandroid.messaging.spaces.detail.Message(it.getId(),it.getPersonEmail().toString(),it.getText(),it.getPersonId().toString(),name)
            //listener.onMessageReceived(message)
            Log.e("OnmessageReceived",message.message)
            messageClientAdapter.addItem(message)
        }
        //val message=com.example.webexandroid.messaging.spaces.detail.Message(it.getId(),it.getPersonEmail().toString(),it.getText(),it.getPersonId().toString())

        //messageClientAdapter.addItem(message)
    }
}