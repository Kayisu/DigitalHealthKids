package com.example.digitalhealthkids.data.usage

import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.toDomain
import com.example.digitalhealthkids.domain.usage.DashboardData
import com.example.digitalhealthkids.domain.usage.UsageRepository

class UsageRepositoryRemoteImpl(
    private val api: UsageApi
) : UsageRepository {

    override suspend fun getDashboard(childId: String): DashboardData {
        val dto = api.getDashboard(childId)
        return dto.toDomain()
    }
}
