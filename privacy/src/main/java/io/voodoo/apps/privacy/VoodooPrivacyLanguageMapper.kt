package io.voodoo.apps.privacy

import com.sourcepoint.cmplibrary.model.MessageLanguage
import java.util.Locale

object VoodooPrivacyLanguageMapper {
    fun getLanguage(): MessageLanguage {
        val result: MessageLanguage = when (Locale.getDefault().language.lowercase()) {
            "en" -> MessageLanguage.ENGLISH
            "fr" -> MessageLanguage.FRENCH
            "de" -> MessageLanguage.GERMAN
            "it" -> MessageLanguage.ITALIAN
            "nl" -> MessageLanguage.DUTCH
            "pt" -> MessageLanguage.PORTUGUESE
            "sv" -> MessageLanguage.SWEDISH
            else -> MessageLanguage.ENGLISH
        }
        return result
    }
}
