package com.crestron.clienttest

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

fun EditText.requestAndShowKeyboard() {
    requestFocus()
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInput(
        InputMethodManager.SHOW_FORCED,
        InputMethodManager.HIDE_IMPLICIT_ONLY
    )
}

fun RecyclerView.smoothScrollAction(
    position: Int,
    action: () -> Unit
) = GlobalScope.launch {
    smoothScrollToPosition(position)
    delay(if (adapter!!.itemCount / 10 < 250) 250 else adapter!!.itemCount / 10L)
    stopScroll()
    withContext(Dispatchers.Main) {
        scrollToPosition(position)
    }
    delay(100)
    action()
}