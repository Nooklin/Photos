package org.nooklin.photos.ui.common

import org.nooklin.photos.R

// all selected are favorites: the action removes them, otherwise it adds the ones that are not yet
fun <T> List<T>.favoriteLabelRes(isFavorite: (T) -> Boolean): Int =
    if (isNotEmpty() && all(isFavorite)) R.string.common_remove_favorite else R.string.common_favorite

fun <T> List<T>.favoriteTargets(isFavorite: (T) -> Boolean): List<T> {
    val allFavorite = isNotEmpty() && all(isFavorite)
    return filter { isFavorite(it) == allFavorite }
}
