package org.nooklin.photos.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.nooklin.photos.R
import org.nooklin.photos.appString
import org.nooklin.photos.data.MediaItem
import org.nooklin.photos.data.local.FaceSummary
import org.nooklin.photos.data.local.LocalMetadataStore
import org.nooklin.photos.data.local.MlTag
import org.nooklin.photos.data.local.PersonEntity
import org.nooklin.photos.data.local.PetMembershipEntity
import org.nooklin.photos.data.local.PhotoTagEntity
import org.nooklin.photos.ui.albums.AlbumPhotoPickerScreen
import org.nooklin.photos.ui.albums.AlbumViewModel
import org.nooklin.photos.ui.albums.AlbumWithPreview
import org.nooklin.photos.ui.common.GroupActions
import org.nooklin.photos.ui.common.StableList
import org.nooklin.photos.ui.common.ZoomLaunch
import org.nooklin.photos.ui.discovered.DiscoveredAlbum
import org.nooklin.photos.ui.discovered.DiscoveredDetailScreen
import org.nooklin.photos.ui.discovered.DiscoveredScreen
import org.nooklin.photos.ui.discovered.DiscoveredViewModel
import org.nooklin.photos.ui.people.PeopleScreen
import org.nooklin.photos.ui.people.PeopleViewModel
import org.nooklin.photos.ui.people.PersonDetailScreen
import org.nooklin.photos.ui.people.PersonPickerEntry
import org.nooklin.photos.ui.people.PersonWithPreview
import org.nooklin.photos.ui.pets.PetDetailScreen
import org.nooklin.photos.ui.pets.PetViewModel
import org.nooklin.photos.ui.pets.PetWithPreview
import org.nooklin.photos.ui.pets.PetsScreen
import org.nooklin.photos.ui.recyclebin.RecycleBinScreen
import org.nooklin.photos.ui.recyclebin.RecycleBinViewerScreen

