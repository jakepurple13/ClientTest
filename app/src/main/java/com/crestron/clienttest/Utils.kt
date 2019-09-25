package com.crestron.clienttest

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun EditText.requestAndShowKeyboard() {
    requestFocus()
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.toggleSoftInput(
        InputMethodManager.SHOW_FORCED,
        InputMethodManager.HIDE_IMPLICIT_ONLY
    )
}