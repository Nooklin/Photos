package org.nooklin.photos.data.local

enum class AlbumSort { NAME, SIZE, MANUAL }

// One set for the library and one for the Hidden Section. Folders only use NAME and SIZE.
data class AlbumOrder(
    val sort: AlbumSort = AlbumSort.NAME,
    val folderSort: AlbumSort = AlbumSort.NAME,
    val manual: List<Long> = emptyList(),
    val homeSort: AlbumSort = AlbumSort.SIZE,
    val homeRows: Int = 1
)

// Manual: albums that are not in the saved order yet are new, they go first (newest id first)
fun <T> List<T>.sortedAlbums(
    mode: AlbumSort,
    manual: List<Long>,
    id: (T) -> Long,
    name: (T) -> String,
    count: (T) -> Int
): List<T> = when (mode) {
    AlbumSort.NAME -> sortedWith(
        compareBy<T, String>(String.CASE_INSENSITIVE_ORDER) { name(it) }.thenBy { id(it) }
    )
    AlbumSort.SIZE -> sortedWith(
        compareByDescending<T> { count(it) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { name(it) }
            .thenBy { id(it) }
    )
    AlbumSort.MANUAL -> {
        val position = HashMap<Long, Int>()
        manual.forEachIndexed { index, albumId -> position[albumId] = index }
        val (known, fresh) = partition { id(it) in position }
        fresh.sortedByDescending { id(it) } + known.sortedBy { position.getValue(id(it)) }
    }
}
