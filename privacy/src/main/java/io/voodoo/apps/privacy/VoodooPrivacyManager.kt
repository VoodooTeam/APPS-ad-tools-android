package io.voodoo.app.privacy

import android.app.Activity
import android.view.View
import com.sourcepoint.cmplibrary.NativeMessageController
import com.sourcepoint.cmplibrary.SpClient
import com.sourcepoint.cmplibrary.core.nativemessage.MessageStructure
import com.sourcepoint.cmplibrary.creation.config
import com.sourcepoint.cmplibrary.creation.delegate.spConsentLibLazy
import com.sourcepoint.cmplibrary.exception.CampaignType
import com.sourcepoint.cmplibrary.model.ConsentAction
import com.sourcepoint.cmplibrary.model.PMTab
import com.sourcepoint.cmplibrary.model.exposed.ActionType
import com.sourcepoint.cmplibrary.model.exposed.SPConsents
import com.sourcepoint.cmplibrary.model.exposed.SpConfig
import io.voodoo.app.privacy.config.CmpPurpose
import io.voodoo.app.privacy.config.CmpPurposeHelper
import io.voodoo.app.privacy.config.CmpVendorHelper
import io.voodoo.app.privacy.config.SourcepointConfiguration
import io.voodoo.app.privacy.model.VoodooPrivacyConsent
import org.json.JSONObject
/**
 *
 */
