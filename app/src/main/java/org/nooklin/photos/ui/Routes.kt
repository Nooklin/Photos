package org.nooklin.photos.ui

internal const val ROUTE_HOME = "home"
internal const val ROUTE_HOME_REORDER = "home/reorder"
internal const val ROUTE_GALLERY = "gallery"
internal const val ROUTE_ALBUMS = "albums"
internal const val ROUTE_RECYCLE_BIN = "recyclebin"
internal const val ROUTE_HIDDEN = "hidden"
internal const val ROUTE_HIDDEN_LIBRARY = "hiddenlibrary"
internal const val ROUTE_HIDDEN_CUSTOMIZE = "hiddencustomize"
internal const val ROUTE_HIDDEN_ALBUMS = "hiddenalbums"
internal const val ROUTE_HIDDEN_FAVORITES = "hiddenfavorites"
internal const val ROUTE_HIDDEN_RECYCLE_BIN = "hiddenrecyclebin"
internal const val ROUTE_HIDDEN_VIDEOS = "hiddenvideos"
internal const val ROUTE_HIDDEN_SLOW_MOTION = "hiddenslowmotion"
internal const val ROUTE_HIDDEN_RAW = "hiddenraw"
internal const val ROUTE_HIDDEN_SCREENSHOTS = "hiddenscreenshots"
internal const val ROUTE_HIDDEN_PANORAMAS = "hiddenpanoramas"
internal const val ROUTE_HIDDEN_SCREEN_RECORDINGS = "hiddenscreenrecordings"
internal const val ROUTE_HIDDEN_RECENTLY_SAVED = "hiddenrecentlysaved"
internal const val ROUTE_HIDDEN_CALENDAR = "hiddencalendar"
internal const val ROUTE_HIDDEN_SEARCH = "hiddensearch"
internal const val ROUTE_HIDDEN_DUPLICATES = "hiddenduplicates"
internal const val ROUTE_SETTINGS = "settings"
internal const val ROUTE_SEARCH = "search"
internal const val ROUTE_CALENDAR = "calendar"
internal const val ROUTE_ON_THIS_DAY = "onthisday"
internal const val ROUTE_HIDDEN_ON_THIS_DAY = "hiddenonthisday"
internal const val ROUTE_DUPLICATES = "duplicates"
internal const val ROUTE_DISCOVERED = "discovered"
internal const val ROUTE_PEOPLE = "people"
internal const val ROUTE_PETS = "pets"

internal val ZOOM_VIEWER_ROUTES = setOf(
    "viewer/{index}", "hiddenviewer/{index}", "hiddenlibraryviewer/{index}",
    "hiddenfavoritesviewer/{index}", "hiddenvideosviewer/{index}", "hiddenslowmotionviewer/{index}",
    "hiddenrawviewer/{index}", "hiddenscreenshotsviewer/{index}", "hiddenrecentlysavedviewer/{index}",
    "hiddenpanoramasviewer/{index}", "hiddenscreenrecordingsviewer/{index}",
    "recyclebinviewer/{index}", "hiddenrecyclebinviewer/{index}"
)
internal val ZOOM_GRID_ROUTES = setOf(
    ROUTE_HOME, ROUTE_GALLERY, "album/{id}", "category/{name}", ROUTE_RECYCLE_BIN,
    ROUTE_HIDDEN, ROUTE_HIDDEN_LIBRARY, ROUTE_HIDDEN_FAVORITES, ROUTE_HIDDEN_VIDEOS,
    ROUTE_HIDDEN_SLOW_MOTION, ROUTE_HIDDEN_RAW, ROUTE_HIDDEN_SCREENSHOTS,
    ROUTE_HIDDEN_PANORAMAS, ROUTE_HIDDEN_SCREEN_RECORDINGS,
    ROUTE_HIDDEN_RECENTLY_SAVED, ROUTE_HIDDEN_RECYCLE_BIN, ROUTE_HIDDEN_SEARCH, "hiddenalbums/{albumId}",
    "discovered/{tagId}", "pets/{petId}", "people/{personId}", ROUTE_SEARCH, ROUTE_CALENDAR, ROUTE_ON_THIS_DAY, ROUTE_HIDDEN_ON_THIS_DAY
)

internal fun isZoomPair(a: String?, b: String?): Boolean =
    (a in ZOOM_VIEWER_ROUTES && b in ZOOM_GRID_ROUTES) || (b in ZOOM_VIEWER_ROUTES && a in ZOOM_GRID_ROUTES)
