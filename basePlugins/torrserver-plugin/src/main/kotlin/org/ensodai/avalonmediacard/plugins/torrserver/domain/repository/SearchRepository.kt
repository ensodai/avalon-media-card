package org.ensodai.avalonmediacard.plugins.torrserver.domain.repository

import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettResult

interface SearchRepository {
    suspend fun searchProwlarr(url: String, apiKey: String, query: String): List<JackettResult>
    suspend fun searchJackett(url: String, apiKey: String, query: String): List<JackettResult>
}
