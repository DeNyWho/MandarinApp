package com.example.mandarin.repository

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.mandarin.pagingsource.PagingSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import kotlinx.coroutines.flow.Flow

class DatabaseRepository {

    fun getDataStream(query: Query): Flow<PagingData<DataSnapshot>> {
        return Pager(
            config = PagingConfig(
                pageSize = DATABASE_PAGE_SIZE,
                prefetchDistance = DATABASE_PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = InvalidatingPagingSourceFactory{ PagingSource(query) }
        ).flow
    }


    companion object {
        const val DATABASE_PAGE_SIZE = 30
        const val DATABASE_PREFETCH_DISTANCE = 15
    }
}