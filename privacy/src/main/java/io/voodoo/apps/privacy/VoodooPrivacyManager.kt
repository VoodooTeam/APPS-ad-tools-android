package io.voodoo.apps.privacy

import android.app.Activity
import android.util.Log
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.sourcepoint.cmplibrary.NativeMessageController
import com.sourcepoint.cmplibrary.SpClient
import com.sourcepoint.cmplibrary.core.nativemessage.MessageStructure
import com.sourcepoint.cmplibrary.creation.ConfigOption
import com.sourcepoint.cmplibrary.creation.config
import com.sourcepoint.cmplibrary.creation.delegate.spConsentLibLazy
import com.sourcepoint.cmplibrary.exception.CampaignType
import com.sourcepoint.cmplibrary.model.ConsentAction
import com.sourcepoint.cmplibrary.model.PMTab
import com.sourcepoint.cmplibrary.model.exposed.ActionType
import com.sourcepoint.cmplibrary.model.exposed.SPConsents
import com.sourcepoint.cmplibrary.model.exposed.SpConfig
import com.sourcepoint.cmplibrary.util.clearAllData
import io.voodoo.apps.privacy.config.CmpPurpose
import io.voodoo.apps.privacy.config.CmpPurposeHelper
import io.voodoo.apps.privacy.config.CmpVendorHelper
import io.voodoo.apps.privacy.config.SourcepointConfiguration
import io.voodoo.apps.privacy.model.VoodooPrivacyConsent
import org.json.JSONObject

/**
 *
 */
class VoodooPrivacyManager(
    lifecycleOwner: LifecycleOwner,
    private val currentActivity: Activity,
    private var autoShowPopup: Boolean = true,
    private val sourcepointConfiguration: SourcepointConfiguration,
    private var onConsentReceived: ((VoodooPrivacyConsent) -> Unit)? = null,
    private var onUiReady: (() -> Unit)? = null,
    private var onError: ((Throwable) -> Unit)? = null,
    private var onStatusUpdate: ((ConsentStatus) -> Unit)? = null
) : DefaultLifecycleObserver {

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    private var doNotSellDataEnabled = false
    private var forceAutoShow = false
    private var consentStatus: ConsentStatus = ConsentStatus.NA
    private var receivedConsent: SPConsents? = null
    private val cmpConfig: SpConfig = config {
        accountId = sourcepointConfiguration.accountId
        propertyId = sourcepointConfiguration.propertyId
        propertyName = sourcepointConfiguration.propertyName
        messLanguage = VoodooPrivacyLanguageMapper.getLanguage()
        +CampaignType.GDPR
        +CampaignType.USNAT to setOf(ConfigOption.TRANSITION_CCPA_AUTH)
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

    private fun loadMessage() {
        setConsentStatus(ConsentStatus.LOADING)
        spConsentLib.loadMessage()
    }

    /**
     * Show consent edit settings
     */
    fun changePrivacyConsent() {
        if (!isFirstMessageLoaded()) {
            Log.w("PrivacyManager", "Privacy -- first message is not loaded yet")
            return
        }

        if (isPrivacyApplies()) {
            forceAutoShow = true
            setConsentStatus(ConsentStatus.LOADING)
            if (isUsNatApplicable()) {
                spConsentLib.loadPrivacyManager(
                    sourcepointConfiguration.usMspsPrivacyManagerId,
                    PMTab.PURPOSES,
                    CampaignType.USNAT
                )
            } else {
                spConsentLib.loadPrivacyManager(
                    sourcepointConfiguration.gdprPrivacyManagerId,
                    PMTab.PURPOSES,
                    CampaignType.GDPR,
                )
            }
        } else {
            Log.w("PrivacyManager", "Privacy -- not available in your country")
        }

    }

    /**
     * Close consent edit setting
     *
     * @return true if it was closed
     */
    fun closeIfVisible(): Boolean {
        return viewToShow?.let {
            spConsentLib.removeView(it)
            setConsentStatus(ConsentStatus.NA)
            viewToShow = null
            true
        } ?: false
    }

    /**
     * Initialize the consent manager and download the FTL message
     */
    fun initializeConsent() {
        loadMessage()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        onConsentReceived = null
        onStatusUpdate = null
        onError = null
        onUiReady = null
        spConsentLib.dispose()
        super.onDestroy(owner)
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
     *
     */
    fun setOnStatusUpdate(onStatusUpdate: (ConsentStatus) -> Unit) {
        this.onStatusUpdate = onStatusUpdate
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
            doNotSellDataEnabled = doNotSellDataEnabled,
            gdprApplicable = isGdprApplicable()
        )
    }

    fun isFirstMessageLoaded(): Boolean {
        return receivedConsent != null
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

    private fun processConsent(consent: SPConsents) {
        //The SDK will set consentedToAll as True if user allow us to sell / share their data
        //It will return false if user tick the Do Not Sell / Share my data
        if (consent.usNat?.consent?.statuses?.consentedToAll == false) {
            doNotSellDataEnabled = true
        }

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
        consentStatus = if (status == ConsentStatus.RECEIVED && !isPrivacyApplies()) {
            ConsentStatus.NON_APPLICABLE
        } else
            status
        onStatusUpdate?.invoke(consentStatus)
    }

    fun clearConsent() {
        clearAllData(context = currentActivity)
        receivedConsent = null
        setConsentStatus(ConsentStatus.NA)
    }

    internal inner class LocalClient : SpClient {
        override fun onUIFinished(view: View) {
            viewToShow = null
            spConsentLib.removeView(view)
        }

        override fun onUIReady(view: View) {
            viewToShow = view
            setConsentStatus(ConsentStatus.UI_READY)
            if (autoShowPopup || forceAutoShow) {
                setConsentStatus(ConsentStatus.UI_SHOWN)
                spConsentLib.showView(view)
                forceAutoShow = false
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

        override fun onAction(view: View, consentAction: ConsentAction): ConsentAction {
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


