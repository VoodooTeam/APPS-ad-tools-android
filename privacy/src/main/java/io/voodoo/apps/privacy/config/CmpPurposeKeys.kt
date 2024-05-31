package io.voodoo.app.privacy.config

/**
 * Maintain the configuration of the purpose in this classes
 */
enum class CmpPurpose(private val key: String): BaseCmpEnum {
    USE_LIMITED_DATA_TO_SELECT_ADVERTISING("6656fcd5a0fa9305065e562c"),
    STORE_AND_ACCESS_INFO_ON_DEVICE("6656fcd5a0fa9305065e56a3"),
    CREATE_PROFILE_FOR_ADVERTISING("6656fcd5a0fa9305065e55e7"),
    USE_PROFILE_FOR_ADVERTISING("6656fcd5a0fa9305065e557e"),
    MEASURE_ADVERTISING_PERFORMANCE("6656fcd5a0fa9305065e5415"),
    MEASURE_CONTENT_PERFORMANCE("6656fcd5a0fa9305065e5490"),
    GATHER_AUDIENCE_STATISTICS("6656fcd5a0fa9305065e54a9"),
    DEVELOP_AND_IMPROVE_SERVICE("6656fcd5a0fa9305065e54f3"),
    //    CREATE_PROFILE_FOR_PERSONALISED_CONTENT("6656fcd5a0fa9305065e5574"), //Not used in any vendor
//    USE_PROFILE_FOR_PERSONALISED_CONTENT("6656fcd5a0fa9305065e556a"), //Not used in any vendor
    USE_LIMITED_DATA_TO_SELECT_CONTENT("6656fcd5a0fa9305065e5561");

    override fun getKey(): String {
        return key
    }
}

object CmpPurposeHelper: BaseCmpHelper<CmpPurpose>()