class VoodooPrivacyManager (
    private val currentActivity: Activity,
    private var autoShowPopup: Boolean = true,
    private var onConsentReceived: ((VoodooPrivacyConsent) -> Unit)? = null,
    private var onUiReady: (() -> Unit)? = null,
    private var onError: ((Throwable) -> Unit)? = null,
    private var onStatusUpdate: ((ConsentStatus) -> Unit)? = null
) {
    private var forceAutoShow = false
    private var consentStatus: ConsentStatus = ConsentStatus.NA
    private var receivedConsent: SPConsents? = null
    private val cmpConfig: SpConfig = config {
        accountId = SourcepointConfiguration.accountId
        propertyId = SourcepointConfiguration.propertyId
        propertyName = SourcepointConfiguration.propertyName
        messLanguage = VoodooPrivacyLanguageMapper.getLanguage()
        +CampaignType.GDPR
        +CampaignType.CCPA
    }

    private val spConsentLib by spConsentLibLazy {
        activity = currentActivity
        spClient = LocalClient()
        spConfig = cmpConfig
    }

    private var viewToShow: View? = null

    /**
     * Determine if the consent view is ready to be displayed or not
     */
    fun consentViewReady() = viewToShow != null

    /**
     * Show consent view dialog manually after consent is being loaded
     */
    fun showDialog(): Boolean {
        return if (consentViewReady()) {
            viewToShow?.let {
                spConsentLib.showView(it)
                setConsentStatus(ConsentStatus.UI_SHOWN)
            }
            viewToShow = null
            true
        } else {
            false
        }
    }

    private fun loadMessage(){
        setConsentStatus(ConsentStatus.LOADING)
        spConsentLib.loadMessage()
    }

    /**
     * Show consent edit settings
     */
    fun forceShowReConsent() {
        forceAutoShow = true
        setConsentStatus(ConsentStatus.LOADING)
        spConsentLib.loadPrivacyManager(
            SourcepointConfiguration.privacyManagerId,
            PMTab.PURPOSES,
            CampaignType.GDPR
        )
    }

    /**
     * Initialize the consent manager and download the FTL message
     */
    fun initializeConsent() {
        loadMessage()
    }

    /**
     * Need to be called on onDestroy lifecycle of the activity
     */
    fun onDestroy() {
        onConsentReceived = null
        onStatusUpdate = null
        onError = null
        onUiReady = null
        spConsentLib.dispose()
    }

    /**
     * Set the onUiReady callback, onUiReady will be called once the consent dialog is ready
     * to be displayed
     */
    fun setOnUiReady(onUiReady: (() -> Unit)) {
        this.onUiReady = onUiReady
    }

    /**
     * Set the onConsentReady callback, onConsentReady will be called once the client received
     * the saved consent
     */
    fun setOnConsentReady(onConsentReceived: ((VoodooPrivacyConsent) -> Unit)?) {
        this.onConsentReceived = onConsentReceived
    }

    /**
     * Set the onErrorCallback, onError will be called if there are an error when loading the consent
     */
    fun setOnError(onError: ((Throwable) -> Unit)) {
        this.onError = onError
    }

    private fun getPrivacyConsent(): VoodooPrivacyConsent {
        return VoodooPrivacyConsent(
            adConsent = CmpPurposeHelper.get(CmpPurpose.STORE_AND_ACCESS_INFO_ON_DEVICE) &&
                    CmpPurposeHelper.get(CmpPurpose.USE_LIMITED_DATA_TO_SELECT_ADVERTISING) &&
                    CmpPurposeHelper.get(CmpPurpose.CREATE_PROFILE_FOR_ADVERTISING) &&
                    CmpPurposeHelper.get(CmpPurpose.MEASURE_ADVERTISING_PERFORMANCE) &&
                    CmpPurposeHelper.get(CmpPurpose.USE_PROFILE_FOR_ADVERTISING) &&
                    CmpPurposeHelper.get(CmpPurpose.USE_LIMITED_DATA_TO_SELECT_CONTENT),
            analyticsConsent = CmpPurposeHelper.get(CmpPurpose.STORE_AND_ACCESS_INFO_ON_DEVICE) &&
                    CmpPurposeHelper.get(CmpPurpose.MEASURE_ADVERTISING_PERFORMANCE) &&
                    CmpPurposeHelper.get(CmpPurpose.MEASURE_CONTENT_PERFORMANCE) &&
                    CmpPurposeHelper.get(CmpPurpose.DEVELOP_AND_IMPROVE_SERVICE) &&
                    CmpPurposeHelper.get(CmpPurpose.GATHER_AUDIENCE_STATISTICS),
            privacyApplicable = isPrivacyApplies()
        )
    }

    fun isPrivacyApplies(): Boolean {
        return isGdprApplicable() || isUsNatApplicable() || isCcpaApplicable()
    }

    private fun isGdprApplicable(): Boolean {
        return receivedConsent?.gdpr?.consent?.applies == true
    }

    private fun isUsNatApplicable(): Boolean {
        return receivedConsent?.usNat?.consent?.applies == true
    }

    private fun isCcpaApplicable(): Boolean {
        return receivedConsent?.ccpa?.consent?.applies == true
    }

    fun getStatus(): ConsentStatus {
        return consentStatus
    }

    private fun processConsent(consent: SPConsents){
        val gdprGrants = consent.gdpr?.consent?.grants ?: mapOf()
        CmpPurposeHelper.setInitialized()
        CmpVendorHelper.setInitialized()
        gdprGrants.entries.forEach { grant ->
            CmpVendorHelper.put(grant.key, grant.value.granted)
            grant.value.purposeGrants.forEach { purpose ->
                CmpPurposeHelper.put(purpose.key, purpose.value)
            }
        }
    }

    private fun setConsentStatus(status: ConsentStatus) {
        consentStatus = if(status == ConsentStatus.RECEIVED && !isPrivacyApplies()) {
            ConsentStatus.NON_APPLICABLE
        } else
            status
        onStatusUpdate?.invoke(consentStatus)
    }

    internal inner class LocalClient : SpClient {
        override fun onUIFinished(view: View) {
            viewToShow = null
            spConsentLib.removeView(view)
        }

        override fun onUIReady(view: View) {
            setConsentStatus(ConsentStatus.UI_READY)
            if (autoShowPopup || forceAutoShow) {
                setConsentStatus(ConsentStatus.UI_SHOWN)
                spConsentLib.showView(view)
                forceAutoShow = false
            } else {
                viewToShow = view
            }
            onUiReady?.invoke()
        }

        override fun onNativeMessageReady(
            message: MessageStructure,
            messageController: NativeMessageController
        ) {
        }

        override fun onError(error: Throwable) {
            setConsentStatus(ConsentStatus.ERROR)
            onError?.invoke(error)
        }

        override fun onMessageReady(message: JSONObject) {

        }

        override fun onConsentReady(consent: SPConsents) {
            processConsent(consent)
            receivedConsent = consent
            setConsentStatus(ConsentStatus.RECEIVED)
            onConsentReceived?.invoke(getPrivacyConsent())
        }

        override fun onAction(view: View, consentAction: ConsentAction): ConsentAction  {
            if (consentAction.actionType == ActionType.SHOW_OPTIONS && !autoShowPopup) {
                forceAutoShow = true
            }
            return consentAction
        }

        override fun onNoIntentActivitiesFound(url: String) {
        }

        override fun onSpFinished(sPConsents: SPConsents) {

        }
    }

    enum class ConsentStatus {
        NA,
        NON_APPLICABLE,
        LOADING,
        UI_READY,
        UI_SHOWN,
        ERROR,
        RECEIVED
    }
}


