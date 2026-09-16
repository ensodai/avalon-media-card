package org.ensodai.avalonmediacard.presentation.screens.mediaSources.action

import org.ensodai.avalonmediacard.presentation.core.mvi.BaseActions
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.MediaSourceUiItem
import org.ensodai.avalonmediacard.presentation.screens.mediaSources.model.SourceSubFilter

data class MediaSourcesActions(
    val onSelectTab: (Int) -> Unit,
    val onSelectSubFilter: (SourceSubFilter?) -> Unit,
    val onItemClick: (MediaSourceUiItem) -> Unit,
    val onToggleAddTorrent: () -> Unit,
    val onPickTorrentFile: () -> Unit,
    val onTorrentFileSelected: (fileName: String, bytes: ByteArray) -> Unit,
    val onRefresh: () -> Unit,
    val onClose: () -> Unit
) : BaseActions()
