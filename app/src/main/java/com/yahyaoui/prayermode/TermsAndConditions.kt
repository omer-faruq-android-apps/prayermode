package com.yahyaoui.prayermode

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.core.net.toUri
import android.graphics.Color
import android.text.SpannableString
import android.text.style.UnderlineSpan
import androidx.core.graphics.drawable.toDrawable

class TermsAndConditions : DialogFragment() {

    private val tag = "TermsAndConditions"
    companion object {
         const val TERMS_URL = "https://prayermode.github.io/prayermode/terms.html"
         const val PRIVACY_URL = "https://prayermode.github.io/prayermode/privacy.html"
         const val DONATION_URL = "https://www.paypal.me/prayermode"
    }
    interface TermsAndConditionsListener {
        fun onTermsAccepted()
        fun onTermsDeclined()
    }

    private var listener: TermsAndConditionsListener? = null
    private lateinit var tvMessage: TextView
    private lateinit var tvTermsLink: TextView
    private lateinit var tvPrivacyLink: TextView
    private lateinit var sharedHelper: SharedHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TermsAndConditionsListener) listener = context
        else throw ClassCastException("$context must implement TermsAndConditionsListener")
        sharedHelper = SharedHelper(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_terms_and_conditions, container, false)

        tvMessage = view.findViewById(R.id.tvMessage)
        tvTermsLink = view.findViewById(R.id.tvTermsLink)
        tvPrivacyLink = view.findViewById(R.id.tvPrivacyLink)

        val btnAccept: Button = view.findViewById(R.id.btnAcceptTerms)
        val btnDecline: Button = view.findViewById(R.id.btnDeclineTerms)
        val spannable = SpannableString(getString(R.string.terms_dialog_link))
        spannable.setSpan(UnderlineSpan(), 0, spannable.length, 0)
        tvTermsLink.text = spannable
        tvTermsLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, TERMS_URL.toUri())
            startActivity(intent)
        }

        val spannablePrivacy = SpannableString(getString(R.string.privacy_link))
        spannablePrivacy.setSpan(UnderlineSpan(), 0, spannablePrivacy.length, 0)
        tvPrivacyLink.text = spannablePrivacy
        tvPrivacyLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, PRIVACY_URL.toUri())
            startActivity(intent)
        }

        btnAccept.setOnClickListener {
            sharedHelper.saveTermsAccepted(true)
            if (BuildConfig.DEBUG) Log.d(tag, "Terms accepted.")
            listener?.onTermsAccepted()
            dismiss()
        }

        btnDecline.setOnClickListener {
            if (BuildConfig.DEBUG) Log.d(tag, "Terms declined.")
            listener?.onTermsDeclined()
            dismiss()
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return dialog
    }
}