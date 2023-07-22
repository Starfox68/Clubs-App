package com.shaphr.accessanotes

import android.os.Environment


abstract class FileManagerAbstract {

    protected val filePath = "${Environment.getExternalStorageDirectory()}/Download"
    protected val disclaimer = "This notes document was generated through AccessaNotes from an " +
            "audio recording using AI. Information may be inaccurate. Use at your own caution."
    protected val titleSize = 24
    protected val bodySize = 16
    protected val disclaimerSize = 14
    protected val imageWidth = 300F
    protected val imageHeight = 100F

    fun writeNote(title: String, content: List<Any>) {
        val doc = createDoc(title, content)
        writeDoc(title, doc)
    }

    // Content must be list of strings/bitmap images
    protected abstract fun createDoc(title: String, content: List<Any>): Any
    protected abstract fun writeDoc(title: String, doc: Any)
}
