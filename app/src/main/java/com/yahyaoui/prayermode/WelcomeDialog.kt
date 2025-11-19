package com.yahyaoui.prayermode

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment

class WelcomeDialog : DialogFragment() {

    interface WelcomeDialogListener {
        fun onNextClicked()
    }

    private var listener: WelcomeDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is WelcomeDialogListener) listener = context
        else throw ClassCastException("$context must implement WelcomeDialogListener")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_welcome, container, false)
        val btnNext: Button = view.findViewById(R.id.btnNext)
        btnNext.setOnClickListener {
            if (BuildConfig.DEBUG) Log.d("WelcomeDialog", "Next button clicked.")
            listener?.onNextClicked()
            dismiss()
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        val params = dialog?.window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params as android.view.WindowManager.LayoutParams
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                return@setOnKeyListener true
            }
            false
        }
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return dialog
    }
}