package eu.kanade.tachiyomi.widget.preference

import yokai.util.koin.get
import android.os.Bundle
import android.view.View
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.launch
import yokai.i18n.MR
import yokai.util.lang.getString

class TrackLoginDialog(usernameLabelRes: StringResource? = null, bundle: Bundle? = null) :
    LoginDialogPreference(usernameLabelRes, bundle) {

    private val service = get<TrackManager>().getService(args.getLong("key"))!!

    override var canLogout = true

    constructor(service: TrackService, usernameLabelRes: StringResource?) :
        this(usernameLabelRes, Bundle().apply { putLong("key", service.id) })

    override fun setCredentialsOnView(view: View) = with(view) {
        val serviceName = context.getString(service.nameRes())
        binding.dialogTitle.text = context.getString(MR.strings.log_in_to_, serviceName)
        binding.username.setText(service.getUsername())
        binding.password.setText(service.getPassword())
    }

    override fun checkLogin() {
        v?.apply {
            binding.login.startAnimation()
            if (binding.username.text.isNullOrBlank() || binding.password.text.isNullOrBlank()) {
                errorResult()
                context.toast(MR.strings.username_must_not_be_blank)
                return
            }

            dialog?.setCancelable(false)
            dialog?.setCanceledOnTouchOutside(false)
            val user = binding.username.text.toString()
            val pass = binding.password.text.toString()
            scope.launch {
                try {
                    val result = withIOContext { service.login(user, pass) }
                    if (result) {
                        dialog?.dismiss()
                        context.toast(MR.strings.successfully_logged_in)
                    } else {
                        errorResult()
                    }
                } catch (error: Exception) {
                    errorResult()
                    error.message?.let { context.toast(it) }
                }
            }
        }
    }

    private fun errorResult() {
        v?.apply {
            dialog?.setCancelable(true)
            dialog?.setCanceledOnTouchOutside(true)
            binding.login.revertAnimation {
                binding.login.text = activity!!.getString(MR.strings.unknown_error)
            }
        }
    }

    override fun onDialogClosed() {
        super.onDialogClosed()
        (targetController as? Listener)?.trackLoginDialogClosed(service)
    }

    interface Listener {
        fun trackLoginDialogClosed(service: TrackService)
    }
}
