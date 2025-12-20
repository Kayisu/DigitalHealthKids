package com.example.digitalhealthkids.data.usage

import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.core.network.usage.toDomain
import com.example.digitalhealthkids.domain.usage.*

class UsageRepositoryRemoteImpl(
    private val api: UsageApi
) : UsageRepository {

    override suspend fun getDashboard(userId: String): DashboardData {
        val dto = api.getDashboard(userId)
        return dto.toDomain()
    }

    override suspend fun getAppDetail(userId: String, packageName: String, targetDate: String): AppDetail {
        val dto = api.getAppDetail(userId, packageName, targetDate)
        return dto.toDomain()
    }
}