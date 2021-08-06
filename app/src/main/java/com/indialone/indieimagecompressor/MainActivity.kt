package com.indialone.indieimagecompressor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.indialone.indieimagecompressor.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import id.zelory.compressor.Compressor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOError
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var originalImage: File
    private lateinit var compressedImage: File
    private var filePath: String = ""
    private var path: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/IndieImageCompressor")
    private val RESULT_IMAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        askPermission()

        filePath = path.absolutePath

        if (!path.exists()) {
            path.mkdirs()
        }

        mBinding.seekbarQuality.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mBinding.tvQuality.text = "Quality: $progress"
                seekBar!!.max = 100
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        mBinding.btnPick.setOnClickListener {
            openGallery()
        }

        mBinding.btnCompress.setOnClickListener {
            val quality: Int = mBinding.seekbarQuality.progress
            val width: Int = Integer.parseInt(mBinding.etWidth.text.toString())
            val height: Int = Integer.parseInt(mBinding.etHeight.text.toString())

            try {
                Log.e("original compressed", "$originalImage")
                compressedImage = Compressor(this)
                    .setMaxWidth(width)
                    .setMaxHeight(height)
                    .setQuality(quality)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setDestinationDirectoryPath(filePath)
                    .compressToFile(originalImage)


                val finalFile = File(filePath, compressedImage.name)
                val finalBitmap: Bitmap = BitmapFactory.decodeFile(finalFile.absolutePath)
                mBinding.ivCompressed.setImageBitmap(finalBitmap)
                mBinding.tvCompressed.setText("Size: ${Formatter.formatShortFileSize(this@MainActivity, finalFile.length())}")
                Toast.makeText(this, "Image Compressed and Saved!", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error while compressing", Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(intent, RESULT_IMAGE)
    }

    private fun askPermission() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {

                }

                override fun onPermissionRationaleShouldBeShown(
                    request: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token!!.continuePermissionRequest()
                }
            }).check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            mBinding.btnCompress.visibility = View.VISIBLE
                    val imageUri = data!!.data
                    try {
                        val imageStream = contentResolver.openInputStream(imageUri!!)
                        val selectedImage: Bitmap = BitmapFactory.decodeStream(imageStream)
                        mBinding.ivOriginal.setImageBitmap(selectedImage)

                        Log.e("image uri", "${imageUri.path}")
                        originalImage = File(getRealPathFromUri(this, imageUri))
                        Log.e("original image", "$originalImage")
                        mBinding.tvOriginal.setText("Size: ${Formatter.formatShortFileSize(this, originalImage.length())}")
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Something went wrong!!", Toast.LENGTH_SHORT).show()
                    }

        } else {
            Toast.makeText(this, "No image selected!!", Toast.LENGTH_SHORT).show()
        }


    }

    private fun getRealPathFromUri(context: Context, contentUri: Uri?): String {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
            val column_index: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } finally {
            cursor?.close()
        }
    }

}