internal fun NavGraphBuilder.collectionRoutes(
    viewModel: GalleryViewModel,
    albumViewModel: AlbumViewModel,
    discoveredViewModel: DiscoveredViewModel,
    petViewModel: PetViewModel,
    peopleViewModel: PeopleViewModel,
    gridColumnsState: State<Int>,
    uiStateHolder: State<GalleryUiState>,
    recycleBinEntriesState: State<List<LocalMetadataStore.RecycleBinEntry>>,
    petMembershipsState: State<List<PetMembershipEntity>>,
    albumTargets: List<AlbumWithPreview>,
    groupActions: GroupActions,
    petsWithPreview: List<PetWithPreview>,
    analysisTagsState: State<List<PhotoTagEntity>>,
    discoveredAlbums: List<DiscoveredAlbum>,
    peopleState: State<List<PersonEntity>>,
    facesState: State<List<FaceSummary>>,
    peopleWithPreview: List<PersonWithPreview>,
    peopleByDisplayName: Map<Long, String>,
    navController: NavHostController,
    capturedZoomLaunchState: MutableState<ZoomLaunch?>,
    viewerItemsOverrideState: MutableState<StableList<MediaItem>?>,
    openViewerZoomed: (Any, String) -> Unit,
    navigateToDiscoveredAlbum: (String) -> Unit
) {
    val gridColumns by gridColumnsState
    val uiState by uiStateHolder
    val recycleBinEntries by recycleBinEntriesState
    val petMemberships by petMembershipsState
    val analysisTags by analysisTagsState
    val people by peopleState
    val faces by facesState
    val capturedZoomLaunch by capturedZoomLaunchState
    var viewerItemsOverride by viewerItemsOverrideState
    composable(ROUTE_DISCOVERED) {
        DiscoveredScreen(
            albums = discoveredAlbums,
            gridColumns = gridColumns,
            onAlbumClick = { tagId -> navigateToDiscoveredAlbum(tagId) },
            onDeleteCategories = { tagIds ->
                discoveredViewModel.clearCategories(tagIds)
                if (MlTag.PEOPLE.id in tagIds) {
                    peopleViewModel.clearAll()
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
    composable("discovered/{tagId}") { entry ->
        val tagId = entry.arguments?.getString("tagId") ?: ""
        val album = discoveredAlbums.find { it.tagId == tagId }
        val photos = remember(analysisTags, uiState.allItems, tagId) {
            val matchedIds = analysisTags.filter { it.tag == tagId }.map { it.mediaId }.toSet()
            uiState.allItems.filter { it.id in matchedIds }
        }
        DiscoveredDetailScreen(
            label = album?.label ?: MlTag.fromId(tagId)?.label ?: tagId,
            photos = photos,
            gridColumns = gridColumns,
            existingAlbums = albumTargets,
            isPetsCategory = tagId == MlTag.PETS.id,
            existingPets = petsWithPreview,
            onAddToPet = { petId, item -> petViewModel.addToPet(petId, item.id) },
            onCreatePetAndAdd = { name, items ->
                petViewModel.createPet(name) { petId ->
                    items.forEach { item -> petViewModel.addToPet(petId, item.id) }
                }
            },
            onOpenPets = { navController.navigate(ROUTE_PETS) },
            onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
            onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
            onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) },
            groupActions = groupActions,
            onBack = { navController.popBackStack() },
            onItemClick = { item, discoveredPhotos ->
                viewerItemsOverride = StableList(discoveredPhotos)
                openViewerZoomed(item.id, "viewer/${discoveredPhotos.indexOf(item)}")
            }
        )
    }
    composable(ROUTE_PETS) {
        PetsScreen(
            pets = petsWithPreview,
            gridColumns = gridColumns,
            onPetClick = { petId -> navController.navigate("pets/$petId") },
            onCreatePet = { name -> petViewModel.createPet(name) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("pets/{petId}") { entry ->
        val petId = entry.arguments?.getString("petId")?.toLongOrNull() ?: -1L
        val pet = petsWithPreview.find { it.id == petId }
        val memberIds = remember(petMemberships, petId) {
            petMemberships.filter { it.petId == petId }.map { it.mediaId }.toSet()
        }
        val petPhotos = remember(uiState.allItems, memberIds) {
            uiState.allItems.filter { it.id in memberIds }
        }
        PetDetailScreen(
            petName = pet?.name ?: appString(R.string.nooklin_photos_app_pet),
            photos = petPhotos,
            gridColumns = gridColumns,
            groupActions = groupActions,
            onBack = { navController.popBackStack() },
            onItemClick = { item, petPhotoList ->
                viewerItemsOverride = StableList(petPhotoList)
                openViewerZoomed(item.id, "viewer/${petPhotoList.indexOf(item)}")
            },
            onAddPhotos = { navController.navigate("pets/$petId/pick") },
            onRename = { newName -> petViewModel.renamePet(petId, newName) },
            onDeletePet = { petViewModel.deletePet(petId) }
        )
    }
    composable("pets/{petId}/pick") { entry ->
        val petId = entry.arguments?.getString("petId")?.toLongOrNull() ?: -1L
        val pet = petsWithPreview.find { it.id == petId }
        val memberIds = remember(petMemberships, petId) {
            petMemberships.filter { it.petId == petId }.map { it.mediaId }.toSet()
        }
        val petsTaggedItems = remember(analysisTags, uiState.allItems) {
            val taggedIds = analysisTags.filter { it.tag == MlTag.PETS.id }.map { it.mediaId }.toSet()
            uiState.allItems.filter { it.id in taggedIds }
        }
        AlbumPhotoPickerScreen(
            albumName = pet?.name ?: appString(R.string.nooklin_photos_app_pet),
            items = petsTaggedItems,
            alreadyInAlbum = memberIds,
            gridColumns = gridColumns,
            onConfirm = { selectedIds ->
                selectedIds.forEach { mediaId -> petViewModel.addToPet(petId, mediaId) }
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }
    composable(ROUTE_PEOPLE) {
        PeopleScreen(
            people = peopleWithPreview,
            gridColumns = gridColumns,
            onPersonClick = { personId -> navController.navigate("people/$personId") },
            onAddName = { personId, name -> peopleViewModel.rename(personId, name) },
            onPreviewCombine = { threshold -> peopleViewModel.previewCombine(threshold) },
            onCombine = { threshold, onDone -> peopleViewModel.combineSimilar(threshold, onDone) },
            onBack = { navController.popBackStack() }
        )
    }
    composable("people/{personId}") { entry ->
        val personId = entry.arguments?.getString("personId")?.toLongOrNull() ?: -1L

        LaunchedEffect(people, personId) {
            if (people.isNotEmpty() && people.none { it.id == personId }) {
                navController.popBackStack()
            }
        }

        val displayName = peopleByDisplayName[personId] ?: appString(R.string.nooklin_photos_app_unknown_2)
        val personFaces = remember(faces, personId) { faces.filter { it.personId == personId } }
        val facesByMediaId = remember(personFaces) {
            personFaces.groupBy({ it.mediaId }, { it.id })
        }
        val photos = remember(personFaces, uiState.allItems) {
            val mediaIds = personFaces.map { it.mediaId }.distinct().toSet()
            uiState.allItems.filter { it.id in mediaIds }
        }
        val otherPeople = remember(peopleWithPreview, personId) {
            peopleWithPreview.filter { it.id != personId }
                .map { PersonPickerEntry(it.id, it.displayName) }
        }
        PersonDetailScreen(
            displayName = displayName,
            isNamed = people.find { it.id == personId }?.name != null,
            photos = photos,
            facesByMediaId = facesByMediaId,
            gridColumns = gridColumns,
            existingAlbums = albumTargets,
            otherPeople = otherPeople,
            onRename = { newName -> peopleViewModel.rename(personId, newName) },
            onMergeWith = { otherId -> peopleViewModel.merge(personId, otherId) },
            onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
            onMoveToRecycleBin = { item -> viewModel.moveToRecycleBin(item) },
            onAddToAlbum = { albumId, item -> albumViewModel.addToAlbum(albumId, item.id) },
            onSplitOutFaces = { faceIds -> peopleViewModel.splitOutFaces(faceIds) },
            groupActions = groupActions,
            onBack = { navController.popBackStack() },
            onItemClick = { item, personPhotos ->
                viewerItemsOverride = StableList(personPhotos)
                openViewerZoomed(item.id, "viewer/${personPhotos.indexOf(item)}")
            }
        )
    }
    composable(ROUTE_RECYCLE_BIN) {
        val normalRecycleBinEntries = recycleBinEntries
        RecycleBinScreen(
            entries = normalRecycleBinEntries,
            gridColumns = gridColumns,
            onRestore = viewModel::restoreFromRecycleBin,
            onPurgeExpired = viewModel::purgeExpiredFromRecycleBin,
            onItemClick = { index -> openViewerZoomed(normalRecycleBinEntries[index].mediaId, "recyclebinviewer/$index") },
            onBack = { navController.popBackStack() }
        )
    }
    composable("recyclebinviewer/{index}") { entry ->
        val index = entry.arguments?.getString("index")?.toIntOrNull() ?: 0
        val normalRecycleBinEntries = recycleBinEntries
        RecycleBinViewerScreen(
            entries = StableList(normalRecycleBinEntries),
            zoomLaunch = capturedZoomLaunch,
            startIndex = index,
            onBack = { navController.popBackStack() },
            onRestore = viewModel::restoreFromRecycleBin
        )
    }
}
