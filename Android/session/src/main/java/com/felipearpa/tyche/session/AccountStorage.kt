package com.felipearpa.tyche.session

interface AccountStorage {
    suspend fun store(snapshot: CurrentAccountSnapshot)
    suspend fun delete()
    suspend fun retrieve(): CurrentAccountSnapshot?
}
