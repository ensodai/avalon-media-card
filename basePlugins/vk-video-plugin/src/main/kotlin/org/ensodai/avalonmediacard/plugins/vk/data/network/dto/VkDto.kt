package org.ensodai.avalonmediacard.plugins.vk.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VkMethodAnonymTokenResponse(
    val response: VkMethodAnonymTokenDataDto? = null,
    val error: VkErrorDto? = null
)

@Serializable
data class VkMethodAnonymTokenDataDto(
    val token: String? = null,
    @SerialName("expired_at") val expiredAt: Long? = null
)

@Serializable
data class VkAnonymTokenResponse(
    val data: VkAnonymTokenDataDto? = null,
    val error: VkErrorDto? = null
)

@Serializable
data class VkAnonymTokenDataDto(
    @SerialName("access_token") val accessToken: String? = null,
    val expires: Long? = null,
    @SerialName("expired_at") val expiredAt: Long? = null
)

@Serializable
data class VkRootResponseDto(
    val response: VkCatalogResponseDto? = null,
    val error: VkErrorDto? = null
)

@Serializable
data class VkErrorDto(
    @SerialName("error_code") val errorCode: Int? = null,
    @SerialName("error_msg") val errorMsg: String? = null
)

@Serializable
data class VkCatalogResponseDto(
    @SerialName("catalog_videos") val catalogVideos: List<VkCatalogVideoDto> = emptyList(),
    val albums: List<VkAlbumDto> = emptyList(),
    val videos: List<VkVideoDto> = emptyList()
)

@Serializable
data class VkAlbumDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val title: String,
    val count: Int = 0,
    val image: List<VkImageDto>? = null,
    @SerialName("updated_time") val updatedTime: Long? = null
)

@Serializable
data class VkVideoGetResponseDto(
    val response: VkVideoGetPayloadDto? = null,
    val error: VkErrorDto? = null
)

@Serializable
data class VkVideoGetPayloadDto(
    val count: Int = 0,
    val items: List<VkVideoDto> = emptyList()
)

@Serializable
data class VkCatalogVideoDto(
    val video: VkVideoDto? = null
)

@Serializable
data class VkVideoDto(
    val id: Long,
    @SerialName("owner_id") val ownerId: Long,
    val title: String,
    val description: String? = null,
    val duration: Long = 0L,
    val files: VkVideoFilesDto? = null,
    val subtitles: List<VkSubtitleDto>? = null,
    val views: Long? = null,
    val image: List<VkImageDto>? = null,
    @SerialName("owner_name") val ownerName: String? = null
)

@Serializable
data class VkVideoFilesDto(
    val mp4_2160: String? = null,
    val mp4_1440: String? = null,
    val mp4_1080: String? = null,
    val mp4_720: String? = null,
    val mp4_480: String? = null,
    val mp4_360: String? = null,
    val mp4_240: String? = null,
    val mp4_144: String? = null,
    val hls: String? = null,
    @SerialName("hls_fmp4") val hlsFmp4: String? = null,
    @SerialName("hls_streams") val hlsStreams: String? = null,
    @SerialName("dash_streams") val dashStreams: String? = null,
    @SerialName("dash_sep") val dashSep: String? = null,
    @SerialName("failover_host") val failoverHost: String? = null
)

@Serializable
data class VkSubtitleDto(
    val lang: String? = null,
    val title: String? = null,
    @SerialName("is_auto") val isAuto: Boolean = false,
    val url: String? = null,
    @SerialName("manifest_name") val manifestName: String? = null
)

@Serializable
data class VkImageDto(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)
