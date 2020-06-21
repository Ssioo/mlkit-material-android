package com.google.firebase.ml.md.views

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ml.md.R
import com.google.firebase.ml.md.models.Data
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    lateinit var gridAdapter: GridAdapter
    lateinit var data: Data
    var imgNo: Int = 1

    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        db.collection("data").document("imag2").get()
                .addOnSuccessListener { documentSnapshot ->
                    Log.i("DATA", documentSnapshot.data.toString())
                    data = documentSnapshot.toObject(Data::class.java)!!
                    doOnNextGetDb()
                }.addOnFailureListener {
                    it.printStackTrace()
                }

    }

    fun doOnNextGetDb() {
        val datum = data.images?.get(imgNo)!!
        gridAdapter = GridAdapter(datum.width, datum.height) {
            Toast.makeText(this, "Click", Toast.LENGTH_SHORT).show()
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(50)
        }
        rv_touch_grid.adapter = gridAdapter
        Glide.with(this)
                .load(datum.imgUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        pb_loading.visibility = View.GONE
                        iv_img.alpha = 1f
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        pb_loading.visibility = View.GONE
                        iv_img.alpha = 1f
                        return false
                    }

                })
                .centerCrop()
                .into(iv_img)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                db.collection("log").document().set(messages)
            }
        }
    }
}