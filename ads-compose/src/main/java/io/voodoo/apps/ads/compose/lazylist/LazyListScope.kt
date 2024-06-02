package io.voodoo.apps.ads.compose.lazylist

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import io.voodoo.apps.ads.compose.model.AdHolder


/**
 * Adds a [count] of items with automatic ad management.
 *
 * - lazyListIndex is the index of an item in the LazyList layout (impacted by ads)
 * - itemIndex is the index of an item in the original list (not impacted by ads)
 *
 * @see [LazyListScope.items]
 */
inline fun LazyListScope.items(
    adMediator: LazyListAdMediator,
    count: Int,
    noinline adKey: ((lazyListIndex: Int) -> Any)? = null,
    noinline key: ((lazyListIndex: Int, itemIndex: Int) -> Any)? = null,
    noinline adContentType: ((lazyListIndex: Int) -> Any)? = null,
    noinline contentType: ((lazyListIndex: Int, itemIndex: Int) -> Any)? = null,
    crossinline adContent: @Composable LazyItemScope.(lazyListIndex: Int, ad: AdHolder) -> Unit,
    crossinline itemContent: @Composable LazyItemScope.(lazyListIndex: Int, itemIndex: Int) -> Unit
) {
    adMediator.itemCount = count
    items(
        count = adMediator.totalItemCount,
        // Don't provide key/contentType by default, because LazyListAdMediator.adIndices
        // can change often, we don't want to impact perf and recompute every key / contentType
        // everytime that happens if the dev using this doesn't want to specify key/contentType anyway
        key = if (key != null || adKey != null) {
            { index ->
                if (adMediator.hasAdAt(index)) {
                    adKey?.invoke(index)
                } else {
                    key?.invoke(index, adMediator.getItemIndex(index))
                } ?: index
            }
        } else {
            null
        },
        contentType = if (contentType != null || adContentType != null) {
            { index ->
                if (adMediator.hasAdAt(index)) {
                    adContentType?.invoke(index)
                } else {
                    contentType?.invoke(index, adMediator.getItemIndex(index))
                }
            }
        } else {
            { null }
        }
    ) { index ->
        val isAd by remember(index) { derivedStateOf { adMediator.hasAdAt(index) } }
        if (isAd) {
            val adHolder = remember(index) {
                adMediator.getAdAt(index)?.let { AdHolder(it) }
            }

            if (adHolder != null) {
                DisposableEffect(adHolder) {
                    val ad = adHolder.ad
                    onDispose { adMediator.releaseAd(ad) }
                }

                adContent(index, adHolder)
            }
        } else {
            itemContent(index, adMediator.getItemIndex(index))
        }
    }
}

/**
 * Adds a list of items with automatic ad management.
 *
 * @see [LazyListScope.items]
 */
inline fun <T> LazyListScope.items(
    adMediator: LazyListAdMediator,
    items: List<T>,
    noinline adKey: ((lazyListIndex: Int) -> Any)? = null,
    noinline key: ((item: T) -> Any)? = null,
    noinline adContentType: ((lazyListIndex: Int) -> Any)? = null,
    noinline contentType: ((item: T) -> Any)? = null,
    crossinline adContent: @Composable LazyItemScope.(lazyListIndex: Int, ad: AdHolder) -> Unit,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    items(
        adMediator = adMediator,
        count = items.size,
        adKey = adKey,
        key = if (key != null) {
            { _, itemIndex ->
                items.getOrNull(itemIndex)?.let { key(it) }
                    ?: "error_$itemIndex"
            }
        } else {
            null
        },
        adContentType = adContentType,
        contentType = if (contentType != null) {
            { _, itemIndex ->
                items.getOrNull(itemIndex)?.let { contentType(it) }
                    ?: "error_$itemIndex"
            }
        } else {
            null
        },
        adContent = adContent,
    ) { _, itemIndex ->
        val item = items.getOrNull(itemIndex)
        if (item != null) {
            itemContent(item)
        }
    }
}

/**
 * Adds a list of items with automatic ad management.
 *
 * - lazyListIndex is the index of an item in the LazyList layout (impacted by ads)
 * - itemIndex is the index of an item in the original list (not impacted by ads)
 *
 * @see [LazyListScope.items]
 */
inline fun <T> LazyListScope.itemsIndexed(
    adMediator: LazyListAdMediator,
    items: List<T>,
    noinline adKey: ((lazyListIndex: Int) -> Any)? = null,
    noinline key: ((lazyListIndex: Int, itemIndex: Int, item: T) -> Any)? = null,
    noinline adContentType: ((lazyListIndex: Int) -> Any)? = null,
    noinline contentType: ((lazyListIndex: Int, itemIndex: Int, item: T) -> Any)? = null,
    crossinline adContent: @Composable LazyItemScope.(lazyListIndex: Int, ad: AdHolder) -> Unit,
    crossinline itemContent: @Composable LazyItemScope.(lazyListIndex: Int, itemIndex: Int, item: T) -> Unit
) {
    items(
        adMediator = adMediator,
        count = items.size,
        adKey = adKey,
        key = if (key != null) {
            { lazyListIndex, itemIndex ->
                items.getOrNull(itemIndex)?.let { key(lazyListIndex, itemIndex, it) }
                    ?: "error_$itemIndex"
            }
        } else {
            null
        },
        adContentType = adContentType,
        contentType = if (contentType != null) {
            { lazyListIndex, itemIndex ->
                items.getOrNull(itemIndex)?.let { contentType(lazyListIndex, itemIndex, it) }
                    ?: "error_$itemIndex"
            }
        } else {
            null
        },
        adContent = adContent,
    ) { lazyListIndex, itemIndex ->
        val item = items.getOrNull(itemIndex)
        if (item != null) {
            itemContent(lazyListIndex, itemIndex, item)
        }
    }
}
