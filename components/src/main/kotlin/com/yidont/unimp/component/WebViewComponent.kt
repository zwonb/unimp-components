package com.yidont.unimp.component

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.yidont.library.utils.fileToUri
import io.dcloud.feature.uniapp.UniSDKInstance
import io.dcloud.feature.uniapp.ui.action.AbsComponentData
import io.dcloud.feature.uniapp.ui.component.AbsVContainer
import io.dcloud.feature.uniapp.ui.component.UniComponent
import io.dcloud.feature.uniapp.ui.component.UniComponentProp
import java.io.File

class WebViewComponent(
    instance: UniSDKInstance?,
    parent: AbsVContainer<*>?,
    componentData: AbsComponentData<*>?
) : UniComponent<WebView>(instance, parent, componentData) {

    private var chromeClient: MyWebChromeClient? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun initComponentHostView(context: Context): WebView {
        val webView = WebView(mUniSDKInstance.context).apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString()
                    if (url != null) {
                        view?.loadUrl(url)
                        return true
                    }
                    return false
                }
            }
            webChromeClient = MyWebChromeClient(context).also {
                chromeClient = it
            }

            addJsObject()
        }
        return webView
    }

    @UniComponentProp(name = "loadUrl")
    fun setLoadUrl(url: String) {
        hostView.loadUrl(url)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            1 -> chromeClient?.onChooseFile(
                resultCode == Activity.RESULT_OK,
                chromeClient?.takePhotoFile?.fileToUri(context)
            )

            2 -> chromeClient?.onChooseFile(
                resultCode == Activity.RESULT_OK,
                chromeClient?.videoFile?.fileToUri(context)
            )

            3 -> {
                val uri = data?.data
                chromeClient?.onChooseFile(resultCode == Activity.RESULT_OK, uri)
            }
        }
    }

    override fun onActivityDestroy() {
        chromeClient = null
        hostView.removeJavascriptInterface("JsObject")
        hostView.destroy()
    }

    private fun WebView.addJsObject() {
        class JsObject {

            @JavascriptInterface
            fun refresh() {
                (mUniSDKInstance.context as? Activity)?.runOnUiThread {
                    // vue3 编译后变成 小写加"-"
                    fireEvent("on-refresh")
                }
            }

            @JavascriptInterface
            fun goUserHome() {
                (mUniSDKInstance.context as? Activity)?.runOnUiThread {
                    fireEvent("on-go-user-home")
                }
            }

        }

        addJavascriptInterface(JsObject(), "JsObject")
    }

    class MyWebChromeClient(private val context: Context) : WebChromeClient() {

        val takePhotoFile get() = File(context.externalCacheDir, "take_photo.png")
        val videoFile get() = File(context.externalCacheDir, "video.mp4")
        private var callback: ValueCallback<Array<Uri>>? = null

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            callback = filePathCallback
            val type = fileChooserParams?.acceptTypes?.firstOrNull() ?: "*/*"
            val captureEnabled = fileChooserParams?.isCaptureEnabled ?: false
            val activity = context as Activity
            if (captureEnabled) {
                when (type) {
                    "image/*" -> try {
                        if (ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), 99)
                        }
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, takePhotoFile.fileToUri(context))
                        }
                        activity.startActivityForResult(intent, 1)
                    } catch (e: Throwable) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        return false
                    }

                    "video/*" -> try {
                        if (ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), 99)
                        }
                        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, videoFile.fileToUri(context))
                        }
                        activity.startActivityForResult(intent, 2)
                    } catch (e: Throwable) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        return false
                    }

                    "audio/*" -> try {
                        val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                        activity.startActivityForResult(intent, 3)
                    } catch (e: Throwable) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        return false
                    }
                }
            } else {
                try {
//                if (fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE) {
//                    val intent = Intent(Intent.ACTION_GET_CONTENT)
//                        .addCategory(Intent.CATEGORY_OPENABLE)
//                        .setType(type)
//                        .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
//                    activity.startActivityForResult(intent, 3)
//                } else {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(type)
                    activity.startActivityForResult(intent, 3)
//                }
                } catch (e: Exception) {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    return false
                }
            }
            return true
        }

        fun onChooseFile(success: Boolean, uri: Uri?) {
            val uris = if (success && uri != null) arrayOf(uri) else null
            callback?.onReceiveValue(uris)
        }

        fun onChooseFiles(uri: List<Uri>?) {
            val uris = if (!uri.isNullOrEmpty()) uri.toTypedArray() else null
            callback?.onReceiveValue(uris)
        }

    }
}
