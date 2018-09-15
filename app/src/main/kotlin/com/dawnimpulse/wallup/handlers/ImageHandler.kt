package com.dawnimpulse.wallup.handlers

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import androidx.core.widget.toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.dawnimpulse.wallup.utils.C
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


/*
ISC License

Copyright 2018, Saksham (DawnImpulse)

Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted,
provided that the above copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE
OR PERFORMANCE OF THIS SOFTWARE.*/

/**
 * @author Saksham
 *
 * @note Last Branch Update - master
 * @note Created on 2018-05-15 by Saksham
 *
 * @note Updates :
 *  Saksham - 2018 05 25 - recent - adding bitmap callback
 *  Saksham - 2018 09 07 - master - adding bitmap callback
 */
object ImageHandler {

    /**
     * Use to set image on a view using glide
     * @param lifecycle
     * @param view
     * @param url
     */
    fun setImageInView(lifecycle: Lifecycle, view: ImageView, url: String) {
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                Glide.with(view.context)
                        .load(url)
                        .into(view)
            }
        })
    }

    /**
     * get image as bitmap from url
     * @param lifecycle
     * @param context
     * @param url
     */
    fun getImageAsBitmap(lifecycle: Lifecycle, context: Context, url: String, callback: (bitmap: Bitmap) -> Unit) {
        Glide.with(context)
                .load(url)
                .asBitmap()
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
                        lifecycle.addObserver(object : LifecycleObserver {
                            var yes = true
                            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                            fun onResume() {
                                if (yes) {
                                    resource.let {
                                        callback(it!!)
                                    }
                                    yes = false
                                }
                            }
                        })
                    }
                })

    }

    /**
     * sharing image via intent
     */
    fun shareImage(context: Context, bitmap: Bitmap, name: String, url: String) {
        launch {
            try {
                val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), C.WALLUP)
                val filePath = File(folder, "$name.jpg")

                if (!folder.exists())
                    folder.mkdir()
                if (!filePath.exists())
                    filePath.createNewFile()
                //val filePath = File(context.externalCacheDir, "${C.WALLUP}.jpg")

                val stream = FileOutputStream(filePath)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                stream.flush()
                stream.close()

                val shareIntent = Intent()
                val uri = FileProvider.getUriForFile(context, context.packageName, filePath)
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(filePath))
                else
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);

                shareIntent.putExtra(Intent.EXTRA_TEXT, "View this image & many more on Unsplash." +
                        " Download wallup to access Unsplash amazing library \n\nWallup - ${C.WALLUP_PLAY} \n\nImage on Unsplash -  $url")
                shareIntent.type = "image/jpg"
                (context as AppCompatActivity).runOnUiThread {
                    context.startActivity(Intent.createChooser(shareIntent, "Choose an app"))
                }
            } catch (e: IOException) {
                e.printStackTrace()
                context.toast("error sharing image (${C.ERROR_CODE_2})")
            }
        }
    }
}