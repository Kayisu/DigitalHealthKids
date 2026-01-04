package com.example.digitalhealthkids.core.di

import android.content.Context
import com.example.digitalhealthkids.core.network.AuthApi
import com.example.digitalhealthkids.core.network.BaseUrlStore
import com.example.digitalhealthkids.core.network.NetworkConstants
import com.example.digitalhealthkids.core.network.ai.AiApi
import com.example.digitalhealthkids.core.network.usage.UsageApi
import com.example.digitalhealthkids.data.auth.AuthRepositoryImplementation
import com.example.digitalhealthkids.data.ai.AiRepositoryRemoteImpl
import com.example.digitalhealthkids.data.usage.UsageRepositoryRemoteImpl
import com.example.digitalhealthkids.domain.auth.AuthRepository
import com.example.digitalhealthkids.domain.ai.AiRepository
import com.example.digitalhealthkids.data.auth.AuthRepositoryRemoteImpl
import com.example.digitalhealthkids.domain.usage.UsageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit
import com.example.digitalhealthkids.core.network.policy.PolicyApi
import com.example.digitalhealthkids.data.local.PolicyManager
import com.example.digitalhealthkids.data.policy.PolicyRepositoryImpl
import com.example.digitalhealthkids.domain.policy.PolicyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.digitalhealthkids.core.network.FailoverInterceptor

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(baseUrlStore: BaseUrlStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(FailoverInterceptor(baseUrlStore))
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providePolicyApi(retrofit: Retrofit): PolicyApi =
        retrofit.create(PolicyApi::class.java)

    @Provides
    @Singleton
    fun providePolicyManager(@ApplicationContext context: Context): PolicyManager =
        PolicyManager(context)

    @Provides
    @Singleton
    fun providePolicyRepository(
        api: PolicyApi,
        manager: PolicyManager
    ): PolicyRepository = PolicyRepositoryImpl(api, manager)

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConstants.LOCAL_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthRepository(api: AuthApi): AuthRepository =
        AuthRepositoryImplementation(api)

    @Provides
    @Singleton
    fun provideUsageApi(retrofit: Retrofit): UsageApi =
        retrofit.create(UsageApi::class.java)

    @Provides
    @Singleton
    fun provideAiApi(retrofit: Retrofit): AiApi =
        retrofit.create(AiApi::class.java)

    @Provides
    @Singleton
    fun provideUsageRepository(api: UsageApi): UsageRepository =
        UsageRepositoryRemoteImpl(api)

    @Provides
    @Singleton
    fun provideAiRepository(api: AiApi): AiRepository =
        AiRepositoryRemoteImpl(api)
}
