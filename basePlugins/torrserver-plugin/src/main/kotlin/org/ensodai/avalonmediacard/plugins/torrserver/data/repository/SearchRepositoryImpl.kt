package org.ensodai.avalonmediacard.plugins.torrserver.data.repository

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.SearchRepository
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettResult
import org.ensodai.avalonmediacard.plugins.torrserver.data.network.providers.JackettClient
import org.ensodai.avalonmediacard.plugins.torrserver.data.network.providers.ProwlarrClient

class SearchRepositoryImpl(private val context: PluginContext) : SearchRepository {
    override suspend fun searchProwlarr(url: String, apiKey: String, query: String): List<JackettResult> {
        return try {
            ProwlarrClient(context.httpClient, url, apiKey, context.logger).search(query)
        } catch (e: Exception) {
            context.logger.error("Ошибка при поиске через Prowlarr", e)
            emptyList()
        }
    }

    override suspend fun searchJackett(url: String, apiKey: String, query: String): List<JackettResult> {
        return try {
            JackettClient(context.httpClient, url, apiKey, context.logger).search(query)
        } catch (e: Exception) {
            context.logger.error("Ошибка при поиске через Jackett", e)
            emptyList()
        }
    }
}
